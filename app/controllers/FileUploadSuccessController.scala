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

import controllers.FileUploadSuccessController.viewModel
import controllers.actions._
import models.SchemeId.Srn
import models.{Mode, UploadKey, UploadStatus, UploadSuccess}
import navigation.Navigator
import play.api.i18n._
import play.api.mvc._
import services.{SaveService, UploadService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class FileUploadSuccessController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             @Named("sipp") navigator: Navigator,
                                             uploadService: UploadService,
                                             saveService: SaveService,
                                             identifyAndRequireData: IdentifyAndRequireData,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: ContentPageView
                                           )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadStatus(UploadKey.fromRequest(srn)).map {
      case Some(upload: UploadStatus.Success) => Ok(view(viewModel(srn, upload.name, mode)))
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    uploadService.getUploadResult(UploadKey.fromRequest(srn)).flatMap {
      case Some(UploadSuccess(memberDetails)) if memberDetails.nonEmpty => // TODO That needs to be fixed with what we have
        Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad.url)) // Pagination needs to be fixed
      case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}

object FileUploadSuccessController {
  def viewModel(srn: Srn, fileName: String, mode: Mode): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      title = "fileUploadSuccess.title",
      heading = "fileUploadSuccess.heading",
      ContentPageViewModel(isLargeHeading = true),
      onSubmit = routes.FileUploadSuccessController.onSubmit(srn, mode)
    ).withButtonText("site.continue")
      .withDescription(
        ParagraphMessage(
          Message("fileUploadSuccess.paragraph", fileName)
        ) ++ ParagraphMessage(
          LinkMessage(
            Message("fileUploadSuccess.checkYourAnswersLink"),
            controllers.routes.UnauthorisedController.onPageLoad.url //TODO: wire up correct page!
          )
        )
      )
}