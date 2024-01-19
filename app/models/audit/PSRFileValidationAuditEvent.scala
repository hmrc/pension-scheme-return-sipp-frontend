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

import models.{DateRange, MinimalDetails, PensionSchemeId, SchemeDetails}

case class PSRFileValidationAuditEvent(
  pensionSchemeId: PensionSchemeId,
  minimalDetails: MinimalDetails,
  schemeDetails: SchemeDetails,
  taxYear: DateRange,
  validationCheckStatus: String,
  fileValidationTimeInMilliSeconds: Long,
  numberOfEntries: Int,
  numberOfFailures: Int
) extends AuthorizedAuditEvent {

  override def auditType: String = "PensionSchemeReturnFileValidationCheck"

  override def additionalDetails: Map[String, String] = Map(
    "TaxYear" -> s"${taxYear.from.getYear}-${taxYear.to.getYear}",
    "ValidationCheckStatus" -> validationCheckStatus,
    "FileValidationTimeInMilliSeconds" -> fileValidationTimeInMilliSeconds.toString,
    "NumberOfEntries" -> numberOfEntries.toString,
    "NumberOfFailures" -> numberOfFailures.toString
  )
}
