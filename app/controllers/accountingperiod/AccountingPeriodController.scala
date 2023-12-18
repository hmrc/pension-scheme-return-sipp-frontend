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

import com.google.inject.Inject
import config.Refined.Max3
import controllers.actions._
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
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear
import utils.FormUtils._
import utils.ListUtils.ListOps
import utils.RefinedUtils.RefinedIntOps
import viewmodels.DisplayMessage.ParagraphMessage
import viewmodels.implicits._
import viewmodels.models.{DateRangeViewModel, FormPageViewModel}
import views.html.DateRangeView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class AccountingPeriodController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: DateRangeView,
  formProvider: DateRangeFormProvider,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(usedAccountingPeriods: List[DateRange] = List(), taxYear: TaxYear = TaxYear.current) =
    AccountingPeriodController.form(formProvider, taxYear, usedAccountingPeriods)

  private val viewModel = AccountingPeriodController.viewModel _

  def onPageLoad(srn: Srn, index: Max3, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      getWhichTaxYear(srn) { taxYear =>
        Ok(
          view(
            form(taxYear = taxYear).fromUserAnswers(AccountingPeriodPage(srn, index, mode)),
            viewModel(srn, index, mode)
          )
        )
      }
    }

  def onSubmit(srn: Srn, index: Max3, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val usedAccountingPeriods = duplicateAccountingPeriods(srn, index)
      request.userAnswers.get(WhichTaxYearPage(srn)) match {
        case Some(dateRange: DateRange) =>
          form(usedAccountingPeriods, TaxYear(dateRange.from.getYear))
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
              value =>
                for {
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(AccountingPeriodPage(srn, index, mode), value))
                  _ <- saveService.save(updatedAnswers)
                } yield Redirect(navigator.nextPage(AccountingPeriodPage(srn, index, mode), mode, updatedAnswers))
            )
        case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def duplicateAccountingPeriods(srn: Srn, index: Max3)(implicit request: DataRequest[_]): List[DateRange] =
    request.userAnswers.list(AccountingPeriods(srn)).removeAt(index.arrayIndex)

  private def getWhichTaxYear(
    srn: Srn
  )(f: TaxYear => Result)(implicit request: DataRequest[_]): Result =
    request.userAnswers.get(WhichTaxYearPage(srn)) match {
      case Some(taxYear) => f(TaxYear(taxYear.from.getYear))
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
}

object AccountingPeriodController {

  def form(
    formProvider: DateRangeFormProvider,
    taxYear: TaxYear,
    usedAccountingPeriods: List[DateRange]
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
    Some(DateRange(taxYear.starts, taxYear.finishes)),
    Some("accountingPeriod.startDate.error.outsideTaxYear"),
    Some("accountingPeriod.endDate.error.outsideTaxYear"),
    Some("accountingPeriod.startDate.error.duplicate"),
    usedAccountingPeriods
  )

  def viewModel(srn: Srn, index: Max3, mode: Mode): FormPageViewModel[DateRangeViewModel] = DateRangeViewModel(
    "accountingPeriod.title",
    "accountingPeriod.heading",
    Some(ParagraphMessage("accountingPeriod.description")),
    routes.AccountingPeriodController.onSubmit(srn, index, mode)
  )
}
