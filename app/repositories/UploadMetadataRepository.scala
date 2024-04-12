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

import config.{Crypto, FrontendAppConfig}
import models.SchemeId.asSrn
import models.UploadKey.separator
import models.UploadStatus.UploadStatus
import models._
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import repositories.UploadMetadataRepository.MongoUpload
import repositories.UploadMetadataRepository.MongoUpload.{SensitiveUploadStatus, SensitiveUploadValidationState}
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs._
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.Function.unlift
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadMetadataRepository @Inject()(
  mongoComponent: MongoComponent,
  clock: Clock,
  appConfig: FrontendAppConfig,
  crypto: Crypto
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[MongoUpload](
      collectionName = "upload",
      mongoComponent = mongoComponent,
      domainFormat = MongoUpload.format(crypto.getCrypto),
      indexes = Seq(
        IndexModel(Indexes.ascending("id"), IndexOptions().unique(true)),
        IndexModel(Indexes.ascending("reference"), IndexOptions().unique(true)),
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(appConfig.uploadTtl, TimeUnit.SECONDS)
        )
      ),
      replaceIndexes = false
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val cryptoEncDec: Encrypter with Decrypter = crypto.getCrypto

  import UploadMetadataRepository._

  def upsert(details: UploadDetails): Future[Unit] =
    collection
      .findOneAndUpdate(
        filter = equal("id", details.key.toBson()),
        update = combine(
          set("reference", details.reference.toBson()),
          set("status", SensitiveUploadStatus(details.status).toBson()),
          set("lastUpdated", details.lastUpdated.toBson())
        ),
        options = FindOneAndUpdateOptions().upsert(true) // inserts a new record if a document with a provided Id cannot be found
      )
      .toFuture()
      .map(_ => ())

  def getUploadDetails(key: UploadKey): Future[Option[UploadDetails]] =
    collection.find(equal("id", key.toBson())).headOption().map(_.map(toUploadDetails))

  def updateStatus(reference: Reference, newStatus: UploadStatus): Future[Unit] =
    collection
      .findOneAndUpdate(
        filter = equal("reference", reference.toBson()),
        update = combine(
          set("status", SensitiveUploadStatus(newStatus).toBson()),
          set("lastUpdated", Instant.now(clock).toBson())
        ),
        options = FindOneAndUpdateOptions().upsert(false)
      )
      .toFuture()
      .map(_ => ())

  def setValidationState(key: UploadKey, validationState: UploadState): Future[Unit] =
    collection
      .findOneAndUpdate(
        filter = equal("id", key.toBson()),
        update = combine(
          set("validationState", SensitiveUploadValidationState(validationState).toBson()),
          set("lastUpdated", Instant.now(clock).toBson())
        )
      )
      .toFuture()
      .map(_ => ())

  def getValidationState(key: UploadKey): Future[Option[UploadState]] =
    collection
      .find(equal("id", key.value.toBson()))
      .headOption()
      .map(_.flatMap(_.validationState.map(_.decryptedValue)))

  def remove(key: UploadKey): Future[Unit] =
    collection
      .deleteOne(equal("id", key.toBson()))
      .toFuture()
      .map(_ => ())

  private def toUploadDetails(mongoUpload: MongoUpload): UploadDetails = UploadDetails(
    mongoUpload.key,
    mongoUpload.reference,
    mongoUpload.status.decryptedValue,
    mongoUpload.lastUpdated
  )
}

object UploadMetadataRepository {

  case class MongoUpload(
    key: UploadKey,
    reference: Reference,
    status: SensitiveUploadStatus,
    lastUpdated: Instant,
    validationState: Option[SensitiveUploadValidationState]
  )

  object MongoUpload {

    case class SensitiveUploadValidationState(override val decryptedValue: UploadState) extends Sensitive[UploadState]

    implicit def sensitiveUploadFormat(
      implicit crypto: Encrypter with Decrypter
    ): Format[SensitiveUploadValidationState] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveUploadValidationState.apply)

    case class SensitiveUploadStatus(override val decryptedValue: UploadStatus) extends Sensitive[UploadStatus]

    implicit def sensitiveUploadStatusFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveUploadStatus] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveUploadStatus.apply)

    def reads(implicit crypto: Encrypter with Decrypter): Reads[MongoUpload] =
      (__ \ "id")
        .read[UploadKey]
        .and((__ \ "reference").read[Reference])
        .and((__ \ "status").read[SensitiveUploadStatus])
        .and((__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat))
        .and((__ \ "validationState").readNullable[SensitiveUploadValidationState])(MongoUpload.apply _)

    def writes(implicit crypto: Encrypter with Decrypter): OWrites[MongoUpload] =
      (__ \ "id")
        .write[UploadKey]
        .and((__ \ "reference").write[Reference])
        .and((__ \ "status").write[SensitiveUploadStatus])
        .and((__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat))
        .and((__ \ "validationState").writeNullable[SensitiveUploadValidationState])(
          unlift(MongoUpload.unapply)
        )

    implicit def format(implicit crypto: Encrypter with Decrypter): OFormat[MongoUpload] = OFormat(reads, writes)
  }

  implicit val uploadKeyReads: Reads[UploadKey] = Reads.StringReads.flatMap(_.split(separator).toList match {
    case List(userId, asSrn(srn), redirectKey) => Reads.pure(UploadKey(userId, srn, redirectKey))
    case key => Reads.failed(s"Upload key $key is in wrong format. It should be userId${separator}srn")
  })

  implicit val uploadKeyWrites: Writes[UploadKey] = Writes.StringWrites.contramap(_.value)

  implicit val uploadedSuccessfullyFormat: OFormat[UploadStatus.Success] = Json.format[UploadStatus.Success]
  implicit val errorDetailsFormat: OFormat[ErrorDetails] = Json.format[ErrorDetails]
  implicit val uploadedFailedFormat: OFormat[UploadStatus.Failed] = Json.format[UploadStatus.Failed]
  implicit val uploadedInProgressFormat: OFormat[UploadStatus.InProgress.type] =
    Json.format[UploadStatus.InProgress.type]
  implicit val uploadedStatusFormat: OFormat[UploadStatus] = Json.format[UploadStatus]
  implicit val referenceFormat: Format[Reference] = stringFormat[Reference](Reference(_), _.reference)
  implicit val uploadValidatingFormat: OFormat[UploadValidating] = Json.format[UploadValidating]
  implicit val uploadedFormat: OFormat[Uploaded.type] = Json.format[Uploaded.type]
  implicit val uploadValidatedFormat: OFormat[UploadValidated] = Json.format[UploadValidated]
  implicit val validationExceptionFormat: OFormat[ValidationException.type] = Json.format[ValidationException.type]

  implicit val uploadStateFormat: OFormat[UploadState] = Json.format[UploadState]

  private def stringFormat[A](to: String => A, from: A => String): Format[A] =
    Format[A](Reads.StringReads.map(to), Writes.StringWrites.contramap(from))
}
