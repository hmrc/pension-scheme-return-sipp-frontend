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

import connectors.PSRConnector
import models.SchemeId.Pstr
import models.backend.responses.MemberDetails
import models.requests.DataRequest
import models.requests.psr.{EtmpPsrStatus, ReportDetails}
import models.{DateRange, FormBundleNumber, JourneyType, SchemeDetailsItems, VersionTaxYear}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportDetailsService @Inject()(
  schemeDateService: SchemeDateService,
  taxYearService: TaxYearService,
  connector: PSRConnector
)(implicit ec: ExecutionContext) {

  def getSchemeDetailsItems(fbNumber: FormBundleNumber, pstr: Pstr)(
    implicit hc: HeaderCarrier
  ): Future[SchemeDetailsItems] =
    connector
      .getPSRSubmission(pstr.value, optFbNumber = Some(fbNumber.value), None, None)
      .map(SchemeDetailsItems.fromPSRSubmission)

  def getMemberDetails(fbNumber: FormBundleNumber, pstr: Pstr)(
    implicit hc: HeaderCarrier
  ): Future[List[MemberDetails]] =
    connector.getMemberDetails(pstr.value, optFbNumber = Some(fbNumber.value), None, None).map(_.members)

  def deleteMemberDetail(fbNumber: FormBundleNumber, pstr: Pstr, memberDetails: MemberDetails)(
    implicit hc: HeaderCarrier
  ): Future[Unit] =
    connector.deleteMember(pstr.value, JourneyType.Amend, optFbNumber = Some(fbNumber.value), None, None, memberDetails)

  def getReportDetails()(implicit request: DataRequest[_]): ReportDetails = {
    val (version, dateRange) = VersionTaxYear
      .optFromSession(request.session)
      .map(versionTaxYear => Some(versionTaxYear.version) -> versionTaxYear.taxYearDateRange)
      .getOrElse(None -> DateRange.from(taxYearService.current))

    ReportDetails(
      pstr = request.schemeDetails.pstr,
      status = EtmpPsrStatus.Compiled,
      periodStart = dateRange.from,
      periodEnd = dateRange.to,
      schemeName = Some(request.schemeDetails.schemeName),
      version = version
    )
  }

}
