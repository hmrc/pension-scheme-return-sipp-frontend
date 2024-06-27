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
import config.FrontendAppConfig
import connectors.PSRConnector
import controllers.ViewTaskListController.SchemeDetailsItems
import controllers.actions._
import models.Journey
import models.SchemeId.Srn
import models.backend.responses.PSRSubmissionResponse
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.TaskListStatus._
import viewmodels.models._
import views.html.TaskListView

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class ViewTaskListController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView,
  appConfig: FrontendAppConfig,
  psrConnector: PSRConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, fbNumber: String): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val overviewURL = s"${appConfig.pensionSchemeReturnFrontend.baseUrl}/pension-scheme-return/${srn.value}/overview"

      psrConnector
        .getPSRSubmission(
          request.schemeDetails.pstr,
          optFbNumber = Some(fbNumber),
          optPsrVersion = None,
          optPeriodStartDate = None
        )
        .map { submission =>
          val dates = TaxYear(submission.details.periodStart.getYear)
          val viewModel = ViewTaskListController.viewModel(
            srn,
            request.schemeDetails.schemeName,
            dates.starts,
            dates.finishes,
            overviewURL,
            SchemeDetailsItems.fromPSRSubmission(submission)
          )

          Ok(view(viewModel))
        }

  }
}

object ViewTaskListController {

  case class SchemeDetailsItems(
    isLandOrPropertyInterestPopulated: Boolean,
    isLandOrPropertyArmsLengthPopulated: Boolean,
    isTangiblePropertyPopulated: Boolean,
    isSharesPopulated: Boolean,
    isAssetsPopulated: Boolean,
    isLoansPopulated: Boolean
  )

  object SchemeDetailsItems {
    def fromPSRSubmission(submissionResponse: PSRSubmissionResponse): SchemeDetailsItems = SchemeDetailsItems(
      isLandOrPropertyInterestPopulated = submissionResponse.landConnectedParty.nonEmpty,
      isLandOrPropertyArmsLengthPopulated = submissionResponse.landArmsLength.nonEmpty,
      //TODO: implement these bellow
      isTangiblePropertyPopulated = false,
      isSharesPopulated = false,
      isAssetsPopulated = false,
      isLoansPopulated = false
    )
  }

  private val emptyTaskListItem: TaskListItemViewModel =
    TaskListItemViewModel(
      Message("tasklist.empty.interest.title"),
      Completed
    )
  private def schemeDetailsSection(
    srn: Srn,
    schemeName: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.schemedetails"

    TaskListSectionViewModel(
      s"$prefix.title",
      getBasicSchemeDetailsTaskListItem(schemeName, prefix)
    )
  }

  private def getBasicSchemeDetailsTaskListItem(
    schemeName: String,
    prefix: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.JourneyRecoveryController.onPageLoad().url
      ),
      Completed
    )

  private def landOrPropertySection(
    srn: Srn,
    schemeName: String,
    isLandOrPropertyInterestPopulated: Boolean,
    isLandOrPropertyArmsLengthPopulated: Boolean
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.landorproperty"

    TaskListSectionViewModel(
      s"$prefix.title",
      if (isLandOrPropertyInterestPopulated) getLandOrPropertyInterestTaskListItem(srn, schemeName, prefix)
      else emptyTaskListItem,
      if (isLandOrPropertyArmsLengthPopulated) getLandOrPropertyArmsLengthTaskListItem(srn, schemeName, prefix)
      else emptyTaskListItem
    )
  }

  private def getLandOrPropertyInterestTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.interest.title", schemeName),
        controllers.routes.DownloadCsvController
          .downloadEtmpFile(
            srn,
            Journey.InterestInLandOrProperty,
            None,
            Some("2024-06-03"),
            Some("001")
          )
          .url
      ),
      Completed
    )

  private def getLandOrPropertyArmsLengthTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.armslength.title", schemeName),
        controllers.routes.DownloadCsvController
          .downloadEtmpFile(
            srn,
            Journey.ArmsLengthLandOrProperty,
            None,
            Some("2024-06-03"),
            Some("001")
          )
          .url
      ),
      Completed
    )

  private def tangiblePropertySection(
    schemeName: String,
    isTangiblePropertyPopulated: Boolean
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.tangibleproperty"

    TaskListSectionViewModel(
      s"$prefix.title",
      if (isTangiblePropertyPopulated)
        TaskListItemViewModel(
          LinkMessage(
            Message(s"$prefix.details.title", schemeName),
            controllers.routes.JourneyRecoveryController.onPageLoad().url
          ),
          Completed
        )
      else
        emptyTaskListItem
    )
  }

  private def loanSection(
    schemeName: String,
    isLoansPopulated: Boolean
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.loans"

    TaskListSectionViewModel(
      s"$prefix.title",
      if (isLoansPopulated) getLoanTaskListItem(schemeName, prefix) else emptyTaskListItem
    )
  }

  private def getLoanTaskListItem(
    schemeName: String,
    prefix: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.JourneyRecoveryController.onPageLoad().url
      ),
      Completed
    )

  private def sharesSection(
    schemeName: String,
    isSharesPopulated: Boolean
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.shares"

    TaskListSectionViewModel(
      s"$prefix.title",
      if (isSharesPopulated) getSharesTaskListItem(schemeName, prefix) else emptyTaskListItem
    )
  }

  private def getSharesTaskListItem(
    schemeName: String,
    prefix: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.JourneyRecoveryController.onPageLoad().url
      ),
      Completed
    )

  private def assetsSection(
    schemeName: String,
    isAssetsPopulated: Boolean
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.assets"

    TaskListSectionViewModel(
      s"$prefix.title",
      if (isAssetsPopulated) getAssetsTaskListItem(schemeName, prefix) else emptyTaskListItem
    )
  }

  private def getAssetsTaskListItem(
    schemeName: String,
    prefix: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.JourneyRecoveryController.onPageLoad().url
      ),
      Completed
    )

  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    overviewURL: String,
    visibleItems: SchemeDetailsItems
  ): PageViewModel[TaskListViewModel] = {

    val viewModelSections = NonEmptyList.of(
      schemeDetailsSection(srn, schemeName),
      landOrPropertySection(
        srn,
        schemeName,
        visibleItems.isLandOrPropertyInterestPopulated,
        visibleItems.isLandOrPropertyArmsLengthPopulated
      ),
      tangiblePropertySection(schemeName, visibleItems.isTangiblePropertyPopulated),
      loanSection(schemeName, visibleItems.isLoansPopulated),
      sharesSection(schemeName, visibleItems.isSharesPopulated),
      assetsSection(schemeName, visibleItems.isAssetsPopulated)
    )

    val viewModel = TaskListViewModel(
      sections = viewModelSections,
      postActionLink = Some(
        LinkMessage(
          "viewtasklist.return",
          overviewURL
        )
      )
    )

    PageViewModel(
      Message("viewtasklist.title", startDate.show, endDate.show),
      Message("viewtasklist.heading", startDate.show, endDate.show),
      viewModel
    ).withDescription(
      ParagraphMessage(Message("viewtasklist.description", startDate.show)) ++
        ParagraphMessage(
          LinkMessage(
            "viewtasklist.view.versions",
            controllers.routes.JourneyRecoveryController.onPageLoad().url
          )
        ) ++
        ParagraphMessage(Message("viewtasklist.view.hint"))
    )
  }

}
