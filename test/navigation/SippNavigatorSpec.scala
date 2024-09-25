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
import models.FileAction.Validating
import models.Journey.InterestInLandOrProperty
import models.JourneyType
import models.TypeOfViewChangeQuestion.{ChangeReturn, ViewReturn}
import org.scalacheck.Gen
import pages._
import services.validation.csv.CsvDocumentValidatorConfig
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class SippNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  private val mockConfig = mock[CsvDocumentValidatorConfig]
  when(mockConfig.errorLimit).thenReturn(25)

  val navigator: Navigator = new SippNavigator(mockConfig)
  val journeyType = JourneyType.Standard

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
          .navigateToWithData(
            CheckReturnDatesPage,
            Gen.const(true),
            (srn, _) => controllers.routes.AssetsHeldController.onPageLoad(srn)
          )
          .withName("go from check return dates page to assets held page when yes is selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            AssetsHeldPage,
            Gen.const(false),
            controllers.routes.BasicDetailsCheckYourAnswersController.onPageLoad
          )
          .withName("go from assets held page to declaration page when no is selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            AssetsHeldPage,
            Gen.const(true),
            controllers.routes.BasicDetailsCheckYourAnswersController.onPageLoad
          )
          .withName("go from assets held page to basic check your answers page when yes is selected")
      )

      act.like(
        normalmode
          .navigateTo(
            BasicDetailsCheckYourAnswersPage,
            (srn, _) => controllers.routes.TaskListController.onPageLoad(srn),
            srn => defaultUserAnswers.unsafeSet(AssetsHeldPage(srn), true)
          )
          .withName("go from check your answers to task list page if any assets selected yes")
      )

      act.like(
        normalmode
          .navigateTo(
            BasicDetailsCheckYourAnswersPage,
            (srn, _) => controllers.routes.DeclarationController.onPageLoad(srn, None),
            srn => defaultUserAnswers.unsafeSet(AssetsHeldPage(srn), false)
          )
          .withName("go from check your answers to declaration page if any assets selected no")
      )

      act.like(
        normalmode
          .navigateToWithData(
            JourneyContributionsHeldPage(_, InterestInLandOrProperty),
            Gen.const(true),
            (srn, _) => controllers.routes.DownloadTemplateFilePageController.onPageLoad(srn, InterestInLandOrProperty, journeyType)
          )
          .withName("go from Land or property contribution page to download template file page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            JourneyContributionsHeldPage(_, InterestInLandOrProperty),
            Gen.const(false),
            (srn, _) => controllers.routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from Land or property contribution page to task list page if no selected")
      )

      act.like(
        normalmode
          .navigateToWithData(
            CheckFileNamePage(_, InterestInLandOrProperty, JourneyType.Standard),
            Gen.const(false),
            (srn, _) =>
              controllers.routes.UploadFileController.onPageLoad(srn, InterestInLandOrProperty, JourneyType.Standard)
          )
          .withName("go from check your interest land or property file page to upload page again if user selects no")
      )

      act.like(
        normalmode
          .navigateToWithData(
            CheckFileNamePage(_, InterestInLandOrProperty, journeyType),
            Gen.const(true),
            (srn, _) => controllers.routes.LoadingPageController.onPageLoad(srn, Validating, InterestInLandOrProperty, journeyType)
          )
          .withName("go from check your interest land or property file page to validating page if user selects yes")
      )

      act.like(
        normalmode
          .navigateToWithData(
            NewFileUploadPage(_, InterestInLandOrProperty, journeyType),
            Gen.const(true),
            (srn, _) => controllers.routes.DownloadTemplateFilePageController.onPageLoad(srn, InterestInLandOrProperty, journeyType)
          )
          .withName("go from New file to download template file page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            NewFileUploadPage(_, InterestInLandOrProperty, journeyType),
            Gen.const(false),
            (srn, _) => controllers.routes.TaskListController.onPageLoad(srn)
          )
          .withName("go from New file to task list page is selected false")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => ViewChangeQuestionPage(srn),
            Gen.const(ViewReturn),
            (srn, _) => controllers.routes.ViewTaskListController.onPageLoad(srn, None)
          )
          .withName("go from view change question page to view Task list controller")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => ViewChangeQuestionPage(srn),
            Gen.const(ChangeReturn),
            (srn, _) => controllers.routes.UpdateMemberDetailsQuestionController.onPageLoad(srn)
          )
          .withName("go from view change question page to update member details question page")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => UpdateMemberDetailsQuestionPage(srn),
            Gen.const(true),
            (srn, _) => controllers.routes.ViewChangeMembersController.onPageLoad(srn, 1, None)
          )
          .withName("go from view update member details question page to member detail list")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => UpdateMemberDetailsQuestionPage(srn),
            Gen.const(false),
            (srn, _) => controllers.routes.ChangeTaskListController.onPageLoad(srn)
          )
          .withName("go from view update member details question page to change task list")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => RemoveMemberQuestionPage(srn),
            Gen.const(false),
            (srn, _) => controllers.routes.ViewChangeMembersController.onPageLoad(srn, 1, None)
          )
          .withName("go from remove member question to view/change members if user selected no")
      )

      act.like(
        normalmode
          .navigateToWithData(
            srn => RemoveMemberQuestionPage(srn),
            Gen.const(true),
            (srn, _) => controllers.routes.ViewChangeMembersController.onPageLoad(srn, 1, None)
          )
          .withName("go from remove member question to view/change members if user selected yes")
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
