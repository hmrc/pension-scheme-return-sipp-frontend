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

//package repositories
//
//import config.{Crypto, FrontendAppConfig}
//import models.SchemeId.asSrn
//import models.UploadKey.separator
//import models.UploadStatus.UploadStatus
//import models._
//import models.csv.CsvDocumentState
//import org.mongodb.scala.model.Filters.equal
//import org.mongodb.scala.model.Updates.{combine, set}
//import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes}
//import play.api.libs.functional.syntax._
//import play.api.libs.json._
//import repositories.UploadMetadataRepository.MongoUpload
//import repositories.UploadMetadataRepository.MongoUpload.{SensitiveUploadStatus, SensitiveUploadValidationState}
//import uk.gov.hmrc.crypto.json.JsonEncryption
//import uk.gov.hmrc.crypto.{Decrypter, Encrypter, Sensitive}
//import uk.gov.hmrc.mongo.MongoComponent
//import uk.gov.hmrc.mongo.play.json.Codecs._
//import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
//import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
//
//import java.time.{Clock, Instant}
//import java.util.concurrent.TimeUnit
//import javax.inject.{Inject, Singleton}
//import scala.Function.unlift
//import scala.concurrent.{ExecutionContext, Future}
//
//@Singleton
//class CsvDocumentStateRepository @Inject()(
//  mongoComponent: MongoComponent,
//  clock: Clock,
//  appConfig: FrontendAppConfig,
//  crypto: Crypto
//)(implicit ec: ExecutionContext)
//    extends PlayMongoRepository[CsvDocumentState](
//      collectionName = "csv-document-state",
//      mongoComponent = mongoComponent,
//      domainFormat = CsvDocumentState.format,
//      indexes = Seq(
//        IndexModel(Indexes.ascending("id"), IndexOptions().unique(true)),
//        IndexModel(Indexes.ascending("reference"), IndexOptions().unique(true)),
//        IndexModel(
//          Indexes.ascending("lastUpdated"),
//          IndexOptions()
//            .name("lastUpdatedIdx")
//            .expireAfter(appConfig.uploadTtl, TimeUnit.SECONDS)
//        )
//      ),
//      replaceIndexes = false
//    ) {
//
//  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
//  implicit val cryptoEncDec: Encrypter with Decrypter = crypto.getCrypto
//
//  import UploadMetadataRepository._
//
//  def insert(details: UploadDetails): Future[Unit] =
//    collection
//      .insertOne(toMongoUpload(details))
//      .toFuture()
//      .map(_ => ())
//
//  def getUploadDetails(key: UploadKey): Future[Option[UploadDetails]] =
//    collection.find(equal("id", key.toBson())).headOption().map(_.map(toUploadDetails))
//
//  def remove(key: UploadKey): Future[Unit] =
//    collection
//      .deleteOne(equal("id", key.toBson()))
//      .toFuture()
//      .map(_ => ())
//}
//
//object UploadMetadataRepository {
//
//  case class MongoState(
//    key: UploadKey,
//    reference: Reference,
//    status: SensitiveUploadStatus,
//    lastUpdated: Instant,
//    validationState: Option[SensitiveUploadValidationState]
//  )
//
//  object MongoUpload {
//
//    case class SensitiveCsvDocumentState(override val decryptedValue: CsvDocumentState) extends Sensitive[CsvDocumentState]
//
//    implicit def sensitiveCsvDocumentFormat(
//      implicit crypto: Encrypter with Decrypter
//    ): Format[SensitiveCsvDocumentState] =
//      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveCsvDocumentState.apply)
//
//    def reads(implicit crypto: Encrypter with Decrypter): Reads[MongoUpload] =
//      (__ \ "id")
//        .read[UploadKey]
//        .and((__ \ "reference").read[Reference])
//        .and((__ \ "status").read[SensitiveUploadStatus])
//        .and((__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat))
//        .and((__ \ "validationState").readNullable[SensitiveUploadValidationState])(MongoUpload.apply _)
//
//    def writes(implicit crypto: Encrypter with Decrypter): OWrites[MongoUpload] =
//      (__ \ "id")
//        .write[UploadKey]
//        .and((__ \ "reference").write[Reference])
//        .and((__ \ "status").write[SensitiveUploadStatus])
//        .and((__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat))
//        .and((__ \ "validationState").writeNullable[SensitiveUploadValidationState])(
//          unlift(MongoUpload.unapply)
//        )
//
//    implicit def format(implicit crypto: Encrypter with Decrypter): OFormat[MongoUpload] = OFormat(reads, writes)
//  }
//
//  implicit val uploadKeyReads: Reads[UploadKey] = Reads.StringReads.flatMap(_.split(separator).toList match {
//    case List(userId, asSrn(srn), redirectKey) => Reads.pure(UploadKey(userId, srn, redirectKey))
//    case key => Reads.failed(s"Upload key $key is in wrong format. It should be userId${separator}srn")
//  })
//
//  implicit val uploadKeyWrites: Writes[UploadKey] = Writes.StringWrites.contramap(_.value)
//
//  implicit val uploadedSuccessfullyFormat: OFormat[UploadStatus.Success] = Json.format[UploadStatus.Success]
//  implicit val errorDetailsFormat: OFormat[ErrorDetails] = Json.format[ErrorDetails]
//  implicit val uploadedFailedFormat: OFormat[UploadStatus.Failed] = Json.format[UploadStatus.Failed]
//  implicit val uploadedInProgressFormat: OFormat[UploadStatus.InProgress.type] =
//    Json.format[UploadStatus.InProgress.type]
//  implicit val uploadedStatusFormat: OFormat[UploadStatus] = Json.format[UploadStatus]
//  implicit val referenceFormat: Format[Reference] = stringFormat[Reference](Reference(_), _.reference)
//  implicit val uploadValidatingFormat: OFormat[UploadValidating] = Json.format[UploadValidating]
//  implicit val uploadedFormat: OFormat[Uploaded.type] = Json.format[Uploaded.type]
//  implicit val uploadValidatedFormat: OFormat[UploadValidated.type] = Json.format[UploadValidated.type]
//
//  implicit val uploadStateFormat: OFormat[UploadState] = Json.format[UploadState]
//
//  private def stringFormat[A](to: String => A, from: A => String): Format[A] =
//    Format[A](Reads.StringReads.map(to), Writes.StringWrites.contramap(from))
//}
