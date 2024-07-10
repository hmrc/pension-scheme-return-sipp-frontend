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
import controllers.ViewTaskListController.SchemeDetailsItems
import models.DateRange
import models.backend.responses.{AccountingPeriod, AccountingPeriodDetails, PSRSubmissionResponse}
import models.requests.psr.EtmpPsrStatus.Compiled
import models.requests.psr.ReportDetails
import org.mockito.ArgumentMatchers.any
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import uk.gov.hmrc.time.TaxYear
import views.html.TaskListView

import scala.concurrent.Future

class ViewTaskListControllerSpec extends ControllerBaseSpec {

  private val schemeDateRange: DateRange = DateRange.from(TaxYear(earliestDate.getYear))
  private val url: String = s"http://localhost:10701/pension-scheme-return/${srn.value}/overview"

  private val mockConnector = mock[PSRConnector]
  private val mockReportDetails: ReportDetails = ReportDetails("test", Compiled, earliestDate, latestDate, None, None)
  private val mockAccPeriodDetails: AccountingPeriodDetails =
    AccountingPeriodDetails(None, accountingPeriods = List.empty[AccountingPeriod])

  override val additionalBindings: List[GuiceableModule] = List(
    bind[PSRConnector].toInstance(mockConnector)
  )

  override def beforeEach(): Unit = {
    reset(mockConnector)
    when(mockConnector.getPSRSubmission(any(), any(), any(), any())(any()))
      .thenReturn(
        Future.successful(
          PSRSubmissionResponse(mockReportDetails, mockAccPeriodDetails, None, None, None, None, None, None)
        )
      )
  }

  "ViewTaskListController" - {

    val visibleItems = SchemeDetailsItems(
      isLandOrPropertyInterestPopulated = false,
      isLandOrPropertyArmsLengthPopulated = false,
      isTangiblePropertyPopulated = false,
      isSharesPopulated = false,
      isAssetsPopulated = false,
      isLoansPopulated = false
    )

    lazy val viewModel = ViewTaskListController.viewModel(
      srn,
      schemeName,
      schemeDateRange.from,
      schemeDateRange.to,
      url,
      visibleItems,
      fbNumber
    )
    lazy val onPageLoad = routes.ViewTaskListController.onPageLoad(srn)

    act.like(renderView(onPageLoad, addToSession = Seq(("fbNumber", fbNumber))) { implicit app => implicit request =>
      val view = injected[TaskListView]
      view(viewModel)
    }.withName("task list renders OK"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

  }
}
