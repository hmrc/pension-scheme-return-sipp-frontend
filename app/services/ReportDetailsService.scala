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

import config.Constants
import connectors.PSRConnector
import models.FormBundleNumber
import models.SchemeId.Pstr
import models.backend.responses.{MemberDetails, PsrAssetCountsResponse}
import models.requests.DataRequest
import models.requests.common.YesNo
import models.requests.psr.{EtmpPsrStatus, ReportDetails}
import pages.AssetsHeldPage
import play.api.mvc.AnyContent
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportDetailsService @Inject() (
  taxYearService: TaxYearService,
  connector: PSRConnector
)(implicit ec: ExecutionContext) {

  def getAssetCounts(fbNumber: Option[FormBundleNumber], taxYear: Option[String], version: Option[String], pstr: Pstr)(
    implicit
    hc: HeaderCarrier,
    request: DataRequest[AnyContent]
  ): Future[Option[PsrAssetCountsResponse]] =
    connector.getPsrAssetCounts(pstr.value, optFbNumber = fbNumber.map(_.value), taxYear, version)

  def getMemberDetails(fbNumber: FormBundleNumber, pstr: Pstr)(implicit
    hc: HeaderCarrier,
    request: DataRequest[AnyContent]
  ): Future[List[MemberDetails]] =
    connector.getMemberDetails(pstr.value, optFbNumber = Some(fbNumber.value), None, None).map(_.members)

  def getReportDetails()(implicit request: DataRequest[?]): ReportDetails = {
    val version = request.session
      .get(Constants.version)

    val dateRange = taxYearService.fromRequest()

    val memberTransaction = request.userAnswers.get(AssetsHeldPage(request.srn)).getOrElse(true)

    ReportDetails(
      pstr = request.schemeDetails.pstr,
      status = EtmpPsrStatus.Compiled,
      periodStart = dateRange.from,
      periodEnd = dateRange.to,
      schemeName = Some(request.schemeDetails.schemeName),
      version = version,
      memberTransactions = YesNo(memberTransaction)
    )
  }

}
