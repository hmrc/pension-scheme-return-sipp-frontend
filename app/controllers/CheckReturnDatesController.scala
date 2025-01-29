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

import cats.syntax.traverse.*
import cats.implicits.toShow
import config.RefinedTypes.OneToThree
import controllers.actions.*
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.{DataRequest, FormBundleOrTaxYearRequest}
import models.{DateRange, MinimalSchemeDetails, Mode, PensionSchemeId, VersionTaxYear}
import navigation.Navigator
import pages.CheckReturnDatesPage
import pages.accountingperiod.AccountingPeriodPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{SaveService, SchemeDateService, SchemeDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class CheckReturnDatesController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  schemeDetailsService: SchemeDetailsService,
  schemeDateService: SchemeDateService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form = CheckReturnDatesController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withTaxYear(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying
      getMinimalSchemeDetails(dataRequest.pensionSchemeId, srn) { details =>
        val preparedForm = dataRequest.userAnswers.fillForm(CheckReturnDatesPage(srn), form)

        getWhichTaxYear(Some(request.versionTaxYear)) { taxYear =>
          val viewModel = CheckReturnDatesController.viewModel(srn, mode, taxYear.from, taxYear.to, details)
          Future.successful(Ok(view(preparedForm, viewModel)))
        }
      }
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withFormBundleOrVersionAndTaxYear(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying
      getMinimalSchemeDetails(dataRequest.pensionSchemeId, srn) { details =>
        getWhichTaxYear(request.versionTaxYear) { taxYear =>
          val viewModel =
            CheckReturnDatesController.viewModel(srn, mode, taxYear.from, taxYear.to, details)

          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel))),
              value =>
                for {
                  _ <- if (!value) setCachedDateRanges(srn, mode, request) else Future.unit
                  updatedAnswers <- Future.fromTry(dataRequest.userAnswers.set(CheckReturnDatesPage(srn), value))
                  _ <- saveService.save(updatedAnswers)
                } yield Redirect(navigator.nextPage(CheckReturnDatesPage(srn), mode, updatedAnswers))
            )
        }
      }
    }

  private def getMinimalSchemeDetails(id: PensionSchemeId, srn: Srn)(
    f: MinimalSchemeDetails => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] =
    schemeDetailsService.getMinimalSchemeDetails(id, srn).flatMap {
      case Some(schemeDetails) => f(schemeDetails)
      case None => Future.successful(Redirect(routes.UnauthorisedController.onPageLoad))
    }

  private def getWhichTaxYear(
    versionTaxYear: Option[VersionTaxYear]
  )(f: DateRange => Future[Result]): Future[Result] =
    versionTaxYear.fold(
      Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    )(versionTaxYear => f(versionTaxYear.taxYearDateRange))

  private def setCachedDateRanges[A](srn: Srn, mode: Mode, request: FormBundleOrVersionTaxYearRequest[A])(implicit
    headerCarrier: HeaderCarrier
  ) =
    schemeDateService
      .returnAccountingPeriods(request)
      .map { maybePeriods =>
        val periods = maybePeriods.toList
          .flatMap(_.toList)
          .zipWithIndex
          .traverse { case (date, index) => refineV[OneToThree](index + 1).map(_ -> date) }

        periods match
          case Left(_) => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case Right(value) =>
            value.map { case (index, dateRange) =>
              request.underlying.userAnswers.set(AccountingPeriodPage(srn, index, mode), dateRange)
            }
      }

}

object CheckReturnDatesController {

  def form(formProvider: YesNoPageFormProvider): Form[Boolean] =
    formProvider("checkReturnDates.error.required", "checkReturnDates.error.invalid")

  private def max(d1: LocalDate, d2: LocalDate): LocalDate =
    if (d1.isAfter(d2)) d1 else d2

  private def min(d1: LocalDate, d2: LocalDate): LocalDate =
    if (d1.isAfter(d2)) d2 else d1

  def viewModel(
    srn: Srn,
    mode: Mode,
    fromDate: LocalDate,
    toDate: LocalDate,
    schemeDetails: MinimalSchemeDetails
  ): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      Message("checkReturnDates.title"),
      Message("checkReturnDates.heading"),
      YesNoPageViewModel(
        legend = Some(Message("checkReturnDates.legend"))
      ),
      onSubmit = routes.CheckReturnDatesController.onSubmit(srn, mode)
    ).withDescription(
      ParagraphMessage(
        Message(
          "checkReturnDates.description",
          max(schemeDetails.openDate.getOrElse(fromDate), fromDate).show,
          min(schemeDetails.windUpDate.getOrElse(toDate), toDate).show
        )
      )
    ).withButtonText("site.continue")
}
