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

package connectors.cache

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{badRequest, notFound, ok}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.cache.PensionSchemeUser.{Administrator, Practitioner}
import models.cache.{PensionSchemeUser, SessionData}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import connectors.BaseConnectorSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class SessionDataCacheConnectorSpec extends BaseConnectorSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override implicit lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("microservice.services.pensionAdministrator.port" -> wireMockPort)

  val externalId = "test-id"
  lazy val url: String = s"/pension-administrator/journey-cache/session-data-self"

  def stubGet(response: ResponseDefinitionBuilder): StubMapping =
    stubGet(url, response)

  def stubDelete(response: ResponseDefinitionBuilder): StubMapping =
    stubDelete(url, response)

  def response(pensionSchemeUser: PensionSchemeUser): String =
    s"""{"administratorOrPractitioner": "$pensionSchemeUser"}"""

  def okResponse(pensionSchemeUser: PensionSchemeUser): ResponseDefinitionBuilder =
    ok(response(pensionSchemeUser)).withHeader("Content-Type", "application/json")

  def connector(implicit app: Application): SessionDataCacheConnector = injected[SessionDataCacheConnector]

  "fetch" - {

    "return an administrator" in runningApplication { implicit app =>
      stubGet(okResponse(Administrator))

      connector.fetch(externalId).futureValue mustBe Some(SessionData(Administrator))
    }

    "return a practitioner" in runningApplication { implicit app =>
      stubGet(okResponse(Practitioner))

      connector.fetch(externalId).futureValue mustBe Some(SessionData(Practitioner))
    }

    "return none" in runningApplication { implicit app =>
      stubGet(notFound)

      connector.fetch(externalId).futureValue mustBe None
    }

    "return a failed future for bad request" in runningApplication { implicit app =>
      stubGet(badRequest)

      connector.fetch(externalId).failed.futureValue
    }
  }
}
