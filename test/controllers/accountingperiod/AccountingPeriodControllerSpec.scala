/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.accountingperiod

import controllers.ControllerBaseSpec
import config.RefinedTypes.Max3
import forms.DateRangeFormProvider
import models.{DateRange, NormalMode}
import pages.WhichTaxYearPage
import pages.accountingperiod.AccountingPeriodPage
import play.api.libs.json.JsPath
import services.TaxYearService
import views.html.DateRangeView

class AccountingPeriodControllerSpec extends ControllerBaseSpec {

  private val mockTaxYearService = mock[TaxYearService]
  private val userAnswers = defaultUserAnswers.unsafeSet(WhichTaxYearPage(srn), dateRange)

  override def beforeEach(): Unit = reset(mockTaxYearService)

  "AccountingPeriodController" - {

    val form = AccountingPeriodController.form(DateRangeFormProvider(), defaultTaxYear, List())

    val rangeGen = dateRangeWithinRangeGen(dateRange)
    val dateRangeData = rangeGen.sample.value
    val otherDateRangeData = rangeGen.sample.value

    lazy val viewModel = AccountingPeriodController.viewModel(srn, Nil, Max3.ONE, NormalMode)

    lazy val onPageLoad = routes.AccountingPeriodController.onPageLoad(srn, 1, NormalMode)
    lazy val onSubmit = routes.AccountingPeriodController.onSubmit(srn, 1, NormalMode)

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[DateRangeView]
      view(form, viewModel)
    })

    act.like(
      renderPrePopView(onPageLoad, AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRangeData, userAnswers) {
        implicit app => implicit request =>
          val view = injected[DateRangeView]
          val model = AccountingPeriodController.viewModel(srn, List(dateRangeData), Max3.ONE, NormalMode)
          view(form.fill(dateRangeData), model)
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, userAnswers, formData(form, dateRangeData)*))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

    "allow accounting period to be updated" - {
      act.like(
        saveAndContinue(
          onSubmit,
          userAnswers,
          Some(JsPath \ "accountingPeriods"),
          Seq.empty,
          formData(form, dateRangeData)*
        )
      )
    }

    "return a 400 if range intersects" - {
      val userAnswers =
        emptyUserAnswers
          .set(WhichTaxYearPage(srn), dateRange)
          .get
          .set(AccountingPeriodPage(srn, Max3.ONE, NormalMode), otherDateRangeData)
          .get
          .set(AccountingPeriodPage(srn, Max3.TWO, NormalMode), dateRangeData)
          .get

      act.like(invalidForm(onSubmit, userAnswers, formData(form, dateRangeData)*))
    }
  }
}
