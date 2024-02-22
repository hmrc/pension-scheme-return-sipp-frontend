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
import models.{NormalMode, UserAnswers}
import pages.Page
import pages.interestlandorproperty.DownloadInterestLandOrPropertyTemplateFilePage
import pages.landorproperty.{
  CheckInterestLandOrPropertyFilePage,
  LandOrPropertyContributionsPage,
  UploadInterestLandOrPropertyPage
}
import play.api.mvc.Call

object LandOrPropertyNavigator extends JourneyNavigator {

  val normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {
    case page @ LandOrPropertyContributionsPage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.landorproperty.routes.DownloadInterestLandOrPropertyTemplateFilePageController.onPageLoad(srn)
      } else {
        controllers.routes.TaskListController.onPageLoad(srn)
      }
    case DownloadInterestLandOrPropertyTemplateFilePage(srn) =>
      controllers.routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty)

    case UploadInterestLandOrPropertyPage(srn) =>
      controllers.landorproperty.routes.CheckInterestLandOrPropertyFileController.onPageLoad(srn, NormalMode)

    case page @ CheckInterestLandOrPropertyFilePage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.routes.LoadingPageController.onPageLoad(srn, Validating, InterestInLandOrProperty)
      } else {
        controllers.routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty)
      }
  }

  val checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] = _ =>
    userAnswers => {
      case page @ LandOrPropertyContributionsPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          controllers.landorproperty.routes.DownloadInterestLandOrPropertyTemplateFilePageController.onPageLoad(srn)
        } else {
          controllers.routes.TaskListController.onPageLoad(srn)
        }
      case DownloadInterestLandOrPropertyTemplateFilePage(srn) =>
        controllers.routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty)

      case UploadInterestLandOrPropertyPage(srn) =>
        controllers.landorproperty.routes.CheckInterestLandOrPropertyFileController.onPageLoad(srn, NormalMode)

      case page @ CheckInterestLandOrPropertyFilePage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          controllers.routes.LoadingPageController.onPageLoad(srn, Validating, InterestInLandOrProperty)
        } else {
          controllers.routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty)
        }
    }

}
