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

import controllers.RemoveFileController.{form, viewModel}
import forms.YesNoPageFormProvider
import models.Journey._
import models.{Journey, JourneyType, NormalMode}
import org.mockito.ArgumentMatchers.any
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.{ReportDetailsService, SaveService}
import views.html.YesNoPageView

import scala.concurrent.Future

class RemoveFileControllerSpec extends ControllerBaseSpec {
  private val mockReportDetailsService: ReportDetailsService = mock[ReportDetailsService]
  private val mockSaveService: SaveService = mock[SaveService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[ReportDetailsService].toInstance(mockReportDetailsService)
  )

  override def beforeEach(): Unit = {
    reset(mockSaveService)
    when(mockSaveService.save(any())(any(), any())).thenReturn(Future.successful(()))
  }

  "RemoveFileControllerSpec - Standard Journey - InterestInLandOrProperty" - {
    new TestScope(InterestInLandOrProperty, JourneyType.Standard)
  }

  "RemoveFileControllerSpec - Standard Journey - TangibleMoveableProperty" - {
    new TestScope(TangibleMoveableProperty, JourneyType.Standard)
  }

  "RemoveFileControllerSpec - Standard Journey - OutstandingLoans" - {
    new TestScope(OutstandingLoans, JourneyType.Standard)
  }

  "RemoveFileControllerSpec - Standard Journey - ArmsLengthLandOrProperty" - {
    new TestScope(ArmsLengthLandOrProperty, JourneyType.Standard)
  }

  "RemoveFileControllerSpec - Standard Journey - UnquotedShares" - {
    new TestScope(UnquotedShares, JourneyType.Standard)
  }

  "RemoveFileControllerSpec - Standard Journey - AssetFromConnectedParty" - {
    new TestScope(AssetFromConnectedParty, JourneyType.Standard)
  }

  "RemoveFileControllerSpec - Amend Journey - InterestInLandOrProperty" - {
    new TestScope(InterestInLandOrProperty, JourneyType.Amend)
  }

  "RemoveFileControllerSpec - Amend Journey - TangibleMoveableProperty" - {
    new TestScope(TangibleMoveableProperty, JourneyType.Amend)
  }

  "RemoveFileControllerSpec - Amend Journey - OutstandingLoans" - {
    new TestScope(OutstandingLoans, JourneyType.Amend)
  }

  "RemoveFileControllerSpec - Amend Journey - ArmsLengthLandOrProperty" - {
    new TestScope(ArmsLengthLandOrProperty, JourneyType.Amend)
  }

  "RemoveFileControllerSpec - Amend Journey - UnquotedShares" - {
    new TestScope(UnquotedShares, JourneyType.Amend)
  }

  "RemoveFileControllerSpec - Amend Journey - AssetFromConnectedParty" - {
    new TestScope(AssetFromConnectedParty, JourneyType.Amend)
  }

  class TestScope(journey: Journey, journeyType: JourneyType) {

    private lazy val onPageLoad = routes.RemoveFileController.onPageLoad(srn, journey, journeyType)
    private lazy val onSubmit = controllers.routes.RemoveFileController.onSubmit(srn, journey, journeyType)

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, journey, journeyType, NormalMode))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> "true"))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

  }
}
