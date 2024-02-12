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

import controllers.actions._
import models.FileAction._
import models.{FileAction, Journey}
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PendingFileActionService
import services.PendingFileActionService.{Complete, Pending}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.implicits._
import viewmodels.models.LoadingViewModel
import views.html.LoadingPageView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LoadingPageController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  pendingFileActionService: PendingFileActionService,
  view: LoadingPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, fileAction: FileAction, journey: Journey): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val state = fileAction match {
        case Validating =>
          Future.successful(pendingFileActionService.getValidationState(srn, journey))
        case Uploading =>
          pendingFileActionService.getUploadState(srn, journey)
      }

      state.map {
        case Complete(url) => Redirect(url)
        case Pending =>
          fileAction match {
            case Validating =>
              Ok(view(LoadingPageController.viewModelForValidating(journey)))
            case Uploading =>
              Ok(view(LoadingPageController.viewModelForUploading(journey)))
          }
      }
    }
}

object LoadingPageController {
  def viewModelForValidating(journey: Journey): LoadingViewModel =
    LoadingViewModel(
      s"${journey.name}.loading.validating.title",
      s"${journey.name}.loading.validating.heading",
      s"${journey.name}.loading.validating.description"
    ).refreshPage(Some(3))

  def viewModelForUploading(journey: Journey): LoadingViewModel =
    LoadingViewModel(
      s"${journey.name}.loading.uploading.title",
      s"${journey.name}.loading.uploading.heading",
      s"${journey.name}.loading.uploading.description"
    ).refreshPage(Some(3))
}
