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
import models.PersonalDetailsUpdateData
import pages.UpdatePersonalDetailsQuestionPage
import play.api.mvc.Call
import views.html.TextInputView

class ChangeMembersNinoControllerSpec extends ControllerBaseSpec {
  private lazy val onPageLoad: Call = routes.ChangeMembersNinoController.onPageLoad(srn)
  private lazy val onSubmit: Call = routes.ChangeMembersNinoController.onSubmit(srn)

  private val validForm = List("value" -> nino.value)
  private val badNino = List("value" -> "T_XVB%")

  "ChangeMembersNinoController" - {
    val request = PersonalDetailsUpdateData(memberDetails, memberDetails, isSubmitted = true)
    val answers = defaultUserAnswers.set(UpdatePersonalDetailsQuestionPage(srn), request).get

    act.like(renderView(onPageLoad, answers) { implicit app => implicit request =>
      injected[TextInputView]
        .apply(
          ChangeMembersNinoController.form(injected[TextFormProvider]),
          ChangeMembersNinoController.viewModel(srn)
        )
    })

    act.like(setAndSaveAndContinue(onSubmit, answers, validForm: _*))
    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))
    act.like(invalidForm(onSubmit))
    act.like(invalidForm(onSubmit, badNino: _*))
  }
}
