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

import config.Constants
import connectors.PSRConnector
import controllers.JourneyContributionsHeldController.{form, viewModel}
import forms.YesNoPageFormProvider
import models.Journey.*
import models.JourneyType.Standard
import models.backend.responses.SippPsrJourneySubmissionEtmpResponse
import models.Journey
import pages.JourneyContributionsHeldPage
import views.html.YesNoPageView
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule

import scala.concurrent.Future

class JourneyContributionsHeldControllerSpec extends ControllerBaseSpec {
  private val session: Seq[(String, String)] =
    Seq(
      (Constants.version, "001"),
      (Constants.taxYear, "2020-04-06"),
      (Constants.formBundleNumber, "54321")
    )

  private val mockPsrConnector = mock[PSRConnector]
  private val response = SippPsrJourneySubmissionEtmpResponse("123456")

  override val additionalBindings: List[GuiceableModule] = List(bind[PSRConnector].toInstance(mockPsrConnector))

  "JourneyContributionsHeldController - InterestInLandOrProperty" - {
    when(mockPsrConnector.submitLandOrConnectedProperty(any, any, any, any)(any, any)).thenReturn(Future.successful(response))
    TestScope(InterestInLandOrProperty)
  }

  "JourneyContributionsHeldController - ArmsLengthLandOrProperty" - {
    when(mockPsrConnector.submitLandArmsLength(any, any, any, any)(any, any)).thenReturn(Future.successful(response))
    TestScope(ArmsLengthLandOrProperty)
  }

  "JourneyContributionsHeldController - TangibleMoveableProperty" - {
    when(mockPsrConnector.submitTangibleMoveableProperty(any, any, any, any)(any, any)).thenReturn(Future.successful(response))
    TestScope(TangibleMoveableProperty)
  }

  "JourneyContributionsHeldController - OutstandingLoans" - {
    when(mockPsrConnector.submitOutstandingLoans(any, any, any, any)(any, any)).thenReturn(Future.successful(response))
    TestScope(OutstandingLoans)
  }

  "JourneyContributionsHeldController - UnquotedShares" - {
    when(mockPsrConnector.submitUnquotedShares(any, any, any, any)(any, any)).thenReturn(Future.successful(response))
    TestScope(UnquotedShares)
  }

  "JourneyContributionsHeldController - AssetFromConnectedParty" - {
    when(mockPsrConnector.submitAssetsFromConnectedParty(any, any, any, any)(any, any)).thenReturn(Future.successful(response))
    TestScope(AssetFromConnectedParty)
  }

  class TestScope(journey: Journey) {
    private lazy val onPageLoad =
      controllers.routes.JourneyContributionsHeldController.onPageLoad(srn, journey, Standard)
    private lazy val onSubmit =
      controllers.routes.JourneyContributionsHeldController.onSubmit(srn, journey, Standard)

    act.like(renderView(onPageLoad, addToSession = session) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider], journey), viewModel(srn, journey, schemeName, Standard))
    })

    act.like(renderPrePopView(onPageLoad, JourneyContributionsHeldPage(srn, journey, Standard), addToSession = session, true) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider], journey).fill(true),
            viewModel(srn, journey, schemeName, Standard)
          )
    })

    act.like(
      redirectNextPage(onSubmit, session, "value" -> "true")
    )

    act.like(
      redirectNextPage(onSubmit, session, "value" -> "false")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, session, "value" -> "true"))

    act.like(invalidForm(onSubmit, session))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

  }
}
