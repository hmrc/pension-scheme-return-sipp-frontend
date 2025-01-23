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
import cats.implicits.toShow
import config.RefinedTypes.{Max3, OneToThree, refineUnsafe}
import connectors.PSRConnector
import controllers.accountingperiod.RemoveAccountingPeriodController.viewModel
import controllers.actions.*
import eu.timepit.refined.auto.autoUnwrap
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.backend.responses.AccountingPeriodDetails
import models.requests.{DataRequest, FormBundleOrVersionTaxYearRequest}
import models.{DateRange, Mode}
import navigation.Navigator
import pages.accountingperiod.RemoveAccountingPeriodPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SchemeDateService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import utils.ListUtils.ListOps
import utils.RefinedUtils.arrayIndex
import viewmodels.DisplayMessage.Message
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class RemoveAccountingPeriodController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  schemeDateService: SchemeDateService,
  psrConnector: PSRConnector,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = RemoveAccountingPeriodController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = {
    val indexRefined = refineUnsafe[Int, OneToThree](index)
    identifyAndRequireData.withFormBundleOrVersionAndTaxYear(srn).async { implicit request =>
      withAccountingPeriodAtIndex(indexRefined, request) { period =>
        Ok(view(form, viewModel(srn, indexRefined, period, mode)))
      }
    }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = {
    val indexRefined = refineUnsafe[Int, OneToThree](index)
    identifyAndRequireData.withFormBundleOrVersionAndTaxYear(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            withAccountingPeriodAtIndex(indexRefined, request) { period =>
              BadRequest(view(formWithErrors, viewModel(srn, indexRefined, period, mode)))
            },
          answer =>
            if (answer) {
              for {
                periods <- schemeDateService.returnAccountingPeriods(request).map(_.toList.flatMap(_.toList))
                _ <- psrConnector.updateAccountingPeriodsDetails(AccountingPeriodDetails(periods.removeAt(index)))
              } yield Redirect(navigator.nextPage(RemoveAccountingPeriodPage(srn, mode), mode, dataRequest.userAnswers))
            } else {
              Future
                .successful(
                  Redirect(navigator.nextPage(RemoveAccountingPeriodPage(srn, mode), mode, dataRequest.userAnswers))
                )
            }
        )
    }
  }

  private def withAccountingPeriodAtIndex[A](index: Max3, request: FormBundleOrVersionTaxYearRequest[A])(
    f: DateRange => Result
  )(implicit headerCarrier: HeaderCarrier): Future[Result] =
    schemeDateService.returnAccountingPeriods(request).map {
      case Some(periods) => Try(periods.toList(index.arrayIndex)) match
        case Success(value) => f(value)
        case Failure(_) => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
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
