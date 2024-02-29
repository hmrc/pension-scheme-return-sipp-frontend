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

package navigation

import models.FileAction.Validating
import models.Journey.InterestInLandOrProperty
import models.NormalMode
import org.scalacheck.Gen
import pages.{CheckFileNamePage, UploadSuccessPage}
import pages.landorproperty.LandOrPropertyContributionsPage
import utils.BaseSpec

class LandOrPropertyNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new SippNavigator

  "LandOrPropertyNavigator" - {
    act.like(
      normalmode
        .navigateToWithData(
          LandOrPropertyContributionsPage,
          Gen.const(true),
          (srn, _) => controllers.routes.DownloadTemplateFilePageController.onPageLoad(srn, InterestInLandOrProperty)
        )
        .withName("go from Land or property contribution page to download template file page")
    )

    act.like(
      normalmode
        .navigateToWithData(
          CheckFileNamePage(_, InterestInLandOrProperty),
          Gen.const(false),
          (srn, _) => controllers.routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty)
        )
        .withName("go from check your interest land or property file page to upload page again if user selects no")
    )

    act.like(
      normalmode
        .navigateToWithData(
          CheckFileNamePage(_, InterestInLandOrProperty),
          Gen.const(true),
          (srn, _) => controllers.routes.LoadingPageController.onPageLoad(srn, Validating, InterestInLandOrProperty)
        )
        .withName("go from check your interest land or property file page to validating page if user selects yes")
    )
  }
}
