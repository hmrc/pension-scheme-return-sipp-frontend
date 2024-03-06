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
import com.mongodb.client.gridfs.model.GridFSUploadOptions
import config.Crypto
import models.SchemeId.asSrn
import models.UploadKey.separator
import models._
import org.mongodb.scala._
import org.mongodb.scala.model.Filters.equal
import org.reactivestreams.Publisher
import play.api.libs.json._
import repositories.UploadRepository.MongoUpload.SensitiveUpload
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import uk.gov.hmrc.mongo.play.json.Codecs._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.nio.ByteBuffer
import java.nio.charset.Charset
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

  private def delete(key: UploadKey): Future[Unit] =
    mongo.gridFSBucket
      .find(equal("_id", key.value.toBson()))
      .flatMap(file => mongo.gridFSBucket.delete(file.getId))
      .toFuture()
      .map(_ => {})

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

  def setUploadResult(key: UploadKey, result: Upload): Future[Unit] = {
    val uploadAsBytes =
      Json.toJson(result).toString().getBytes(Charset.forName("UTF-8"))

    val uploadAsSource: Publisher[ByteBuffer] =
      Source
        .single(ByteString.fromArray(uploadAsBytes))
        .map(_.toByteBuffer)
        .runWith(Sink.asPublisher(false))

    delete(key).flatMap(_ => save(key, uploadAsSource))
  }

  def getUploadResult(key: UploadKey): Future[Option[Upload]] =
    mongo.gridFSBucket
      .downloadToObservable(key.value.toBson())
      .foldLeft(ByteString.empty)(_ ++ ByteString(_))
      .toFuture()
      .map { bytes =>
        Json
          .toJson(bytes.utf8String)
          .asOpt[SensitiveUpload]
          .map(_.decryptedValue)
      }

  def findAll()(implicit ec: ExecutionContext): Future[Seq[String]] =
    mongo.gridFSBucket
      .find()
      .toFuture()
      .map(files => files.map(file => file.getId.asString.getValue))
}

object UploadRepository {

  object MongoUpload {

    case class SensitiveUpload(override val decryptedValue: Upload) extends Sensitive[Upload]

    implicit def sensitiveUploadFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveUpload] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveUpload.apply)
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
  implicit val uploadSuccessForLandConnectedPropertyFormat: OFormat[UploadSuccessLandConnectedProperty] =
    Json.format[UploadSuccessLandConnectedProperty]
  implicit val validationErrorsFormat: OFormat[NonEmptyList[ValidationError]] =
    Json.format[NonEmptyList[ValidationError]]
  implicit val uploadUploadErrorsForLandConnectedProperty: OFormat[UploadErrorsLandConnectedProperty] =
    Json.format[UploadErrorsLandConnectedProperty]
  implicit val uploadErrorsFormat: OFormat[UploadErrorsMemberDetails] = Json.format[UploadErrorsMemberDetails]
  implicit val uploadFormatErrorFormat: OFormat[UploadFormatError] = Json.format[UploadFormatError]
  implicit val uploadFormat: OFormat[Upload] = Json.format[Upload]
}
