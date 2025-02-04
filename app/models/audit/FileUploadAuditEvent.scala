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

package models.audit

import models.requests.DataRequest
import models.{DateRange, MinimalDetails, PensionSchemeId, SchemeDetails}
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class FileUploadAuditEvent(
  fileUploadType: String,
  fileUploadStatus: String,
  fileName: String,
  fileReference: String,
  typeOfError: Option[String],
  fileSize: Long,
  validationCompleted: LocalDate,
  pensionSchemeId: PensionSchemeId,
  minimalDetails: MinimalDetails,
  schemeDetails: SchemeDetails,
  taxYear: DateRange
) extends AuthorizedAuditEvent {

  override def auditType: String = "PensionSchemeReturnFileUpload"

  override def additionalDetails: Map[String, String] = {
    val details = Map(
      "fileUploadType" -> fileUploadType,
      "fileUploadStatus" -> fileUploadStatus,
      "fileName" -> fileName,
      "fileReference" -> fileReference,
      "fileSize" -> s"$fileSize",
      "validationCompleted" -> s"$validationCompleted",
      "taxYear" -> s"${taxYear.from.getYear}-${taxYear.to.getYear}",
      "date" -> LocalDate.now().toString
    )

    val optionalDetails = Seq(
      typeOfError.map(error => "typeOfError" -> error)
    ).flatten

    details ++ optionalDetails
  }
}

object FileUploadAuditEvent {
  case class FileUploadAuditContext(
    schemeDetails: SchemeDetails,
    fileUploadType: String,
    fileUploadStatus: String,
    fileName: String,
    fileReference: String,
    fileSize: Long,
    validationCompleted: LocalDate,
    taxYear: DateRange
  )

  implicit val fileUploadAuditContextFormat: OFormat[FileUploadAuditContext] = Json.format[FileUploadAuditContext]

  val SUCCESS = "Success"
  val ERROR = "Error"

  val ERROR_UNDER: Some[String] = Some("Under 25")
  val ERROR_OVER: Some[String] = Some("Over 25")
  val ERROR_SIZE_LIMIT: Some[String] = Some("Your File Size exceed the maximum Limit")

  def buildAuditEvent(
    fileUploadType: String,
    fileUploadStatus: String,
    fileName: String,
    fileReference: String,
    fileSize: Long,
    validationCompleted: LocalDate,
    taxYear: DateRange,
    typeOfError: Option[String] = None
  )(implicit
    req: DataRequest[?]
  ): FileUploadAuditEvent = FileUploadAuditEvent(
    fileUploadType = fileUploadType,
    fileUploadStatus = fileUploadStatus,
    fileName = fileName,
    fileReference = fileReference,
    typeOfError = typeOfError,
    fileSize = fileSize,
    validationCompleted = validationCompleted,
    pensionSchemeId = req.pensionSchemeId,
    minimalDetails = req.minimalDetails,
    schemeDetails = req.schemeDetails,
    taxYear = taxYear
  )

  def getAuditContext(auditEvent: FileUploadAuditEvent): FileUploadAuditContext =
    FileUploadAuditContext(
      schemeDetails = auditEvent.schemeDetails,
      fileUploadType = auditEvent.fileUploadType,
      fileUploadStatus = auditEvent.fileUploadStatus,
      fileName = auditEvent.fileName,
      fileReference = auditEvent.fileReference,
      fileSize = auditEvent.fileSize,
      validationCompleted = auditEvent.validationCompleted,
      taxYear = auditEvent.taxYear
    )

}
