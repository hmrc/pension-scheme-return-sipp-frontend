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

import com.github.tomakehurst.wiremock.client.WireMock.{noContent, notFound, serverError}
import models.requests.LandOrConnectedPropertyRequest
import models.requests.psr.EtmpPsrStatus.Compiled
import models.requests.psr.ReportDetails
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.LocalDate

class PSRConnectorSpec extends BaseConnectorSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit override lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("microservice.services.pensionAdministrator.port" -> wireMockPort)

  val baseUrl = "/pension-scheme-return-sipp/psr"
  val mockPstr: String = "00000042IN"
  val mockStartDay: LocalDate = LocalDate.of(2020, 4, 6)
  val mockReportDetails: ReportDetails = ReportDetails("test", Compiled, earliestDate, latestDate, None, None)
  val testRequest: LandOrConnectedPropertyRequest = LandOrConnectedPropertyRequest(reportDetails = mockReportDetails, transactions = None)

  def connector(implicit app: Application): PSRConnector = injected[PSRConnector]

  "Land Arms Length" - {

    "return an InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/land-arms-length", serverError)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/land-arms-length", notFound)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/land-arms-length", noContent)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "Land or Connected Property" - {

    "return an InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/land-or-connected-property", serverError)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/land-or-connected-property", notFound)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/land-or-connected-property", noContent)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "Outstanding Loans" - {

    "return an InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/outstanding-loans", serverError)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/outstanding-loans", notFound)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/outstanding-loans", noContent)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "Assets From Connected Party" - {

    "return an InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/assets-from-connected-party", serverError)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/assets-from-connected-party", notFound)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/assets-from-connected-party", noContent)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "Tangible Moveable Property" - {

    "return an InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/tangible-moveable-property", serverError)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/tangible-moveable-property", notFound)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/tangible-moveable-property", noContent)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "Unquoted Shares" - {

    "return an InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/unquoted-shares", serverError)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/unquoted-shares", notFound)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>

      stubPut(s"$baseUrl/unquoted-shares", noContent)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "PSR versions" - {

    "return an InternalServerException" in runningApplication { implicit app =>

      stubGet(s"$baseUrl/versions/$mockPstr", serverError)

      val result = connector.getPsrVersions(mockPstr, mockStartDay)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>

      stubGet(s"$baseUrl/versions/$mockPstr", notFound)

      val result = connector.getPsrVersions(mockPstr, mockStartDay)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>

      stubGet(s"$baseUrl/versions/$mockPstr", noContent)

      val result = connector.getPsrVersions(mockPstr, mockStartDay)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

}
