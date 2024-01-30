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

package controllers

import cats.data.NonEmptyList
import cats.implicits.toShow
import com.google.inject.Inject
import controllers.actions._
import models.SchemeId.Srn
import models.{DateRange, NormalMode, UserAnswers}
import pages.CheckReturnDatesPage
import pages.accountingperiod.AccountingPeriods
import pages.landorproperty.LandOrPropertyContributionsPage
import pages.memberdetails.CheckMemberDetailsFilePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.TaxYearService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{Heading2, InlineMessage, LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.TaskListStatus._
import viewmodels.models._
import views.html.TaskListView

import java.time.LocalDate

class TaskListController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView,
  taxYearService: TaxYearService
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val dates = DateRange.from(taxYearService.current)
    val viewModel = TaskListController.viewModel(
      srn,
      request.schemeDetails.schemeName,
      dates.from,
      dates.to,
      request.userAnswers
    )
    Ok(view(viewModel))
  }
}

object TaskListController {

  def messageKey(prefix: String, suffix: String, status: TaskListStatus): String =
    status match {
      case UnableToStart | NotStarted => s"$prefix.add.$suffix"
      case _ => s"$prefix.change.$suffix"
    }

  private def schemeDetailsSection(
    srn: Srn,
    schemeName: String,
    userAnswers: UserAnswers
  ): TaskListSectionViewModel = {
    val prefix = "tasklist.schemedetails"

    TaskListSectionViewModel(
      s"$prefix.title",
      getBasicSchemeDetailsTaskListItem(srn, schemeName, prefix, userAnswers)
    )
  }

  private def getBasicSchemeDetailsTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers
  ): TaskListItemViewModel = {
    val taskListStatus: TaskListStatus = schemeDetailsStatus(srn, userAnswers)

    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        taskListStatus match {
          case Completed =>
            controllers.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
          case _ =>
            controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
        }
      ),
      taskListStatus
    )
  }

  private def memberDetailsSection(
    srn: Srn,
    schemeName: String,
    userAnswers: UserAnswers
  ): TaskListSectionViewModel = {
    val prefix = "tasklist.members"

    TaskListSectionViewModel(
      s"$prefix.title",
      getMemberDetailsTaskListItem(srn, schemeName, prefix, userAnswers)
    )
  }

  private def getMemberDetailsTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers
  ): TaskListItemViewModel = {
    val taskListStatus: TaskListStatus = memberDetailsStatus(srn, userAnswers)

    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        taskListStatus match {
          case InProgress =>
            controllers.memberdetails.routes.CheckMemberDetailsFileController.onPageLoad(srn, NormalMode).url
          case _ =>
            controllers.routes.DownloadTemplateFilePageController.onPageLoad(srn).url
        }
      ),
      taskListStatus
    )
  }

  private def landOrPropertySection(
    srn: Srn,
    schemeName: String,
    userAnswers: UserAnswers
  ): TaskListSectionViewModel = {
    val prefix = "tasklist.landorproperty"

    TaskListSectionViewModel(
      s"$prefix.title",
      getLandOrPropertyInterestTaskListItem(srn, schemeName, prefix, userAnswers),
      getLandOrPropertyArmsLengthTaskListItem(srn, schemeName, prefix, userAnswers)
    )
  }

  private def getLandOrPropertyInterestTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers
  ): TaskListItemViewModel = {
    val taskListStatus: TaskListStatus = landOrPropertyInterestStatus(srn, userAnswers)

    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(s"$prefix.interest.title", schemeName),
        controllers.landorproperty.routes.LandOrPropertyContributionsController.onPageLoad(srn, NormalMode).url
      ),
      taskListStatus,
      srn,
      userAnswers
    )

    TaskListItemViewModel(message, status)
  }


  private def getLandOrPropertyArmsLengthTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers
  ): TaskListItemViewModel = {
    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(s"$prefix.armslength.title", schemeName),
        controllers.routes.UnauthorisedController.onPageLoad.url
      ),
      NotStarted,
      srn,
      userAnswers
    )

    TaskListItemViewModel(message, status)
  }

  private def tangiblePropertySection(
    srn: Srn,
    schemeName: String,
    userAnswers: UserAnswers
  ): TaskListSectionViewModel = {
    val prefix = "tasklist.tangibleproperty"

    TaskListSectionViewModel(
      s"$prefix.title",
      getTangiblePropertyTaskListItem(srn, schemeName, prefix, userAnswers)
    )
  }

  private def getTangiblePropertyTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers
  ): TaskListItemViewModel = {
    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.UnauthorisedController.onPageLoad.url
      ),
      NotStarted,
      srn,
      userAnswers
    )

    TaskListItemViewModel(message, status)
  }

  private def loanSection(
    srn: Srn,
    schemeName: String,
    userAnswers: UserAnswers
  ): TaskListSectionViewModel = {
    val prefix = "tasklist.loans"

    TaskListSectionViewModel(
      s"$prefix.title",
      getLoanTaskListItem(srn, schemeName, prefix, userAnswers)
    )
  }

  private def getLoanTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers
  ): TaskListItemViewModel = {
    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.UnauthorisedController.onPageLoad.url
      ),
      NotStarted,
      srn,
      userAnswers
    )

    TaskListItemViewModel(message, status)
  }

  private def sharesSection(
    srn: Srn,
    schemeName: String,
    userAnswers: UserAnswers
  ): TaskListSectionViewModel = {
    val prefix = "tasklist.shares"

    TaskListSectionViewModel(
      s"$prefix.title",
      getSharesTaskListItem(srn, schemeName, prefix, userAnswers)
    )
  }

  private def getSharesTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers
  ): TaskListItemViewModel = {

    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.UnauthorisedController.onPageLoad.url
      ),
      NotStarted,
      srn,
      userAnswers
    )

    TaskListItemViewModel(message, status)
  }

  private def assetsSection(
                             srn: Srn,
                             schemeName: String,
                             userAnswers: UserAnswers
                           ): TaskListSectionViewModel = {
    val prefix = "tasklist.assets"

    TaskListSectionViewModel(
      s"$prefix.title",
      getAssetsTaskListItem(srn, schemeName, prefix, userAnswers)
    )
  }

  private def getAssetsTaskListItem(
     srn: Srn,
     schemeName: String,
     prefix: String,
     userAnswers: UserAnswers
   ): TaskListItemViewModel = {

    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.UnauthorisedController.onPageLoad.url
      ),
      NotStarted,
      srn,
      userAnswers
    )

    TaskListItemViewModel(message, status)
  }

  private val declarationSection = {
    val prefix = "tasklist.declaration"

    TaskListSectionViewModel(
      s"$prefix.title",
      Right(NonEmptyList.of(
        TaskListItemViewModel(
          Message(s"$prefix.incomplete"),
          UnableToStart
        )
      )),
      Some(
        LinkMessage(
          s"$prefix.saveandreturn",
          controllers.routes.UnauthorisedController.onPageLoad.url
        )
      )
    )
  }

  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    userAnswers: UserAnswers
  ): PageViewModel[TaskListViewModel] = {

    val viewModel = TaskListViewModel(
      schemeDetailsSection(srn, schemeName, userAnswers),
      memberDetailsSection(srn, schemeName, userAnswers),
      landOrPropertySection(srn, schemeName, userAnswers),
      tangiblePropertySection(srn, schemeName, userAnswers),
      loanSection(srn, schemeName, userAnswers),
      sharesSection(srn, schemeName, userAnswers),
      assetsSection(srn, schemeName, userAnswers),
      declarationSection
    )

    val items = viewModel.sections.toList.flatMap(_.items.fold(_ => Nil, _.toList))
    val completed = items.count(_.status == Completed)
    val total = items.length

    PageViewModel(
      Message("tasklist.title", startDate.show, endDate.show),
      Message("tasklist.heading", startDate.show, endDate.show),
      viewModel
    ).withDescription(
      Heading2("tasklist.subheading") ++
        ParagraphMessage(Message("tasklist.description", completed, total))
    )
  }

  def schemeDetailsStatus(srn: Srn, userAnswers: UserAnswers): TaskListStatus = {
    val checkReturnDates = userAnswers.get(CheckReturnDatesPage(srn))
    val accountingPeriods: List[DateRange] = userAnswers.list(AccountingPeriods(srn))

    if (checkReturnDates.isEmpty) {
      NotStarted
    } else if (checkReturnDates.contains(false) && accountingPeriods.isEmpty) {
      InProgress
    } else {
      Completed
    }
  }

  def memberDetailsStatus(srn: Srn, userAnswers: UserAnswers): TaskListStatus = {
    val checkMemberDetailsFilePage = userAnswers.get(CheckMemberDetailsFilePage(srn))

    checkMemberDetailsFilePage match {
      case Some(checked) => if (checked) Completed else InProgress
      case _ => NotStarted
    }
  }

  def landOrPropertyInterestStatus(srn: Srn, userAnswers: UserAnswers): TaskListStatus = {
    val landOrPropertyContributionsPage: Option[Boolean] = userAnswers.get(LandOrPropertyContributionsPage(srn))

    landOrPropertyContributionsPage match {
      case Some(_) => Completed
      case _ => NotStarted
    }
  }
  def checkQuestionLock(
    linkMessage: LinkMessage,
    taskListStatus: TaskListStatus,
    srn: Srn,
    userAnswers: UserAnswers
  ): (InlineMessage, TaskListStatus) = {
    val schemeDetails = schemeDetailsStatus(srn, userAnswers)
    val memberDetails = memberDetailsStatus(srn, userAnswers)

    (schemeDetails, memberDetails) match {
      case (Completed, Completed) => linkMessage -> taskListStatus
      case _ => linkMessage.content -> UnableToStart
    }
  }
}
