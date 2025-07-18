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
import controllers.accountingperiod.RemoveAccountingPeriodController.*
import config.RefinedTypes.Max3
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.accountingperiod.AccountingPeriodPage
import views.html.YesNoPageView

class RemoveAccountingPeriodControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.RemoveAccountingPeriodController.onPageLoad(srn, 1, NormalMode)
  private lazy val onSubmit = routes.RemoveAccountingPeriodController.onSubmit(srn, 1, NormalMode)

  private val period = dateRangeGen.sample.value
  private val otherPeriod = dateRangeGen.sample.value

  private val userAnswers = defaultUserAnswers
    .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), period)
    .unsafeSet(AccountingPeriodPage(srn, Max3.TWO, NormalMode), otherPeriod)

  "RemoveSchemeBankAccountController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, Max3.ONE, period, NormalMode)
      )
    })

    act.like(redirectToPage(onPageLoad, controllers.routes.JourneyRecoveryController.onPageLoad()))
    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))
    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
