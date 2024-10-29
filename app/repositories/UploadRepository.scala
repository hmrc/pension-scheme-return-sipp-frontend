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

import cats.implicits.{toFunctorOps, toTraverseOps}
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import config.Crypto
import models.*
import models.csv.CsvRowState
import org.mongodb.scala.*
import org.mongodb.scala.gridfs.GridFSUploadObservable
import org.mongodb.scala.model.Filters.{equal, lte}
import org.reactivestreams.Publisher
import play.api.libs.json.*
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import uk.gov.hmrc.mongo.play.json.Codecs.*
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.nio.ByteBuffer
import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadRepository @Inject() (mongo: MongoGridFsConnection, crypto: Crypto)(implicit
  ec: ExecutionContext
) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val cryptoEncDec: Encrypter & Decrypter = crypto.getCrypto

  def delete(key: UploadKey): Future[Unit] =
    mongo.gridFSBucket
      .find(equal("_id", key.value.toBson))
      .toFuture()
      .flatMap(files => files.traverse(file => mongo.gridFSBucket.delete(file.getId).toFuture()))
      .void

  def publish(key: UploadKey, bytes: Publisher[ByteBuffer]): GridFSUploadObservable[Unit] =
    mongo.gridFSBucket
      .uploadFromObservable(
        id = key.toBson,
        filename = key.value,
        source = bytes.toObservable(),
        options = GridFSUploadOptions()
      )

  def streamUploadResult(key: UploadKey): Publisher[ByteBuffer] =
    mongo.gridFSBucket
      .find(equal("_id", key.value.toBson))
      .flatMap(id =>
        mongo.gridFSBucket
          .downloadToObservable(id.getId)
      )

  def findAllOnOrBefore(now: Instant): Future[Seq[UploadKey]] =
    mongo.gridFSBucket
      .find(lte("uploadDate", now.toBson))
      .toFuture()
      .map(files => files.flatMap(file => UploadKey.fromString(file.getId.asString().getValue)))

}

object UploadRepository {

  object MongoUpload {

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
