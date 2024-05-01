/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import config.RefinedTypes.OneToThree
import eu.timepit.refined.refineMV
import models.Journey.{AssetFromConnectedParty, InterestInLandOrProperty, MemberDetails, OutstandingLoans, TangibleMoveableProperty, UnquotedShares}
import models.{DateRange, NormalMode, UserAnswers}
import pages.accountingperiod.AccountingPeriodPage
import pages.{CheckFileNamePage, CheckReturnDatesPage, JourneyContributionsHeldPage}
import services.TaxYearServiceImpl
import viewmodels.DisplayMessage.{LinkMessage, Message}
import viewmodels.models.TaskListStatus
import viewmodels.models.TaskListStatus.TaskListStatus
import views.html.TaskListView

class TaskListControllerSpec extends ControllerBaseSpec {

  val taxYearService = new TaxYearServiceImpl()
  val schemeDateRange: DateRange = DateRange.from(taxYearService.current)

  "TaskListController" - {

    lazy val viewModel = TaskListController.viewModel(
      srn,
      schemeName,
      schemeDateRange.from,
      schemeDateRange.to,
      defaultUserAnswers
    )
    lazy val onPageLoad = routes.TaskListController.onPageLoad(srn)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[TaskListView]
      view(viewModel)
    }.withName("task list renders OK"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    "schemeDetailsSection - basic details" - {
      "NotStarted" - {
        "check dates page absent" in {
          testViewModel(
            defaultUserAnswers,
            0,
            0,
            expectedStatus = TaskListStatus.NotStarted,
            expectedTitleKey = "tasklist.schemedetails.title",
            expectedLinkContentKey = "tasklist.schemedetails.details.title",
            expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
          )
        }
      }

      "inProgress" - {
        "stopped after dates page" in {
          val userAnswersPopulated =
            defaultUserAnswers
              .unsafeSet(CheckReturnDatesPage(srn), false)

          testViewModel(
            userAnswersPopulated,
            0,
            0,
            expectedStatus = TaskListStatus.InProgress,
            expectedTitleKey = "tasklist.schemedetails.title",
            expectedLinkContentKey = "tasklist.schemedetails.details.title",
            expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
          )
        }
      }

      "completed" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)

        testViewModel(
          userAnswers,
          0,
          0,
          expectedStatus = TaskListStatus.Completed,
          expectedTitleKey = "tasklist.schemedetails.title",
          expectedLinkContentKey = "tasklist.schemedetails.details.title",
          expectedLinkUrl = controllers.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
        )
      }
    }
  }

  "schemeDetailsSection - land or property interest" - {

    "Incomplete" - {
      "basic details section not complete" in {
        testViewModel(
          defaultUserAnswers,
          2,
          0,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.landorproperty.title",
          expectedLinkContentKey = "tasklist.landorproperty.interest.title",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "yes / no is not selected" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)
            .unsafeSet(CheckFileNamePage(srn, MemberDetails), true)

        testViewModel(
          userAnswers,
          2,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.landorproperty.title",
          expectedLinkContentKey = "tasklist.landorproperty.interest.title",
          expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
            .onPageLoad(srn, InterestInLandOrProperty, NormalMode)
            .url
        )
      }
    }

    "completed" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)
          .unsafeSet(CheckFileNamePage(srn, MemberDetails), true)
          .unsafeSet(JourneyContributionsHeldPage(srn, InterestInLandOrProperty), true)

      testViewModel(
        userAnswers,
        2,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.landorproperty.title",
        expectedLinkContentKey = "tasklist.landorproperty.interest.title.change",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, InterestInLandOrProperty, NormalMode)
          .url
      )
    }
  }

  "schemeDetailsSection - Tangible moveable property" - {

    "Incomplete" - {
      "basic details section not complete" in {
        testViewModel(
          defaultUserAnswers,
          3,
          0,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.tangibleproperty.title",
          expectedLinkContentKey = "tasklist.tangibleproperty.details.title",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "yes / no is not selected" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)
            .unsafeSet(CheckFileNamePage(srn, MemberDetails), true)

        testViewModel(
          userAnswers,
          3,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.tangibleproperty.title",
          expectedLinkContentKey = "tasklist.tangibleproperty.details.title",
          expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
            .onPageLoad(srn, TangibleMoveableProperty, NormalMode)
            .url
        )
      }
    }

    "completed" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)
          .unsafeSet(CheckFileNamePage(srn, MemberDetails), true)
          .unsafeSet(JourneyContributionsHeldPage(srn, TangibleMoveableProperty), true)

      testViewModel(
        userAnswers,
        3,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.tangibleproperty.title",
        expectedLinkContentKey = "tasklist.tangibleproperty.details.title.change",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, TangibleMoveableProperty, NormalMode)
          .url
      )
    }
  }

  "schemeDetailsSection - Outstanding loans" - {

    "Incomplete" - {
      "basic details section not complete" in {
        testViewModel(
          defaultUserAnswers,
          4,
          0,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.loans.title",
          expectedLinkContentKey = "tasklist.loans.details.title",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "yes / no is not selected" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)
            .unsafeSet(CheckFileNamePage(srn, MemberDetails), true)

        testViewModel(
          userAnswers,
          4,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.loans.title",
          expectedLinkContentKey = "tasklist.loans.details.title",
          expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
            .onPageLoad(srn, OutstandingLoans, NormalMode)
            .url
        )
      }
    }

    "completed" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)
          .unsafeSet(CheckFileNamePage(srn, MemberDetails), true)
          .unsafeSet(JourneyContributionsHeldPage(srn, OutstandingLoans), true)

      testViewModel(
        userAnswers,
        4,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.loans.title",
        expectedLinkContentKey = "tasklist.loans.details.title.change",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, OutstandingLoans, NormalMode)
          .url
      )
    }
  }

  "schemeDetailsSection - Unquoted shares" - {

    "Incomplete" - {
      "basic details section not complete" in {
        testViewModel(
          defaultUserAnswers,
          5,
          0,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.shares.title",
          expectedLinkContentKey = "tasklist.shares.details.title",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "yes / no is not selected" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)
            .unsafeSet(CheckFileNamePage(srn, MemberDetails), true)

        testViewModel(
          userAnswers,
          5,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.shares.title",
          expectedLinkContentKey = "tasklist.shares.details.title",
          expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
            .onPageLoad(srn, UnquotedShares, NormalMode)
            .url
        )
      }
    }

    "completed" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)
          .unsafeSet(CheckFileNamePage(srn, MemberDetails), true)
          .unsafeSet(JourneyContributionsHeldPage(srn, UnquotedShares), true)

      testViewModel(
        userAnswers,
        5,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.shares.title",
        expectedLinkContentKey = "tasklist.shares.details.title.change",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, UnquotedShares, NormalMode)
          .url
      )
    }
  }

  "schemeDetailsSection - Asset from a connected party" - {

    "Incomplete" - {
      "basic details section not complete" in {
        testViewModel(
          defaultUserAnswers,
          6,
          0,
          expectedStatus = TaskListStatus.UnableToStart,
          expectedTitleKey = "tasklist.assets.title",
          expectedLinkContentKey = "tasklist.assets.details.title",
          expectedLinkUrl = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "NotStarted" - {
      "yes / no is not selected" in {
        val userAnswers =
          defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), true)
            .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)
            .unsafeSet(CheckFileNamePage(srn, MemberDetails), true)

        testViewModel(
          userAnswers,
          6,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.assets.title",
          expectedLinkContentKey = "tasklist.assets.details.title",
          expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
            .onPageLoad(srn, AssetFromConnectedParty, NormalMode)
            .url
        )
      }
    }

    "completed" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)
          .unsafeSet(CheckFileNamePage(srn, MemberDetails), true)
          .unsafeSet(JourneyContributionsHeldPage(srn, AssetFromConnectedParty), true)

      testViewModel(
        userAnswers,
        6,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.assets.title",
        expectedLinkContentKey = "tasklist.assets.details.title.change",
        expectedLinkUrl = controllers.routes.JourneyContributionsHeldController
          .onPageLoad(srn, AssetFromConnectedParty, NormalMode)
          .url
      )
    }
  }

  private def testViewModel(
    userAnswersPopulated: UserAnswers,
    sectionIndex: Int,
    itemIndex: Int,
    expectedStatus: TaskListStatus,
    expectedTitleKey: String,
    expectedLinkContentKey: String,
    expectedLinkUrl: String
  ) = {
    val customViewModel = TaskListController.viewModel(
      srn,
      schemeName,
      schemeDateRange.from,
      schemeDateRange.to,
      userAnswersPopulated
    )
    val sections = customViewModel.page.sections.toList
    sections(sectionIndex).title.key mustBe expectedTitleKey
    sections(sectionIndex).items.foreach { list =>
      val item = list.toList(itemIndex)
      item.status mustBe expectedStatus
      item.link match {
        case LinkMessage(content, url, _) =>
          content.key mustBe expectedLinkContentKey
          url mustBe expectedLinkUrl

        case Message(key, _) =>
          key mustBe expectedLinkContentKey

        case other => fail(s"unexpected display message $other")
      }
    }
  }
}
