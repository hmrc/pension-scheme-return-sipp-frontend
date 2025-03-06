/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.implicits.toFunctorOps
import controllers.RemoveFileSuccessController.viewModel
import controllers.actions.IdentifyAndRequireData
import models.SchemeId.Srn
import models.{Journey, JourneyType, Mode}
import navigation.Navigator
import pages.{RemoveFilePage, RemoveFileSuccessPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.{PageViewModel, ResultViewModel}
import views.html.ResultView
import viewmodels.implicits.*

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class RemoveFileSuccessController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ResultView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, journey: Journey, journeyType: JourneyType, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val nextPage = navigator.nextPage(RemoveFileSuccessPage(srn, journey, journeyType), mode, request.userAnswers)
      saveService
        .removeAndSave(request.userAnswers, RemoveFilePage(srn, journey, journeyType))
        .as(Ok(view(viewModel(nextPage))))
    }
}

object RemoveFileSuccessController {
  def viewModel(nextPage: Call): PageViewModel[ResultViewModel] =
    PageViewModel(
      "fileDelete.success.heading",
      "fileDelete.success.heading",
      ResultViewModel("site.continue", nextPage.url)
    )
}
