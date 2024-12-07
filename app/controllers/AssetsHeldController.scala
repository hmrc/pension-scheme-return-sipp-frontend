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

import cats.implicits.toShow
import connectors.PSRConnector
import controllers.AssetsHeldController.{form, viewModel}
import controllers.actions.*
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, Mode}
import navigation.Navigator
import pages.AssetsHeldPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReportDetailsService, SaveService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{Heading2, ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class AssetsHeldController @Inject() (
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

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withVersionAndTaxYear(srn) { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      val preparedForm = dataRequest.userAnswers.fillForm(AssetsHeldPage(srn), form(formProvider))
      Ok(
        view(
          preparedForm,
          viewModel(srn, dataRequest.schemeDetails.schemeName, request.versionTaxYear.taxYearDateRange)
        )
      )
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withVersionAndTaxYear(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying
      form(formProvider)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  viewModel(srn, dataRequest.schemeDetails.schemeName, request.versionTaxYear.taxYearDateRange)
                )
              )
            ),
          value =>
            for {
              _ <- if (value) Future.unit else psrConnector.createEmptyPsr(reportDetailsService.getReportDetails())
              updatedAnswers <- Future
                .fromTry(dataRequest.userAnswers.set(AssetsHeldPage(srn), value))
              _ <- saveService.save(updatedAnswers)
              redirectTo <- Future
                .successful(
                  Redirect(navigator.nextPage(AssetsHeldPage(srn), mode, updatedAnswers))
                )
            } yield redirectTo
        )
    }
}

object AssetsHeldController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "assets.held.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, taxYear: DateRange): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "assets.held.title",
      Message("assets.held.heading", taxYear.from.show, taxYear.to.show),
      controllers.routes.AssetsHeldController.onSubmit(srn)
    ).withDescription(
      ParagraphMessage("assets.held.content.explanation") ++
        ListMessage(
          ListType.Bullet,
          "assets.held.content.interestLandOrProperty",
          "assets.held.content.armsLengthLandOrProperty",
          "assets.held.content.tangibleProperty",
          "assets.held.content.loans",
          "assets.held.content.shares",
          "assets.held.content.connectedParty"
        )
        ++ Heading2(Message("assets.held.content.heading", schemeName))
    )
}
