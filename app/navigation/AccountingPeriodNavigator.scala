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

import config.RefinedTypes.OneToThree
import controllers.routes
import eu.timepit.refined.refineV
import models.{NormalMode, UserAnswers}
import pages.Page
import pages.accountingperiod.{
  AccountingPeriodCheckYourAnswersPage,
  AccountingPeriodListPage,
  AccountingPeriodPage,
  AccountingPeriods,
  RemoveAccountingPeriodPage
}
import play.api.mvc.Call
import eu.timepit.refined.auto.autoUnwrap

object AccountingPeriodNavigator extends JourneyNavigator {

  val normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case AccountingPeriodPage(srn, index, mode) =>
      controllers.accountingperiod.routes.AccountingPeriodCheckYourAnswersController.onPageLoad(srn, index, mode)

    case AccountingPeriodCheckYourAnswersPage(srn, mode) =>
      controllers.accountingperiod.routes.AccountingPeriodListController.onPageLoad(srn, mode)

    case AccountingPeriodListPage(srn, false, _) =>
      routes.AssetsHeldController.onPageLoad(srn)

    case AccountingPeriodListPage(srn, true, mode) =>
      val count = userAnswers.list(AccountingPeriods(srn)).length
      refineV[OneToThree](count + 1).fold(
        _ => controllers.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, mode),
        index => controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, index, NormalMode)
      )

    case RemoveAccountingPeriodPage(srn, mode) =>
      controllers.accountingperiod.routes.AccountingPeriodListController.onPageLoad(srn, mode)
  }

  val checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = _ =>
    userAnswers => {
      case AccountingPeriodPage(srn, index, mode) =>
        controllers.accountingperiod.routes.AccountingPeriodCheckYourAnswersController.onPageLoad(srn, index, mode)

      case AccountingPeriodCheckYourAnswersPage(srn, mode) =>
        controllers.accountingperiod.routes.AccountingPeriodListController.onPageLoad(srn, mode)

      case AccountingPeriodListPage(srn, false, _) =>
        routes.AssetsHeldController.onPageLoad(srn)

      case AccountingPeriodListPage(srn, true, mode) =>
        val count = userAnswers.list(AccountingPeriods(srn)).length
        refineV[OneToThree](count + 1).fold(
          _ => controllers.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, mode),
          index => controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, index, mode)
        )

      case RemoveAccountingPeriodPage(srn, mode) =>
        controllers.accountingperiod.routes.AccountingPeriodListController.onPageLoad(srn, mode)
    }

}
