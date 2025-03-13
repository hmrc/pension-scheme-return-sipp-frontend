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

import controllers.actions.*
import forms.UploadNewFileQuestionPageFormProvider
import models.SchemeId.{Pstr, Srn}
import models.backend.responses.PsrAssetCountsResponse
import models.requests.DataRequest
import models.{FormBundleNumber, Journey, JourneyType, Mode}
import navigation.Navigator
import pages.{NewFileUploadPage, RemoveFilePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReportDetailsService, SaveService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{DownloadLinkMessage, LinkMessage, Message}
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, ViewChangeNewFileQuestionPageViewModel}
import views.html.UploadNewFileQuestionView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class NewFileUploadController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: UploadNewFileQuestionPageFormProvider,
  view: UploadNewFileQuestionView,
  saveService: SaveService,
  reportDetailsService: ReportDetailsService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, journey: Journey, journeyType: JourneyType, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withFormBundleOrVersionAndTaxYear(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      val fbNumber = request.formBundleNumber
      val versionTaxYear = request.versionTaxYear
      val taxYear = versionTaxYear.map(_.taxYear)
      val version = versionTaxYear.map(_.version)

      for {
        assetCounts <- reportDetailsService
          .getAssetCounts(fbNumber, taxYear, version, Pstr(dataRequest.schemeDetails.pstr))
        preparedForm = dataRequest.userAnswers
          .fillForm(NewFileUploadPage(srn, journey, journeyType), NewFileUploadController.form(formProvider))
        _ <- saveService.removeAndSave(dataRequest.userAnswers, RemoveFilePage(srn, journey, journeyType))
      } yield Ok(
        view(
          preparedForm,
          NewFileUploadController.viewModel(srn, journey, fbNumber, taxYear, version, assetCounts, journeyType)
        )
      )
    }

  def onSubmit(srn: Srn, journey: Journey, journeyType: JourneyType, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withFormBundleOrVersionAndTaxYear(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      val fbNumber = request.formBundleNumber
      val versionTaxYear = request.versionTaxYear
      val taxYear = versionTaxYear.map(_.taxYear)
      val version = versionTaxYear.map(_.version)

      reportDetailsService
        .getAssetCounts(fbNumber, taxYear, version, Pstr(dataRequest.schemeDetails.pstr))
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
                      NewFileUploadController
                        .viewModel(srn, journey, fbNumber, taxYear, version, schemeDetails, journeyType)
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future
                    .fromTry(dataRequest.userAnswers.set(NewFileUploadPage(srn, journey, journeyType), value))
                  _ <- saveService.save(updatedAnswers)
                  redirectTo <- Future
                    .successful(
                      Redirect(navigator.nextPage(NewFileUploadPage(srn, journey, journeyType), mode, updatedAnswers))
                    )
                } yield redirectTo
            )
        }
    }
}

object NewFileUploadController {

  private val keyBase = "fileUpload"

  def form(formProvider: UploadNewFileQuestionPageFormProvider): Form[Boolean] =
    formProvider(s"$keyBase.error.required")

  def viewModel(
    srn: Srn,
    journey: Journey,
    fbNumber: Option[FormBundleNumber],
    taxYear: Option[String],
    version: Option[String],
    assetCounts: Option[PsrAssetCountsResponse],
    journeyType: JourneyType
  ): FormPageViewModel[ViewChangeNewFileQuestionPageViewModel] =
    getViewModel(
      assetCounts.map(_.getPopulatedField(journey)).getOrElse(0),
      srn,
      journey,
      fbNumber,
      taxYear,
      version,
      journeyType
    )

  private def getViewModel(
    assetCount: Int,
    srn: Srn,
    journey: Journey,
    fbNumber: Option[FormBundleNumber],
    taxYear: Option[String],
    version: Option[String],
    journeyType: JourneyType
  ): FormPageViewModel[ViewChangeNewFileQuestionPageViewModel] = {
    val journeyKeyBase = s"$keyBase.${journey.entryName}"
    val isSectionPopulated = assetCount > 0

    ViewChangeNewFileQuestionPageViewModel(
      title = Message(s"$journeyKeyBase.title"),
      heading = Message(s"$journeyKeyBase.heading"),
      question = if (assetCount > 0) Message(s"$keyBase.question") else Message(s"$keyBase.questionNoFile"),
      hint = Message(s"$keyBase.hint"),
      messageOrLinkMessage = Either.cond(
        isSectionPopulated,
        DownloadLinkMessage(
          Message(s"$keyBase.downloadLink"),
          routes.DownloadCsvController.downloadEtmpFile(srn, journey, fbNumber.map(_.value), taxYear, version).url
        ),
        Message(s"$keyBase.noPreviousAsset")
      ),
      removeLink = Option.when(isSectionPopulated)(
        LinkMessage(
          Message(s"$keyBase.removeLink"),
          routes.RemoveFileController.onPageLoad(srn, journey, journeyType).url,
          Message(s"$keyBase.hidden.removeLink")
        )
      ),
      countMessage = Option.when(isSectionPopulated)(Message(s"$keyBase.records", assetCount)),
      onSubmit = routes.NewFileUploadController.onSubmit(srn, journey, journeyType)
    )
  }
}
