/*
 * Copyright 2024 HM Revenue & Customs
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

import controllers.actions._
import forms.UploadNewFileQuestionPageFormProvider
import models.SchemeId.{Pstr, Srn}
import models.requests.DataRequest
import models.{FormBundleNumber, Journey, Mode, SchemeDetailsItems}
import navigation.Navigator
import pages.ViewChangeNewFileUploadPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReportDetailsService, SaveService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{LinkMessage, Message}
import viewmodels.models.{FormPageViewModel, ViewChangeNewFileQuestionPageViewModel}
import views.html.ViewChangeUploadNewFileQuestionView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ViewChangeNewFileUploadController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: UploadNewFileQuestionPageFormProvider,
  view: ViewChangeUploadNewFileQuestionView,
  saveService: SaveService,
  reportDetailsService: ReportDetailsService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, journey: Journey, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      val formBundleNumber = request.formBundleNumber

      reportDetailsService.getSchemeDetailsItems(formBundleNumber, Pstr(dataRequest.schemeDetails.pstr)).map {
        schemeDetails =>
          val preparedForm =
            dataRequest.userAnswers
              .fillForm(ViewChangeNewFileUploadPage(srn, journey), ViewChangeNewFileUploadController.form(formProvider))

          Ok(
            view(
              preparedForm,
              ViewChangeNewFileUploadController.viewModel(srn, journey, formBundleNumber, schemeDetails)
            )
          )
      }

    }

  def onSubmit(srn: Srn, journey: Journey, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      val formBundleNumber = request.formBundleNumber

      reportDetailsService
        .getSchemeDetailsItems(formBundleNumber, Pstr(dataRequest.schemeDetails.pstr))
        .flatMap { schemeDetails =>
          NewFileUploadController
            .form(formProvider)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      ViewChangeNewFileUploadController
                        .viewModel(srn, journey, formBundleNumber, schemeDetails)
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future
                    .fromTry(dataRequest.userAnswers.set(ViewChangeNewFileUploadPage(srn, journey), value))
                  _ <- saveService.save(updatedAnswers)
                  redirectTo <- Future
                    .successful(
                      Redirect(navigator.nextPage(ViewChangeNewFileUploadPage(srn, journey), mode, updatedAnswers))
                    )
                } yield redirectTo
            )
        }
    }
}

object ViewChangeNewFileUploadController {

  private val keyBase = "viewChangeNewFileUpload"

  def form(formProvider: UploadNewFileQuestionPageFormProvider): Form[Boolean] =
    formProvider(s"$keyBase.error.required")

  def viewModel(
    srn: Srn,
    journey: Journey,
    fbNumber: FormBundleNumber,
    schemeDetailsItems: SchemeDetailsItems
  ): FormPageViewModel[ViewChangeNewFileQuestionPageViewModel] =
    journey match {
      case Journey.InterestInLandOrProperty =>
        getViewModel(schemeDetailsItems.isLandOrPropertyInterestPopulated, srn, journey, fbNumber)
      case Journey.ArmsLengthLandOrProperty =>
        getViewModel(
          schemeDetailsItems.isLandOrPropertyArmsLengthPopulated,
          srn,
          journey,
          fbNumber
        )
      case Journey.TangibleMoveableProperty =>
        getViewModel(schemeDetailsItems.isTangiblePropertyPopulated, srn, journey, fbNumber)
      case Journey.OutstandingLoans =>
        getViewModel(schemeDetailsItems.isLoansPopulated, srn, journey, fbNumber)
      case Journey.UnquotedShares =>
        getViewModel(schemeDetailsItems.isSharesPopulated, srn, journey, fbNumber)
      case Journey.AssetFromConnectedParty =>
        getViewModel(schemeDetailsItems.isAssetsPopulated, srn, journey, fbNumber)
    }

  private def getViewModel(
    isSectionPopulated: Boolean,
    srn: Srn,
    journey: Journey,
    formBundleNumber: FormBundleNumber
  ) = {
    val journeyKeyBase = journeyMessageKeyBase(journey)

    ViewChangeNewFileQuestionPageViewModel(
      title = Message(s"$journeyKeyBase.title"),
      heading = Message(s"$journeyKeyBase.heading"),
      question = if (isSectionPopulated) Message(s"$keyBase.question") else Message(s"$keyBase.questionNoFile"),
      hint = Message(s"$keyBase.hint"),
      messageOrLinkMessage =
        if (isSectionPopulated)
          Right(
            LinkMessage(
              Message(journeyKeyBase),
              controllers.routes.DownloadCsvController
                .downloadEtmpFile(srn, journey, Some(formBundleNumber.value), None, None)
                .url
            )
          )
        else
          Left(Message(s"$keyBase.noPreviousAsset")),
      onSubmit = routes.ViewChangeNewFileUploadController.onSubmit(srn, journey)
    )
  }

  private def journeyMessageKeyBase(journey: Journey) = {
    val journeyKeyBase = journey match {
      case Journey.InterestInLandOrProperty => "interestLandProperty"
      case Journey.ArmsLengthLandOrProperty => "armsLength"
      case Journey.TangibleMoveableProperty => "tangibleMoveableProperty"
      case Journey.OutstandingLoans => "outstandingLoans"
      case Journey.UnquotedShares => "unquotedShares"
      case Journey.AssetFromConnectedParty => "assetFromConnectedParty"
    }
    s"$keyBase.$journeyKeyBase"
  }
}
