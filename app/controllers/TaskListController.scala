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
import models.Journey.{
  ArmsLengthLandOrProperty,
  AssetFromConnectedParty,
  InterestInLandOrProperty,
  OutstandingLoans,
  TangibleMoveableProperty,
  UnquotedShares
}
import models.SchemeId.Srn
import models.{DateRange, Journey, NormalMode, UserAnswers}
import pages.accountingperiod.AccountingPeriods
import pages.{CheckReturnDatesPage, TaskListStatusPage}
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

  def messageKey(prefix: String, section: String, status: TaskListStatus): String =
    status match {
      case UnableToStart | NotStarted | InProgress | CompletedWithoutUpload => s"$prefix.$section.title"
      case _ => s"$prefix.$section.title.change"
    }

  def messageLink(srn: Srn, journey: Journey, status: TaskListStatus): String =
    status match {
      case UnableToStart | NotStarted | InProgress | CompletedWithoutUpload =>
        controllers.routes.JourneyContributionsHeldController.onPageLoad(srn, journey, NormalMode).url
      case _ =>
        controllers.routes.NewFileUploadController.onPageLoad(srn, journey).url
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
    val taskListStatus: TaskListStatus = getTaskListStatus(srn, InterestInLandOrProperty, userAnswers)

    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(messageKey(prefix, "interest", taskListStatus), schemeName),
        messageLink(srn, InterestInLandOrProperty, taskListStatus)
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
    val taskListStatus: TaskListStatus = getTaskListStatus(srn, ArmsLengthLandOrProperty, userAnswers)

    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(messageKey(prefix, "armslength", taskListStatus), schemeName),
        messageLink(srn, ArmsLengthLandOrProperty, taskListStatus)
      ),
      taskListStatus,
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

    val taskListStatus: TaskListStatus = getTaskListStatus(srn, TangibleMoveableProperty, userAnswers)

    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(messageKey(prefix, "details", taskListStatus), schemeName),
        messageLink(srn, TangibleMoveableProperty, taskListStatus)
      ),
      taskListStatus,
      srn,
      userAnswers
    )

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(message, status)
    )
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
    val taskListStatus: TaskListStatus = getTaskListStatus(srn, OutstandingLoans, userAnswers)

    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(messageKey(prefix, "details", taskListStatus), schemeName),
        messageLink(srn, OutstandingLoans, taskListStatus)
      ),
      taskListStatus,
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
    val taskListStatus: TaskListStatus = getTaskListStatus(srn, UnquotedShares, userAnswers)

    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(messageKey(prefix, "details", taskListStatus), schemeName),
        messageLink(srn, UnquotedShares, taskListStatus)
      ),
      taskListStatus,
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
    val taskListStatus: TaskListStatus = getTaskListStatus(srn, AssetFromConnectedParty, userAnswers)

    val (message, status) = checkQuestionLock(
      LinkMessage(
        Message(messageKey(prefix, "details", taskListStatus), schemeName),
        messageLink(srn, AssetFromConnectedParty, taskListStatus)
      ),
      taskListStatus,
      srn,
      userAnswers
    )

    TaskListItemViewModel(message, status)
  }

  private def declarationSection(isLinkVisible: Boolean, srn: Srn) = {
    val prefix = "tasklist.declaration"

    TaskListSectionViewModel(
      s"$prefix.title",
      Right(
        if (isLinkVisible)
          NonEmptyList.of(
            TaskListItemViewModel(
              LinkMessage(
                s"$prefix.complete",
                controllers.routes.DeclarationController.onPageLoad(srn, None).url
              ),
              NotStarted
            )
          )
        else
          NonEmptyList.of(
            TaskListItemViewModel(
              Message(s"$prefix.incomplete"),
              UnableToStart
            )
          )
      ),
      Some(
        LinkMessage(
          s"$prefix.saveandreturn",
          controllers.routes.UnauthorisedController.onPageLoad.url
        )
      )
    )
  }

  private def getTotalAndCompleted(sections: List[TaskListSectionViewModel]): (Int, Int) = {
    val items = sections.flatMap(_.items.fold(_ => Nil, _.toList))
    val completed = items.count(item => item.status == Completed || item.status == CompletedWithoutUpload)
    val total = items.length
    (total, completed)
  }

  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    userAnswers: UserAnswers
  ): PageViewModel[TaskListViewModel] = {

    val viewModelSections = NonEmptyList.of(
      schemeDetailsSection(srn, schemeName, userAnswers),
      landOrPropertySection(srn, schemeName, userAnswers),
      tangiblePropertySection(srn, schemeName, userAnswers),
      loanSection(srn, schemeName, userAnswers),
      sharesSection(srn, schemeName, userAnswers),
      assetsSection(srn, schemeName, userAnswers)
    )

    val (numSections, numCompleted) = getTotalAndCompleted(viewModelSections.toList)
    val isDeclarationLinkVisible = numSections == numCompleted

    val viewModel = TaskListViewModel(viewModelSections :+ declarationSection(isDeclarationLinkVisible, srn))
    val (totalSections, completedSections) = getTotalAndCompleted(viewModel.sections.toList)

    PageViewModel(
      Message("tasklist.title", startDate.show, endDate.show),
      Message("tasklist.heading", startDate.show, endDate.show),
      viewModel
    ).withDescription(
      Heading2("tasklist.subheading") ++
        ParagraphMessage(Message("tasklist.description", completedSections, totalSections)) ++
        ParagraphMessage(Message("tasklist.declaration.letUsKnow"))
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

  private def getTaskListStatus(srn: Srn, journey: Journey, userAnswers: UserAnswers): TaskListStatus = {
    val journeyContributionsHeldPage: Option[TaskListStatusPage.Status] =
      userAnswers.get(TaskListStatusPage(srn, journey))

    journeyContributionsHeldPage match {
      case Some(status) =>
        if (status.completedWithNo) CompletedWithoutUpload
        else Completed
      case _ => NotStarted
    }
  }

  private def checkQuestionLock(
    linkMessage: LinkMessage,
    taskListStatus: TaskListStatus,
    srn: Srn,
    userAnswers: UserAnswers
  ): (InlineMessage, TaskListStatus) = {
    val schemeDetails = schemeDetailsStatus(srn, userAnswers)

    schemeDetails match {
      case Completed => linkMessage -> taskListStatus
      case _ => linkMessage.content -> UnableToStart
    }
  }
}
