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
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
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
  view: LoadingPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, action: String, page: String): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      action match {
        case VALIDATING => {
          Future(Ok(view(LoadingPageController.viewModelForValidating(srn, action, page))))
        }
        case UPLOADING =>
          Future(Ok(view(LoadingPageController.viewModelForUploading(srn, action, page))))
      }
  }
}

object LoadingPageController {
  def viewModelForValidating(srn: Srn, action: String, page: String): LoadingViewModel =
    LoadingViewModel(
      s"$page.loading.validating.title",
      s"$page.loading.validating.heading",
      s"$page.loading.validating.description",
      srn,
      action,
      page
    )

  def viewModelForUploading(srn: Srn, action: String, page: String): LoadingViewModel =
    LoadingViewModel(
      s"$page.loading.uploading.title",
      s"$page.loading.uploading.heading",
      s"$page.loading.uploading.description",
      srn,
      action,
      page
    )
}
