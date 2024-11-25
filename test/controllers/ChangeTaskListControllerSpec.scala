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
import models.backend.responses.{AccountingPeriodDetails, PSRSubmissionResponse, Versions}
import models.requests.psr.EtmpPsrStatus.Compiled
import models.requests.psr.ReportDetails
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.ReportDetailsService
import services.view.TaskListViewModelService
import services.view.TaskListViewModelService.{SchemeSectionsStatus, SectionStatus, ViewMode}
import views.html.TaskListView

import java.time.LocalDate
import scala.concurrent.Future

class ChangeTaskListControllerSpec extends ControllerBaseSpec {
  private val session: Seq[(String, String)] =
    Seq(("fbNumber", "fbNumber"))

  private val mockReportDetailsService = mock[ReportDetailsService]
  private val mockPsrConnector = mock[PSRConnector]

  val versions: Versions = Versions(None, None, None, None, None, None, None)

  private val mockAccPeriodDetails: AccountingPeriodDetails =
    AccountingPeriodDetails(None, accountingPeriods = None)

  val mockReportDetails: ReportDetails = ReportDetails("test", Compiled, earliestDate, latestDate, None, None)

  val response = PSRSubmissionResponse(
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

  override val additionalBindings: List[GuiceableModule] = List(
    bind[PSRConnector].toInstance(mockPsrConnector),
    bind[ReportDetailsService].toInstance(mockReportDetailsService)
  )

  "ChangeTaskListController" - {

    val schemeSectionsStatus = SchemeSectionsStatus(
      SectionStatus.Declared,
      SectionStatus.Empty,
      SectionStatus.Empty,
      SectionStatus.Empty,
      SectionStatus.Empty,
      SectionStatus.Empty,
      SectionStatus.Empty
    )

    val vc = TaskListViewModelService(ViewMode.Change)

    lazy val viewModel = vc.viewModel(
      srn,
      schemeName,
      LocalDate.of(1970, 4, 6),
      LocalDate.of(1971, 4, 5),
      s"http://localhost:10701/pension-scheme-return/${srn.value}/overview",
      schemeSectionsStatus,
      ""
    )
    lazy val onPageLoad = routes.ChangeTaskListController.onPageLoad(srn)

    act.like(renderView(onPageLoad, defaultUserAnswers, session) { implicit app => implicit request =>
      val view = injected[TaskListView]
      view(viewModel)
    }.before {
      when(mockPsrConnector.getPSRSubmission(any, any, any, any)(any))
        .thenReturn(Future.successful(response))
    }.withName("change task list renders OK"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))
  }

}
