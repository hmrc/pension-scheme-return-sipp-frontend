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

case class EmailAuditEvent(
  pensionSchemeId: PensionSchemeId,
  minimalDetails: MinimalDetails,
  schemeDetails: SchemeDetails,
  taxYear: DateRange,
  event: String,
  emailAddress: String,
  reportVersion: String
) extends AuthorizedAuditEvent {

  override def auditType: String = "PensionSchemeReturnEmailEvent"

  override def additionalDetails: Map[String, String] = Map(
    "status" -> event,
    "reportVersion" -> reportVersion,
    "taxYear" -> s"${taxYear.from.getYear}-${taxYear.to.getYear}",
    "email" -> emailAddress
  )
}

object EmailAuditEvent {

  val Sent = "EmailSent"

  def buildAuditEvent(
    taxYear: DateRange,
    reportVersion: String
  )(implicit
    req: DataRequest[?]
  ): EmailAuditEvent =
    EmailAuditEvent(
      pensionSchemeId = req.pensionSchemeId,
      minimalDetails = req.minimalDetails,
      schemeDetails = req.schemeDetails,
      taxYear = taxYear,
      event = Sent,
      emailAddress = req.minimalDetails.email,
      reportVersion = reportVersion
    )

}
