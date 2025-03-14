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
import controllers.routes
import eu.timepit.refined.auto.autoUnwrap
import models.FileAction.Validating
import models.TypeOfViewChangeQuestion.*
import models.{JourneyType, NormalMode, UploadErrors, UploadFormatError, UserAnswers}
import pages.*
import play.api.mvc.Call
import services.validation.csv.CsvDocumentValidatorConfig

import javax.inject.Inject

class SippNavigator @Inject() (csvUploadValidatorConfig: CsvDocumentValidatorConfig) extends Navigator {

  val sippNavigator: JourneyNavigator = new JourneyNavigator {
    override def normalRoutes: UserAnswers => PartialFunction[Page, Call] = userAnswers => {

      case WhichTaxYearPage(srn) => routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)

      case page @ CheckReturnDatesPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          routes.AssetsHeldController.onPageLoad(srn)
        } else {
          controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, Max3.ONE, NormalMode)
        }

      case AssetsHeldPage(srn) =>
        routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode)

      case BasicDetailsCheckYourAnswersPage(srn) =>
        if (userAnswers.get(AssetsHeldPage(srn)).contains(true)) {
          routes.TaskListController.onPageLoad(srn)
        } else {
          routes.DeclarationController.onPageLoad(srn, None)
        }

      case page @ JourneyContributionsHeldPage(srn, journey, journeyType) =>
        if (userAnswers.get(page).contains(true)) {
          if (journeyType == JourneyType.Standard) {
            routes.DownloadTemplateFilePageController.onPageLoad(srn, journey)
          } else {
            routes.UploadFileController.onPageLoad(srn, journey, JourneyType.Amend)
          }
        } else {
          if (journeyType == JourneyType.Standard) {
            routes.TaskListController.onPageLoad(srn)
          } else {
            routes.ChangeTaskListController.onPageLoad(srn)
          }
        }

      case DownloadTemplateFilePage(srn, journey, journeyType) =>
        routes.UploadFileController.onPageLoad(srn, journey, journeyType)

      case page @ CheckFileNamePage(srn, journey, journeyType) =>
        if (userAnswers.get(page).contains(true)) {
          routes.LoadingPageController.onPageLoad(srn, Validating, journey, journeyType)
        } else {
          routes.UploadFileController.onPageLoad(srn, journey, journeyType)
        }

      case page @ NewFileUploadPage(srn, journey, journeyType) =>
        if (userAnswers.get(page).contains(true)) {
          if (journeyType == JourneyType.Standard) {
            routes.DownloadTemplateFilePageController.onPageLoad(srn, journey)
          } else {
            routes.UploadFileController.onPageLoad(srn, journey, JourneyType.Amend)
          }
        } else {
          if (journeyType == JourneyType.Standard) {
            routes.TaskListController.onPageLoad(srn)
          } else {
            routes.ChangeTaskListController.onPageLoad(srn)
          }
        }

      case page @ RemoveFilePage(srn, journey, journeyType) =>
        if (userAnswers.get(page).contains(true)) {
          routes.RemoveFileSuccessController.onPageLoad(srn, journey, journeyType)
        } else {
          routes.NewFileUploadController.onPageLoad(srn, journey, journeyType)
        }

      case RemoveFileSuccessPage(srn, journey, journeyType) =>
        routes.JourneyContributionsHeldController.onPageLoad(srn, journey, journeyType)

      case UploadSuccessPage(srn, _, journeyType) =>
        journeyType match {
          case JourneyType.Standard => routes.TaskListController.onPageLoad(srn)
          case JourneyType.Amend => routes.ChangeTaskListController.onPageLoad(srn)
        }

      case UploadErrorPage(srn, journey, journeyType, _: UploadFormatError) =>
        routes.FileUploadTooManyErrorsController.onPageLoad(srn, journey, journeyType)

      case UploadErrorPage(srn, journey, journeyType, ue: UploadErrors)
          if ue.errors.size <= csvUploadValidatorConfig.errorLimit =>
        routes.FileUploadErrorSummaryController.onPageLoad(srn, journey, journeyType)

      case UploadErrorPage(srn, journey, journeyType, _: UploadErrors) =>
        routes.FileUploadTooManyErrorsController.onPageLoad(srn, journey, journeyType)

      case UploadErrorSummaryPage(srn, journey, journeyType) =>
        routes.UploadFileController.onPageLoad(srn, journey, journeyType)

      case FileUploadTooManyErrorsPage(srn, journey, journeyType) =>
        routes.UploadFileController.onPageLoad(srn, journey, journeyType)

      case DeclarationPage(srn) =>
        routes.ReturnSubmittedController.onPageLoad(srn)

      case page @ ViewChangeQuestionPage(srn) =>
        if (userAnswers.get(page).contains(ViewReturn))
          routes.ViewTaskListController.onPageLoad(srn, None)
        else
          routes.UpdateMemberDetailsQuestionController.onPageLoad(srn)

      case page @ UpdateMemberDetailsQuestionPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          routes.ViewChangeMembersController.onPageLoad(srn, 1, None)
        } else {
          routes.ChangeTaskListController.onPageLoad(srn)
        }

      case UpdateMembersDOBQuestionPage(srn) =>
        routes.ViewChangePersonalDetailsController.onPageLoad(srn)

      case ViewBasicDetailsCheckYourAnswersPage(srn) =>
        if (userAnswers.get(ViewChangeQuestionPage(srn)).contains(ViewReturn))
          routes.ViewTaskListController.onPageLoad(srn, None)
        else
          routes.ChangeTaskListController.onPageLoad(srn)

      case page @ RemoveMemberQuestionPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          routes.RemoveMemberSuccessController.onPageLoad(srn)
        } else {
          routes.ViewChangeMembersController.onPageLoad(srn, 1, None)
        }

      case RemoveMemberSuccessPage(srn) =>
        routes.UpdateAnotherMemberQuestionController.onPageLoad(srn)

      case page @ UpdateAnotherMemberQuestionPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          routes.ViewChangeMembersController.onPageLoad(srn, 1, None)
        } else {
          routes.ChangeTaskListController.onPageLoad(srn)
        }

      case page @ UpdatePersonalDetailsMemberHasNinoQuestionPage(srn) =>
        if (userAnswers.get(page).contains(true)) {
          routes.ChangeMembersNinoController.onPageLoad(srn)
        } else {
          routes.ChangeMembersNoNinoReasonController.onPageLoad(srn)
        }

      case UpdateMemberNinoPage(srn) =>
        routes.ViewChangePersonalDetailsController.onPageLoad(srn)

      case UpdateMembersFirstNamePage(srn) =>
        routes.ViewChangePersonalDetailsController.onPageLoad(srn)

      case UpdateMembersLastNamePage(srn) =>
        routes.ViewChangePersonalDetailsController.onPageLoad(srn)

      case UpdateMembersNoNinoReasonPage(srn) =>
        routes.ViewChangePersonalDetailsController.onPageLoad(srn)
    }

    override def checkRoutes: UserAnswers => UserAnswers => PartialFunction[Page, Call] =
      _ =>
        userAnswers => {
          case page @ CheckReturnDatesPage(srn) =>
            if (userAnswers.get(page).contains(true)) {
              if (userAnswers.get(ViewChangeQuestionPage(srn)).contains(ChangeReturn))
                routes.ViewBasicDetailsCheckYourAnswersController.onPageLoad(srn)
              else
                routes.AssetsHeldController.onPageLoad(srn)
            } else {
              controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, Max3.ONE, NormalMode)
            }

          case BasicDetailsCheckYourAnswersPage(srn) =>
            routes.TaskListController.onPageLoad(srn)

          case page @ JourneyContributionsHeldPage(srn, journey, journeyType) =>
            if (userAnswers.get(page).contains(true)) {
              if (journeyType == JourneyType.Standard) {
                routes.DownloadTemplateFilePageController.onPageLoad(srn, journey)
              } else {
                routes.UploadFileController.onPageLoad(srn, journey, JourneyType.Amend)
              }
            } else {
              if (journeyType == JourneyType.Standard) {
                routes.TaskListController.onPageLoad(srn)
              } else {
                routes.ChangeTaskListController.onPageLoad(srn)
              }
            }

          case DownloadTemplateFilePage(srn, journey, journeyType) =>
            routes.UploadFileController.onPageLoad(srn, journey, journeyType)

          case page @ CheckFileNamePage(srn, journey, journeyType) =>
            if (userAnswers.get(page).contains(true)) {
              routes.LoadingPageController.onPageLoad(srn, Validating, journey, journeyType)
            } else {
              routes.UploadFileController.onPageLoad(srn, journey, journeyType)
            }

          case page @ NewFileUploadPage(srn, journey, journeyType) =>
            if (userAnswers.get(page).contains(true)) {
              if (journeyType == JourneyType.Standard) {
                routes.DownloadTemplateFilePageController.onPageLoad(srn, journey)
              } else {
                routes.UploadFileController.onPageLoad(srn, journey, JourneyType.Amend)
              }
            } else {
              if (journeyType == JourneyType.Standard) {
                routes.TaskListController.onPageLoad(srn)
              } else {
                routes.ChangeTaskListController.onPageLoad(srn)
              }
            }

          case page @ RemoveFilePage(srn, journey, journeyType) =>
            if (userAnswers.get(page).contains(true)) {
              routes.JourneyContributionsHeldController.onPageLoad(srn, journey, journeyType)
            } else {
              routes.NewFileUploadController.onPageLoad(srn, journey, journeyType)
            }

          case UploadSuccessPage(srn, _, journeyType) =>
            journeyType match {
              case JourneyType.Standard => routes.TaskListController.onPageLoad(srn)
              case JourneyType.Amend => routes.ChangeTaskListController.onPageLoad(srn)
            }

          case DeclarationPage(srn) =>
            routes.ReturnSubmittedController.onPageLoad(srn)

          case ViewBasicDetailsCheckYourAnswersPage(srn) =>
            if (userAnswers.get(ViewChangeQuestionPage(srn)).contains(ViewReturn))
              routes.ViewTaskListController.onPageLoad(srn, None)
            else
              routes.ChangeTaskListController.onPageLoad(srn)
        }
  }

  override def journeys: List[JourneyNavigator] =
    List(
      sippNavigator,
      AccountingPeriodNavigator
    )

  override def defaultNormalMode: Call = routes.IndexController.onPageLoad
  override def defaultCheckMode: Call = routes.IndexController.onPageLoad
}
