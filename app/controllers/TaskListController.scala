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
import connectors.PSRConnector
import com.google.inject.Inject
import controllers.actions.*
import models.Journey.{
  ArmsLengthLandOrProperty,
  AssetFromConnectedParty,
  InterestInLandOrProperty,
  OutstandingLoans,
  TangibleMoveableProperty,
  UnquotedShares
}
import models.requests.common.YesNo
import models.SchemeId.Srn
import models.backend.responses.PsrAssetDeclarationsResponse
import models.requests.DataRequest
import models.{DateRange, FormBundleNumber, Journey, JourneyType, NormalMode, UserAnswers}
import pages.accountingperiod.AccountingPeriods
import pages.{CheckReturnDatesPage, TaskListStatusPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ReportDetailsService
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{InlineMessage, LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits.*
import viewmodels.models.TaskListSectionViewModel.TaskListItemViewModel
import viewmodels.models.TaskListStatus.*
import viewmodels.models.*
import views.html.TaskListView
import config.FrontendAppConfig

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class TaskListController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView,
  reportDetailsService: ReportDetailsService,
  psrConnector: PSRConnector,
  frontendAppConfig: FrontendAppConfig
)(implicit executionContext: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identifyAndRequireData.withFormBundleOrVersionAndTaxYear(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying
      val version = request.versionTaxYear.map(_.version)
      val taxYearStartDate = request.versionTaxYear.map(_.taxYear)
      val reportDetails = reportDetailsService.getReportDetails()
      val dates = reportDetails.taxYearDateRange
      val pstr = reportDetails.pstr

      for {
        fbNumber <- resolveFbNumber(request.formBundleNumber, pstr, dates.from)

        viewModel <- getAssetDeclarations(pstr, fbNumber, taxYearStartDate, version)(hc(dataRequest))
          .map { assetDeclarations =>
            TaskListController.viewModel(
              srn,
              fbNumber,
              dataRequest.schemeDetails.schemeName,
              dates.from,
              dates.to,
              dataRequest.userAnswers,
              assetDeclarations,
              frontendAppConfig.urls.overviewUrl(srn)
            )
          }
      } yield Ok(view(viewModel))
    }

  private def getAssetDeclarations(
    pstr: String,
    fbNumber: Option[String],
    taxYearStartDate: Option[String],
    version: Option[String]
  )(implicit headerCarrier: HeaderCarrier): Future[Option[PsrAssetDeclarationsResponse]] =
    psrConnector
      .getPsrAssetDeclarations(
        pstr,
        fbNumber,
        taxYearStartDate,
        version
      )
      .map(Some(_))
      .recover { case _: NotFoundException => None }

  private def resolveFbNumber(maybeFbNumber: Option[FormBundleNumber], pstr: String, from: LocalDate)(implicit
    headerCarrier: HeaderCarrier
  ) =
    maybeFbNumber.fold(fbNumberFromVersion(pstr, from))(fb => Future.successful(Some(fb.value)))

  private def fbNumberFromVersion(pstr: String, from: LocalDate)(implicit headerCarrier: HeaderCarrier) =
    psrConnector
      .getPsrVersions(pstr, from)
      .map(_.sortBy(_.reportVersion).lastOption.map(_.reportFormBundleNumber))
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
        controllers.routes.NewFileUploadController.onPageLoad(srn, journey, JourneyType.Standard).url
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
    userAnswers: UserAnswers,
    psrAssetDeclarationsResponse: Option[PsrAssetDeclarationsResponse]
  ): TaskListSectionViewModel = {
    val prefix = "tasklist.landorproperty"

    TaskListSectionViewModel(
      s"$prefix.title",
      getLandOrPropertyInterestTaskListItem(srn, schemeName, prefix, userAnswers, psrAssetDeclarationsResponse),
      getLandOrPropertyArmsLengthTaskListItem(srn, schemeName, prefix, userAnswers, psrAssetDeclarationsResponse)
    )
  }

  private def getLandOrPropertyInterestTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers,
    psrAssetDeclarationsResponse: Option[PsrAssetDeclarationsResponse]
  ): TaskListItemViewModel = {
    val taskListStatus: TaskListStatus =
      getTaskListStatus(InterestInLandOrProperty, psrAssetDeclarationsResponse)

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
    userAnswers: UserAnswers,
    psrAssetDeclarationsResponse: Option[PsrAssetDeclarationsResponse]
  ): TaskListItemViewModel = {
    val taskListStatus: TaskListStatus =
      getTaskListStatus(ArmsLengthLandOrProperty, psrAssetDeclarationsResponse)

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
    userAnswers: UserAnswers,
    psrAssetDeclarationsResponse: Option[PsrAssetDeclarationsResponse]
  ): TaskListSectionViewModel = {
    val prefix = "tasklist.tangibleproperty"

    val taskListStatus: TaskListStatus =
      getTaskListStatus(TangibleMoveableProperty, psrAssetDeclarationsResponse)

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
    userAnswers: UserAnswers,
    psrAssetDeclarationsResponse: Option[PsrAssetDeclarationsResponse]
  ): TaskListSectionViewModel = {
    val prefix = "tasklist.loans"

    TaskListSectionViewModel(
      s"$prefix.title",
      getLoanTaskListItem(srn, schemeName, prefix, userAnswers, psrAssetDeclarationsResponse)
    )
  }

  private def getLoanTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers,
    psrAssetDeclarationsResponse: Option[PsrAssetDeclarationsResponse]
  ): TaskListItemViewModel = {
    val taskListStatus: TaskListStatus = getTaskListStatus(OutstandingLoans, psrAssetDeclarationsResponse)

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
    userAnswers: UserAnswers,
    psrAssetDeclarationsResponse: Option[PsrAssetDeclarationsResponse]
  ): TaskListSectionViewModel = {
    val prefix = "tasklist.shares"

    TaskListSectionViewModel(
      s"$prefix.title",
      getSharesTaskListItem(srn, schemeName, prefix, userAnswers, psrAssetDeclarationsResponse)
    )
  }

  private def getSharesTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers,
    psrAssetDeclarationsResponse: Option[PsrAssetDeclarationsResponse]
  ): TaskListItemViewModel = {
    val taskListStatus: TaskListStatus = getTaskListStatus(UnquotedShares, psrAssetDeclarationsResponse)

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
    userAnswers: UserAnswers,
    psrAssetDeclarationsResponse: Option[PsrAssetDeclarationsResponse]
  ): TaskListSectionViewModel = {
    val prefix = "tasklist.assets"

    TaskListSectionViewModel(
      s"$prefix.title",
      getAssetsTaskListItem(srn, schemeName, prefix, userAnswers, psrAssetDeclarationsResponse)
    )
  }

  private def getAssetsTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    userAnswers: UserAnswers,
    psrAssetDeclarationsResponse: Option[PsrAssetDeclarationsResponse]
  ): TaskListItemViewModel = {
    val taskListStatus: TaskListStatus =
      getTaskListStatus(AssetFromConnectedParty, psrAssetDeclarationsResponse)

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

  private def declarationSection(isLinkVisible: Boolean, srn: Srn, fbNumber: Option[String], schemeName: String, schemeDashboardUrl: String) = {
    val prefix = "tasklist.declaration"

    TaskListSectionViewModel(
      s"$prefix.title",
      NonEmptyList.one(
        if (isLinkVisible)
          TaskListItemViewModel(
            LinkMessage(
              s"$prefix.complete",
              controllers.routes.DeclarationController.onPageLoad(srn, fbNumber).url
            ),
            NotStarted
          )
        else
          TaskListItemViewModel(
            Message(s"$prefix.incomplete"),
            UnableToStart
          )
      ),
      Some(
        LinkMessage(
          Message(s"$prefix.saveandreturn", schemeName),
          schemeDashboardUrl
        )
      )
    )
  }

  private def isDeclarationVisible(sections: List[TaskListSectionViewModel]): Boolean = {
    val items = sections.flatMap(_.taskListViewItems)
    val completed = items.count(item => item.status.contains(Completed) || item.status.contains(CompletedWithoutUpload))
    val total = items.length
    total == completed
  }

  def viewModel(
    srn: Srn,
    fbNumber: Option[String],
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    userAnswers: UserAnswers,
    psrAssetDeclarations: Option[PsrAssetDeclarationsResponse],
    schemeDashboardUrl: String
  ): PageViewModel[TaskListViewModel] = {
    val viewModelSections = NonEmptyList.of(
      schemeDetailsSection(srn, schemeName, userAnswers),
      landOrPropertySection(srn, schemeName, userAnswers, psrAssetDeclarations),
      tangiblePropertySection(srn, schemeName, userAnswers, psrAssetDeclarations),
      loanSection(srn, schemeName, userAnswers, psrAssetDeclarations),
      sharesSection(srn, schemeName, userAnswers, psrAssetDeclarations),
      assetsSection(srn, schemeName, userAnswers, psrAssetDeclarations)
    )

    val isDeclarationLinkVisible = isDeclarationVisible(viewModelSections.toList)
    val viewModel = TaskListViewModel(
      viewModelSections :+ declarationSection(isDeclarationLinkVisible, srn, fbNumber, schemeName, schemeDashboardUrl)
    )

    PageViewModel(
      Message("tasklist.title", startDate.show, endDate.show),
      Message("tasklist.heading", startDate.show, endDate.show),
      viewModel
    ).withDescription(
      ParagraphMessage(Message("tasklist.declaration.letUsKnow", schemeName))
    )
  }

  private def schemeDetailsStatus(srn: Srn, userAnswers: UserAnswers): TaskListStatus = {
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

  private def getTaskListStatus(
    journey: Journey,
    psrAssetDeclarationsResponse: Option[PsrAssetDeclarationsResponse]
  ): TaskListStatus =
    psrAssetDeclarationsResponse
      .map { response =>
        response.getPopulatedField(journey) match
          case Some(YesNo.Yes) => Completed
          case Some(YesNo.No) => CompletedWithoutUpload
          case None => NotStarted
      }
      .getOrElse(NotStarted)

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
