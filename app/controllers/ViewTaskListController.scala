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
            SchemeDetailsItems.fromPSRSubmission(submission),
            fbNumber
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
      isTangiblePropertyPopulated = submissionResponse.tangibleProperty.nonEmpty,
      isSharesPopulated = submissionResponse.unquotedShares.nonEmpty,
      isAssetsPopulated = submissionResponse.otherAssetsConnectedParty.nonEmpty,
      isLoansPopulated = submissionResponse.loanOutstanding.nonEmpty
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
    isLandOrPropertyArmsLengthPopulated: Boolean,
    fbNumber: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.landorproperty"

    TaskListSectionViewModel(
      s"$prefix.title",
      if (isLandOrPropertyInterestPopulated) getLandOrPropertyInterestTaskListItem(srn, schemeName, prefix, fbNumber)
      else emptyTaskListItem,
      if (isLandOrPropertyArmsLengthPopulated)
        getLandOrPropertyArmsLengthTaskListItem(srn, schemeName, prefix, fbNumber)
      else emptyTaskListItem
    )
  }

  private def getLandOrPropertyInterestTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    fbNumber: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.interest.title", schemeName),
        controllers.routes.DownloadCsvController
          .downloadEtmpFile(
            srn,
            Journey.InterestInLandOrProperty,
            Some(fbNumber),
            None,
            None
          )
          .url
      ),
      Completed
    )

  private def getLandOrPropertyArmsLengthTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    fbNumber: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.armslength.title", schemeName),
        controllers.routes.DownloadCsvController
          .downloadEtmpFile(
            srn,
            Journey.ArmsLengthLandOrProperty,
            Some(fbNumber),
            None,
            None
          )
          .url
      ),
      Completed
    )

  private def tangiblePropertySection(
    srn: Srn,
    schemeName: String,
    isTangiblePropertyPopulated: Boolean,
    fbNumber: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.tangibleproperty"

    TaskListSectionViewModel(
      s"$prefix.title",
      if (isTangiblePropertyPopulated)
        TaskListItemViewModel(
          LinkMessage(
            Message(s"$prefix.details.title", schemeName),
            controllers.routes.DownloadCsvController
              .downloadEtmpFile(
                srn,
                Journey.TangibleMoveableProperty,
                Some(fbNumber),
                None,
                None
              )
              .url
          ),
          Completed
        )
      else
        emptyTaskListItem
    )
  }

  private def loanSection(
    srn: Srn,
    schemeName: String,
    isLoansPopulated: Boolean,
    fbNumber: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.loans"

    TaskListSectionViewModel(
      s"$prefix.title",
      if (isLoansPopulated) getLoanTaskListItem(srn, schemeName, prefix, fbNumber) else emptyTaskListItem
    )
  }

  private def getLoanTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    fbNumber: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.DownloadCsvController
          .downloadEtmpFile(
            srn,
            Journey.OutstandingLoans,
            Some(fbNumber),
            None,
            None
          )
          .url
      ),
      Completed
    )

  private def sharesSection(
    srn: Srn,
    schemeName: String,
    isSharesPopulated: Boolean,
    fbNumber: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.shares"

    TaskListSectionViewModel(
      s"$prefix.title",
      if (isSharesPopulated) getSharesTaskListItem(srn, schemeName, prefix, fbNumber) else emptyTaskListItem
    )
  }

  private def getSharesTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    fbNumber: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.DownloadCsvController
          .downloadEtmpFile(
            srn,
            Journey.UnquotedShares,
            Some(fbNumber),
            None,
            None
          )
          .url
      ),
      Completed
    )

  private def assetsSection(
    srn: Srn,
    schemeName: String,
    isAssetsPopulated: Boolean,
    fbNumber: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.assets"

    TaskListSectionViewModel(
      s"$prefix.title",
      if (isAssetsPopulated) getAssetsTaskListItem(srn, schemeName, prefix, fbNumber) else emptyTaskListItem
    )
  }

  private def getAssetsTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String,
    fbNumber: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.DownloadCsvController
          .downloadEtmpFile(
            srn,
            Journey.AssetFromConnectedParty,
            Some(fbNumber),
            None,
            None
          )
          .url
      ),
      Completed
    )

  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    overviewURL: String,
    visibleItems: SchemeDetailsItems,
    fbNumber: String
  ): PageViewModel[TaskListViewModel] = {

    val viewModelSections = NonEmptyList.of(
      schemeDetailsSection(srn, schemeName),
      landOrPropertySection(
        srn,
        schemeName,
        visibleItems.isLandOrPropertyInterestPopulated,
        visibleItems.isLandOrPropertyArmsLengthPopulated,
        fbNumber
      ),
      tangiblePropertySection(srn, schemeName, visibleItems.isTangiblePropertyPopulated, fbNumber),
      loanSection(srn, schemeName, visibleItems.isLoansPopulated, fbNumber),
      sharesSection(srn, schemeName, visibleItems.isSharesPopulated, fbNumber),
      assetsSection(srn, schemeName, visibleItems.isAssetsPopulated, fbNumber)
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
            controllers.routes.PsrVersionsController.onPageLoad(srn).url
          )
        ) ++
        ParagraphMessage(Message("viewtasklist.view.hint"))
    )
  }

}
