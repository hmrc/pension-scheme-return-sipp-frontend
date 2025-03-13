/*
 * Copyright 2025 HM Revenue & Customs
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

import models.Journey.InterestInLandOrProperty
import models.JourneyType.Standard
import viewmodels.models.{PageViewModel, ResultViewModel}
import viewmodels.implicits.*
import views.html.ResultView

class RemoveFileSuccessControllerSpec extends ControllerBaseSpec {
  "RemoveFileSuccessController" - {
    lazy val onPageLoad = routes.RemoveFileSuccessController.onPageLoad(srn, InterestInLandOrProperty, Standard)
    lazy val nextPage = routes.JourneyContributionsHeldController.onPageLoad(srn, InterestInLandOrProperty, Standard)

    act.like(
      renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
        val view = injected[ResultView]
        view(
          PageViewModel(
            "fileDelete.success.heading",
            "fileDelete.success.heading",
            ResultViewModel("site.continue", None, nextPage.url)
          )
        )
      }
    )
  }
}
