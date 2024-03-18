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
import repositories.{CsvRowStateSerialization, UploadRepository}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.nio.ByteBuffer
import javax.inject.Inject

class UploadValidatorFs2 @Inject()(
  uploadRepository: UploadRepository,
  crypto: Crypto
) extends Validator {

  val logger: Logger = Logger(classOf[UploadValidatorFs2])
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
      .map(validate[T](_, csvRowValidator))
      .zipWithScan1[CsvDocumentState](CsvDocumentEmpty)(CsvDocumentState.combine)
      //.takeWhile(_._2.count <= 25) TODO make configurable and decide whether to enable this limit
      .broadcastThrough(csvRowStatePipe[T](uploadKey), csvDocumentStatePipe(uploadKey))
      .compile
      .drain

  private def validate[T](csvRow: CsvRow[String], csvRowValidator: CsvRowValidator[T])(
    implicit messages: Messages
  ): CsvRowState[T] = {
    val headerKeys: List[CsvHeaderKey] = csvRow.headers
      .map(_.toList)
      .toList
      .flatten
      .zipWithIndex
      .map { case (key, index) => CsvHeaderKey(key, indexToCsvKey(index), index) }
    val line = csvRow.line.get.intValue

    csvRowValidator.validate(line, csvRow.values, headerKeys)(messages)
  }

  private def csvRowStatePipe[T](
    uploadKey: UploadKey
  )(implicit format: Format[T]): Pipe[IO, (CsvRowState[T], CsvDocumentState), Unit] = { stream =>
    val publisher: Resource[IO, StreamUnicastPublisher[IO, ByteBuffer]] = stream
      .map(_._1)
      .map(CsvRowStateSerialization.write[T])
      .toUnicastPublisher

    fs2.Stream
      .resource(publisher)
      .mapAsync(1)(publisher => IO.fromFuture(IO(uploadRepository.delete(uploadKey))).map(_ => publisher))
      .flatMap(uploadRepository.publish(uploadKey, _).toStreamBuffered[IO](1))
      .map(_ => ())
  }

  private def csvDocumentStatePipe[T](uploadKey: UploadKey): Pipe[IO, (CsvRowState[T], CsvDocumentState), Unit] =
    _.map(_._2)
      .last
      .map(_.getOrElse(CsvDocumentEmpty))
      .mapAsync(1)(_ => IO.unit) //TODO persist the aggregate document state
}
