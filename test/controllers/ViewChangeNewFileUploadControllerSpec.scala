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

import controllers.ViewChangeNewFileUploadController.{form, viewModel}
import forms.UploadNewFileQuestionPageFormProvider
import models.Journey.{ArmsLengthLandOrProperty, InterestInLandOrProperty, OutstandingLoans, TangibleMoveableProperty, UnquotedShares}
import models.{FormBundleNumber, Journey, UserAnswers}
import pages.TaskListStatusPage
import views.html.ViewChangeUploadNewFileQuestionView

class ViewChangeNewFileUploadControllerSpec extends ControllerBaseSpec {

  "ViewChangeNewFileUploadControllerSpec - InterestInLandOrProperty" - {
    new TestScope(InterestInLandOrProperty)
  }

  "ViewChangeNewFileUploadControllerSpec - TangibleMoveableProperty" - {
    new TestScope(TangibleMoveableProperty)
  }

  "ViewChangeNewFileUploadControllerSpec - OutstandingLoans" - {
    new TestScope(OutstandingLoans)
  }

  "ViewChangeNewFileUploadControllerSpec - ArmsLengthLandOrProperty" - {
    new TestScope(ArmsLengthLandOrProperty)
  }

  "ViewChangeNewFileUploadControllerSpec - UnquotedShares" - {
    new TestScope(UnquotedShares)
  }

  class TestScope(journey: Journey) {

    private lazy val onPageLoad = controllers.routes.ViewChangeNewFileUploadController.onPageLoad(srn, journey)

    private val answers: UserAnswers = defaultUserAnswers
      .unsafeSet(
        TaskListStatusPage(srn, journey),
        TaskListStatusPage.Status(completedWithNo = true, 1)
      )

    act.like(renderView(onPageLoad, answers, Seq(("fbNumber", fbNumber))) { implicit app => implicit request =>
      injected[ViewChangeUploadNewFileQuestionView]
        .apply(
          form(injected[UploadNewFileQuestionPageFormProvider]),
          viewModel(srn, journey, FormBundleNumber(fbNumber))
        )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    //act.like(saveAndContinue(onSubmit, "value" -> "true")) TODO: fix me after integrating with View Change Tasklist

    //act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))  TODO: fix me after integrating with View Change Tasklist

  }
}
