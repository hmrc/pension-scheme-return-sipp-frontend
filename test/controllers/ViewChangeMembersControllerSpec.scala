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

import models.backend.responses.MemberDetails
import org.mockito.ArgumentMatchers.any
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import services.ReportDetailsService
import views.html.MemberListView

import java.time.LocalDate
import scala.concurrent.Future

class ViewChangeMembersControllerSpec extends ControllerBaseSpec {

  private val mockMemberDetails =
    Range
      .inclusive(1, 500)
      .toList
      .map(i => MemberDetails(s"first-name-$i", None, s"last-name-$i", None, Some("test"), LocalDate.now()))

  private val mockReportDetailsService = mock[ReportDetailsService]
  override val additionalBindings: List[GuiceableModule] = List(
    bind[ReportDetailsService].toInstance(mockReportDetailsService)
  )

  override def beforeEach(): Unit = {
    reset(mockReportDetailsService)

    when(mockReportDetailsService.getMemberDetails(any(), any())(any()))
      .thenReturn(
        Future.successful(
          mockMemberDetails
        )
      )
  }

  "ViewChangeMembersController" - {
    val stubMessages: Messages = stubMessagesApi(
      messages = Map(
        "en" ->
          Map(
            "searchMembers.removeNotification.title" -> "Scheme member has been removed",
            "searchMembers.removeNotification.paragraph" -> "You must submit the declaration on the task list for any changes made."
          )
      )
    ).preferred(FakeRequest())

    lazy val viewModel = ViewChangeMembersController.viewModel(
      srn,
      1,
      mockMemberDetails
    )(stubMessages)

    lazy val onPageLoad = routes.ViewChangeMembersController.onPageLoad(srn, 1)

    act.like(renderView(onPageLoad, addToSession = Seq(("fbNumber", fbNumber))) { implicit app => implicit request =>
      val view = injected[MemberListView]
      view(viewModel)
    }.withName("task list renders OK"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

  }
}
