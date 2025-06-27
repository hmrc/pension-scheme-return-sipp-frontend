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
import models.UploadStatus.UploadStatus
import models.*
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, set}
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes}
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import repositories.UploadMetadataRepository.MongoUpload
import MongoUpload.{SensitiveUploadStatus, SensitiveUploadValidationState}
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
import models.UploadStatus.{Failed, InProgress, Success}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.*
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadMetadataRepository @Inject() (
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
      replaceIndexes = true
    ) {

  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val cryptoEncDec: Encrypter & Decrypter = crypto.getCrypto

  import UploadMetadataRepository.*
  import cats.implicits.toFunctorOps

  def upsert(details: UploadDetails): Future[Unit] =
    collection
      .findOneAndUpdate(
        filter = equal("id", details.key.toBson),
        update = combine(
          set("reference", details.reference.toBson),
          set("status", SensitiveUploadStatus(details.status).toBson),
          set("lastUpdated", details.lastUpdated.toBson)
        ),
        options = FindOneAndUpdateOptions().upsert(
          true
        ) // inserts a new record if a document with a provided Id cannot be found
      )
      .toFuture()
      .void

  def getUploadDetails(key: UploadKey): Future[Option[UploadDetails]] =
    collection.find(equal("id", key.toBson)).headOption().map(_.map(toUploadDetails))

  def updateStatus(reference: Reference, newStatus: UploadStatus): Future[Unit] =
    collection
      .findOneAndUpdate(
        filter = equal("reference", reference.toBson),
        update = combine(
          set("status", SensitiveUploadStatus(newStatus).toBson),
          set("lastUpdated", Instant.now(clock).toBson)
        ),
        options = FindOneAndUpdateOptions().upsert(false)
      )
      .toFuture()
      .void

  def setValidationState(key: UploadKey, validationState: UploadState): Future[Unit] =
    collection
      .findOneAndUpdate(
        filter = equal("id", key.toBson),
        update = combine(
          set("validationState", SensitiveUploadValidationState(validationState).toBson),
          set("lastUpdated", Instant.now(clock).toBson)
        )
      )
      .toFuture()
      .void

  def getValidationState(key: UploadKey): Future[Option[UploadState]] =
    collection
      .find(equal("id", key.value.toBson))
      .headOption()
      .map(_.flatMap(_.validationState.map(_.decryptedValue)))

  def remove(key: UploadKey): Future[Unit] =
    collection
      .deleteOne(equal("id", key.toBson))
      .toFuture()
      .void

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

    implicit def sensitiveUploadFormat(implicit
      crypto: Encrypter & Decrypter
    ): Format[SensitiveUploadValidationState] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveUploadValidationState.apply)

    case class SensitiveUploadStatus(override val decryptedValue: UploadStatus) extends Sensitive[UploadStatus]

    implicit def sensitiveUploadStatusFormat(implicit crypto: Encrypter & Decrypter): Format[SensitiveUploadStatus] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveUploadStatus.apply)

    def reads(implicit crypto: Encrypter & Decrypter): Reads[MongoUpload] =
      (__ \ "id")
        .read[UploadKey]
        .and((__ \ "reference").read[Reference])
        .and((__ \ "status").read[SensitiveUploadStatus])
        .and((__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat))
        .and((__ \ "validationState").readNullable[SensitiveUploadValidationState])(MongoUpload.apply)

    def writes(implicit crypto: Encrypter & Decrypter): OWrites[MongoUpload] =
      (__ \ "id")
        .write[UploadKey]
        .and((__ \ "reference").write[Reference])
        .and((__ \ "status").write[SensitiveUploadStatus])
        .and((__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat))
        .and((__ \ "validationState").writeNullable[SensitiveUploadValidationState])(
          Tuple.fromProductTyped(_)
        )

    implicit def format(implicit crypto: Encrypter & Decrypter): OFormat[MongoUpload] = OFormat(reads, writes)
  }

  implicit val uploadedInProgressFormat: OFormat[UploadStatus.InProgress.type] =
    Json.format[UploadStatus.InProgress.type]
  implicit val uploadedStatusFormat: OFormat[UploadStatus] = new OFormat[UploadStatus] {

    override def reads(json: JsValue): JsResult[UploadStatus] =
      (json \ "type").validate[String].flatMap {
        case "InProgress" => JsSuccess(InProgress)
        case "Failed"     => Failed.failedFormat.reads(json)
        case "Success"    => Success.successFormat.reads(json)
        case other        => JsError(s"Unknown UploadStatus type: $other")
      }

    override def writes(status: UploadStatus): JsObject = {
      val (base, tpe) = status match {
        case InProgress     => (Json.obj(), "InProgress")
        case f: Failed      => (Failed.failedFormat.writes(f), "Failed")
        case s: Success     => (Success.successFormat.writes(s), "Success")
      }
      base + ("type" -> JsString(tpe))
    }
  }
  implicit val referenceFormat: Format[Reference] = stringFormat[Reference](Reference(_), _.reference)

  private def stringFormat[A](to: String => A, from: A => String): Format[A] =
    Format[A](Reads.StringReads.map(to), Writes.StringWrites.contramap(from))
}
