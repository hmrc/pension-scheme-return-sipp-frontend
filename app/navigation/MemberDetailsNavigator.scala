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

import controllers.UploadMemberDetailsController
import models.FileAction._
import models.Journey._
import models._
import pages._
import pages.memberdetails.CheckMemberDetailsFilePage
import play.api.mvc.Call

object MemberDetailsNavigator extends JourneyNavigator {

  override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

    case UploadMemberDetailsPage(srn) =>
      controllers.routes.LoadingPageController
        .onPageLoad(srn, UPLOADING, MEMBER_DETAILS)

    case LoadingPage(srn, _) =>
      controllers.memberdetails.routes.CheckMemberDetailsFileController.onPageLoad(srn, NormalMode)

    case page @ CheckMemberDetailsFilePage(srn) =>
      if (userAnswers.get(page).contains(true)) {
        controllers.routes.LoadingPageController.onPageLoad(srn, VALIDATING, MEMBER_DETAILS)
      } else {
        controllers.routes.UploadMemberDetailsController.onPageLoad(srn)
      }
  }

  override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
    _ =>
      userAnswers => {
        case UploadMemberDetailsPage(srn) =>
          controllers.routes.LoadingPageController
            .onPageLoad(srn, UPLOADING, MEMBER_DETAILS)

        case LoadingPage(srn, _) =>
          controllers.memberdetails.routes.CheckMemberDetailsFileController.onPageLoad(srn, NormalMode)

        case page @ CheckMemberDetailsFilePage(srn) =>
          if (userAnswers.get(page).contains(true)) {
            controllers.routes.LoadingPageController.onPageLoad(srn, VALIDATING, MEMBER_DETAILS)
          } else {
            controllers.routes.UploadMemberDetailsController.onPageLoad(srn)
          }
      }
}
