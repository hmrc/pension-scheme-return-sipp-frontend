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
import controllers.ViewChangePersonalDetailsController.viewModel
import models.{JourneyType, PersonalDetailsUpdateData}
import models.backend.responses.SippPsrJourneySubmissionEtmpResponse
import models.requests.UpdateMemberDetailsRequest
import org.scalatestplus.mockito.MockitoSugar
import pages.UpdatePersonalDetailsQuestionPage
import play.api.inject.bind
import play.api.test.FakeRequest
import services.SaveService
import views.html.ViewChangePersonalDetailsView

import scala.concurrent.Future

class ViewChangePersonalDetailsControllerSpec extends ControllerBaseSpec with MockitoSugar {

  "ViewChangePersonalDetailsController" - {
    lazy val onPageLoad = routes.ViewChangePersonalDetailsController.onPageLoad(srn)
    lazy val onSubmit = routes.ViewChangePersonalDetailsController.onSubmit(srn)

    val request = PersonalDetailsUpdateData(memberDetails, memberDetails, isSubmitted = false)
    val answers = defaultUserAnswers.set(UpdatePersonalDetailsQuestionPage(srn), request).get

    "onPageLoad" - {

      act.like(
        renderView(onPageLoad, answers, addToSession = Seq(("fbNumber", fbNumber))) {
          implicit app => implicit request =>
            val view = injected[ViewChangePersonalDetailsView]
            view(viewModel(srn, schemeName, memberDetails, hiddenSubmit = true))
        }
      )

      act.like {
        val updatedMemberDetails = memberDetails.copy(firstName = "UpdatedFirstName")
        val request = PersonalDetailsUpdateData(memberDetails, updatedMemberDetails, isSubmitted = false)
        val answers = defaultUserAnswers.set(UpdatePersonalDetailsQuestionPage(srn), request).get

        renderView(onPageLoad, answers, addToSession = Seq(("fbNumber", fbNumber))) {
          implicit app => implicit request =>
            val view = injected[ViewChangePersonalDetailsView]
            view(viewModel(srn, schemeName, updatedMemberDetails, hiddenSubmit = false))
        }.withName("must display the view with submit button")
      }

      act.like(
        journeyRecoveryPage(onPageLoad, Some(defaultUserAnswers))
      )
    }

    "onSubmit" - {

      "must updateMemberDetails and save them when data has been updated" in {
        val updatedMemberDetails = memberDetails.copy(firstName = "UpdatedFirstName")
        val requestData = PersonalDetailsUpdateData(memberDetails, updatedMemberDetails, isSubmitted = false)
        val answers = defaultUserAnswers.set(UpdatePersonalDetailsQuestionPage(srn), requestData).get

        val mockPsrConnector = mock[PSRConnector]
        val mockSaveService = mock[SaveService]

        when(mockPsrConnector.updateMemberDetails(any, any, any, any, any, any)(any))
          .thenReturn(Future.successful(SippPsrJourneySubmissionEtmpResponse(fbNumber)))

        when(mockSaveService.setAndSave(any, any, any)(using any, any, any))
          .thenReturn(Future.successful(answers))

        val appBuilder = applicationBuilder(Some(answers))
          .overrides(
            bind[PSRConnector].toInstance(mockPsrConnector),
            bind[SaveService].toInstance(mockSaveService)
          )

        running(_ => appBuilder) { app =>
          val request = FakeRequest(onSubmit).withSession(("fbNumber", fbNumber))
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UpdateAnotherMemberQuestionController.onPageLoad(srn).url

          verify(mockPsrConnector, times(1)).updateMemberDetails(
            any,
            eqTo(JourneyType.Amend),
            eqTo(fbNumber),
            eqTo(None),
            eqTo(None),
            any[UpdateMemberDetailsRequest]
          )(any)

          verify(mockSaveService, times(1)).setAndSave(
            eqTo(answers),
            eqTo(UpdatePersonalDetailsQuestionPage(srn)),
            eqTo(requestData.copy(isSubmitted = true))
          )(using any, any, any)
        }
      }

      "must not call psrConnector.updateMemberDetails when data has not been updated" in {
        val requestData = PersonalDetailsUpdateData(memberDetails, memberDetails, isSubmitted = false)
        val answers = defaultUserAnswers.set(UpdatePersonalDetailsQuestionPage(srn), requestData).get

        val mockPsrConnector = mock[PSRConnector]
        val mockSaveService = mock[SaveService]

        val appBuilder = applicationBuilder(Some(answers))
          .overrides(
            bind[PSRConnector].toInstance(mockPsrConnector),
            bind[SaveService].toInstance(mockSaveService)
          )

        running(_ => appBuilder) { app =>
          val request = FakeRequest(onSubmit).withSession(("fbNumber", fbNumber))
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.UpdateAnotherMemberQuestionController.onPageLoad(srn).url

          verify(mockPsrConnector, never).updateMemberDetails(any, any, any, any, any, any)(any)
          verify(mockSaveService, never).setAndSave(any, any, any)(using any, any, any)
        }
      }

      act.like(
        journeyRecoveryPage(onSubmit, Some(defaultUserAnswers))
      )
    }
  }
}
