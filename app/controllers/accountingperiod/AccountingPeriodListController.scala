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

package controllers.accountingperiod

import cats.implicits.toShow
import com.google.inject.Inject
import config.Constants.maxAccountingPeriods
import config.RefinedTypes.{Max3, OneToThree}
import eu.timepit.refined.auto.autoUnwrap
import controllers.actions.*
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{DateRange, Mode}
import navigation.Navigator
import pages.accountingperiod.{AccountingPeriodListPage, AccountingPeriods}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.Message
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, ListRow, ListViewModel, RowAction}
import views.html.ListView

import javax.inject.Named
import scala.concurrent.ExecutionContext

class AccountingPeriodListController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  saveService: SaveService,
  view: ListView,
  formProvider: YesNoPageFormProvider
)(implicit ec: ExecutionContext) extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = AccountingPeriodListController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val periods = request.userAnswers.list(AccountingPeriods(srn))

    periods match {
      case Nil => Redirect(controllers.routes.CheckReturnDatesController.onPageLoad(srn, mode))
      case somePeriods =>
        val viewModel = AccountingPeriodListController.viewModel(srn, mode, somePeriods)
        Ok(view(form, viewModel))
    }
  }

  def resetAll(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    saveService.removeAndSave(request.userAnswers, AccountingPeriods(srn)).map { _ =>
      Redirect(routes.AccountingPeriodController.onPageLoad(srn, 1, mode))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val periods = request.userAnswers.list(AccountingPeriods(srn))

    if (periods.length == maxAccountingPeriods) {
      Redirect(navigator.nextPage(AccountingPeriodListPage(srn, addPeriod = false, mode), mode, request.userAnswers))
    } else {
      val viewModel = AccountingPeriodListController.viewModel(srn, mode, periods)

      form
        .bindFromRequest()
        .fold(
          errors => BadRequest(view(errors, viewModel)),
          answer => Redirect(navigator.nextPage(AccountingPeriodListPage(srn, answer, mode), mode, request.userAnswers))
        )
    }
  }
}

object AccountingPeriodListController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider(
      "accountingPeriods.radios.error.required"
    )

  private def rows(srn: Srn, mode: Mode, periods: List[DateRange]): List[ListRow] =
    periods.zipWithIndex.flatMap { case (range, index) =>
      refineV[OneToThree](index + 1).fold(
        _ => Nil,
        index => {
          val actions = if (periods.length == 3) {
            val resetUrl = routes.AccountingPeriodListController.resetAll(srn, mode).url
            val hiddenText = Message("accountingPeriods.resetAll.hiddenText")
            List(RowAction(Message("site.resetAll"), resetUrl, hiddenText)).filter(_ => index == Max3.ONE)
          } else {
            val changeUrl = routes.AccountingPeriodController.onPageLoad(srn, index, mode).url
            val changeHiddenText = Message("accountingPeriods.row.change.hiddenText", range.from.show, range.to.show)
            val removeUrl = routes.RemoveAccountingPeriodController.onPageLoad(srn, index, mode).url
            val removeHiddenText = Message("accountingPeriods.row.remove.hiddenText", range.from.show, range.to.show)
            List(
              RowAction(Message("site.change"), changeUrl, changeHiddenText),
              RowAction(Message("site.remove"), removeUrl, removeHiddenText)
            )
          }
          List(ListRow(Message("accountingPeriods.row", range.from.show, range.to.show), actions))
        }
      )
    }

  def viewModel(srn: Srn, mode: Mode, periods: List[DateRange]): FormPageViewModel[ListViewModel] = {

    val title = if (periods.length == 1) "accountingPeriods.title" else "accountingPeriods.title.plural"
    val heading = if (periods.length == 1) "accountingPeriods.heading" else "accountingPeriods.heading.plural"

    FormPageViewModel(
      Message(title, periods.length),
      Message(heading, periods.length),
      ListViewModel(
        inset = "accountingPeriods.inset",
        rows(srn, mode, periods),
        Message("accountingPeriods.radios"),
        showRadios = periods.length < 3,
        paginatedViewModel = None
      ),
      controllers.accountingperiod.routes.AccountingPeriodListController.onSubmit(srn, mode)
    )
  }
}
