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

import views.html.ViewChangePersonalDetailsView
import ViewChangePersonalDetailsController.viewModel
import models.requests.UpdateMemberDetailsRequest
import pages.UpdatePersonalDetailsQuestionPage

class ViewChangePersonalDetailsControllerSpec extends ControllerBaseSpec {
  "ViewChangePersonalDetailsController" - {
    lazy val onPageLoad = routes.ViewChangePersonalDetailsController.onPageLoad(srn)
    val request = UpdateMemberDetailsRequest(memberDetails, memberDetails)
    val answers = defaultUserAnswers.set(UpdatePersonalDetailsQuestionPage(srn), request).get

    act.like(
      renderView(onPageLoad, answers, addToSession = Seq(("fbNumber", fbNumber))) { implicit app => implicit request =>
        val view = injected[ViewChangePersonalDetailsView]
        view(viewModel(srn, schemeName, memberDetails))
      }
    )

  }
}
