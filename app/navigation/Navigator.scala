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
import models.*
import models.requests.DataRequest
import pages.*
import play.api.libs.json.JsObject
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class RootNavigator @Inject() () extends Navigator {

  val journeys: List[JourneyNavigator] =
    List(new JourneyNavigator {
      override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
        case WhatYouWillNeedPage(srn) =>
          val isDataEmpty = userAnswers.data.decryptedValue == JsObject.empty
          if (isDataEmpty) {
            routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)
          } else {
            routes.CheckReturnDatesController.onPageLoad(srn, CheckMode)
          }
      }

      override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
        _ => _ => PartialFunction.empty
    })

  override def defaultNormalMode: Call = routes.IndexController.onPageLoad

  override def defaultCheckMode: Call = routes.IndexController.onPageLoad
}

trait Navigator {
  def journeys: List[JourneyNavigator]
  def defaultNormalMode: Call
  def defaultCheckMode: Call

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit req: DataRequest[?]): Call = mode match {

    case NormalMode =>
      journeys
        .foldLeft(PartialFunction.empty[Page, Call])((acc, curr) => acc.orElse(curr.normalRoutes(userAnswers)))
        .lift(page)
        .getOrElse(defaultNormalMode)

    case CheckMode =>
      journeys
        .foldLeft(PartialFunction.empty[Page, Call])((acc, curr) =>
          acc.orElse(curr.checkRoutes(req.userAnswers)(userAnswers))
        )
        .lift(page)
        .getOrElse(defaultCheckMode)
  }
}
