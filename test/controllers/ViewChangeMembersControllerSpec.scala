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

import forms.TextFormProvider
import models.backend.responses.MemberDetails
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.Helpers.*
import services.ReportDetailsService
import views.html.MemberListView

import java.time.LocalDate
import scala.concurrent.Future

class ViewChangeMembersControllerSpec extends ControllerBaseSpec {

  private val mockMemberDetails =
    Range
      .inclusive(1, 500)
      .toList
      .map(i => MemberDetails(s"first-name-$i", s"last-name-$i", None, Some("test"), LocalDate.now()))

  private val mockReportDetailsService = mock[ReportDetailsService]
  private val textFormProvider = new TextFormProvider()

  override val additionalBindings: List[GuiceableModule] = List(
    bind[ReportDetailsService].toInstance(mockReportDetailsService)
  )

  override def beforeEach(): Unit = {
    reset(mockReportDetailsService)

    when(mockReportDetailsService.getMemberDetails(any, any)(any))
      .thenReturn(
        Future.successful(
          mockMemberDetails
        )
      )
  }

  "ViewChangeMembersController" - {

    lazy val searchForm = textFormProvider("").fill("")
    lazy val viewModel = ViewChangeMembersController.viewModel(
      srn,
      1,
      mockMemberDetails,
      None,
      displayDeleteSuccess = false,
      displayUpdateSuccess = false
    )(stubMessages())

    lazy val onPageLoad = routes.ViewChangeMembersController.onPageLoad(srn, 1, None)

    act.like(renderView(onPageLoad, addToSession = Seq(("fbNumber", fbNumber))) { implicit app => implicit request =>
      val view = injected[MemberListView]
      view(viewModel, searchForm)
    }.withName("task list renders OK"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))
  }
}
