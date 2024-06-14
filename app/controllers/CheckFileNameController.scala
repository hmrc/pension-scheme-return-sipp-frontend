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

import controllers.CheckFileNameController._
import controllers.actions._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{Journey, Mode, UploadKey, UploadStatus, Uploaded}
import navigation.Navigator
import pages.CheckFileNamePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.ParagraphMessage
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class CheckFileNameController @Inject()(
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
    with I18nSupport {

  def onPageLoad(srn: Srn, journey: Journey, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm(CheckFileNamePage(srn, journey), form(formProvider, journey))
      val uploadKey = UploadKey.fromRequestWithNewTag(srn, journey.uploadRedirectTag)

      uploadService.getUploadStatus(uploadKey).map {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(upload: UploadStatus.Success) => {
          Ok(view(preparedForm, viewModel(srn, journey, Some(upload.name), mode)))
        }
        case Some(_: UploadStatus.Failed) =>
          Ok(view(preparedForm, viewModel(srn, journey, Some(""), mode)))
        case Some(_) => Ok(view(preparedForm, viewModel(srn, journey, None, mode)))
      }
  }

  def onSubmit(srn: Srn, journey: Journey, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val uploadKey = UploadKey.fromRequestWithNewTag(srn, journey.uploadRedirectTag)

      CheckFileNameController
        .form(formProvider, journey)
        .bindFromRequest()
        .fold(
          formWithErrors =>
            getUploadedFile(uploadKey)
              .map(file => BadRequest(view(formWithErrors, viewModel(srn, journey, file.map(_.name), mode)))),
          value =>
            getUploadedFile(uploadKey).flatMap {
              case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              case Some(_) =>
                for {
                  _ <- uploadService.setUploadValidationState(uploadKey, Uploaded)
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckFileNamePage(srn, journey), value))
                  _ <- saveService.save(updatedAnswers)
                } yield Redirect(navigator.nextPage(CheckFileNamePage(srn, journey), mode, updatedAnswers))
            }
        )
  }

  // todo: handle all Upscan upload states
  //       None is an error case as the initial state set on the previous page should be InProgress
  private def getUploadedFile(uploadKey: UploadKey): Future[Option[UploadStatus.Success]] =
    uploadService
      .getUploadStatus(uploadKey)
      .map {
        case Some(upload: UploadStatus.Success) => Some(upload)
        case _ => None
      }
}

object CheckFileNameController {
  def form(formProvider: YesNoPageFormProvider, journey: Journey): Form[Boolean] = formProvider(
    s"${journey.messagePrefix}.file.name.check.error.required"
  )

  def viewModel(
    srn: Srn,
    journey: Journey,
    fileName: Option[String],
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] = {
    val refresh = if (fileName.isEmpty) Some(1) else None
    FormPageViewModel(
      s"${journey.messagePrefix}.file.name.check.title",
      s"${journey.messagePrefix}.file.name.check.heading",
      YesNoPageViewModel(
        legend = Some("file.name.check.legend"),
        yes = Some("file.name.check.yes"),
        no = Some("file.name.check.no")
      ),
      onSubmit = routes.CheckFileNameController.onSubmit(srn, journey, mode)
    ).refreshPage(refresh)
      .withDescription(
        fileName.map(name => ParagraphMessage(name))
      )
  }
}
