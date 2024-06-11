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

import cats.effect.IO
import controllers.NewFileUploadController.{form, viewModel}
import forms.UploadNewFileQuestionPageFormProvider
import models.Journey.{ArmsLengthLandOrProperty, AssetFromConnectedParty, InterestInLandOrProperty, OutstandingLoans, TangibleMoveableProperty, UnquotedShares}
import models.{Journey, UploadKey}
import org.mockito.ArgumentMatchers.any
import pages.NewFileUploadPage
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.validation.ValidateUploadService
import views.html.UploadNewFileQuestionView

class NewFileUploadControllerSpec extends ControllerBaseSpec {

  private val mockUploadService = mock[ValidateUploadService]

  override val additionalBindings: List[GuiceableModule] = List(
    inject.bind[ValidateUploadService].toInstance(mockUploadService),
  )
  when(mockUploadService.countTransactions(any[UploadKey])).thenReturn(IO.pure(1))


  "NewFileUploadControllerSpec - InterestInLandOrProperty" - {
    new TestScope(InterestInLandOrProperty)
  }

  "NewFileUploadControllerSpec - ArmsLengthLandOrProperty" - {
    new TestScope(ArmsLengthLandOrProperty)
  }

  "NewFileUploadControllerSpec - TangibleMoveableProperty" - {
    new TestScope(TangibleMoveableProperty)
  }

  "NewFileUploadControllerSpec - OutstandingLoans" - {
    new TestScope(OutstandingLoans)
  }

  "NewFileUploadControllerSpec - UnquotedShares" - {
    new TestScope(UnquotedShares)
  }

  "NewFileUploadControllerSpec - AssetFromConnectedParty" - {
    new TestScope(AssetFromConnectedParty)
  }

  class TestScope(journey: Journey) {
    private lazy val onPageLoad = controllers.routes.NewFileUploadController.onPageLoad(srn, journey)
    private lazy val onSubmit = controllers.routes.NewFileUploadController.onSubmit(srn, journey)
    lazy val taskListPage = controllers.routes.TaskListController.onPageLoad(srn)
    lazy val nextPage = controllers.routes.UploadFileController.onPageLoad(srn, journey)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[UploadNewFileQuestionView]
        .apply(form(injected[UploadNewFileQuestionPageFormProvider]), viewModel(srn, journey, 1))
    })

    act.like(renderPrePopView(onPageLoad, NewFileUploadPage(srn, journey), true) {
      implicit app => implicit request =>
        injected[UploadNewFileQuestionView]
          .apply(
            form(injected[UploadNewFileQuestionPageFormProvider]).fill(true),
            viewModel(srn, journey, 1)
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
