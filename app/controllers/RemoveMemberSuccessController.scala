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

import cats.implicits.{catsSyntaxOptionId, toFunctorOps}
import controllers.actions.IdentifyAndRequireData
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.{RemoveMemberPage, RemoveMemberSuccessPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.{PageViewModel, ResultViewModel}
import views.html.ResultView
import viewmodels.implicits.*

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext
import RemoveMemberSuccessController.viewModel

class RemoveMemberSuccessController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ResultView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val nextPage = navigator.nextPage(RemoveMemberSuccessPage(srn), mode, request.userAnswers)
      saveService
        .removeAndSave(request.userAnswers, RemoveMemberPage(srn))
        .as(Ok(view(viewModel(nextPage))))
    }

}

object RemoveMemberSuccessController {

  def viewModel(nextPage: Call): PageViewModel[ResultViewModel] =
    PageViewModel(
      "deleteMember.success.title",
      "deleteMember.success.heading",
      ResultViewModel("site.continue", stringToMessage("deleteMember.success.description").some, nextPage.url)
    )
}
