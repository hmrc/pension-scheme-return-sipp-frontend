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

import cats.data.NonEmptyList
import controllers.ViewChangeQuestionController._
import forms.RadioListFormProvider
import models.NormalMode
import views.html.RadioListView
import models.TypeOfViewChangeQuestion.{ChangeReturn, ViewReturn}
import org.mockito.ArgumentMatchers.any
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.Future

class ViewChangeQuestionControllerSpec extends ControllerBaseSpec {

  private val dateRange1 = dateRangeGen.sample.value
  private val testAccountingPeriods = Some(NonEmptyList.of(dateRange1))
  private val taxYear = dateRange1.from.getYear
  private lazy val onPageLoad =
    routes.ViewChangeQuestionController.onPageLoad(srn, fbNumber, NormalMode)
  private lazy val onSubmit =
    routes.ViewChangeQuestionController.onSubmit(srn, fbNumber, taxYear, NormalMode)

  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  "ViewChangeQuestionController" - {

    act.like(
      renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
        val view = injected[RadioListView]

        view(
          form(injected[RadioListFormProvider]),
          viewModel(srn, fbNumber, TaxYear(taxYear), NormalMode)
        )
      }.before(
        when(mockSchemeDateService.returnAccountingPeriodsFromEtmp(any(), any())(any(), any()))
          .thenReturn(Future.successful(testAccountingPeriods))
      )
    )

    "ViewReturn data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> ViewReturn.name))
    }

    "ChangeReturn data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> ChangeReturn.name))
    }

  }
}
