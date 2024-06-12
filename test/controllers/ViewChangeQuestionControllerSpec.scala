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

import controllers.ViewChangeQuestionController._
import forms.RadioListFormProvider
import models.NormalMode
import views.html.RadioListView
import models.TypeOfViewChangeQuestion.{ChangeReturn, ViewReturn}

class ViewChangeQuestionControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad =
    routes.ViewChangeQuestionController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit =
    routes.ViewChangeQuestionController.onSubmit(srn, NormalMode)

  "ViewChangeQuestionController" - {

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      val view = injected[RadioListView]

      view(
        form(injected[RadioListFormProvider]),
        viewModel(srn, NormalMode)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "ViewReturn data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> ViewReturn.name))
    }

    "ChangeReturn data is submitted" - {
      act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> ChangeReturn.name))
    }

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
