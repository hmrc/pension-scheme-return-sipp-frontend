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

import controllers.routes
import models.{NormalMode, UserAnswers}
import pages.{BasicDetailsCheckYourAnswersPage, CheckReturnDatesPage, Page, WhichTaxYearPage}
import play.api.mvc.Call

import javax.inject.Inject

class SippNavigator @Inject()() extends Navigator {

  val sippNavigator: JourneyNavigator = new JourneyNavigator {
    override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

      case WhichTaxYearPage(srn) => routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)

      case page @ CheckReturnDatesPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
        } else {
          routes.IndexController.onPageLoad
        }

      case BasicDetailsCheckYourAnswersPage(_) =>
          routes.IndexController.onPageLoad
    }

    override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
      _ =>
        userAnswers => {
          case page@CheckReturnDatesPage(srn) =>
            if (userAnswers.get(page).contains(true)) {
              routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)
            } else {
              routes.IndexController.onPageLoad
            }
        }
  }

  override def journeys: List[JourneyNavigator] =
    List(
      sippNavigator
    )

  override def defaultNormalMode: Call = controllers.routes.IndexController.onPageLoad
  override def defaultCheckMode: Call = controllers.routes.IndexController.onPageLoad
}
