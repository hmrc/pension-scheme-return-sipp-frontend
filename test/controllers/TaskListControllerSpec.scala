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

import config.Refined.OneToThree
import eu.timepit.refined.refineMV
import models.{DateRange, NormalMode, UserAnswers}
import pages.CheckReturnDatesPage
import pages.accountingperiod.AccountingPeriodPage
import pages.landorproperty.LandOrPropertyContributionsPage
import pages.memberdetails.CheckMemberDetailsFilePage
import services.{TaxYearService, TaxYearServiceImpl}
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
            .unsafeSet(CheckMemberDetailsFilePage(srn), true)

        testViewModel(
          userAnswers,
          2,
          0,
          expectedStatus = TaskListStatus.NotStarted,
          expectedTitleKey = "tasklist.landorproperty.title",
          expectedLinkContentKey = "tasklist.landorproperty.interest.title",
          expectedLinkUrl =
            controllers.landorproperty.routes.LandOrPropertyContributionsController.onPageLoad(srn, NormalMode).url
        )
      }
    }

    "completed" in {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(AccountingPeriodPage(srn, refineMV[OneToThree](1), NormalMode), dateRange)
          .unsafeSet(CheckMemberDetailsFilePage(srn), true)
          .unsafeSet(LandOrPropertyContributionsPage(srn), true)

      testViewModel(
        userAnswers,
        2,
        0,
        expectedStatus = TaskListStatus.Completed,
        expectedTitleKey = "tasklist.landorproperty.title",
        expectedLinkContentKey = "tasklist.landorproperty.interest.title",
        expectedLinkUrl =
          controllers.landorproperty.routes.LandOrPropertyContributionsController.onPageLoad(srn, NormalMode).url
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
    sections(sectionIndex).items.fold(
      _ => "",
      list => {
        val item = list.toList(itemIndex)
        item.status mustBe expectedStatus
        item.link match {
          case LinkMessage(content, url, attrs) =>
            content.key mustBe expectedLinkContentKey
            url mustBe expectedLinkUrl

          case Message(key, args) =>
            key mustBe expectedLinkContentKey

          case other => fail(s"unexpected display message $other")
        }
      }
    )
  }
}
