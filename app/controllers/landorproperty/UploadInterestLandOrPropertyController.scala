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

package controllers.landorproperty

import config.FrontendAppConfig
import controllers.actions._
import controllers.landorproperty.UploadInterestLandOrPropertyController.redirectTag
import models.SchemeId.Srn
import models.enumerations.TemplateFileType
import models.requests.DataRequest
import models.{Mode, Reference, UploadKey}
import navigation.Navigator
import play.api.data.FormError
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{DownloadLinkMessage, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, UploadViewModel}
import views.html.UploadView
import pages.landorproperty.UploadInterestLandOrPropertyPage

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class UploadInterestLandOrPropertyController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
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

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val successRedirectUrl = config.urls.upscan.successEndpoint.format(srn.value, redirectTag)
    val failureRedirectUrl = config.urls.upscan.failureEndpoint.format(srn.value, redirectTag)
    val uploadKey = UploadKey.fromRequest(srn, redirectTag)

    for {
      initiateResponse <- uploadService.initiateUpscan(callBackUrl, successRedirectUrl, failureRedirectUrl)
      _ <- uploadService.registerUploadRequest(uploadKey, Reference(initiateResponse.fileReference.reference))
    } yield Ok(
      view(
        UploadInterestLandOrPropertyController.viewModel(
          initiateResponse.postTarget,
          initiateResponse.formFields,
          collectErrors(srn),
          config.upscanMaxFileSizeMB
        )
      )
    )
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(navigator.nextPage(UploadInterestLandOrPropertyPage(srn), mode, request.userAnswers))
  }

  private def collectErrors(srn: Srn)(implicit request: DataRequest[_]): Option[FormError] =
    request.getQueryString("errorCode").zip(request.getQueryString("errorMessage")).flatMap {
      case ("EntityTooLarge", _) =>
        Some(FormError("file-input", "uploadInterestLandOrProperty.error.size", Seq(config.upscanMaxFileSizeMB)))
      case ("InvalidArgument", "'file' field not found") =>
        Some(FormError("file-input", "uploadInterestLandOrProperty.error.required"))
      case ("EntityTooSmall", _) =>
        Some(FormError("file-input", "uploadInterestLandOrProperty.error.required"))
      case _ => None
    }
}

object UploadInterestLandOrPropertyController {

  val redirectTag = "upload-interest-land-or-property"

  def viewModel(
    postTarget: String,
    formFields: Map[String, String],
    error: Option[FormError],
    maxFileSize: String
  ): FormPageViewModel[UploadViewModel] = {
    FormPageViewModel(
      "uploadInterestLandOrProperty.title",
      "uploadInterestLandOrProperty.heading",
      UploadViewModel(
        detailsContent =
          ParagraphMessage("uploadInterestLandOrProperty.paragraph")
            ++ ParagraphMessage("uploadInterestLandOrProperty.details.paragraph"),
        acceptedFileType = ".csv",
        maxFileSize = maxFileSize,
        formFields,
        error
      ),
      Call("POST", postTarget)
    ).withDescription(
      ParagraphMessage(
        "uploadMemberDetails.hint.text",
        DownloadLinkMessage(
          "uploadMemberDetails.hint.linkText",
          controllers.routes.DownloadTemplateFileController.downloadFile(TemplateFileType.InterestLandOrPropertyTemplateFile).url
        )
      ) ++
        ParagraphMessage("uploadInterestLandOrProperty.paragraph") ++
          ParagraphMessage("uploadInterestLandOrProperty.details.paragraph")
      )
      .withButtonText("site.continue")
  }
}
