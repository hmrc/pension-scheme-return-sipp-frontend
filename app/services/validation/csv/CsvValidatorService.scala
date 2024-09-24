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

package services.validation.csv

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import config.Crypto
import fs2._
import fs2.interop.reactivestreams._
import models._
import models.csv.{CsvDocumentEmpty, CsvDocumentInvalid, CsvDocumentState, CsvRowState}
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.Format
import repositories.{CsvRowStateSerialization, UploadRepository}
import services.validation.Validator
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.nio.ByteBuffer
import javax.inject.Inject

class CsvValidatorService @Inject()(
  uploadRepository: UploadRepository,
  csvDocumentValidator: CsvDocumentValidator,
  crypto: Crypto
) extends Validator {

  val logger: Logger = Logger(classOf[CsvValidatorService])
  private implicit val cryptoEncDec: Encrypter with Decrypter = crypto.getCrypto

  def validateUpload[T](
    stream: fs2.Stream[IO, String],
    csvRowValidator: CsvRowValidator[T],
    csvRowValidationParameters: CsvRowValidationParameters,
    uploadKey: UploadKey
  )(implicit messages: Messages, format: Format[T]): IO[CsvDocumentState] =
    csvDocumentValidator
      .validate(stream, csvRowValidator, csvRowValidationParameters)
      .attempt
      .map {
        case Left(error) =>
          (
            None,
            CsvDocumentInvalid(
              1,
              NonEmptyList.of(ValidationError(0, ValidationErrorType.InvalidRowFormat, error.getMessage))
            )
          )
        case Right(value) =>
          (Some(value._1), value._2)
      }
      .broadcastThrough(csvRowStatePipe[T](uploadKey), csvDocumentStatePipe)
      .reduceSemigroup
      .compile
      .last
      .map(_.getOrElse(CsvDocumentEmpty))

  private def csvRowStatePipe[T](
    uploadKey: UploadKey
  )(implicit format: Format[T]): Pipe[IO, (Option[CsvRowState[T]], CsvDocumentState), CsvDocumentState] = { stream =>
    val publisher: Resource[IO, StreamUnicastPublisher[IO, ByteBuffer]] = stream
      .map(_._1)
      .filter(_.isDefined)
      .map(_.get)
      .map(CsvRowStateSerialization.write[T])
      .toUnicastPublisher

    fs2.Stream
      .resource(publisher)
      .evalMap(publisher => IO.fromFuture(IO(uploadRepository.delete(uploadKey))).as(publisher))
      .flatMap(uploadRepository.publish(uploadKey, _).toStreamBuffered[IO](1))
      .as(CsvDocumentEmpty)
  }

  private def csvDocumentStatePipe[T]: Pipe[IO, (Option[CsvRowState[T]], CsvDocumentState), CsvDocumentState] =
    _.map(_._2).last
      .map(_.getOrElse(CsvDocumentEmpty))
}
