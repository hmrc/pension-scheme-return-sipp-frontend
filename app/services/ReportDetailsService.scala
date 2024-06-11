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

package services

import models.SchemeId.Srn
import models.requests.DataRequest
import models.requests.psr.{EtmpPsrStatus, ReportDetails}
import uk.gov.hmrc.time.TaxYear

import javax.inject.Inject

class ReportDetailsService @Inject()(schemeDateService: SchemeDateService, taxYearService: TaxYearService) {

  def getReportDetails(srn: Srn)(implicit request: DataRequest[_]): ReportDetails = {
    val taxYear = schemeDateService
      .returnAccountingPeriods(srn)
      .map(_.toList.maxBy(_._1.from))
      .map(_._1.from.getYear)
      .map(TaxYear)
      .getOrElse(taxYearService.current)

    ReportDetails(
      pstr = request.schemeDetails.pstr,
      status = EtmpPsrStatus.Compiled,
      periodStart = taxYear.starts,
      periodEnd = taxYear.finishes,
      schemeName = Some(request.schemeDetails.schemeName),
      psrVersion = None
    )
  }

}
