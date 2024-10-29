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

import controllers.NewFileUploadController.{form, viewModel}
import forms.UploadNewFileQuestionPageFormProvider
import models.Journey.{
  ArmsLengthLandOrProperty,
  AssetFromConnectedParty,
  InterestInLandOrProperty,
  OutstandingLoans,
  TangibleMoveableProperty,
  UnquotedShares
}
import models.backend.responses.PsrAssetCountsResponse
import models.{FormBundleNumber, Journey, JourneyType, UserAnswers}
import pages.TaskListStatusPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.ReportDetailsService
import views.html.UploadNewFileQuestionView

import scala.concurrent.Future

class NewFileUploadControllerSpec extends ControllerBaseSpec {

  private val mockDetailsService = mock[ReportDetailsService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[ReportDetailsService].toInstance(mockDetailsService)
  )

  private val assetCounts = PsrAssetCountsResponse(
    interestInLandOrPropertyCount = 1,
    landArmsLengthCount = 1,
    assetsFromConnectedPartyCount = 1,
    tangibleMoveablePropertyCount = 1,
    outstandingLoansCount = 1,
    unquotedSharesCount = 1
  )

  override def beforeEach(): Unit = {
    reset(mockDetailsService)
    when(mockDetailsService.getAssetCounts(any, any, any, any)(any))
      .thenReturn(
        Future.successful(
          Some(assetCounts)
        )
      )
  }

  "NewFileUploadControllerSpec - InterestInLandOrProperty" - {
    TestScope(InterestInLandOrProperty, JourneyType.Standard)
  }

  "NewFileUploadControllerSpec - TangibleMoveableProperty" - {
    TestScope(TangibleMoveableProperty, JourneyType.Standard)
  }

  "NewFileUploadControllerSpec - OutstandingLoans" - {
    TestScope(OutstandingLoans, JourneyType.Standard)
  }

  "NewFileUploadControllerSpec - ArmsLengthLandOrProperty" - {
    TestScope(ArmsLengthLandOrProperty, JourneyType.Standard)
  }

  "NewFileUploadControllerSpec - UnquotedShares" - {
    TestScope(UnquotedShares, JourneyType.Standard)
  }

  "NewFileUploadControllerSpec - AssetFromConnectedParty" - {
    TestScope(AssetFromConnectedParty, JourneyType.Standard)
  }

  "ViewChangeNewFileUploadControllerSpec - InterestInLandOrProperty" - {
    TestScope(InterestInLandOrProperty, JourneyType.Amend)
  }

  "ViewChangeNewFileUploadControllerSpec - TangibleMoveableProperty" - {
    TestScope(TangibleMoveableProperty, JourneyType.Amend)
  }

  "ViewChangeNewFileUploadControllerSpec - OutstandingLoans" - {
    TestScope(OutstandingLoans, JourneyType.Amend)
  }

  "ViewChangeNewFileUploadControllerSpec - ArmsLengthLandOrProperty" - {
    TestScope(ArmsLengthLandOrProperty, JourneyType.Amend)
  }

  "ViewChangeNewFileUploadControllerSpec - UnquotedShares" - {
    TestScope(UnquotedShares, JourneyType.Amend)
  }

  "ViewChangeNewFileUploadControllerSpec - AssetFromConnectedParty" - {
    TestScope(AssetFromConnectedParty, JourneyType.Amend)
  }

  class TestScope(journey: Journey, journeyType: JourneyType) {

    private lazy val onPageLoad = controllers.routes.NewFileUploadController.onPageLoad(srn, journey, journeyType)

    private lazy val onSubmit = controllers.routes.NewFileUploadController.onSubmit(srn, journey, journeyType)

    private val answers: UserAnswers = defaultUserAnswers
      .unsafeSet(
        TaskListStatusPage(srn, journey),
        TaskListStatusPage.Status(completedWithNo = true)
      )

    private val addToSession: Seq[(String, String)] = Seq(("fbNumber", fbNumber))
    act.like(renderView(onPageLoad, answers, addToSession) { implicit app => implicit request =>
      injected[UploadNewFileQuestionView]
        .apply(
          form(injected[UploadNewFileQuestionPageFormProvider]),
          viewModel(
            srn,
            journey,
            Some(FormBundleNumber(fbNumber)),
            None,
            None,
            Some(assetCounts),
            journeyType
          )
        )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, defaultUserAnswers, addToSession, "value" -> "true"))

    act.like(invalidForm(onSubmit, addToSession))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

  }
}
