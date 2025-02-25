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

package connectors

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.PensionSchemeId.{PsaId, PspId}
import models.SchemeId.Srn
import models.{PensionSchemeId, SchemeId}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class SchemeDetailsConnectorSpec extends BaseConnectorSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override implicit lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("microservice.services.pensionsScheme.port" -> wireMockPort)

  object PsaSchemeDetailsHelper {
    def stubGet(psaId: PsaId, schemeId: SchemeId, response: ResponseDefinitionBuilder): StubMapping = {
      val url = s"/pensions-scheme/scheme/${schemeId.value}"

      wireMockServer
        .stubFor(
          get(urlEqualTo(url))
            .withHeader("psaId", equalTo(psaId.value))
            .withHeader("idNumber", equalTo(schemeId.value))
            .withHeader("schemeIdType", equalTo(schemeId.idType))
            .willReturn(response)
        )
    }
  }

  object PspSchemeDetailsHelper {
    def stubGet(pspId: PspId, srn: Srn, response: ResponseDefinitionBuilder): StubMapping = {
      val url = s"/pensions-scheme/psp-scheme/${srn.value}"

      wireMockServer
        .stubFor(
          get(urlEqualTo(url))
            .withHeader("pspId", equalTo(pspId.value))
            .withHeader("srn", equalTo(srn.value))
            .willReturn(response)
        )
    }
  }

  object CheckAssociationHelper {
    val url = "/pensions-scheme/is-psa-associated"

    def stubGet(pensionSchemeId: PensionSchemeId, srn: Srn, response: ResponseDefinitionBuilder): StubMapping = {

      val (idType, idValue) = extractHeader(pensionSchemeId)

      wireMockServer
        .stubFor(
          get(urlEqualTo(url))
            .withHeader(idType, equalTo(idValue))
            .withHeader("schemeReferenceNumber", equalTo(srn.value))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(response)
        )
    }

    private def extractHeader(pensionSchemeId: PensionSchemeId): (String, String) =
      pensionSchemeId match {
        case PsaId(value) => ("psaId", value)
        case PspId(value) => ("pspId", value)
      }
  }

  object ListSchemesHelper {
    val url = "/pensions-scheme/list-of-schemes-self"

    def stubGet(pensionSchemeId: PensionSchemeId, response: ResponseDefinitionBuilder): StubMapping = {

      val idType = pensionSchemeId.fold(_ => "PSA", _ => "PSP")
      val idValue = pensionSchemeId.fold(_.value, _.value)

      wireMockServer
        .stubFor(
          get(urlEqualTo(url))
            .withHeader("idType", equalTo(idType))
            .withHeader("idValue", equalTo(idValue))
            .willReturn(response)
        )
    }
  }

  def connector(implicit app: Application): SchemeDetailsConnector = injected[SchemeDetailsConnector]

  ".details for psa" - {

    val psaId = psaIdGen.sample.value
    val schemeId = schemeIdGen.sample.value
    val expectedResult = schemeDetailsGen.sample.value

    "return scheme details" in runningApplication { implicit app =>
      PsaSchemeDetailsHelper.stubGet(psaId, schemeId, ok(Json.toJson(expectedResult).toString))

      val result = connector.details(psaId, schemeId).futureValue

      result mustBe Some(expectedResult)
    }

    "return none when 404 is sent" in runningApplication { implicit app =>
      PsaSchemeDetailsHelper.stubGet(psaId, schemeId, notFound)

      val result = connector.details(psaId, schemeId).futureValue

      result mustBe None
    }

    "throw error" - {

      "400 response is sent" in runningApplication { implicit app =>
        PsaSchemeDetailsHelper.stubGet(psaId, schemeId, badRequest)

        assertThrows[Exception] {
          connector.details(psaId, schemeId).futureValue
        }
      }

      "500 response is sent" in runningApplication { implicit app =>
        PsaSchemeDetailsHelper.stubGet(psaId, schemeId, serverError)

        assertThrows[Exception] {
          connector.details(psaId, schemeId).futureValue
        }
      }

      "403 response is sent" in runningApplication { implicit app =>
        PsaSchemeDetailsHelper.stubGet(psaId, schemeId, forbidden)

        val httpErrorResponse = intercept[UpstreamErrorResponse] {
          Await.result(connector.details(psaId, schemeId), patienceConfig.timeout)
        }

        httpErrorResponse.statusCode mustBe 403
      }
    }
  }

  ".details for psp" - {
    val pspId = pspIdGen.sample.value
    val srn = srnGen.sample.value
    val expectedResult = schemeDetailsGen.sample.value

    "return scheme details" in runningApplication { implicit app =>
      PspSchemeDetailsHelper.stubGet(pspId, srn, ok(Json.toJson(expectedResult).toString))

      val result = connector.details(pspId, srn).futureValue

      result mustBe Some(expectedResult)
    }

    "return none when 404 is sent" in runningApplication { implicit app =>
      PspSchemeDetailsHelper.stubGet(pspId, srn, notFound)

      val result = connector.details(pspId, srn).futureValue

      result mustBe None
    }

    "throw error" - {

      "400 response is sent" in runningApplication { implicit app =>
        PspSchemeDetailsHelper.stubGet(pspId, srn, badRequest)

        assertThrows[Exception] {
          connector.details(pspId, srn).futureValue
        }
      }

      "500 response is sent" in runningApplication { implicit app =>
        PspSchemeDetailsHelper.stubGet(pspId, srn, serverError)

        assertThrows[Exception] {
          connector.details(pspId, srn).futureValue
        }
      }

      "403 response is sent" in runningApplication { implicit app =>
        PspSchemeDetailsHelper.stubGet(pspId, srn, forbidden())

        val httpErrorResponse = intercept[UpstreamErrorResponse] {
          Await.result(connector.details(pspId, srn), patienceConfig.timeout)
        }

        httpErrorResponse.statusCode mustBe 403
      }
    }
  }

  ".listSchemeDetails for psa" - {

    val psaId = psaIdGen.sample.value
    val expectedResult = listMinimalSchemeDetailsGen.sample.value

    "return scheme details" in runningApplication { implicit app =>
      ListSchemesHelper.stubGet(psaId, ok(Json.toJson(expectedResult).toString))

      connector.listSchemeDetails(psaId).futureValue mustBe Some(expectedResult)
    }

    "return none 404 response is sent" in runningApplication { implicit app =>
      ListSchemesHelper.stubGet(psaId, notFound)

      connector.listSchemeDetails(psaId).futureValue mustBe None
    }

    "throw error" - {

      "400 response is sent" in runningApplication { implicit app =>
        ListSchemesHelper.stubGet(psaId, badRequest)

        assertThrows[Exception] {
          connector.listSchemeDetails(psaId).futureValue
        }
      }

      "500 response is sent" in runningApplication { implicit app =>
        ListSchemesHelper.stubGet(psaId, serverError)

        assertThrows[Exception] {
          connector.listSchemeDetails(psaId).futureValue
        }
      }
    }
  }

  ".listSchemeDetails for psp" - {

    val pspId = pspIdGen.sample.value
    val expectedResult = listMinimalSchemeDetailsGen.sample.value

    "return scheme details" in runningApplication { implicit app =>
      ListSchemesHelper.stubGet(pspId, ok(Json.toJson(expectedResult).toString))

      connector.listSchemeDetails(pspId).futureValue mustBe Some(expectedResult)
    }

    "return none 404 response is sent" in runningApplication { implicit app =>
      ListSchemesHelper.stubGet(pspId, notFound)

      connector.listSchemeDetails(pspId).futureValue mustBe None
    }

    "throw error" - {

      "400 response is sent" in runningApplication { implicit app =>
        ListSchemesHelper.stubGet(pspId, badRequest)

        assertThrows[Exception] {
          connector.listSchemeDetails(pspId).futureValue
        }
      }

      "500 response is sent" in runningApplication { implicit app =>
        ListSchemesHelper.stubGet(pspId, serverError)

        assertThrows[Exception] {
          connector.listSchemeDetails(pspId).futureValue
        }
      }
    }
  }
}
