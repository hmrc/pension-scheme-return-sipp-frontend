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
import eu.timepit.refined.refineMV
import models.FileAction.Validating
import models.TypeOfViewChangeQuestion.ViewReturn
import models.{NormalMode, UploadErrors, UploadFormatError, UserAnswers}
import pages._
import play.api.mvc.Call
import services.validation.csv.CsvDocumentValidatorConfig

import javax.inject.Inject

class SippNavigator @Inject()(csvUploadValidatorConfig: CsvDocumentValidatorConfig) extends Navigator {

  val sippNavigator: JourneyNavigator = new JourneyNavigator {
    override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

      case WhichTaxYearPage(srn) => routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)

      case page @ CheckReturnDatesPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          routes.AssetsHeldController.onPageLoad(srn)
        } else {
          controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)
        }

      case AssetsHeldPage(srn) =>
        controllers.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)

      case BasicDetailsCheckYourAnswersPage(srn) =>
        if (userAnswers.get(AssetsHeldPage(srn)).contains(true)) {
          controllers.routes.TaskListController.onPageLoad(srn)
        } else {
          controllers.routes.DeclarationController.onPageLoad(srn, None)
        }

      case page @ JourneyContributionsHeldPage(srn, journey) =>
        if (userAnswers.get(page).contains(true)) {
          controllers.routes.DownloadTemplateFilePageController.onPageLoad(srn, journey)
        } else {
          controllers.routes.TaskListController.onPageLoad(srn)
        }

      case DownloadTemplateFilePage(srn, journey) =>
        controllers.routes.UploadFileController.onPageLoad(srn, journey)

      case page @ CheckFileNamePage(srn, journey) =>
        if (userAnswers.get(page).contains(true)) {
          controllers.routes.LoadingPageController.onPageLoad(srn, Validating, journey)
        } else {
          controllers.routes.UploadFileController.onPageLoad(srn, journey)
        }

      case page @ NewFileUploadPage(srn, journey) =>
        if (userAnswers.get(page).contains(true)) {
          controllers.routes.DownloadTemplateFilePageController.onPageLoad(srn, journey)
        } else {
          controllers.routes.TaskListController.onPageLoad(srn)
        }

      case page @ ViewChangeNewFileUploadPage(srn, _) =>
        if (userAnswers.get(page).contains(true)) {
          controllers.routes.JourneyRecoveryController.onPageLoad() //TODO: implement correct route for this
        } else {
          controllers.routes.ChangeTaskListController.onPageLoad(srn)
        }

      case UploadSuccessPage(srn, _) =>
        controllers.routes.TaskListController.onPageLoad(srn)

      case UploadErrorPage(srn, journey, _: UploadFormatError) =>
        controllers.routes.FileUploadTooManyErrorsController.onPageLoad(srn, journey)

      case UploadErrorPage(srn, journey, ue: UploadErrors) if ue.errors.size <= csvUploadValidatorConfig.errorLimit =>
        controllers.routes.FileUploadErrorSummaryController.onPageLoad(srn, journey)

      case UploadErrorPage(srn, journey, _: UploadErrors) =>
        controllers.routes.FileUploadTooManyErrorsController.onPageLoad(srn, journey)

      case UploadErrorSummaryPage(srn, journey) =>
        controllers.routes.UploadFileController.onPageLoad(srn, journey)

      case FileUploadTooManyErrorsPage(srn, journey) =>
        controllers.routes.UploadFileController.onPageLoad(srn, journey)

      case DeclarationPage(srn) =>
        controllers.routes.ReturnSubmittedController.onPageLoad(srn)

      case page @ ViewChangeQuestionPage(srn) =>
        if (userAnswers.get(page).contains(ViewReturn))
          controllers.routes.ViewTaskListController.onPageLoad(srn)
        else
          controllers.routes.UpdateMemberDetailsQuestionController.onPageLoad(srn)

      case page @ UpdateMemberDetailsQuestionPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          controllers.routes.ViewChangeMembersController.onPageLoad(srn, 1, None)
        } else {
          controllers.routes.ChangeTaskListController.onPageLoad(srn)
        }

      case UpdateMembersDOBQuestionPage(srn) =>
        controllers.routes.ViewChangePersonalDetailsController.onPageLoad(srn)

      case ViewBasicDetailsCheckYourAnswersPage(srn) =>
        controllers.routes.ViewTaskListController.onPageLoad(srn)

      case RemoveMemberQuestionPage(srn) =>
        controllers.routes.ViewChangeMembersController.onPageLoad(srn, 1, None)

      case page @ UpdatePersonalDetailsMemberHasNinoQuestionPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          controllers.routes.ChangeMembersNinoController.onPageLoad(srn)
        } else {
          controllers.routes.ChangeMembersNoNinoReasonController.onPageLoad(srn)
        }

      case UpdateMemberNinoPage(srn) =>
        controllers.routes.ViewChangePersonalDetailsController.onPageLoad(srn)

      case UpdateMembersFirstNamePage(srn) =>
        controllers.routes.ViewChangePersonalDetailsController.onPageLoad(srn)

      case UpdateMembersLastNamePage(srn) =>
        controllers.routes.ViewChangePersonalDetailsController.onPageLoad(srn)

      case UpdateMembersNoNinoReasonPage(srn) =>
        controllers.routes.ViewChangePersonalDetailsController.onPageLoad(srn)
    }

    override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
      _ =>
        userAnswers => {
          case page @ CheckReturnDatesPage(srn) =>
            if (userAnswers.get(page).contains(true)) {
              routes.AssetsHeldController.onPageLoad(srn)
            } else {
              controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, refineMV(1), NormalMode)
            }

          case BasicDetailsCheckYourAnswersPage(srn) =>
            controllers.routes.TaskListController.onPageLoad(srn)

          case page @ JourneyContributionsHeldPage(srn, journey) =>
            if (userAnswers.get(page).contains(true)) {
              controllers.routes.DownloadTemplateFilePageController.onPageLoad(srn, journey)
            } else {
              controllers.routes.TaskListController.onPageLoad(srn)
            }

          case DownloadTemplateFilePage(srn, journey) =>
            controllers.routes.UploadFileController.onPageLoad(srn, journey)

          case page @ CheckFileNamePage(srn, journey) =>
            if (userAnswers.get(page).contains(true)) {
              controllers.routes.LoadingPageController.onPageLoad(srn, Validating, journey)
            } else {
              controllers.routes.UploadFileController.onPageLoad(srn, journey)
            }

          case page @ NewFileUploadPage(srn, journey) =>
            if (userAnswers.get(page).contains(true)) {
              controllers.routes.UploadFileController.onPageLoad(srn, journey)
            } else {
              controllers.routes.TaskListController.onPageLoad(srn)
            }

          case UploadSuccessPage(srn, _) =>
            controllers.routes.TaskListController.onPageLoad(srn)

          case DeclarationPage(srn) =>
            controllers.routes.ReturnSubmittedController.onPageLoad(srn)

          case ViewBasicDetailsCheckYourAnswersPage(srn) =>
            controllers.routes.ViewTaskListController.onPageLoad(srn)
        }
  }

  override def journeys: List[JourneyNavigator] =
    List(
      sippNavigator,
      AccountingPeriodNavigator
    )

  override def defaultNormalMode: Call = controllers.routes.IndexController.onPageLoad
  override def defaultCheckMode: Call = controllers.routes.IndexController.onPageLoad
}
