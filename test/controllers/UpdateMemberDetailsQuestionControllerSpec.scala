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

import controllers.UpdateMemberDetailsQuestionController.{form, viewModel}
import forms.YesNoPageFormProvider
import pages.UpdateMemberDetailsQuestionPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import views.html.YesNoPageView

class UpdateMemberDetailsQuestionControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.UpdateMemberDetailsQuestionController.onPageLoad(srn)
  private lazy val onSubmit = routes.UpdateMemberDetailsQuestionController.onSubmit(srn)

  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  "UpdateMemberDetailsQuestionController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn))
    })

    act.like(renderPrePopView(onPageLoad, UpdateMemberDetailsQuestionPage(srn), true) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn)
          )
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))

    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

  }
}