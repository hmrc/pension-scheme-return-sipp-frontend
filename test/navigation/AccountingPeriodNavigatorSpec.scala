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

import config.RefinedTypes.Max3
import models.{CheckMode, NormalMode}
import pages.accountingperiod.{
  AccountingPeriodCheckYourAnswersPage,
  AccountingPeriodListPage,
  AccountingPeriodPage,
  AccountingPeriods,
  RemoveAccountingPeriodPage
}
import services.validation.csv.CsvDocumentValidatorConfig
import utils.BaseSpec
import controllers.{accountingperiod, routes}
import org.scalacheck.Gen
import utils.UserAnswersUtils.*

class AccountingPeriodNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  private val mockConfig = mock[CsvDocumentValidatorConfig]
  when(mockConfig.errorLimit).thenReturn(25)

  val navigator: Navigator = SippNavigator(mockConfig)

  "AccountingPeriodNavigator" - {
    "NormalMode" - {
      val mode = NormalMode

      act.like(
        normalmode
          .navigateTo(
            AccountingPeriodPage(_, Max3.ONE, mode),
            (srn, _) =>
              controllers.accountingperiod.routes.AccountingPeriodCheckYourAnswersController
                .onPageLoad(srn, 1, mode)
          )
          .withName("go from accounting period page to check answers page")
      )

      act.like(
        normalmode
          .navigateTo(
            AccountingPeriodCheckYourAnswersPage(_, mode),
            controllers.accountingperiod.routes.AccountingPeriodListController.onPageLoad
          )
          .withName("go from check your answers page to list page")
      )

      act.like {
        val dateRanges = Gen.listOfN(1, dateRangeGen).sample.value
        normalmode
          .navigateTo(
            AccountingPeriodListPage(_, addPeriod = true, mode),
            (srn, _) => accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, 2, mode),
            srn => defaultUserAnswers.unsafeSet(AccountingPeriods(srn), dateRanges)
          )
          .withName("go from list page to accounting period")
      }

      act.like {
        val dateRanges = Gen.listOfN(3, dateRangeGen).sample.value
        normalmode
          .navigateTo(
            AccountingPeriodListPage(_, addPeriod = true, mode),
            (srn, _) => routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, mode),
            srn => defaultUserAnswers.unsafeSet(AccountingPeriods(srn), dateRanges)
          )
          .withName("go from list page to basic details check your answers")
      }

      act.like(
        normalmode
          .navigateTo(
            AccountingPeriodListPage(_, addPeriod = false, mode),
            (srn, _) => controllers.routes.AssetsHeldController.onPageLoad(srn, mode)
          )
          .withName("go from list page to assets held page when no selected")
      )

      act.like(
        normalmode
          .navigateTo(
            RemoveAccountingPeriodPage(_, mode),
            controllers.accountingperiod.routes.AccountingPeriodListController.onPageLoad
          )
          .withName("go from remove page to list page")
      )
    }

    "CheckMode" - {
      val mode = CheckMode

      act.like(
        checkmode
          .navigateTo(
            AccountingPeriodPage(_, Max3.ONE, mode),
            (srn, _) => accountingperiod.routes.AccountingPeriodCheckYourAnswersController.onPageLoad(srn, 1, mode)
          )
          .withName("go from accounting period to accounting period check your answers page")
      )

      act.like(
        checkmode
          .navigateTo(
            AccountingPeriodCheckYourAnswersPage(_, mode),
            (srn, _) => accountingperiod.routes.AccountingPeriodListController.onPageLoad(srn, mode)
          )
          .withName("go from accounting period check your answers to accounting period list page")
      )

      act.like(
        checkmode
          .navigateTo(
            AccountingPeriodListPage(_, addPeriod = false, mode),
            (srn, _) => routes.AssetsHeldController.onPageLoad(srn, mode)
          )
          .withName("go from accounting period list to assets held page")
      )

      act.like {
        val dateRanges = List(dateRangeGen.sample.value)
        checkmode
          .navigateTo(
            AccountingPeriodListPage(_, addPeriod = true, mode),
            (srn, _) => accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, 2, mode),
            srn => defaultUserAnswers.unsafeSet(AccountingPeriods(srn), dateRanges)
          )
          .withName("go from accounting period list to assets held page when addPeriod is true")
      }

      act.like {
        val dateRanges = Gen.listOfN(3, dateRangeGen).sample.value
        checkmode
          .navigateTo(
            AccountingPeriodListPage(_, addPeriod = true, mode),
            (srn, _) => routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, mode),
            srn => defaultUserAnswers.unsafeSet(AccountingPeriods(srn), dateRanges)
          )
          .withName("go from accounting period list to basic details check your answers page if date range count is 3")
      }
    }
  }
}
