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

import controllers.ViewChangeDoesMemberHasNinoController.{form, viewModel}
import forms.YesNoPageFormProvider
import models.PersonalDetailsUpdateData
import pages.{UpdatePersonalDetailsMemberHasNinoQuestionPage, UpdatePersonalDetailsQuestionPage}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import views.html.YesNoPageView

class ViewChangeDoesMemberHasNinoControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ViewChangeDoesMemberHasNinoController.onPageLoad(srn)
  private lazy val onSubmit = routes.ViewChangeDoesMemberHasNinoController.onSubmit(srn)

  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  "ViewChangeDoesMemberHasNinoControllerSpec" - {

    val request = PersonalDetailsUpdateData(memberDetails, memberDetails, isSubmitted = true)
    val answers = defaultUserAnswers.set(UpdatePersonalDetailsQuestionPage(srn), request).get

    act.like(renderView(onPageLoad, answers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, memberDetails))
    })

    act.like(renderPrePopView(onPageLoad, UpdatePersonalDetailsMemberHasNinoQuestionPage(srn), true, answers) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]), // Not loading with true/false for pre-population
            viewModel(srn, memberDetails)
          )
    })

    act.like(redirectNextPage(onSubmit, answers, "value" -> "true"))

    act.like(redirectNextPage(onSubmit, answers, "value" -> "false"))

    act.like(saveAndContinue(onSubmit, answers, "value" -> "true"))

    act.like(invalidForm(onSubmit, answers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

  }
}
