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

import models.Journey.InterestInLandOrProperty
import models.{UploadErrorsLandConnectedProperty, UploadFormatError, UserAnswers}
import pages.Page
import pages.landorproperty._
import play.api.mvc.Call

object LandOrPropertyNavigator extends JourneyNavigator {

  val normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ LandOrPropertyContributionsPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.routes.DownloadTemplateFilePageController.onPageLoad(srn, InterestInLandOrProperty)
      } else {
        controllers.routes.TaskListController.onPageLoad(srn)
      }

    case LandOrPropertyUploadErrorPage(srn, _: UploadFormatError) =>
      controllers.landorproperty.routes.FileUploadErrorSummaryController.onPageLoad(srn)

    case LandOrPropertyUploadErrorPage(srn, UploadErrorsLandConnectedProperty(_, errs)) if errs.size <= 25 =>
      controllers.landorproperty.routes.FileUploadErrorSummaryController.onPageLoad(srn)

    case LandOrPropertyUploadErrorPage(srn, _: UploadErrorsLandConnectedProperty) =>
      controllers.landorproperty.routes.FileUploadErrorSummaryController.onPageLoad(srn)

    case LandOrPropertyUploadErrorSummaryPage(srn, journey) =>
      controllers.routes.UploadFileController.onPageLoad(srn, journey)
  }

  val checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = _ =>
    userAnswers => {
      case page @ LandOrPropertyContributionsPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          controllers.routes.DownloadTemplateFilePageController.onPageLoad(srn, InterestInLandOrProperty)
        } else {
          controllers.routes.TaskListController.onPageLoad(srn)
        }
    }
}
