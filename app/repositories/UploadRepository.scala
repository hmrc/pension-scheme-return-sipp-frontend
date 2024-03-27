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

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import cats.data.NonEmptyList
import cats.implicits.toTraverseOps
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import config.Crypto
import models.SchemeId.asSrn
import models.UploadKey.separator
import models._
import models.csv.CsvRowState
import org.mongodb.scala._
import org.mongodb.scala.gridfs.GridFSUploadObservable
import org.mongodb.scala.model.Filters.{equal, lte}
import org.reactivestreams.Publisher
import play.api.libs.json._
import repositories.UploadRepository.MongoUpload.SensitiveUpload
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import uk.gov.hmrc.mongo.play.json.Codecs._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadRepository @Inject()(mongo: MongoGridFsConnection, crypto: Crypto)(
  implicit ec: ExecutionContext,
  mat: Materializer
) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val cryptoEncDec: Encrypter with Decrypter = crypto.getCrypto

  import UploadRepository._

  def delete(key: UploadKey): Future[Unit] =
    mongo.gridFSBucket
      .find(equal("_id", key.value.toBson()))
      .toFuture()
      .map(files => files.traverse(file => mongo.gridFSBucket.delete(file.getId).toFuture().map(_ => {})))

  private def save(key: UploadKey, bytes: Publisher[ByteBuffer]) =
    mongo.gridFSBucket
      .uploadFromObservable(
        id = key.toBson(),
        filename = key.value,
        source = bytes.toObservable(),
        options = new GridFSUploadOptions()
      )
      .toFuture()
      .map(_ => {})

  def publish(key: UploadKey, bytes: Publisher[ByteBuffer]): GridFSUploadObservable[Unit] =
    mongo.gridFSBucket
      .uploadFromObservable(
        id = key.toBson(),
        filename = key.value,
        source = bytes.toObservable(),
        options = new GridFSUploadOptions()
      )

  def setUploadResult(key: UploadKey, result: Upload): Future[Unit] = {
    val uploadAsBytes =
      Json.toJson(SensitiveUpload(result)).toString().getBytes(StandardCharsets.UTF_8)

    val uploadAsSource: Publisher[ByteBuffer] =
      Source
        .single(ByteString.fromArray(uploadAsBytes))
        .map(_.toByteBuffer)
        .runWith(Sink.asPublisher(false))

    delete(key).flatMap(_ => save(key, uploadAsSource))
  }

  def getUploadResult(key: UploadKey): Future[Option[Upload]] =
    mongo.gridFSBucket
      .find(equal("_id", key.value.toBson()))
      .flatMap(
        id =>
          mongo.gridFSBucket
            .downloadToObservable(id.getId)
            .foldLeft(ByteString.empty)(_ ++ ByteString(_))
            .map { bytes =>
              Json
                .parse(bytes.utf8String)
                .asOpt[SensitiveUpload]
                .map(_.decryptedValue)
            }
      )
      .headOption()
      .map(_.flatten)

  def streamUploadResult(key: UploadKey): Publisher[ByteBuffer] =
    mongo.gridFSBucket
      .find(equal("_id", key.value.toBson()))
      .flatMap(
        id =>
          mongo.gridFSBucket
            .downloadToObservable(id.getId)
      )

  def findAllOnOrBefore(now: Instant): Future[Seq[UploadKey]] =
    mongo.gridFSBucket
      .find(lte("uploadDate", now.toBson()))
      .toFuture()
      .map(
        files => files.flatMap(file => UploadKey.fromString(file.getId.asString().getValue))
      )

}

object UploadRepository {

  object MongoUpload {

    case class SensitiveUpload(override val decryptedValue: Upload) extends Sensitive[Upload]
    case class SensitiveCsvRow[T](override val decryptedValue: CsvRowState[T]) extends Sensitive[CsvRowState[T]]

    implicit def sensitiveUploadFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveUpload] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveUpload.apply)

    implicit def sensitiveCsvRowFormat[T](
      implicit crypto: Encrypter with Decrypter,
      format: Format[T]
    ): Format[SensitiveCsvRow[T]] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveCsvRow[T](_))(CsvRowState.csvRowStateFormat[T], crypto)
  }

  implicit val uploadKeyReads: Reads[UploadKey] = Reads.StringReads.flatMap(_.split(separator).toList match {
    case List(userId, asSrn(srn), redirectKey) => Reads.pure(UploadKey(userId, srn, redirectKey))
    case key => Reads.failed(s"Upload key $key is in wrong format. It should be userId${separator}srn")
  })

  implicit val uploadKeyWrites: Writes[UploadKey] = Writes.StringWrites.contramap(_.value)
  implicit val uploadedSuccessfullyFormat: OFormat[UploadStatus.Success] =
    Json.format[UploadStatus.Success]
  implicit val errorDetailsFormat: OFormat[ErrorDetails] = Json.format[ErrorDetails]
  implicit val uploadedFailedFormat: OFormat[UploadStatus.Failed] = Json.format[UploadStatus.Failed]
  implicit val uploadedInProgressFormat: OFormat[UploadStatus.InProgress.type] =
    Json.format[UploadStatus.InProgress.type]
  implicit val memberDetailsFormat: OFormat[NonEmptyList[MemberDetailsUpload]] =
    Json.format[NonEmptyList[MemberDetailsUpload]]
  implicit val uploadedFormat: OFormat[Uploaded.type] = Json.format[Uploaded.type]
  implicit val uploadSuccessFormat: OFormat[UploadSuccessMemberDetails] = Json.format[UploadSuccessMemberDetails]
  implicit val uploadFormatErrorFormat: OFormat[UploadFormatError] = Json.format[UploadFormatError]
  implicit val uploadFormat: OFormat[Upload] = Json.format[Upload]
}
