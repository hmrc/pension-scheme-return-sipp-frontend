/*
 * Copyright 2024 HM Revenue & Customs
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

import models.SchemeId.Srn
import play.api.i18n._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.{ETMPErrorReceivedView, ETMPRequestDataSizeExceedErrorView}

import javax.inject.Inject
import scala.annotation.unused

class ETMPErrorReceivedController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  viewForEtmpError: ETMPErrorReceivedView,
  viewForEtmpRequestDataSizeExceedError: ETMPRequestDataSizeExceedErrorView
) extends FrontendBaseController
    with I18nSupport {

  def onEtmpErrorPageLoadWithSrn(@unused srn: Srn): Action[AnyContent] = onEtmpErrorPageLoad
  def onEtmpErrorPageLoad: Action[AnyContent] = Action { implicit request =>
    InternalServerError(viewForEtmpError())
  }

  def onEtmpRequestDataSizeExceedErrorPageLoadWithSrn(@unused srn: Srn): Action[AnyContent] =
    onEtmpRequestDataSizeExceedErrorPageLoad

  def onEtmpRequestDataSizeExceedErrorPageLoad: Action[AnyContent] = Action { implicit request =>
    EntityTooLarge(viewForEtmpRequestDataSizeExceedError())
  }
}
