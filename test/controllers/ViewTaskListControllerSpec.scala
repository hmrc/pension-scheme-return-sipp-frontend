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

package controllers

import connectors.PSRConnector
import models.{DateRange, UserAnswers}
import models.backend.responses.{AccountingPeriodDetails, PSRSubmissionResponse, PsrAssetDeclarationsResponse, Versions}
import models.requests.{AllowedAccessRequest, DataRequest}
import models.requests.common.YesNo
import models.requests.psr.EtmpPsrStatus.Submitted
import models.requests.psr.ReportDetails
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import services.view.TaskListViewModelService
import services.view.TaskListViewModelService.{SchemeSectionsStatus, ViewMode}
import uk.gov.hmrc.time.TaxYear
import views.html.TaskListView

import scala.concurrent.Future

class ViewTaskListControllerSpec extends ControllerBaseSpec {

  private val schemeDateRange: DateRange = DateRange.from(TaxYear(earliestDate.getYear))
  private val url: String = s"http://localhost:10701/pension-scheme-return/${srn.value}/overview"

  private val mockConnector = mock[PSRConnector]
  private val mockReportDetails: ReportDetails =
    ReportDetails("test", Submitted, earliestDate, latestDate, None, None, YesNo.Yes)
  private val mockAccPeriodDetails: AccountingPeriodDetails =
    AccountingPeriodDetails(None, accountingPeriods = None)

  private val defaultUserAnswers: UserAnswers = UserAnswers("id")
  val allowedAccessRequest: AllowedAccessRequest[AnyContent] =
    allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val dataRequest: DataRequest[AnyContent] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  override val additionalBindings: List[GuiceableModule] = List(
    bind[PSRConnector].toInstance(mockConnector)
  )

  val versions: Versions = Versions(None, None, None, None, None, None, None)

  val submissionResponse = PSRSubmissionResponse(
    mockReportDetails,
    Some(mockAccPeriodDetails),
    None,
    None,
    None,
    None,
    None,
    None,
    versions
  )

  val assetDeclarationsResponse = PsrAssetDeclarationsResponse(
    armsLengthLandOrProperty = Some(YesNo.No),
    interestInLandOrProperty = Some(YesNo.No),
    tangibleMoveableProperty = Some(YesNo.No),
    outstandingLoans = Some(YesNo.No),
    unquotedShares = Some(YesNo.No),
    assetFromConnectedParty = Some(YesNo.No)
  )

  override def beforeEach(): Unit = {
    reset(mockConnector)
    when(mockConnector.getPSRSubmission(any, any, any, any)(any, any))
      .thenReturn(
        Future.successful(submissionResponse)
      )

    when(mockConnector.getPsrAssetDeclarations(any, any, any, any)(any, any))
      .thenReturn(
        Future.successful(assetDeclarationsResponse)
      )
  }

  "ViewTaskListController" - {

    val schemeSectionsStatus = SchemeSectionsStatus.fromPSRSubmission(submissionResponse, assetDeclarationsResponse)

    val service = TaskListViewModelService(ViewMode.View)

    lazy val viewModel = service.viewModel(
      srn,
      schemeName,
      schemeDateRange.from,
      schemeDateRange.to,
      url,
      schemeSectionsStatus,
      fbNumber
    )
    lazy val onPageLoad = routes.ViewTaskListController.onPageLoad(srn, None)

    act.like(renderView(onPageLoad, addToSession = Seq(("fbNumber", fbNumber))) { implicit app => implicit request =>
      val view = injected[TaskListView]
      view(viewModel)
    }.withName("task list renders OK"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))
  }
}
