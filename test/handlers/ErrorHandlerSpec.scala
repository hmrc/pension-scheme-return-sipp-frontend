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

package handlers

import controllers.ControllerBaseSpec
import models.error.{EtmpRequestDataSizeExceedError, EtmpServerError}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.{RemoteConnection, RequestTarget}
import play.api.mvc.{AnyContentAsEmpty, Headers, RequestHeader, Result}
import play.api.test.FakeRequest
import play.twirl.api.Html

import scala.concurrent.Future

class ErrorHandlerSpec extends ControllerBaseSpec {

  implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  private val app = GuiceApplicationBuilder()
    .configure("play.filters.csp.nonce.enabled" -> false)

  private lazy val handler: ErrorHandler = app.injector().instanceOf[ErrorHandler]
  private val title: String = "testTitle"
  private val heading: String = "testHeading"
  private val message: String = "testMessage"

  "must redirect to ETMPErrorReceivedController in case of EtmpServerError" in {
    val result: Future[Result] = handler.onServerError(new FakeRequestHeader, new EtmpServerError(message))

    status(result) mustBe SEE_OTHER
    redirectLocation(result).value mustEqual controllers.routes.ETMPErrorReceivedController.onEtmpErrorPageLoad.url
  }

  "must redirect to ETMPErrorReceivedController in case of EtmpRequestDataSizeExceedError" in {
    val result: Future[Result] =
      handler.onServerError(new FakeRequestHeader, new EtmpRequestDataSizeExceedError(message))

    status(result) mustBe SEE_OTHER
    redirectLocation(
      result
    ).value mustEqual controllers.routes.ETMPErrorReceivedController.onEtmpRequestDataSizeExceedErrorPageLoad.url
  }

  "must redirect to ETMPErrorReceivedController in case of RuntimeException" in {
    val result: Future[Result] = handler.onServerError(new FakeRequestHeader, new RuntimeException(message))

    status(result) mustBe SEE_OTHER
    redirectLocation(result).value mustEqual controllers.routes.ETMPErrorReceivedController.onEtmpErrorPageLoad.url
  }

  "must render standardErrorTemplate" in {
    val result: Future[Html] = handler.standardErrorTemplate(title, heading, message)(new FakeRequestHeader)
    result.value mustBe defined

  }
}

class FakeRequestHeader extends RequestHeader {
  override val target: RequestTarget = RequestTarget("/context/some-path", "/context/some-path", Map.empty)

  override def method: String = "POST"

  override def version: String = "HTTP/1.1"

  override def headers: Headers = new Headers(Seq.empty)

  override def connection: RemoteConnection = RemoteConnection("", secure = true, None)

  override def attrs: TypedMap = TypedMap()
}
