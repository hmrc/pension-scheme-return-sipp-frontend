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
import models.{DateRange, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{InsetTextMessage, LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.TaskListStatus._
import viewmodels.models._
import views.html.TaskListView

import java.time.LocalDate

class ViewTaskListController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val dates = DateRange.from(TaxYear(2023)) // TODO: Implement fetching correct Tax Year based on SRN

    val viewModel = ViewTaskListController.viewModel(
      srn,
      request.schemeDetails.schemeName,
      dates.from,
      dates.to
    )

    Ok(view(viewModel))
  }
}

object ViewTaskListController {

  private def schemeDetailsSection(
    srn: Srn,
    schemeName: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.schemedetails"

    TaskListSectionViewModel(
      s"$prefix.title",
      getBasicSchemeDetailsTaskListItem(srn, schemeName, prefix)
    )
  }

  private def getBasicSchemeDetailsTaskListItem(
    srn: Srn,
    schemeName: String,
    prefix: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.details.title", schemeName),
        controllers.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode).url
      ),
      Completed
    )

  private def landOrPropertySection(
    schemeName: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.landorproperty"

    TaskListSectionViewModel(
      s"$prefix.title",
      getLandOrPropertyInterestTaskListItem(schemeName, prefix),
      getLandOrPropertyArmsLengthTaskListItem(schemeName, prefix)
    )
  }

  private def getLandOrPropertyInterestTaskListItem(
    schemeName: String,
    prefix: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.interest.title", schemeName),
        controllers.routes.JourneyRecoveryController.onPageLoad().url
      ),
      Completed
    )

  private def getLandOrPropertyArmsLengthTaskListItem(
    schemeName: String,
    prefix: String
  ): TaskListItemViewModel =
    TaskListItemViewModel(
      LinkMessage(
        Message(s"$prefix.armslength.title", schemeName),
        controllers.routes.JourneyRecoveryController.onPageLoad().url
      ),
      Completed
    )

  private def tangiblePropertySection(
    schemeName: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.tangibleproperty"

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(s"$prefix.details.title", schemeName),
          controllers.routes.JourneyRecoveryController.onPageLoad().url
        ),
        Completed
      )
    )
  }

  private def loanSection(
    schemeName: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.loans"

    TaskListSectionViewModel(
      s"$prefix.title",
      getLoanTaskListItem(schemeName, prefix)
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
    schemeName: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.shares"

    TaskListSectionViewModel(
      s"$prefix.title",
      getSharesTaskListItem(schemeName, prefix)
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
    schemeName: String
  ): TaskListSectionViewModel = {
    val prefix = "viewtasklist.assets"

    TaskListSectionViewModel(
      s"$prefix.title",
      getAssetsTaskListItem(schemeName, prefix)
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

  private def declarationSection(srn: Srn) = {
    val prefix = "viewtasklist.declaration"

    TaskListSectionViewModel(
      s"$prefix.title",
      Right(
        NonEmptyList.of(
          TaskListItemViewModel(
            LinkMessage(
              s"$prefix.complete",
              controllers.routes.DeclarationController.onPageLoad(srn).url
            ),
            NotStarted
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

  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate
  ): PageViewModel[TaskListViewModel] = {

    val viewModelSections = NonEmptyList.of(
      schemeDetailsSection(srn, schemeName),
      landOrPropertySection(schemeName),
      tangiblePropertySection(schemeName),
      loanSection(schemeName),
      sharesSection(schemeName),
      assetsSection(schemeName)
    )

    val viewModel = TaskListViewModel(viewModelSections :+ declarationSection(srn))

    PageViewModel(
      Message("viewtasklist.title", startDate.show, endDate.show),
      Message("viewtasklist.heading", startDate.show, endDate.show),
      viewModel
    ).withDescription(
      ParagraphMessage(Message("viewtasklist.description", LocalDate.of(2022, 1, 1).show)) ++
        ParagraphMessage(Message("viewtasklist.view.versions")) ++
        InsetTextMessage(Message("viewtasklist.change.hint"))
    )
  }

}
