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
import cats.syntax.option.catsSyntaxOptionId

import java.time.LocalDate
import scala.concurrent.Future

class ViewChangeMembersControllerSpec extends ControllerBaseSpec {

  private val mockMemberDetails =
    (1 to 500)
      .map(i => MemberDetails(s"first-name-$i", s"last-name-$i", None, Some("test"), LocalDate.now()))
      .toList

  private val mockReportDetailsService = mock[ReportDetailsService]
  private val textFormProvider = TextFormProvider()

  override val additionalBindings: List[GuiceableModule] = List(
    bind[ReportDetailsService].toInstance(mockReportDetailsService)
  )

  override def beforeEach(): Unit = {
    reset(mockReportDetailsService)
    when(mockReportDetailsService.getMemberDetails(any, any)(any, any))
      .thenReturn(Future.successful(mockMemberDetails))
  }

  "ViewChangeMembersController" - {

    lazy val viewModel = ViewChangeMembersController.viewModel(srn, 1, mockMemberDetails, None)(stubMessages())
    lazy val searchForm = textFormProvider("").fill("")

    lazy val onPageLoad = routes.ViewChangeMembersController.onPageLoad(srn, 1, None)
    lazy val onSearch = routes.ViewChangeMembersController.onSearch(srn)
    lazy val onSearchPageLoad = routes.ViewChangeMembersController.onPageLoad(srn, 1, Some("search text"))
    lazy val onSearchClear = routes.ViewChangeMembersController.onSearchClear(srn)
    lazy val redirectToUpdateMember = routes.ViewChangeMembersController.redirectToUpdateMemberDetails(
      srn,
      "updatedFirst",
      "updatedLast",
      "01/01/2022",
      "nino".some,
      None
    )
    lazy val afterUpdateMember = routes.ViewChangePersonalDetailsController.onPageLoad(srn)
    lazy val redirectToRemoveMember = routes.ViewChangeMembersController.redirectToRemoveMember(
      srn,
      "first",
      "last",
      "01/01/2021",
      "nino".some
    )
    lazy val removeMember = routes.RemoveMemberController.onPageLoad(srn)

    val addToSession = Seq("fbNumber" -> fbNumber)

    act.like(
      renderView(onPageLoad, addToSession = addToSession) { implicit app => implicit request =>
        val view = injected[MemberListView]
        view(viewModel, searchForm)
      }.withName("task list renders OK")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      redirectToPage(onSearch, onSearchPageLoad, addToSession, "value" -> "search text")
        .withName("Redirect to the search result if a search value is provided")
    )

    act.like(
      redirectToPage(onSearch, onPageLoad, addToSession)
        .withName("Redirect to main page if no search text is provided")
    )

    act.like(redirectToPage(onSearchClear, onPageLoad, addToSession))

    act.like(
      redirectToPage(redirectToUpdateMember, afterUpdateMember, addToSession)
        .withName("Redirect to personal details page on member update")
    )

    act.like(
      redirectToPage(redirectToRemoveMember, removeMember, addToSession)
        .withName("Redirect to remove member page")
    )
  }
}
