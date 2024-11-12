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
import controllers.actions.*
import models.FileAction.Uploading
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Journey, JourneyType, Reference, UploadKey}
import play.api.data.FormError
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.ListType.Bullet
import viewmodels.DisplayMessage.{LinkMessage, ListMessage, ParagraphMessage}
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, UploadViewModel}
import views.html.UploadView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UploadFileController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  view: UploadView,
  uploadService: UploadService,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def callBackUrl(implicit req: Request[?]): String =
    routes.UploadCallbackController.callback.absoluteURL(secure = config.secureUpscanCallBack)

  def onPageLoad(srn: Srn, journey: Journey, journeyType: JourneyType): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val successRedirectUrl =
        config.urls.withBaseUrl(routes.LoadingPageController.onPageLoad(srn, Uploading, journey, journeyType).url)
      val failureRedirectUrl =
        config.urls.withBaseUrl(routes.UploadFileController.onPageLoad(srn, journey, journeyType).url)
      val uploadKey = UploadKey.fromRequest(srn, journey.uploadRedirectTag)

      for {
        initiateResponse <- uploadService.initiateUpscan(callBackUrl, successRedirectUrl, failureRedirectUrl)
        _ <- uploadService.registerUploadRequest(uploadKey, Reference(initiateResponse.fileReference.reference))
        viewModel = UploadFileController.viewModel(
          journey,
          journeyType,
          initiateResponse.postTarget,
          initiateResponse.formFields,
          collectErrors()
        )
      } yield Ok(view(viewModel))
    }

  private def collectErrors()(implicit request: DataRequest[?]): Option[FormError] =
    request.getQueryString("errorCode").zip(request.getQueryString("errorMessage")).flatMap {
      case ("EntityTooLarge", _) =>
        Some(FormError("file-input", "generic.upload.error.size", Seq(config.upscanMaxFileSizeMB)))
      case ("InvalidArgument", "'file' field not found") =>
        Some(FormError("file-input", "generic.upload.error.required"))
      case ("InvalidArgument", "'file' invalid file format") =>
        Some(FormError("file-input", "generic.upload.error.format"))
      case ("REJECTED", _) =>
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
    journeyType: JourneyType,
    postTarget: String,
    formFields: Map[String, String],
    error: Option[FormError]
  ): FormPageViewModel[UploadViewModel] = {
    val prefix = s"${journey.messagePrefix}.${journeyType.entryName.toLowerCase}"
    FormPageViewModel(
      s"$prefix.upload.title",
      s"$prefix.upload.heading",
      UploadViewModel(formFields, error),
      Call("POST", postTarget)
    ).withDescription(getDescription(prefix, journey, journeyType)).withButtonText("site.saveAndContinue")
  }

  private def getDescription(prefix: String, journey: Journey, journeyType: JourneyType) =
    journeyType match {
      case JourneyType.Standard =>
        ParagraphMessage(s"$prefix.upload.paragraph") ++
          ParagraphMessage(s"$prefix.upload.details.paragraph1") ++
          ParagraphMessage(s"$prefix.upload.details.paragraph2")
      case JourneyType.Amend =>
        ParagraphMessage(
          LinkMessage(
            s"$prefix.upload.paragraph.textWithLink",
            routes.DownloadTemplateFileController.downloadFile(journey.templateFileType).url
          ),
          " ",
          s"$prefix.upload.paragraph.rest"
        ) ++ ParagraphMessage(s"$prefix.upload.details.paragraph") ++
          ParagraphMessage("generic.upload.amend.listItemsHeader") ++
          ListMessage(
            Bullet,
            "generic.upload.amend.listItems1",
            "generic.upload.amend.listItems2",
            "generic.upload.amend.listItems3"
          )
    }
}
