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
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Mode, Reference, UploadKey}
import navigation.Navigator
import pages.UploadMemberDetailsPage
import play.api.data.FormError
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.ParagraphMessage
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, UploadViewModel}
import views.html.UploadView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class UploadMemberDetailsController @Inject()(
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
    val redirectTag = "upload-your-member-details"
    val successRedirectUrl = config.urls.upscan.successEndpoint.format(srn.value, redirectTag)
    val failureRedirectUrl = config.urls.upscan.failureEndpoint.format(srn.value, redirectTag)
    val uploadKey = UploadKey.fromRequest(srn)

    for {
      initiateResponse <- uploadService.initiateUpscan(callBackUrl, successRedirectUrl, failureRedirectUrl)
      _ <- uploadService.registerUploadRequest(uploadKey, Reference(initiateResponse.fileReference.reference))
    } yield Ok(
      view(
        UploadMemberDetailsController.viewModel(
          initiateResponse.postTarget,
          initiateResponse.formFields,
          collectErrors(srn),
          config.upscanMaxFileSizeMB
        )
      )
    )
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    Redirect(navigator.nextPage(UploadMemberDetailsPage(srn), mode, request.userAnswers))
  }

  private def collectErrors(srn: Srn)(implicit request: DataRequest[_]): Option[FormError] =
    request.getQueryString("errorCode").zip(request.getQueryString("errorMessage")).flatMap {
      case ("EntityTooLarge", _) =>
        Some(FormError("file-input", "uploadMemberDetails.error.size", Seq(config.upscanMaxFileSizeMB)))
      case ("InvalidArgument", "'file' field not found") =>
        Some(FormError("file-input", "uploadMemberDetails.error.required"))
      case ("EntityTooSmall", _) =>
        Some(FormError("file-input", "uploadMemberDetails.error.required"))
      case _ => None
    }
}

object UploadMemberDetailsController {

  def viewModel(
    postTarget: String,
    formFields: Map[String, String],
    error: Option[FormError],
    maxFileSize: String
  ): FormPageViewModel[UploadViewModel] =
    FormPageViewModel(
      "uploadMemberDetails.title",
      "uploadMemberDetails.heading",
      UploadViewModel(
        detailsContent =
          ParagraphMessage("uploadMemberDetails.paragraph") ++ ParagraphMessage(
            "uploadMemberDetails.details.paragraph"
          ),
        acceptedFileType = ".csv",
        maxFileSize = maxFileSize,
        formFields,
        error
      ),
      Call("POST", postTarget)
    ).withDescription(
        ParagraphMessage("uploadMemberDetails.paragraph") ++ ParagraphMessage("uploadMemberDetails.details.paragraph")
      )
      .withButtonText("site.continue")
}
