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

package models

import models.SchemeId.Srn
import models.SchemeId.asSrn
import models.requests.DataRequest
import play.api.libs.json.*
import play.api.mvc.QueryStringBindable
import utils.HttpUrl

import java.net.URL
import java.time.Instant
import scala.annotation.unused

case class Reference(reference: String)

object Reference {
  implicit val referenceReader: Reads[Reference] = Reads.StringReads.map(Reference(_))
  implicit val referenceWrites: Writes[Reference] = Writes.StringWrites.contramap(_.reference)
}

case class UploadForm(href: String, fields: Map[String, String])

case class PreparedUpload(reference: Reference, uploadRequest: UploadForm)

object PreparedUpload {

  implicit val uploadFormFormat: Format[UploadForm] = Json.format[UploadForm]

  implicit val format: Format[PreparedUpload] = Json.format[PreparedUpload]
}

case class UpscanInitiateRequest(
  callbackUrl: String,
  successRedirect: Option[String] = None,
  errorRedirect: Option[String] = None,
  minimumFileSize: Option[Int] = None,
  maximumFileSize: Option[Int] = None
)

object UpscanInitiateRequest {
  implicit val format: OFormat[UpscanInitiateRequest] = Json.format[UpscanInitiateRequest]
}

case class UploadKey(userId: String, srn: Srn, page: String) {
  val value: String = userId + UploadKey.separator + srn.value + UploadKey.separator + page
}

object UploadKey {
  val separator = "&&"

  implicit val uploadKeyReads: Reads[UploadKey] = Reads.StringReads.flatMap(_.split(separator).toList match {
    case List(userId, asSrn(srn), redirectKey) => Reads.pure(UploadKey(userId, srn, redirectKey))
    case key => Reads.failed(s"Upload key $key is in wrong format. It should be userId${separator}srn")
  })

  implicit val uploadKeyWrites: Writes[UploadKey] = Writes.StringWrites.contramap(_.value)

  def fromRequest(srn: Srn, page: String)(implicit req: DataRequest[?]): UploadKey =
    UploadKey(req.getUserId, srn, page)

  def fromString(key: String): Option[UploadKey] =
    key.split(separator).toList match {
      case userId :: srnString :: page :: Nil =>
        Srn(srnString).map(srn => UploadKey(userId, srn, page))
      case _ => None
    }
}

case class UpscanFileReference(reference: String)

case class UpscanInitiateResponse(
  fileReference: UpscanFileReference,
  postTarget: String,
  formFields: Map[String, String]
)

object UpscanInitiateResponse {
  implicit val refFormat: OFormat[UpscanFileReference] = Json.format[UpscanFileReference]
  implicit val format: OFormat[UpscanInitiateResponse] = Json.format[UpscanInitiateResponse]
}

object UploadStatus {

  sealed trait UploadStatus // needs to be in the same closure as it's subtypes for Json.format to work

  case object InProgress extends UploadStatus

  case class Failed(failureDetails: ErrorDetails) extends UploadStatus {
    def asQueryParams: String = s"errorCode=${failureDetails.failureReason}&errorMessage=${failureDetails.message}"
  }

  object Failed {
    def incorrectFileFormatQueryParam = "errorCode=InvalidArgument&errorMessage='file' invalid file format"

    implicit val failedFormat: OFormat[Failed] = Json.format[Failed]
  }

  case class Success(name: String, mimeType: String, downloadUrl: String, size: Option[Long]) extends UploadStatus
  object Success {
    implicit val successFormat: OFormat[Success] = Json.format[Success]
  }
}

case class UploadId(value: String) extends AnyVal

object UploadId {

  implicit def queryBinder(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[UploadId] =
    stringBinder.transform(UploadId(_), _.value)
}

sealed trait CallbackBody {
  def reference: Reference
}

case class ReadyCallbackBody(
  reference: Reference,
  downloadUrl: URL,
  uploadDetails: UploadCallbackDetails
) extends CallbackBody

case class FailedCallbackBody(
  reference: Reference,
  failureDetails: ErrorDetails
) extends CallbackBody

case class UploadCallbackDetails(
  uploadTimestamp: Instant,
  checksum: String,
  fileMimeType: String,
  fileName: String,
  size: Long
)

case class UploadDetails(
  key: UploadKey,
  reference: Reference,
  status: UploadStatus.UploadStatus,
  lastUpdated: Instant
)

case class ErrorDetails(failureReason: String, message: String)

object ErrorDetails {
  implicit val errorDetailsFormat: OFormat[ErrorDetails] = Json.format[ErrorDetails]
}

object CallbackBody {
  // must be in scope to create Reads for ReadyCallbackBody
  @unused private implicit val urlFormat: Format[URL] = HttpUrl.format

  implicit val uploadDetailsReads: Reads[UploadCallbackDetails] = Json.reads[UploadCallbackDetails]

  implicit val readyCallbackBodyReads: Reads[ReadyCallbackBody] = Json.reads[ReadyCallbackBody]

  implicit val failedCallbackBodyReads: Reads[FailedCallbackBody] = Json.reads[FailedCallbackBody]

  implicit val reads: Reads[CallbackBody] = (json: JsValue) =>
    json \ "fileStatus" match {
      case JsDefined(JsString("READY")) => implicitly[Reads[ReadyCallbackBody]].reads(json)
      case JsDefined(JsString("FAILED")) => implicitly[Reads[FailedCallbackBody]].reads(json)
      case JsDefined(value) => JsError(s"Invalid type distriminator: $value")
      case JsUndefined() => JsError(s"Missing type distriminator")
      case _ => JsError(s"Missing type distriminator")
    }
}
