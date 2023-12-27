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

import eu.timepit.refined.refineMV
import org.scalacheck.Gen
import utils.BaseSpec
import pages.{BasicDetailsCheckYourAnswersPage, CheckReturnDatesPage, WhichTaxYearPage}

class SippNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new SippNavigator

  "SippNavigator" - {

    "NormalMode" - {
      act.like(
        normalmode
          .navigateTo(_ => UnknownPage, (_, _) => controllers.routes.IndexController.onPageLoad)
          .withName("redirect any unknown pages to index page")
      )

      act.like(
        normalmode
          .navigateTo(
            WhichTaxYearPage,
            controllers.routes.CheckReturnDatesController.onPageLoad
          )
          .withName("go from which tax year page to check return dates page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            CheckReturnDatesPage,
            Gen.const(false),
            controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad(_, refineMV(1), _)
          )
          .withName("go from check return dates page to accounting period page when no is selected")
      )

      act.like(
        normalmode
          .navigateTo(
            BasicDetailsCheckYourAnswersPage,
            (srn, _) => controllers.routes.DownloadTemplateFilePageController.onPageLoad(srn)
          )
          .withName("go from check your answers to download template file page")
      )
    }

    "CheckMode" - {

      act.like(
        checkmode
          .navigateTo(_ => UnknownPage, (_, _) => controllers.routes.IndexController.onPageLoad)
          .withName("redirect any unknown pages to index page")
      )
    }
  }
}
