/*
 * Copyright 2024 HM Revenue & Customs
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

package models.requests.psr

import models.SchemeId.Srn
import models.requests.DataRequest
import pages.WhichTaxYearPage
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class ReportDetails(
  pstr: String,
  status: EtmpPsrStatus,
  periodStart: LocalDate,
  periodEnd: LocalDate,
  schemeName: Option[String],
  psrVersion: Option[String]
)

object ReportDetails {
  implicit val format: OFormat[ReportDetails] = Json.format[ReportDetails]

  def toReportDetails(srn: Srn)(implicit request: DataRequest[_]): ReportDetails = {
    val taxYear = request.userAnswers.get(WhichTaxYearPage(srn))

    ReportDetails(
      pstr = request.schemeDetails.pstr,
      status = EtmpPsrStatus.Compiled,
      periodStart = taxYear.get.from,
      periodEnd = taxYear.get.to,
      schemeName = Some(request.schemeDetails.schemeName),
      psrVersion = None
    )
  }
}
