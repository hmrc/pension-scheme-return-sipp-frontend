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

import config.FrontendAppConfig
import controllers.actions._
import models.FileAction.Uploading
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Journey, Reference, UploadKey}
import play.api.data.FormError
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.ParagraphMessage
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, UploadViewModel}
import views.html.UploadView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UploadFileController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  view: UploadView,
  uploadService: UploadService,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def callBackUrl(implicit req: Request[_]): String =
    controllers.routes.UploadCallbackController.callback.absoluteURL(secure = config.secureUpscanCallBack)

  def onPageLoad(srn: Srn, journey: Journey): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val successRedirectUrl =
        controllers.routes.LoadingPageController.onPageLoad(srn, Uploading, journey).absoluteURL()
      val failureRedirectUrl = config.urls.upscan.failureEndpoint.format(srn.value, journey.uploadRedirectTag)
      val uploadKey = UploadKey.fromRequest(srn, journey.uploadRedirectTag)

      for {
        initiateResponse <- uploadService.initiateUpscan(callBackUrl, successRedirectUrl, failureRedirectUrl)
        _ <- uploadService.registerUploadRequest(uploadKey, Reference(initiateResponse.fileReference.reference))
      } yield Ok(
        view(
          UploadFileController.viewModel(
            journey,
            initiateResponse.postTarget,
            initiateResponse.formFields,
            collectErrors(),
            config.upscanMaxFileSizeMB
          )
        )
      )
  }

  private def collectErrors()(implicit request: DataRequest[_]): Option[FormError] =
    request.getQueryString("errorCode").zip(request.getQueryString("errorMessage")).flatMap {
      case ("EntityTooLarge", _) =>
        Some(FormError("file-input", "generic.upload.error.size", Seq(config.upscanMaxFileSizeMB)))
      case ("InvalidArgument", "'file' field not found") =>
        Some(FormError("file-input", "generic.upload.error.required"))
      case ("InvalidArgument", "'file' invalid file format") =>
        Some(FormError("file-input", "generic.upload.error.format"))
      case ("EntityTooSmall", _) =>
        Some(FormError("file-input", "generic.upload.error.required"))
      case ("QUARANTINE", _) =>
        Some(FormError("file-input", "generic.upload.error.malicious"))
      case ("UNKNOWN", _) =>
        Some(FormError("file-input", "generic.upload.error.unknown"))
      case _ => None
    }
}

object UploadFileController {
  def viewModel(
    journey: Journey,
    postTarget: String,
    formFields: Map[String, String],
    error: Option[FormError],
    maxFileSize: String
  ): FormPageViewModel[UploadViewModel] =
    FormPageViewModel(
      s"${journey.name}.upload.title",
      s"${journey.name}.upload.heading",
      UploadViewModel(
        detailsContent =
          ParagraphMessage(s"${journey.name}.upload.paragraph") ++ ParagraphMessage(
            s"${journey.name}.upload.details.paragraph"
          ),
        acceptedFileType = ".csv",
        maxFileSize = maxFileSize,
        formFields,
        error
      ),
      Call("POST", postTarget)
    ).withDescription(
        ParagraphMessage(s"${journey.name}.upload.paragraph") ++ ParagraphMessage(
          s"${journey.name}.upload.details.paragraph"
        )
      )
      .withButtonText("site.continue")
}
