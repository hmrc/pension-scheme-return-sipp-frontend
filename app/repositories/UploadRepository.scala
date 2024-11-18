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

package repositories

import config.Crypto
import models.*
import models.csv.CsvRowState
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.reactivestreams.Publisher
import play.api.libs.json.*
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client
import uk.gov.hmrc.objectstore.client.play.*
import uk.gov.hmrc.objectstore.client.play.Implicits.*
import uk.gov.hmrc.objectstore.client.{ObjectSummaryWithMd5, Path}

import java.nio.ByteBuffer
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadRepository @Inject() (
  crypto: Crypto,
  objectStoreClient: PlayObjectStoreClient
)(implicit
  ec: ExecutionContext,
  ac: ActorSystem
) {
  implicit val cryptoEncDec: Encrypter & Decrypter = crypto.getCrypto
  private val directory: Path.Directory = Path.Directory("uploads")

  def save(key: UploadKey, bytes: Publisher[ByteBuffer])(implicit
    hc: HeaderCarrier
  ): Future[ObjectSummaryWithMd5] =
    objectStoreClient
      .putObject[Source[ByteString, NotUsed]](
        directory.file(key.value),
        Source.fromPublisher(bytes).map(ByteString(_))
      )

  def retrieve(
    key: UploadKey
  )(implicit headerCarrier: HeaderCarrier): Future[Option[Source[ByteString, NotUsed]]] =
    objectStoreClient
      .getObject[Source[ByteString, NotUsed]](directory.file(key.value))
      .map(_.map(_.content))

}

object UploadRepository {

  object ObjectStoreUpload {

    case class SensitiveUpload(override val decryptedValue: Upload) extends Sensitive[Upload]
    case class SensitiveCsvRow[T](override val decryptedValue: CsvRowState[T]) extends Sensitive[CsvRowState[T]]

    implicit def sensitiveUploadFormat(implicit crypto: Encrypter & Decrypter): Format[SensitiveUpload] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveUpload.apply)

    implicit def sensitiveCsvRowFormat[T](implicit
      crypto: Encrypter & Decrypter,
      format: Format[T]
    ): Format[SensitiveCsvRow[T]] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveCsvRow[T](_))(using CsvRowState.csvRowStateFormat[T], crypto)
  }

  implicit val uploadedInProgressFormat: OFormat[UploadStatus.InProgress.type] =
    Json.format[UploadStatus.InProgress.type]
  implicit val uploadFormatErrorFormat: OFormat[UploadFormatError] = Json.format[UploadFormatError]
  implicit val uploadFormat: OFormat[Upload] = Json.format[Upload]
}
