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

import controllers.ViewChangeQuestionController.*
import forms.RadioListFormProvider
import models.TypeOfViewChangeQuestion.{ChangeReturn, ViewReturn}
import models.requests.common.YesNo.Yes
import models.requests.psr.EtmpPsrStatus.Submitted
import models.{BasicDetails, FormBundleNumber, NormalMode}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import services.{SchemeDateService, TaxYearService}
import uk.gov.hmrc.time.TaxYear
import views.html.RadioListView

import scala.concurrent.Future

class ViewChangeQuestionControllerSpec extends ControllerBaseSpec {

  private val taxYearDateRange = dateRangeGen.sample.value
  private val basicDetails = BasicDetails(None, taxYearDateRange, Yes, Submitted, Yes)
  private val taxYear = taxYearDateRange.from.getYear
  private lazy val onPageLoad =
    routes.ViewChangeQuestionController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit =
    routes.ViewChangeQuestionController.onSubmit(srn, fbNumber, taxYear, NormalMode)

  private val mockTaxYearService: TaxYearService = mock[TaxYearService]
  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private val mockWhichTaxYearController: WhichTaxYearController = mock[WhichTaxYearController]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[TaxYearService].toInstance(mockTaxYearService),
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[WhichTaxYearController].toInstance(mockWhichTaxYearController)
  )

  "ViewChangeQuestionController" - {

    act.like(
      renderView(onPageLoad, defaultUserAnswers, Seq(("fbNumber", fbNumber))) { implicit app => implicit request =>
        val view = injected[RadioListView]

        view(
          form(injected[RadioListFormProvider]),
          viewModel(srn, fbNumber, TaxYear(taxYear), NormalMode)
        )
      }.before {

        when(mockTaxYearService.fromRequest()(any))
          .thenReturn(taxYearDateRange)

        when(mockSchemeDateService.returnBasicDetails(any, any[FormBundleNumber])(any, any, any))
          .thenReturn(Future.successful(Some(basicDetails)))
      }
    )

    "ViewReturn data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> ViewReturn.entryName))
    }

    "ChangeReturn data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> ChangeReturn.entryName))
    }

    "must redirect to Journey Recovery Controller when FormBundleNumber is missing in session" in {
      val appBuilder = applicationBuilder(Some(defaultUserAnswers))

      running(_ => appBuilder) { app =>
        val request = FakeRequest(onPageLoad)
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "return BAD_REQUEST when invalid data is submitted" - {
      act.like(
        invalidForm(onSubmit, defaultUserAnswers, "value" -> "invalidOption")
      )
    }

    "return BAD_REQUEST when no data is submitted" - {
      act.like(
        invalidForm(onSubmit, defaultUserAnswers)
      )
    }

  }
}
