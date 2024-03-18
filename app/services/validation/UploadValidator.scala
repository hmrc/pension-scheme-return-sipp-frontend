/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.validation

import cats.effect.IO
import cats.effect.kernel.Resource
import config.Crypto
import fs2.RaiseThrowable.fromApplicativeError
import fs2._
import fs2.data.csv.ParseableHeader.StringParseableHeader
import fs2.data.csv.{lowlevel, CsvRow}
import fs2.interop.reactivestreams._
import models._
import models.csv.{CsvDocumentEmpty, CsvDocumentState, CsvRowState}
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.Format
import repositories.{CsvRowStateSerialization, UploadMetadataRepository, UploadRepository}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import UploadValidator._

import java.nio.ByteBuffer
import javax.inject.Inject

class UploadValidator @Inject()(
  uploadRepository: UploadRepository,
  uploadMetadataRepository: UploadMetadataRepository,
  crypto: Crypto
) extends Validator {

  val logger: Logger = Logger(classOf[UploadValidator])
  private implicit val cryptoEncDec: Encrypter with Decrypter = crypto.getCrypto

  def validateUpload[T](
    stream: fs2.Stream[IO, String],
    csvRowValidator: CsvRowValidator[T],
    uploadKey: UploadKey
  )(implicit messages: Messages, format: Format[T]): IO[Unit] =
    stream
      .through(lowlevel.rows[IO, String]())
      .through(lowlevel.headers[IO, String])
      .tail
      .mapAsync(FieldValidationParallelism)(validate[T](_, csvRowValidator))
      .zipWithScan1[CsvDocumentState](CsvDocumentEmpty)(CsvDocumentState.combine)
      //.takeWhile(_._2.count <= 25) TODO make configurable and decide whether to enable this limit
      .through(csvRowStatePipe[T](uploadKey))
      .map(_._2)
      .compile
      .last
      .map(_.getOrElse(CsvDocumentEmpty))
      .flatMap(persistCsvDocumentState(uploadKey, _))

  private def validate[T](csvRow: CsvRow[String], csvRowValidator: CsvRowValidator[T])(
    implicit messages: Messages
  ): IO[CsvRowState[T]] = {
    val headerKeys: List[CsvHeaderKey] = csvRow.headers
      .map(_.toList)
      .toList
      .flatten
      .zipWithIndex
      .map { case (key, index) => CsvHeaderKey(key.trim, indexToCsvKey(index), index) }
    val line = csvRow.line.get.intValue

    IO(csvRowValidator.validate(line, csvRow.values, headerKeys)(messages))
  }

  private def csvRowStatePipe[T](
    uploadKey: UploadKey
  )(implicit format: Format[T]): Pipe[IO, (CsvRowState[T], CsvDocumentState), (CsvRowState[T], CsvDocumentState)] = {
    stream =>
      val publisher: Resource[IO, StreamUnicastPublisher[IO, ByteBuffer]] = stream
        .map(_._1)
        .map(CsvRowStateSerialization.write[T])
        .toUnicastPublisher

      fs2.Stream
        .resource(publisher)
        .evalMap(publisher => IO.fromFuture(IO(uploadRepository.delete(uploadKey))).map(_ => publisher))
        .flatMap(uploadRepository.publish(uploadKey, _).toStreamBuffered[IO](1))
        .flatMap(_ => stream)
  }

  private def persistCsvDocumentState(uploadKey: UploadKey, csvDocumentState: CsvDocumentState): IO[Unit] =
    IO.fromFuture(IO(uploadMetadataRepository.setValidationState(uploadKey, UploadValidated(csvDocumentState))))
}

object UploadValidator {
  val FieldValidationParallelism: Int = 8
}
