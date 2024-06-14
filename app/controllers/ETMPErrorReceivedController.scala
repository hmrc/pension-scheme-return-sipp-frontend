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

import controllers.actions._
import models.SchemeId.Srn
import navigation.Navigator
import play.api.i18n._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ETMPErrorReceivedView

import javax.inject.{Inject, Named}
import scala.concurrent.Future

class ETMPErrorReceivedController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             val controllerComponents: MessagesControllerComponents,
                                             @Named("sipp") navigator: Navigator,
                                             allowAccess: AllowAccessActionProvider,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             view: ETMPErrorReceivedView,
                                             identify: IdentifierAction
                                           ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identify.andThen(allowAccess(srn)).andThen(getData).andThen(requireData).async { implicit request =>
    Future.successful(Ok(view()))
  }
}
