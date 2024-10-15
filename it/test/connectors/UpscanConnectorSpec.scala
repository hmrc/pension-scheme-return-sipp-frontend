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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.{PreparedUpload, Reference, UploadForm, UpscanFileReference, UpscanInitiateResponse}
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

class UpscanConnectorSpec extends BaseConnectorSpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  override implicit lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("microservice.services.upscan.port" -> wireMockServer.port())

  private val callBackUrl = "http://localhost:9000/test-callback-url"
  private val successRedirectUrl = "/test-successful-redirect-url"
  private val failureRedirectUrl = "/test-failure-redirect-url"

  private val reference = "test-ref"
  private val postTarget = "test-post-target"
  private val formFields = Map("test" -> "fields")

  def connector(implicit app: Application): UpscanConnector = injected[UpscanConnector]

  "UpscanConnector" - {
    ".initiate" - {
      "return an upscan initiation response" in runningApplication { implicit app =>
        val httpResponse = PreparedUpload(Reference(reference), UploadForm(postTarget, formFields))
        val expectedResponse = UpscanInitiateResponse(UpscanFileReference(reference), postTarget, formFields)

        UpscanHelper.stubPost(ok(Json.toJson(httpResponse).toString))

        val result = connector.initiate(callBackUrl, successRedirectUrl, failureRedirectUrl).futureValue

        result mustBe expectedResponse
      }

      "should throw exception when upscan returns 400 Bad Request" in runningApplication { implicit app =>
        UpscanHelper.stubPost(
          badRequest().withBody("Bad Request")
        )

        val result = connector.initiate(callBackUrl, successRedirectUrl, failureRedirectUrl)

        whenReady(result.failed) { exception =>
          exception mustBe a[UpstreamErrorResponse]
          val errorResponse = exception.asInstanceOf[UpstreamErrorResponse]
          errorResponse.statusCode mustBe BAD_REQUEST
        }
      }

      "should throw exception when upscan returns 500 Internal Server Error" in runningApplication { implicit app =>
        UpscanHelper.stubPost(
          serverError().withBody("Internal Server Error")
        )

        val result = connector.initiate(callBackUrl, successRedirectUrl, failureRedirectUrl)

        whenReady(result.failed) { exception =>
          exception mustBe a[UpstreamErrorResponse]
          val errorResponse = exception.asInstanceOf[UpstreamErrorResponse]
          errorResponse.statusCode mustBe INTERNAL_SERVER_ERROR
        }
      }

      "should throw exception when upscan returns invalid JSON" in runningApplication { implicit app =>
        UpscanHelper.stubPost(
          ok("Invalid JSON")
        )

        val result = connector.initiate(callBackUrl, successRedirectUrl, failureRedirectUrl)

        whenReady(result.failed) { exception =>
          exception mustBe a[com.fasterxml.jackson.core.JsonParseException]
        }
      }
    }

    ".download" - {
      "should return HttpResponse for successful download" in runningApplication { implicit app =>
        val downloadUrl = s"${wireMockUrl}/download/test-file"
        val fileContent = "File content"

        wireMockServer.stubFor(
          get(urlEqualTo("/download/test-file"))
            .willReturn(ok(fileContent))
        )

        val result = connector.download(downloadUrl).futureValue

        result.status mustBe OK
        result.body mustBe fileContent
      }
    }
  }

  object UpscanHelper {
    val url = "/upscan/v2/initiate"

    def stubPost(response: ResponseDefinitionBuilder): StubMapping =
      wireMockServer
        .stubFor(
          post(urlEqualTo(url))
            .withHeader(HeaderNames.CONTENT_TYPE, equalTo("application/json"))
            .willReturn(response)
        )
  }
}
