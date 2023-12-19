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

package controllers
import controllers.BasicDetailsCheckYourAnswersController._
import models.{CheckMode, NormalMode}
import views.html.CheckYourAnswersView

class BasicDetailsCheckYourAnswersControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, NormalMode)
  private lazy val onSubmitInCheckMode = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, CheckMode)

  "BasicDetailsCheckYourAnswersPageController" - {

    val pensionSchemeId = pensionSchemeIdGen.sample.value

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[CheckYourAnswersView].apply(
        viewModel(
          srn,
          NormalMode,
          individualDetails.fullName,
          psaId.value,
          defaultSchemeDetails,
          None
        )
      )
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}