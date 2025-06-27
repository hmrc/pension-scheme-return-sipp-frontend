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

import cats.data.NonEmptyList
import models.FileAction.Validating
import models.Journey.InterestInLandOrProperty
import models.TypeOfViewChangeQuestion.{ChangeReturn, ViewReturn}
import models.{Journey, JourneyType, NormalMode, UploadErrors, UploadFormatError}
import models.JourneyType.Standard
import models.JourneyType.Amend
import org.scalacheck.Gen
import pages.*
import services.validation.csv.CsvDocumentValidatorConfig
import utils.BaseSpec
import utils.UserAnswersUtils.*
import controllers.routes
import controllers.accountingperiod

class SippNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  private val mockConfig = mock[CsvDocumentValidatorConfig]
  val errorLimit = 25
  when(mockConfig.errorLimit).thenReturn(errorLimit)

  val navigator: Navigator = SippNavigator(mockConfig)

  "SippNavigator" - {

    "NormalMode" - {
      act.like(
        normalmode
          .navigateTo(_ => UnknownPage, (_, _) => routes.IndexController.onPageLoad)
          .withName("redirect any unknown pages to index page")
      )

      act.like(
        normalmode
          .navigateTo(
            WhichTaxYearPage.apply,
            routes.CheckReturnDatesController.onPageLoad
          )
          .withName("go from which tax year page to check return dates page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            CheckReturnDatesPage.apply,
            Gen.const(false),
            controllers.accountingperiod.routes.AccountingPeriodController.onPageLoad(_, 1, _)
          )
          .withName("go from check return dates page to accounting period page when no is selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            CheckReturnDatesPage.apply,
            Gen.const(true),
            (srn, _) => routes.AssetsHeldController.onPageLoad(srn)
          )
          .withName("go from check return dates page to assets held page when yes is selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            AssetsHeldPage.apply,
            Gen.const(false),
            routes.BasicDetailsCheckYourAnswersController.onPageLoad
          )
          .withName("go from assets held page to declaration page when no is selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            AssetsHeldPage.apply,
            Gen.const(true),
            routes.BasicDetailsCheckYourAnswersController.onPageLoad
          )
          .withName("go from assets held page to basic check your answers page when yes is selected")
      )

      act.like(
        normalmode
          .navigateTo(
            BasicDetailsCheckYourAnswersPage.apply,
            (srn, _) => routes.TaskListController.onPageLoad(srn),
            srn => defaultUserAnswers.unsafeSet(AssetsHeldPage(srn), true)
          )
          .withName("go from check your answers to task list page if any assets selected yes")
      )

      act.like(
        normalmode
          .navigateTo(
            BasicDetailsCheckYourAnswersPage.apply,
            (srn, _) => routes.DeclarationController.onPageLoad(srn, None),
            srn => defaultUserAnswers.unsafeSet(AssetsHeldPage(srn), false)
          )
          .withName("go from check your answers to declaration page if any assets selected no")
      )

      act.like(
        normalmode
          .navigateToWithData(
            JourneyContributionsHeldPage(_, InterestInLandOrProperty, Standard),
            Gen.const(true),
            (srn, _) =>
              routes.DownloadTemplateFilePageController
                .onPageLoad(srn, InterestInLandOrProperty)
          )
          .withName(
            "go from Land or property contribution page (normal) to download template file page if yes selected"
          )
      )

      act.like(
        normalmode
          .navigateToWithData(
            JourneyContributionsHeldPage(_, InterestInLandOrProperty, Amend),
            Gen.const(true),
            (srn, _) =>
              routes.UploadFileController
                .onPageLoad(srn, InterestInLandOrProperty, JourneyType.Amend)
          )
          .withName("go from Land or property contribution page (amend) to upload template file page if yes selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            JourneyContributionsHeldPage(_, InterestInLandOrProperty, Standard),
            Gen.const(false),
            (srn, _) => routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from Land or property contribution page (normal) to task list page if no selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            JourneyContributionsHeldPage(_, InterestInLandOrProperty, Amend),
            Gen.const(false),
            (srn, _) => routes.ChangeTaskListController.onPageLoad(srn)
          )
          .withName("go from Land or property contribution page (Amend) to task list page if no selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            CheckFileNamePage(_, InterestInLandOrProperty, Standard),
            Gen.const(false),
            (srn, _) => routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty, Standard)
          )
          .withName("go from check your interest land or property file page to upload page again if user selects no")
      )

      act.like(
        normalmode
          .navigateToWithData(
            CheckFileNamePage(_, InterestInLandOrProperty, Standard),
            Gen.const(true),
            (srn, _) =>
              routes.LoadingPageController
                .onPageLoad(srn, Validating, InterestInLandOrProperty, Standard)
          )
          .withName("go from check your interest land or property file page to validating page if user selects yes")
      )

      act.like(
        normalmode
          .navigateToWithData(
            NewFileUploadPage(_, InterestInLandOrProperty, Standard),
            Gen.const(true),
            (srn, _) =>
              routes.DownloadTemplateFilePageController
                .onPageLoad(srn, InterestInLandOrProperty)
          )
          .withName("go from New file to download template file page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            NewFileUploadPage(_, InterestInLandOrProperty, Standard),
            Gen.const(false),
            (srn, _) => routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from New file to task list page is selected false")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => ViewChangeQuestionPage(srn),
            Gen.const(ViewReturn),
            (srn, _) => routes.ViewTaskListController.onPageLoad(srn, None)
          )
          .withName("go from view change question page to view Task list controller")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => ViewChangeQuestionPage(srn),
            Gen.const(ChangeReturn),
            (srn, _) => routes.UpdateMemberDetailsQuestionController.onPageLoad(srn)
          )
          .withName("go from view change question page to update member details question page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => UpdateMemberDetailsQuestionPage(srn),
            Gen.const(true),
            (srn, _) => routes.ViewChangeMembersController.onPageLoad(srn, 1, None)
          )
          .withName("go from view update member details question page to member detail list")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => UpdateMemberDetailsQuestionPage(srn),
            Gen.const(false),
            (srn, _) => routes.ChangeTaskListController.onPageLoad(srn)
          )
          .withName("go from view update member details question page to change task list")
      )

      act.like(
        normalmode
          .navigateTo(
            UpdateMembersDOBQuestionPage.apply,
            (srn, _) => routes.ViewChangePersonalDetailsController.onPageLoad(srn)
          )
          .withName("go from update members DoB page to V/C personal details page")
      )

      act.like(
        normalmode
          .navigateTo(
            ViewBasicDetailsCheckYourAnswersPage.apply,
            (srn, _) => routes.ViewTaskListController.onPageLoad(srn, None),
            _ => defaultUserAnswers.unsafeSet(ViewChangeQuestionPage(srn), ViewReturn)
          )
          .withName("go from basic details check your answers page to view task list")
      )

      act.like(
        normalmode
          .navigateTo(
            ViewBasicDetailsCheckYourAnswersPage.apply,
            (srn, _) => routes.ChangeTaskListController.onPageLoad(srn)
          )
          .withName("go from basic details check your answers page to change task list")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => RemoveMemberQuestionPage(srn),
            Gen.const(false),
            (srn, _) => routes.ViewChangeMembersController.onPageLoad(srn, 1, None)
          )
          .withName("go from remove member question to view/change members if user selected no")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => RemoveMemberQuestionPage(srn),
            Gen.const(true),
            (srn, _) => routes.RemoveMemberSuccessController.onPageLoad(srn)
          )
          .withName("go from remove member question to remove member success page if user selected yes")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => RemoveMemberSuccessPage(srn),
            Gen.const(true),
            (srn, _) => routes.UpdateAnotherMemberQuestionController.onPageLoad(srn)
          )
          .withName("go from remove member success page to update another member question")
      )

      act.like(
        normalmode
          .navigateToWithData(
            UpdateAnotherMemberQuestionPage.apply,
            Gen.const(true),
            (srn, _) => routes.ViewChangeMembersController.onPageLoad(srn, 1, None)
          )
          .withName("go from update another member question to v/c members")
      )

      act.like(
        normalmode
          .navigateTo(
            UpdateAnotherMemberQuestionPage.apply,
            (srn, _) => routes.ChangeTaskListController.onPageLoad(srn)
          )
          .withName("go from update another member question to change task list")
      )

      act.like(
        normalmode
          .navigateToWithData(
            UpdatePersonalDetailsMemberHasNinoQuestionPage.apply,
            Gen.const(true),
            (srn, _) => routes.ChangeMembersNinoController.onPageLoad(srn)
          )
          .withName("go from member has nino question to change nino page")
      )

      act.like(
        normalmode
          .navigateTo(
            UpdatePersonalDetailsMemberHasNinoQuestionPage.apply,
            (srn, _) => routes.ChangeMembersNoNinoReasonController.onPageLoad(srn)
          )
          .withName("go from member has nino question to reason no nino page")
      )

      act.like(
        normalmode
          .navigateTo(
            UpdateMemberNinoPage.apply,
            (srn, _) => routes.ViewChangePersonalDetailsController.onPageLoad(srn)
          )
          .withName("go from update nino to v/c personal details page")
      )

      act.like(
        normalmode
          .navigateTo(
            UpdateMembersFirstNamePage.apply,
            (srn, _) => routes.ViewChangePersonalDetailsController.onPageLoad(srn)
          )
          .withName("go from update first name to v/c personal details page")
      )

      act.like(
        normalmode
          .navigateTo(
            UpdateMembersLastNamePage.apply,
            (srn, _) => routes.ViewChangePersonalDetailsController.onPageLoad(srn)
          )
          .withName("go from update last name to v/c personal details page")
      )

      act.like(
        normalmode
          .navigateTo(
            UpdateMembersNoNinoReasonPage.apply,
            (srn, _) => routes.ViewChangePersonalDetailsController.onPageLoad(srn)
          )
          .withName("go from update reason no nino to v/c personal details page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => NewFileUploadPage(srn, journey = InterestInLandOrProperty, journeyType = Standard),
            Gen.const(true),
            (srn, _) => routes.DownloadTemplateFilePageController.onPageLoad(srn, InterestInLandOrProperty)
          )
          .withName("go from new file upload to outstanding return task list when selected yes")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => NewFileUploadPage(srn, journey = InterestInLandOrProperty, journeyType = Standard),
            Gen.const(false),
            (srn, _) => routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from new file upload to outstanding return task list when selected no")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => NewFileUploadPage(srn, journey = InterestInLandOrProperty, journeyType = Amend),
            Gen.const(true),
            (srn, _) => routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty, Amend)
          )
          .withName("go from new file upload to view/change task list when selected yes")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => NewFileUploadPage(srn, journey = InterestInLandOrProperty, journeyType = Amend),
            Gen.const(false),
            (srn, _) => routes.ChangeTaskListController.onPageLoad(srn)
          )
          .withName("go from new file upload to view/change task list when selected no")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => RemoveFilePage(srn, journey = InterestInLandOrProperty, journeyType = Standard),
            Gen.const(true),
            (srn, _) => routes.RemoveFileSuccessController.onPageLoad(srn, InterestInLandOrProperty, Standard)
          )
          .withName("go from remove file upload to new file upload in standard journey when selected yes")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => RemoveFileSuccessPage(srn, journey = InterestInLandOrProperty, Standard),
            Gen.const(true),
            (srn, _) => routes.JourneyContributionsHeldController.onPageLoad(srn, InterestInLandOrProperty, Standard)
          )
      )

      act.like(
        normalmode
          .navigateTo(
            UploadSuccessPage(_, InterestInLandOrProperty, Standard),
            (srn, _) => routes.TaskListController.onPageLoad(srn)
          )
          .withName("navigate to task list from upload success page (Standard)")
      )

      act.like(
        normalmode
          .navigateTo(
            UploadSuccessPage(_, InterestInLandOrProperty, Amend),
            (srn, _) => routes.ChangeTaskListController.onPageLoad(srn)
          )
          .withName("navigate to change task list from upload success page (Amend)")
      )

      act.like(
        normalmode
          .navigateTo(
            UploadErrorPage(_, InterestInLandOrProperty, Standard, UploadFormatError(validationErrorGen.sample.value)),
            (srn, _) => routes.FileUploadTooManyErrorsController.onPageLoad(srn, InterestInLandOrProperty, Standard)
          )
          .withName("navigate to file upload too many errors page if it's an upload format error")
      )

      act.like {
        val errors = Gen.listOfN(errorLimit, validationErrorGen).map(NonEmptyList.fromListUnsafe).sample.value
        normalmode
          .navigateTo(
            UploadErrorPage(_, InterestInLandOrProperty, Standard, UploadErrors(errors)),
            (srn, _) => routes.FileUploadErrorSummaryController.onPageLoad(srn, InterestInLandOrProperty, Standard)
          )
          .withName(s"navigate to error summary if the number of errors is <= the limit [$errorLimit]")
      }

      act.like {
        val errors = Gen.listOfN(errorLimit + 1, validationErrorGen).map(NonEmptyList.fromListUnsafe).sample.value
        normalmode
          .navigateTo(
            UploadErrorPage(_, InterestInLandOrProperty, Standard, UploadErrors(errors)),
            (srn, _) => routes.FileUploadTooManyErrorsController.onPageLoad(srn, InterestInLandOrProperty, Standard)
          )
          .withName(s"navigate to too many errors page if the number of errors is > the limit [$errorLimit]")
      }

      act.like(
        normalmode
          .navigateTo(
            UploadErrorSummaryPage(_, InterestInLandOrProperty, Standard),
            (srn, _) => routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty, Standard)
          )
          .withName(s"navigate to upload file controller from error summary page")
      )

      act.like(
        normalmode
          .navigateTo(
            FileUploadTooManyErrorsPage(_, InterestInLandOrProperty, Standard),
            (srn, _) => routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty, Standard)
          )
          .withName(s"navigate to upload file controller from too many errors page")
      )

      act.like(
        normalmode
          .navigateTo(
            DeclarationPage.apply,
            (srn, _) => routes.ReturnSubmittedController.onPageLoad(srn)
          )
          .withName(s"navigate to return submitted from declaration page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => RemoveFilePage(srn, journey = InterestInLandOrProperty, journeyType = Standard),
            Gen.const(false),
            (srn, _) =>
              routes.NewFileUploadController
                .onPageLoad(srn, journey = InterestInLandOrProperty, journeyType = Standard)
          )
          .withName("go from remove file upload to new file upload in standard journey when selected no")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => RemoveFilePage(srn, journey = InterestInLandOrProperty, journeyType = Standard),
            Gen.const(true),
            (srn, _) =>
              routes.RemoveFileSuccessController
                .onPageLoad(srn, journey = InterestInLandOrProperty, journeyType = Standard)
          )
          .withName("go from remove file upload to JourneyContributionsHeld in standard journey when selected yes")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => RemoveFilePage(srn, journey = InterestInLandOrProperty, journeyType = Amend),
            Gen.const(false),
            (srn, _) =>
              routes.NewFileUploadController
                .onPageLoad(srn, journey = InterestInLandOrProperty, journeyType = Amend)
          )
          .withName("go from remove file upload to new file upload in amend journey when selected no")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => RemoveFilePage(srn, journey = InterestInLandOrProperty, journeyType = Amend),
            Gen.const(true),
            (srn, _) =>
              routes.RemoveFileSuccessController
                .onPageLoad(srn, journey = InterestInLandOrProperty, journeyType = Amend)
          )
          .withName("go from remove file upload to JourneyContributionsHeld in amend journey when selected yes")
      )

    }

    "CheckMode" - {
      act.like(
        checkmode
          .navigateTo(_ => UnknownPage, (_, _) => routes.IndexController.onPageLoad)
          .withName("redirect any unknown pages to index page")
      )

      act.like(
        checkmode
          .navigateToWithData(
            CheckReturnDatesPage.apply,
            Gen.const(true),
            (srn, _) => routes.AssetsHeldController.onPageLoad(srn)
          )
          .withName("go from check return dates to assets held page")
      )

      act.like(
        checkmode
          .navigateTo(
            CheckReturnDatesPage.apply,
            (srn, _) => accountingperiod.routes.AccountingPeriodController.onPageLoad(srn, 1, NormalMode)
          )
          .withName("go from check return dates to accounting period")
      )

      act.like(
        checkmode
          .navigateTo(
            BasicDetailsCheckYourAnswersPage.apply,
            (srn, _) => routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from basic details check to task list")
      )

      act.like(
        checkmode
          .navigateTo(
            DownloadTemplateFilePage(_, InterestInLandOrProperty, Standard),
            (srn, _) => routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty, Standard)
          )
          .withName("go from download template page to upload file page")
      )

      act.like(
        checkmode
          .navigateToWithData(
            CheckFileNamePage(_, InterestInLandOrProperty, Standard),
            Gen.const(true),
            (srn, _) => routes.LoadingPageController.onPageLoad(srn, Validating, InterestInLandOrProperty, Standard)
          )
          .withName("go from check file name page to loading page")
      )

      act.like(
        checkmode
          .navigateTo(
            CheckFileNamePage(_, InterestInLandOrProperty, Standard),
            (srn, _) => routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty, Standard)
          )
          .withName("go from check file name page to upload file page")
      )

      act.like(
        checkmode
          .navigateToWithData(
            NewFileUploadPage(_, InterestInLandOrProperty, Standard),
            Gen.const(true),
            (srn, _) => routes.DownloadTemplateFilePageController.onPageLoad(srn, InterestInLandOrProperty)
          )
          .withName("go from new file upload [Standard] to download template file page")
      )

      act.like(
        checkmode
          .navigateToWithData(
            NewFileUploadPage(_, InterestInLandOrProperty, Amend),
            Gen.const(true),
            (srn, _) => routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty, Amend)
          )
          .withName("go from new file upload [Amend] to upload file page")
      )

      act.like(
        checkmode
          .navigateTo(
            NewFileUploadPage(_, InterestInLandOrProperty, Standard),
            (srn, _) => routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from new file upload [Standard] to task list page")
      )

      act.like(
        checkmode
          .navigateTo(
            NewFileUploadPage(_, InterestInLandOrProperty, Amend),
            (srn, _) => routes.ChangeTaskListController.onPageLoad(srn)
          )
          .withName("go from new file upload [Amend] to change task list page")
      )

      act.like(
        checkmode
          .navigateTo(
            UploadSuccessPage(_, InterestInLandOrProperty, Standard),
            (srn, _) => routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from upload success [Standard] to task list page")
      )

      act.like(
        checkmode
          .navigateTo(
            UploadSuccessPage(_, InterestInLandOrProperty, Amend),
            (srn, _) => routes.ChangeTaskListController.onPageLoad(srn)
          )
          .withName("go from upload success [Amend] to task list page")
      )

      act.like(
        checkmode
          .navigateTo(
            DeclarationPage(_),
            (srn, _) => routes.ReturnSubmittedController.onPageLoad(srn)
          )
          .withName("go from declaration to return submitted page")
      )

      act.like(
        checkmode
          .navigateTo(
            ViewBasicDetailsCheckYourAnswersPage(_),
            (srn, _) => routes.ViewTaskListController.onPageLoad(srn, None),
            srn => defaultUserAnswers.unsafeSet(ViewChangeQuestionPage(srn), ViewReturn)
          )
          .withName("go from view basic details check your answers to view task list page")
      )

      act.like(
        checkmode
          .navigateTo(
            ViewBasicDetailsCheckYourAnswersPage(_),
            (srn, _) => routes.ChangeTaskListController.onPageLoad(srn)
          )
          .withName("go from view basic details check your answers to change task list page")
      )

      act.like(
        checkmode
          .navigateToWithData(
            JourneyContributionsHeldPage(_, InterestInLandOrProperty, Standard),
            Gen.const(true),
            (srn, _) =>
              routes.DownloadTemplateFilePageController
                .onPageLoad(srn, InterestInLandOrProperty)
          )
          .withName(
            "go from Land or property contribution page (normal) to download template file page if yes selected"
          )
      )

      act.like(
        checkmode
          .navigateToWithData(
            JourneyContributionsHeldPage(_, InterestInLandOrProperty, Amend),
            Gen.const(true),
            (srn, _) =>
              routes.UploadFileController
                .onPageLoad(srn, InterestInLandOrProperty, JourneyType.Amend)
          )
          .withName("go from Land or property contribution page (amend) to upload template file page if yes selected")
      )

      act.like(
        checkmode
          .navigateToWithData(
            JourneyContributionsHeldPage(_, InterestInLandOrProperty, Standard),
            Gen.const(false),
            (srn, _) => routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from Land or property contribution page (normal) to task list page if no selected")
      )

      act.like(
        checkmode
          .navigateToWithData(
            JourneyContributionsHeldPage(_, InterestInLandOrProperty, Amend),
            Gen.const(false),
            (srn, _) => routes.ChangeTaskListController.onPageLoad(srn)
          )
          .withName("go from Land or property contribution page (Amend) to task list page if no selected")
      )
    }

    act.like(
      checkmode
        .navigateToWithData(
          srn => RemoveFilePage(srn, journey = InterestInLandOrProperty, journeyType = Standard),
          Gen.const(false),
          (srn, _) =>
            routes.NewFileUploadController
              .onPageLoad(srn, journey = InterestInLandOrProperty, journeyType = Standard)
        )
        .withName("go from remove file upload to new file upload in standard journey when selected no")
    )

    act.like(
      checkmode
        .navigateToWithData(
          srn => RemoveFilePage(srn, journey = InterestInLandOrProperty, journeyType = Standard),
          Gen.const(true),
          (srn, _) =>
            routes.JourneyContributionsHeldController
              .onPageLoad(srn, journey = InterestInLandOrProperty, journeyType = Standard)
        )
        .withName("go from remove file upload to JourneyContributionsHeld in standard journey when selected yes")
    )

    act.like(
      checkmode
        .navigateToWithData(
          srn => RemoveFilePage(srn, journey = InterestInLandOrProperty, journeyType = Amend),
          Gen.const(false),
          (srn, _) =>
            routes.NewFileUploadController
              .onPageLoad(srn, journey = InterestInLandOrProperty, journeyType = Amend)
        )
        .withName("go from remove file upload to new file upload in amend journey when selected no")
    )

    act.like(
      normalmode
        .navigateToWithData(
          srn => RemoveFilePage(srn, journey = InterestInLandOrProperty, journeyType = Amend),
          Gen.const(true),
          (srn, _) =>
            routes.RemoveFileSuccessController
              .onPageLoad(srn, journey = InterestInLandOrProperty, journeyType = Amend)
        )
        .withName("go from remove file upload to JourneyContributionsHeld in amend journey when selected yes")
    )
  }
}
