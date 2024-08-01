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
import models.backend.responses.MemberDetails
import models.error.EtmpServerError
import models.requests.{AssetsFromConnectedPartyRequest, LandOrConnectedPropertyRequest, OutstandingLoanRequest, TangibleMoveablePropertyRequest, UnquotedShareRequest}
import models.requests.psr.EtmpPsrStatus.Compiled
import models.requests.psr.ReportDetails
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PSRConnectorSpec extends BaseConnectorSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override implicit lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure("microservice.services.pensionSchemeReturn.port" -> wireMockPort)

  val baseUrl = "/pension-scheme-return-sipp/psr"
  val mockPstr: String = "00000042IN"
  val mockStartDay: LocalDate = LocalDate.of(2020, 4, 6)
  val mockReportDetails: ReportDetails = ReportDetails("test", Compiled, earliestDate, latestDate, None, None)
  val testRequest: LandOrConnectedPropertyRequest = LandOrConnectedPropertyRequest(reportDetails = mockReportDetails, transactions = None)
  val testOutstandingRequest: OutstandingLoanRequest = OutstandingLoanRequest(reportDetails = mockReportDetails, transactions = None)
  val testAssetsFromConnectedPartyRequest: AssetsFromConnectedPartyRequest = AssetsFromConnectedPartyRequest(reportDetails = mockReportDetails, transactions = None)
  val testTangibleMoveablePropertyRequest: TangibleMoveablePropertyRequest = TangibleMoveablePropertyRequest(reportDetails = mockReportDetails, transactions = None)
  val testUnquotedShareRequest: UnquotedShareRequest = UnquotedShareRequest(reportDetails = mockReportDetails, transactions = None)

  def connector(implicit app: Application): PSRConnector = injected[PSRConnector]

  "Land Arms Length" - {

    "return an InternalServerException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-arms-length", serverError)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-arms-length", notFound)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
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

      val result = connector.submitLandOrConnectedProperty(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-or-connected-property", notFound)

      val result = connector.submitLandOrConnectedProperty(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-or-connected-property", noContent)

      val result = connector.submitLandOrConnectedProperty(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "Outstanding Loans" - {

    "return an InternalServerException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/outstanding-loans", serverError)

      val result = connector.submitOutstandingLoans(testOutstandingRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/outstanding-loans", notFound)

      val result = connector.submitOutstandingLoans(testOutstandingRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/outstanding-loans", noContent)

      val result = connector.submitOutstandingLoans(testOutstandingRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "Assets From Connected Party" - {

    "return an InternalServerException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/assets-from-connected-party", serverError)

      val result = connector.submitAssetsFromConnectedParty(testAssetsFromConnectedPartyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/assets-from-connected-party", notFound)

      val result = connector.submitAssetsFromConnectedParty(testAssetsFromConnectedPartyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/assets-from-connected-party", noContent)

      val result = connector.submitAssetsFromConnectedParty(testAssetsFromConnectedPartyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "Tangible Moveable Property" - {

    "return an InternalServerException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/tangible-moveable-property", serverError)

      val result = connector.submitTangibleMoveableProperty(testTangibleMoveablePropertyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/tangible-moveable-property", notFound)

      val result = connector.submitTangibleMoveableProperty(testTangibleMoveablePropertyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/tangible-moveable-property", noContent)

      val result = connector.submitTangibleMoveableProperty(testTangibleMoveablePropertyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "Unquoted Shares" - {

    "return an InternalServerException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/unquoted-shares", serverError)

      val result = connector.submitUnquotedShares(testUnquotedShareRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/unquoted-shares", notFound)

      val result = connector.submitUnquotedShares(testUnquotedShareRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/unquoted-shares", noContent)

      val result = connector.submitUnquotedShares(testUnquotedShareRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "PSR versions" - {

    "return an InternalServerException" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/versions/$mockPstr?startDate=${mockStartDay.format(DateTimeFormatter.ISO_DATE)}", serverError)

      val result = connector.getPsrVersions(mockPstr, mockStartDay)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/versions/$mockPstr?startDate=${mockStartDay.format(DateTimeFormatter.ISO_DATE)}", notFound)

      val result = connector.getPsrVersions(mockPstr, mockStartDay)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return am InternalServerException" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/versions/$mockPstr?startDate=${mockStartDay.format(DateTimeFormatter.ISO_DATE)}", noContent)

      val result = connector.getPsrVersions(mockPstr, mockStartDay)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
      }
    }
  }

  "Delete member" - {

    val memberDetails = MemberDetails(
      firstName = "Name",
      lastName = "Surname",
      nino = Some("AB123456C"),
      reasonNoNINO = None,
      dateOfBirth = LocalDate.now()
    )

    "return RuntimeException if parameters are not correct" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/delete-member/$mockPstr", serverError)

      val thrownException = intercept[RuntimeException] {
        connector.deleteMember(mockPstr, None, None, None, memberDetails)
      }

      // Assert that the correct exception is thrown with the expected message
      thrownException.getMessage mustBe "Query Parameters not correct!"
    }

    "return an InternalServerException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/delete-member/$mockPstr?fbNumber=fbNumber", serverError)

      val result = connector.deleteMember(mockPstr, Some("fbNumber"), None, None, memberDetails)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a noContent (InternalServerException)" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/delete-member/$mockPstr?fbNumber=fbNumber", notFound())

      val result = connector.deleteMember(mockPstr, Some("fbNumber"), None, None, memberDetails)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }
  }

}
