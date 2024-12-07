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

import controllers.CheckFileNameController.*
import controllers.actions.*
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{Journey, JourneyType, Mode, UploadKey, UploadStatus}
import models.UploadState.*
import navigation.Navigator
import pages.CheckFileNamePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.ParagraphMessage
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class CheckFileNameController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  uploadService: UploadService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(srn: Srn, journey: Journey, journeyType: JourneyType, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(CheckFileNamePage(srn, journey, journeyType), form(formProvider, journey))
      val uploadKey = UploadKey.fromRequest(srn, journey.uploadRedirectTag)

      uploadService.getUploadStatus(uploadKey).map {
        case None =>
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(upload: UploadStatus.Success) =>
          Ok(view(preparedForm, viewModel(srn, journey, journeyType, Some(upload.name), mode)))
        case Some(failure: UploadStatus.Failed) =>
          logger.error(s"Upload failed for $uploadKey, details: ${failure.failureDetails}")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case _ =>
          logger.warn(s"Upload was not started, redirecting to upload file page")
          Redirect(controllers.routes.UploadFileController.onPageLoad(srn, journey, journeyType))
      }
    }

  def onSubmit(srn: Srn, journey: Journey, journeyType: JourneyType, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val uploadKey = UploadKey.fromRequest(srn, journey.uploadRedirectTag)

      CheckFileNameController
        .form(formProvider, journey)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            getUploadedFile(uploadKey)
              .map(file =>
                BadRequest(view(formWithErrors, viewModel(srn, journey, journeyType, file.map(_.name), mode)))
              ),
          value =>
            getUploadedFile(uploadKey).flatMap {
              case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              case Some(_) =>
                for {
                  _ <- uploadService.setUploadValidationState(uploadKey, Uploaded)
                  updatedAnswers <- Future
                    .fromTry(request.userAnswers.set(CheckFileNamePage(srn, journey, journeyType), value))
                  _ <- saveService.save(updatedAnswers)
                } yield Redirect(navigator.nextPage(CheckFileNamePage(srn, journey, journeyType), mode, updatedAnswers))
            }
        )
    }

  private def getUploadedFile(uploadKey: UploadKey): Future[Option[UploadStatus.Success]] =
    uploadService
      .getUploadStatus(uploadKey)
      .map {
        case Some(upload: UploadStatus.Success) =>
          Some(upload)
        case Some(UploadStatus.Failed(reason)) =>
          logger.error(s"Upload failed due to: $reason")
          None
        case _ =>
          None
      }
}

object CheckFileNameController {
  def form(formProvider: YesNoPageFormProvider, journey: Journey): Form[Boolean] = formProvider(
    s"${journey.messagePrefix}.file.name.check.error.required"
  )

  def viewModel(
    srn: Srn,
    journey: Journey,
    journeyType: JourneyType,
    fileName: Option[String],
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] = {
    val refresh = if (fileName.isEmpty) Some(1) else None
    FormPageViewModel(
      s"${journey.messagePrefix}.file.name.check.title",
      s"${journey.messagePrefix}.file.name.check.heading",
      YesNoPageViewModel(
        legend = Some("file.name.check.legend"),
        hint = None,
        yes = Some("file.name.check.yes"),
        no = Some("file.name.check.no"),
        details = None
      ),
      onSubmit = routes.CheckFileNameController.onSubmit(srn, journey, journeyType, mode)
    ).refreshPage(refresh)
      .withDescription(
        fileName.map(name => ParagraphMessage(name))
      )
  }
}
