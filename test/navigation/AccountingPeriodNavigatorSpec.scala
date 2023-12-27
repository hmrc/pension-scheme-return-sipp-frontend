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

import config.Refined.OneToThree
import eu.timepit.refined.refineMV
import generators.IndexGen
import models.NormalMode
import pages.accountingperiod.{AccountingPeriodCheckYourAnswersPage, AccountingPeriodListPage, AccountingPeriodPage}
import utils.BaseSpec

class AccountingPeriodNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new SippNavigator

  "AccountingPeriodNavigator" - {

    act.like(
      normalmode
        .navigateTo(
          AccountingPeriodPage(_, refineMV(1), NormalMode),
          (srn, _) =>
            controllers.accountingperiod.routes.AccountingPeriodCheckYourAnswersController
              .onPageLoad(srn, refineMV(1), NormalMode)
        )
        .withName("go from accounting period page to check answers page")
    )

    act.like(
      normalmode
        .navigateTo(
          AccountingPeriodCheckYourAnswersPage(_, NormalMode),
          controllers.accountingperiod.routes.AccountingPeriodListController.onPageLoad
        )
        .withName("go from check your answers page to list page")
    )

    act.like(
      normalmode
        .navigateFromListPage(
          AccountingPeriodListPage(_, addPeriod = true, NormalMode),
          AccountingPeriodPage(_, _, NormalMode),
          dateRangeGen,
          IndexGen[OneToThree](1, 3),
          controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad,
          controllers.routes.BasicDetailsCheckYourAnswersController.onPageLoad
        )
        .withName("go from list page")
    )

    act.like(
      normalmode
        .navigateTo(
          AccountingPeriodListPage(_, addPeriod = false, NormalMode),
          controllers.routes.BasicDetailsCheckYourAnswersController.onPageLoad
        )
        .withName("go from list page to bank account page when no selected")
    )

  }
}
