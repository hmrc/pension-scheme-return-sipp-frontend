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
import views.html.CountedTextAreaView

import scala.util.Random

class ChangeMembersNoNinoReasonControllerSpec extends ControllerBaseSpec {
  private lazy val onPageLoad: Call = routes.ChangeMembersNoNinoReasonController.onPageLoad(srn)
  private lazy val onSubmit: Call = routes.ChangeMembersNoNinoReasonController.onSubmit(srn)

  private val validForm = List("value" -> "test reason")
  private val nameTooLong = List("value" -> Random.nextString(161))

  "ChangeMembersNoNinoReasonController" - {
    val request = PersonalDetailsUpdateData(memberDetails, memberDetails, isSubmitted = true)
    val answers = defaultUserAnswers.set(UpdatePersonalDetailsQuestionPage(srn), request).get

    act.like(renderView(onPageLoad, answers) { implicit app => implicit request =>
      injected[CountedTextAreaView]
        .apply(
          ChangeMembersNoNinoReasonController.form(injected[TextFormProvider]),
          ChangeMembersNoNinoReasonController.viewModel(srn, memberDetails)
        )
    })

    act.like(updateAndSaveAndContinue(onSubmit, answers, validForm*))
    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))
    act.like(invalidForm(onSubmit, answers))
    act.like(invalidForm(onSubmit, answers, nameTooLong*))
    act.like(journeyRecoveryPage(onPageLoad))
  }
}
