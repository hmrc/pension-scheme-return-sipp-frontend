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

import models.Journey
import models.Journey.{
  ArmsLengthLandOrProperty,
  AssetFromConnectedParty,
  InterestInLandOrProperty,
  MemberDetails,
  UnquotedShares
}
import views.html.ContentPageView

class DownloadTemplateFilePageControllerSpec extends ControllerBaseSpec {

  "Download　MemberDetails　file template" - {
    new TestScope {
      override val journey: Journey = MemberDetails
    }
  }

  "Download　InterestInLandOrProperty　file template" - {
    new TestScope {
      override val journey: Journey = InterestInLandOrProperty
    }
  }

  "Download　ArmsLengthLandOrProperty　file template" - {
    new TestScope {
      override val journey: Journey = ArmsLengthLandOrProperty
    }
  }

  "Download　UnquotedShares　file template" - {
    new TestScope {
      override val journey: Journey = UnquotedShares
    }
  }

  "Download　AssetFromConnectedParty　file template" - {
    new TestScope {
      override val journey: Journey = AssetFromConnectedParty
    }
  }

  trait TestScope {
    val journey: Journey

    lazy val viewModel = DownloadTemplateFilePageController.viewModel(srn, journey)

    lazy val onPageLoad = controllers.routes.DownloadTemplateFilePageController.onPageLoad(srn, journey)
    lazy val onSubmit = controllers.routes.DownloadTemplateFilePageController.onSubmit(srn, journey)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[ContentPageView]
      view(viewModel)
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(continue(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
