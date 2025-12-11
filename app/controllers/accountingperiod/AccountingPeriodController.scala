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

import cats.data.NonEmptyList
import cats.syntax.show.toShow
import com.google.inject.Inject
import config.RefinedTypes.{refineUnsafe, Max3, OneToThree}
import config.FrontendAppConfig
import eu.timepit.refined.auto.autoUnwrap
import controllers.actions.*
import forms.DateRangeFormProvider
import forms.mappings.errors.DateFormErrors
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, Mode}
import navigation.Navigator
import pages.WhichTaxYearPage
import pages.accountingperiod.{AccountingPeriodPage, AccountingPeriods}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{SaveService, TaxYearService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear
import utils.FormUtils.*
import utils.DateTimeUtils.localDateShow
import utils.ListUtils.ListOps
import utils.RefinedUtils.arrayIndex
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.ListType.Bullet
import viewmodels.DisplayMessage.{InsetTextMessage, ListMessage, Message, ParagraphMessage}
import viewmodels.implicits.*
import viewmodels.models.{DateRangeViewModel, FormPageViewModel}
import views.html.DateRangeView
import scala.util.chaining.scalaUtilChainingOps

import java.time.LocalDate
import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class AccountingPeriodController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: DateRangeView,
  formProvider: DateRangeFormProvider,
  saveService: SaveService,
  taxYearService: TaxYearService,
  config: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(usedAccountingPeriods: List[DateRange] = List(), taxYear: TaxYear) =
    AccountingPeriodController.form(formProvider, taxYear, usedAccountingPeriods, config.allowedStartDateRange)

  private val viewModel = AccountingPeriodController.viewModel

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = {
    val indexRefined = refineUnsafe[Int, OneToThree](index)
    identifyAndRequireData(srn) { implicit request =>
      getWhichTaxYear(srn) { taxYear =>
        val allAccountingPeriods = request.userAnswers.list(AccountingPeriods(srn))
        Ok(
          view(
            form(taxYear = taxYear).fromUserAnswers(AccountingPeriodPage(srn, indexRefined, mode)),
            viewModel(srn, allAccountingPeriods, indexRefined, mode)
          )
        )
      }
    }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = {
    val indexRefined = refineUnsafe[Int, OneToThree](index)
    identifyAndRequireData(srn).async { implicit request =>
      val usedAccountingPeriods = duplicateAccountingPeriods(srn, indexRefined)
      val dateRange = request.userAnswers
        .get(WhichTaxYearPage(srn))
        .getOrElse(DateRange.from(taxYearService.current))

      form(usedAccountingPeriods, TaxYear(dateRange.from.getYear))
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(BadRequest(view(formWithErrors, viewModel(srn, usedAccountingPeriods, indexRefined, mode)))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(AccountingPeriodPage(srn, indexRefined, mode), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(AccountingPeriodPage(srn, indexRefined, mode), mode, updatedAnswers))
        )
    }
  }

  def duplicateAccountingPeriods(srn: Srn, index: Max3)(implicit request: DataRequest[?]): List[DateRange] =
    request.userAnswers.list(AccountingPeriods(srn)).removeAt(index.arrayIndex)

  private def getWhichTaxYear(
    srn: Srn
  )(f: TaxYear => Result)(implicit request: DataRequest[?]): Result =
    request.userAnswers.get(WhichTaxYearPage(srn)) match {
      case Some(taxYear) => f(TaxYear(taxYear.from.getYear))
      case None => f(taxYearService.current)
    }
}

object AccountingPeriodController {

  def form(
    formProvider: DateRangeFormProvider,
    taxYear: TaxYear,
    usedAccountingPeriods: List[DateRange],
    allowedStartDateRange: LocalDate
  ): Form[DateRange] = formProvider(
    DateFormErrors(
      "accountingPeriod.startDate.error.required.all",
      "accountingPeriod.startDate.error.required.day",
      "accountingPeriod.startDate.error.required.month",
      "accountingPeriod.startDate.error.required.year",
      "accountingPeriod.startDate.error.required.two",
      "accountingPeriod.startDate.error.invalid.date",
      "accountingPeriod.startDate.error.invalid.characters"
    ),
    DateFormErrors(
      "accountingPeriod.endDate.error.required.all",
      "accountingPeriod.endDate.error.required.day",
      "accountingPeriod.endDate.error.required.month",
      "accountingPeriod.endDate.error.required.year",
      "accountingPeriod.endDate.error.required.two",
      "accountingPeriod.endDate.error.invalid.date",
      "accountingPeriod.endDate.error.invalid.characters"
    ),
    "accountingPeriod.endDate.error.range.invalid",
    Some(DateRange(allowedStartDateRange, taxYear.finishes)),
    Some("accountingPeriod.startDate.error.outsideTaxYear"),
    Some("accountingPeriod.endDate.error.outsideTaxYear"),
    Some("accountingPeriod.startDate.error.duplicate"),
    Some("accountingPeriod.endDate.error.duplicate"),
    Some("accountingPeriod.error.gap"),
    usedAccountingPeriods
  )

  def viewModel(
    srn: Srn,
    accountingPeriods: List[DateRange],
    index: Max3,
    mode: Mode
  ): FormPageViewModel[DateRangeViewModel] = DateRangeViewModel(
    "accountingPeriod.title",
    "accountingPeriod.heading",
    Some {
      ParagraphMessage("accountingPeriod.description") ++
        ListMessage(
          Bullet,
          "accountingPeriod.description.listItem1",
          "accountingPeriod.description.listItem2"
        ).pipe { description =>
          NonEmptyList.fromList(accountingPeriods) match {
            case Some(periods) =>
              description ++ ParagraphMessage("accountingPeriod.description.periodsAdded") ++ InsetTextMessage(
                periods.map(range => Message("accountingPeriods.row", range.from.show, range.to.show))
              )
            case None =>
              description
          }
        }
    },
    accountingPeriods,
    routes.AccountingPeriodController.onSubmit(srn, index, mode)
  )
}
