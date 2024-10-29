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

package connectors

import cats.effect.unsafe.implicits.global
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.typesafe.config.ConfigFactory
import sttp.model.StatusCode
import uk.gov.hmrc.http.HeaderCarrier

class UpscanDownloadStreamConnectorTest extends BaseConnectorSpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val config = ConfigFactory.load("application.conf")

  val connector = new UpscanDownloadStreamConnector(config)

  "UpscanDownloadStreamConnector" - {

    "stream" - {

      "return a stream of strings for a valid download URL" in runningApplication { implicit app =>
        val downloadUrl = "/download"
        val responseBody = "file content"

        stubGet(downloadUrl, aResponse().withStatus(StatusCode.Ok.code).withBody(responseBody))

        val result = connector.stream(s"$wireMockUrl$downloadUrl")

        whenReady(result.compile.toList.unsafeToFuture()) { res =>
          res.mkString("") mustBe responseBody
        }
      }
    }
  }
}
