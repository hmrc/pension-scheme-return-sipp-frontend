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

import controllers.JourneyContributionsHeldController.{form, viewModel}
import forms.YesNoPageFormProvider
import models.Journey.{
  ArmsLengthLandOrProperty,
  AssetFromConnectedParty,
  InterestInLandOrProperty,
  OutstandingLoans,
  TangibleMoveableProperty,
  UnquotedShares
}
import models.{Journey, NormalMode}
import pages.JourneyContributionsHeldPage
import views.html.YesNoPageView

class JourneyContributionsHeldControllerSpec extends ControllerBaseSpec {

  "JourneyContributionsHeldController - InterestInLandOrProperty" - {
    new TestScope(InterestInLandOrProperty)
  }

  "JourneyContributionsHeldController - ArmsLengthLandOrProperty" - {
    new TestScope(ArmsLengthLandOrProperty)
  }

  "JourneyContributionsHeldController - TangibleMoveableProperty" - {
    new TestScope(TangibleMoveableProperty)
  }

  "JourneyContributionsHeldController - OutstandingLoans" - {
    new TestScope(OutstandingLoans)
  }

  "JourneyContributionsHeldController - UnquotedShares" - {
    new TestScope(UnquotedShares)
  }

  "JourneyContributionsHeldController - AssetFromConnectedParty" - {
    new TestScope(AssetFromConnectedParty)
  }

  class TestScope(journey: Journey) {
    private lazy val onPageLoad =
      controllers.routes.JourneyContributionsHeldController.onPageLoad(srn, journey, NormalMode)
    private lazy val onSubmit =
      controllers.routes.JourneyContributionsHeldController.onSubmit(srn, journey, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider], journey), viewModel(srn, journey, NormalMode, schemeName))
    })

    act.like(renderPrePopView(onPageLoad, JourneyContributionsHeldPage(srn, journey), true) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider], journey).fill(true),
            viewModel(srn, journey, NormalMode, schemeName)
          )
    })

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

  }
}
