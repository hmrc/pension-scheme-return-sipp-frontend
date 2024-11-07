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
import controllers.RemoveMemberController.{form, viewModel}
import forms.YesNoPageFormProvider
import models.UserAnswers
import models.backend.responses.{MemberDetails, SippPsrJourneySubmissionEtmpResponse}
import org.mockito.ArgumentCaptor
import org.scalatestplus.mockito.MockitoSugar
import pages.{RemoveMemberPage, RemoveMemberQuestionPage}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import services.{SaveService, SchemeDetailsService}
import views.html.YesNoPageView
import cats.syntax.option.*

import java.time.LocalDate
import scala.concurrent.Future

class RemoveMemberControllerSpec extends ControllerBaseSpec with MockitoSugar {

  private lazy val onPageLoad = routes.RemoveMemberController.onPageLoad(srn)
  private lazy val onSubmit = routes.RemoveMemberController.onSubmit(srn)

  private val mockPsrConnector: PSRConnector = mock[PSRConnector]
  private val mockSaveService: SaveService = mock[SaveService]
  private val mockSchemeDetailsService: SchemeDetailsService = mock[SchemeDetailsService]
  private val dob: LocalDate = LocalDate.of(2000, 1, 1)
  private val member: MemberDetails = MemberDetails(
    firstName = "Name",
    lastName = "Surname",
    nino = Some("AB123456C"),
    reasonNoNINO = None,
    dateOfBirth = dob
  )
  val updated: UserAnswers = defaultUserAnswers.set(RemoveMemberPage(srn), member).get

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PSRConnector].toInstance(mockPsrConnector),
    bind[SaveService].toInstance(mockSaveService),
    bind[SchemeDetailsService].toInstance(mockSchemeDetailsService)
  )

  override def beforeEach(): Unit =  {
    reset(mockPsrConnector, mockSaveService, mockSchemeDetailsService)
    when(mockSchemeDetailsService.getMinimalSchemeDetails(any, any)(any, any))
      .thenReturn(Future.successful(minimalSchemeDetails.some))
  }

  "RemoveMemberController" - {

    "onPageLoad" - {

      act.like(
        renderView(onPageLoad, updated) { implicit app => implicit request =>
          injected[YesNoPageView]
            .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, member, schemeName))
        }
      )

      act.like(
        renderPrePopView(onPageLoad, RemoveMemberQuestionPage(srn), true, updated) { implicit app => implicit request =>
          injected[YesNoPageView]
            .apply(
              form(injected[YesNoPageFormProvider]).fill(true),
              viewModel(srn, member, schemeName)
            )
        }
      )

      act.like(
        journeyRecoveryPage(onPageLoad, Some(defaultUserAnswers))
      )
    }

    "onSubmit" - {

      "must save the answer and delete the member when the user answers Yes" in {
        val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

        when(mockSaveService.save(captor.capture())(any, any)).thenReturn(Future.successful(()))
        when(mockPsrConnector.deleteMember(any, any, any, any, any, any)(any))
          .thenReturn(Future.successful(SippPsrJourneySubmissionEtmpResponse(fbNumber)))

        val appBuilder = applicationBuilder(Some(updated))

        running(_ => appBuilder) { app =>
          val request =
            FakeRequest(onSubmit).withFormUrlEncodedBody("value" -> "true").withSession("fbNumber" -> fbNumber)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER

          verify(mockSaveService, times(1)).save(any)(any, any)
          verify(mockPsrConnector, times(1)).deleteMember(any, any, any, any, any, any)(any)
          verify(mockSchemeDetailsService, times(1)).getMinimalSchemeDetails(any, any)(any, any)

          val updatedAnswers = captor.getValue
          updatedAnswers.get(RemoveMemberQuestionPage(srn)).value mustEqual true
        }
      }

      "must save the answer and not delete the member when the user answers No" in {
        val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

        when(mockSaveService.save(captor.capture())(any, any)).thenReturn(Future.successful(()))

        val appBuilder = applicationBuilder(Some(updated))

        running(_ => appBuilder) { app =>
          val request =
            FakeRequest(onSubmit).withFormUrlEncodedBody("value" -> "false").withSession("fbNumber" -> fbNumber)
          val result = route(app, request).value

          status(result) mustEqual SEE_OTHER

          verify(mockSaveService, times(1)).save(any)(any, any)
          verify(mockPsrConnector, never).deleteMember(any, any, any, any, any, any)(any)
          verify(mockSchemeDetailsService, times(1)).getMinimalSchemeDetails(any, any)(any, any)

          val updatedAnswers = captor.getValue
          updatedAnswers.get(RemoveMemberQuestionPage(srn)).value mustEqual false
        }
      }

      act.like(invalidForm(onSubmit, updated, Seq(("fbNumber", fbNumber))))

      act.like(
        journeyRecoveryPage(onSubmit, Some(defaultUserAnswers))
      )
    }
  }
}
