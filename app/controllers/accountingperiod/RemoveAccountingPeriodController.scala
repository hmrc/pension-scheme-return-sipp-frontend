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
import config.RefinedTypes.{refineUnsafe, Max3, OneToThree}
import eu.timepit.refined.auto.autoUnwrap
import controllers.accountingperiod.RemoveAccountingPeriodController.viewModel
import controllers.actions.*
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, Mode}
import navigation.Navigator
import pages.accountingperiod.{AccountingPeriodPage, RemoveAccountingPeriodPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.Message
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RemoveAccountingPeriodController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = RemoveAccountingPeriodController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = {
    val indexRefined = refineUnsafe[Int, OneToThree](index)
    identifyAndRequireData(srn) { implicit request =>
      withAccountingPeriodAtIndex(srn, indexRefined, mode) { period =>
        Ok(view(form, viewModel(srn, indexRefined, period, mode)))
      }
    }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = {
    val indexRefined = refineUnsafe[Int, OneToThree](index)
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              withAccountingPeriodAtIndex(srn, indexRefined, mode) { period =>
                BadRequest(view(formWithErrors, viewModel(srn, indexRefined, period, mode)))
              }
            ),
          answer =>
            if (answer) {
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.remove(AccountingPeriodPage(srn, indexRefined, mode)))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(RemoveAccountingPeriodPage(srn, mode), mode, updatedAnswers))
            } else {
              Future
                .successful(
                  Redirect(navigator.nextPage(RemoveAccountingPeriodPage(srn, mode), mode, request.userAnswers))
                )
            }
        )
    }
  }

  private def withAccountingPeriodAtIndex(srn: Srn, index: Max3, mode: Mode)(
    f: DateRange => Result
  )(implicit request: DataRequest[?]): Result =
    request.userAnswers.get(AccountingPeriodPage(srn, index, mode)) match {
      case Some(bankAccount) => f(bankAccount)
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

}

object RemoveAccountingPeriodController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeAccountingPeriod.error.required"
  )

  def viewModel(srn: Srn, index: Max3, dateRange: DateRange, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("removeAccountingPeriod.title", dateRange.from.show, dateRange.to.show),
      Message("removeAccountingPeriod.heading", dateRange.from.show, dateRange.to.show),
      routes.RemoveAccountingPeriodController.onSubmit(srn, index, mode)
    )
}
