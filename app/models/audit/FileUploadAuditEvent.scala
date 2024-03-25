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

  override def auditType: String = "fileUpload"

  override def additionalDetails: Map[String, String] = {
    val details = Map(
      "fileUploadType" -> fileUploadType,
      "fileUploadStatus" -> fileUploadStatus,
      "fileName" -> fileName,
      "fileReference" -> fileReference,
      "fileSize" -> s"$fileSize",
      "validationCompleted" -> s"$validationCompleted",
      "TaxYear" -> s"${taxYear.from.getYear}-${taxYear.to.getYear}",
      "Date" -> LocalDate.now().toString
    )

    val optionalDetails = Seq(
      typeOfError.map(error => "typeOfError" -> error)
    ).flatten

    details ++ optionalDetails
  }
}

object FileUploadAuditEvent {
  val SUCCESS = "Success"
  val ERROR = "Error"

  val ERROR_UNDER = Some("Under 25")
  val ERROR_OVER = Some("Over 25")

  def buildAuditEvent(
    fileUploadType: String,
    fileUploadStatus: String,
    fileName: String,
    fileReference: String,
    fileSize: Long,
    validationCompleted: LocalDate,
    taxYear: DateRange,
    typeOfError: Option[String] = None
  )(
    implicit req: DataRequest[_]
  ) = FileUploadAuditEvent(
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
}
