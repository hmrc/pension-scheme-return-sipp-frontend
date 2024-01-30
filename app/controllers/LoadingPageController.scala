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

import controllers.LoadingPageController.{PAGE_TYPE_UPLOADING, PAGE_TYPE_VALIDATING}
import controllers.actions._
import models.SchemeId.Srn
import models.{NormalMode, UploadKey, UploadStatus}
import pages.LoadingPage
import navigation.Navigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.implicits._
import viewmodels.models.LoadingViewModel
import views.html.LoadingPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class LoadingPageController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  uploadService: UploadService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: LoadingPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, pageType: String): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    pageType match {
      case PAGE_TYPE_VALIDATING => {
        Future(Ok(view(LoadingPageController.viewModelForValidating(srn))))
      }
      case PAGE_TYPE_UPLOADING =>
        Future(Ok(view(LoadingPageController.viewModelForUploading(srn))))
//        val uploadKey = UploadKey.fromRequest(srn, UploadMemberDetailsController.redirectTag)
//
//        uploadService.getUploadStatus(uploadKey).map {
//          case None =>
//            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
//          case Some(upload: UploadStatus.Success) =>
//            Redirect(navigator.nextPage(LoadingPage(srn, NormalMode), NormalMode, request.userAnswers))
//          case Some(failure: UploadStatus.Failed) =>
//            Redirect(navigator.nextPage(LoadingPage(srn, NormalMode), NormalMode, request.userAnswers))
//          case Some(UploadStatus.InProgress) =>
//            Ok(view(LoadingPageController.viewModelForUploading(srn)))
//        }
    }
  }
}

object LoadingPageController {

  val PAGE_TYPE_VALIDATING = "validating-file"
  val PAGE_TYPE_UPLOADING = "uploading-file"
  def viewModelForValidating(srn: Srn): LoadingViewModel =
    LoadingViewModel(
      "loading.validating.title",
      "loading.validating.heading",
      "loading.validating.description"
    ).refreshPage(Some(10000))

  def viewModelForUploading(srn: Srn): LoadingViewModel =
    LoadingViewModel(
      "loading.uploading.title",
      "loading.uploading.heading",
      "loading.uploading.description"
    ).refreshPage(Some(10000))
}
