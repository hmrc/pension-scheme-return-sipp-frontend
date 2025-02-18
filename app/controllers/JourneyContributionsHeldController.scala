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

import cats.syntax.option.*
import config.Constants
import connectors.PSRConnector
import controllers.JourneyContributionsHeldController.{form, viewModel}
import controllers.actions.*
import forms.YesNoPageFormProvider
import models.JourneyType.Standard
import models.SchemeId.Srn
import models.requests.*
import models.requests.psr.ReportDetails
import models.{FormBundleNumber, Journey, JourneyType, NormalMode}
import navigation.Navigator
import pages.{JourneyContributionsHeldPage, RemoveFilePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReportDetailsService, SaveService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class JourneyContributionsHeldController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView,
  psrConnector: PSRConnector,
  reportDetailsService: ReportDetailsService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, journey: Journey, journeyType: JourneyType): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(
          JourneyContributionsHeldPage(srn, journey, journeyType),
          form(formProvider, journey)
        )
      val showSuccessNotificationFileRemoved = request.userAnswers
        .get(RemoveFilePage(srn, journey, journeyType))
        .getOrElse(false)

      saveService.removeAndSave(request.userAnswers, RemoveFilePage(srn, journey, journeyType))
      Ok(
        view(
          preparedForm,
          viewModel(srn, journey, request.schemeDetails.schemeName, journeyType, showSuccessNotificationFileRemoved)
        )
      )
    }

  def onSubmit(srn: Srn, journey: Journey, journeyType: JourneyType) : Action[AnyContent] =
    identifyAndRequireData.withFormBundleOrVersionAndTaxYear(srn).async { request =>
      implicit val dataRequest = request.underlying

      form(formProvider, journey)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(formWithErrors, viewModel(srn, journey, dataRequest.schemeDetails.schemeName, journeyType))
              )
            ),
          value =>
            for {
              formBundleNumber <-
                if (value) Future.successful(request.formBundleNumber)
                else submitEmptyJourney(journey, reportDetailsService.getReportDetails(), srn)
              updatedAnswers <- Future
                .fromTry(dataRequest.userAnswers.set(JourneyContributionsHeldPage(srn, journey, journeyType), value))
              _ <- saveService.save(updatedAnswers)
              redirectTo <- {
                val redirect =
                  Redirect(
                    navigator
                      .nextPage(JourneyContributionsHeldPage(srn, journey, journeyType), NormalMode, updatedAnswers)
                  )
                Future.successful(
                  formBundleNumber
                    .map { formBundleNumber =>
                      redirect.addingToSession(Constants.formBundleNumber -> formBundleNumber.value)
                    }
                    .getOrElse {
                      redirect
                    }
                )
              }
            } yield redirectTo
        )
    }

  private def submitEmptyJourney(
    journey: Journey,
    reportDetails: ReportDetails,
    srn: Srn
  )(implicit headerCarrier: HeaderCarrier, request: DataRequest[?]): Future[Option[FormBundleNumber]] =
    (journey match {
      case Journey.InterestInLandOrProperty =>
        psrConnector.submitLandOrConnectedProperty(LandOrConnectedPropertyRequest(reportDetails, None), Standard, journey, srn)
      case Journey.ArmsLengthLandOrProperty =>
        psrConnector.submitLandArmsLength(LandOrConnectedPropertyRequest(reportDetails, None), Standard, journey, srn)
      case Journey.TangibleMoveableProperty =>
        psrConnector.submitTangibleMoveableProperty(TangibleMoveablePropertyRequest(reportDetails, None), Standard, journey, srn)
      case Journey.OutstandingLoans =>
        psrConnector.submitOutstandingLoans(OutstandingLoanRequest(reportDetails, None), Standard, journey, srn)
      case Journey.UnquotedShares =>
        psrConnector.submitUnquotedShares(UnquotedShareRequest(reportDetails, None), Standard, journey, srn)
      case Journey.AssetFromConnectedParty =>
        psrConnector.submitAssetsFromConnectedParty(AssetsFromConnectedPartyRequest(reportDetails, None), Standard, journey, srn)
    }).map(response => FormBundleNumber(response.formBundleNumber).some)
}

object JourneyContributionsHeldController {
  def form(formProvider: YesNoPageFormProvider, journey: Journey): Form[Boolean] = formProvider(
    s"${journey.messagePrefix}.held.error.required"
  )

  def viewModel(
    srn: Srn,
    journey: Journey,
    schemeName: String,
    journeyType: JourneyType,
    showSuccessNotificationFileRemoved: Boolean = false
  )(implicit messages: Messages) : FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      s"${journey.messagePrefix}.held.title",
      Message(s"${journey.messagePrefix}.held.heading", schemeName),
      controllers.routes.JourneyContributionsHeldController.onSubmit(srn, journey, journeyType),
      showNotificationBanner =
        if (showSuccessNotificationFileRemoved) {
          Some(
            (
              "success",
              None,
              messages("fileDelete.successNotification.heading"),
              None
            )
          )
        } else {
          None
        }
    )
}
