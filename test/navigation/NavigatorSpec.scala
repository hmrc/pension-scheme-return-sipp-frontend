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

import controllers.ControllerBaseSpec
import models.{CheckMode, NormalMode}
import pages.WhatYouWillNeedPage
import utils.BaseSpec

class NavigatorSpec extends BaseSpec with NavigatorBehaviours with ControllerBaseSpec {

  val navigator = RootNavigator()

  "Navigator" - {

    "NormalMode" - {
      act.like(
        normalmode
          .navigateTo(_ => UnknownPage, (_, _) => controllers.routes.IndexController.onPageLoad)
          .withName("redirect any unknown pages to index page")
      )

      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedPage(_),
            (srn, _) => controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode),
            _ => emptyUserAnswers
          )
          .withName("go from accounting period page to check answers page when empty")
      )

      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedPage(_),
            (srn, _) => controllers.routes.CheckReturnDatesController.onPageLoad(srn, CheckMode)
          )
          .withName("go from accounting period page to check answers page when non empty")
      )

    }

    "CheckMode" - {
      act.like(
        normalmode
          .navigateTo(_ => UnknownPage, (_, _) => controllers.routes.IndexController.onPageLoad)
          .withName("redirect any unknown pages to index page")
      )
    }
  }
}
