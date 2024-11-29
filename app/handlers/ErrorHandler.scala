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

package handlers

import models.error.{EtmpRequestDataSizeExceedError, EtmpServerError}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{RequestHeader, Result}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.ErrorTemplate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject() (
  val messagesApi: MessagesApi,
  view: ErrorTemplate
)(implicit override val ec: ExecutionContext)
    extends FrontendErrorHandler
    with I18nSupport
    with Logging {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    rh: RequestHeader
  ): Future[Html] =
    Future.successful(view(pageTitle, heading, message))

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
    exception match {
      case e: EtmpServerError =>
        logEtmpServerError(request, e)
        Future.successful(Redirect(controllers.routes.ETMPErrorReceivedController.onEtmpErrorPageLoad))
      case e: EtmpRequestDataSizeExceedError =>
        logEtmpFileSizeExceedError(request, e)
        Future.successful(
          Redirect(controllers.routes.ETMPErrorReceivedController.onEtmpRequestDataSizeExceedErrorPageLoad)
        )
      case _ =>
        Future.successful(Redirect(controllers.routes.ETMPErrorReceivedController.onEtmpErrorPageLoad))
    }

  private def logEtmpServerError(request: RequestHeader, ex: Throwable): Unit =
    logger.error(s"! %sEtmp server error, for (${request.method}) [${request.uri}] -> ", ex)

  private def logEtmpFileSizeExceedError(request: RequestHeader, ex: Throwable): Unit =
    logger.error(s"! %sEtmp file size exceed error, for (${request.method}) [${request.uri}] -> ", ex)

}
