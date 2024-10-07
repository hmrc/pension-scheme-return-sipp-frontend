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

import cats.data.NonEmptyList
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, jsonResponse, notFound, serverError}
import models.backend.responses.SippPsrJourneySubmissionEtmpResponse
import models.error.{EtmpRequestDataSizeExceedError, EtmpServerError}
import models.requests._
import models.requests.psr.EtmpPsrStatus.Compiled
import models.requests.psr.ReportDetails
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import util.TestTransactions

import java.time.LocalDate

class PSRConnectorSpec extends BaseConnectorSpec with TestTransactions {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val fbNumber = "TestFbNumber"
  implicit val req: DataRequest[AnyContentAsEmpty.type] =
    DataRequest(
      allowedAccessRequestGen(FakeRequest().withSession("fbNumber" -> fbNumber)).sample.value,
      arbitraryUserData.arbitrary.sample.get
    )
  private val maxRequestSize = 1024

  override implicit lazy val applicationBuilder: GuiceApplicationBuilder =
    super.applicationBuilder.configure(
      "microservice.services.pensionSchemeReturn.port" -> wireMockPort,
      "etmpConfig.maxRequestSize" -> maxRequestSize
    )

  val baseUrl = "/pension-scheme-return-sipp/psr"
  val mockPstr: String = "00000042IN"
  val mockStartDay: LocalDate = LocalDate.of(2020, 4, 6)
  val mockReportDetails: ReportDetails = ReportDetails("test", Compiled, earliestDate, latestDate, None, None)
  val testRequest: LandOrConnectedPropertyRequest =
    LandOrConnectedPropertyRequest(reportDetails = mockReportDetails, transactions = None)
  val testOutstandingRequest: OutstandingLoanRequest =
    OutstandingLoanRequest(reportDetails = mockReportDetails, transactions = None)
  val testAssetsFromConnectedPartyRequest: AssetsFromConnectedPartyRequest =
    AssetsFromConnectedPartyRequest(reportDetails = mockReportDetails, transactions = None)
  val testTangibleMoveablePropertyRequest: TangibleMoveablePropertyRequest =
    TangibleMoveablePropertyRequest(reportDetails = mockReportDetails, transactions = None)
  val testUnquotedShareRequest: UnquotedShareRequest =
    UnquotedShareRequest(reportDetails = mockReportDetails, transactions = None)

  val sippPsrJourneySubmissionEtmpResponse: SippPsrJourneySubmissionEtmpResponse =
    SippPsrJourneySubmissionEtmpResponse("new-form-bundle-number")

  val journeySubmissionCreatedResponse =
    jsonResponse(Json.stringify(Json.toJson(sippPsrJourneySubmissionEtmpResponse)), 201)

  def connector(implicit app: Application): PSRConnector = injected[PSRConnector]

  "Land Arms Length" - {

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-arms-length?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-arms-length?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-arms-length?journeyType=Standard&fbNumber=$fbNumber", journeySubmissionCreatedResponse)

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-arms-length?journeyType=Standard&fbNumber=$fbNumber", aResponse().withStatus(413))

      val result = connector.submitLandArmsLength(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(landConnectedPartyTransaction))
      )
      val result = connector.submitLandArmsLength(fullSizeRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }

  "Land or Connected Property" - {

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-or-connected-property?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitLandOrConnectedProperty(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-or-connected-property?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitLandOrConnectedProperty(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/land-or-connected-property?journeyType=Standard&fbNumber=$fbNumber",
        journeySubmissionCreatedResponse
      )

      val result = connector.submitLandOrConnectedProperty(testRequest)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/land-or-connected-property?journeyType=Standard&fbNumber=$fbNumber",
        aResponse().withStatus(413)
      )

      val result = connector.submitLandOrConnectedProperty(testRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(landConnectedPartyTransaction))
      )
      val result = connector.submitLandOrConnectedProperty(fullSizeRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }

  "Outstanding Loans" - {

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/outstanding-loans?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitOutstandingLoans(testOutstandingRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/outstanding-loans?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitOutstandingLoans(testOutstandingRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/outstanding-loans?journeyType=Standard&fbNumber=$fbNumber", journeySubmissionCreatedResponse)

      val result = connector.submitOutstandingLoans(testOutstandingRequest)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/outstanding-loans?journeyType=Standard&fbNumber=$fbNumber", aResponse().withStatus(413))

      val result = connector.submitOutstandingLoans(testOutstandingRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testOutstandingRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(outstandingLoanTransaction))
      )
      val result = connector.submitOutstandingLoans(fullSizeRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }

  "Assets From Connected Party" - {

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/assets-from-connected-party?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitAssetsFromConnectedParty(testAssetsFromConnectedPartyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/assets-from-connected-party?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitAssetsFromConnectedParty(testAssetsFromConnectedPartyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/assets-from-connected-party?journeyType=Standard&fbNumber=$fbNumber",
        journeySubmissionCreatedResponse
      )

      val result = connector.submitAssetsFromConnectedParty(testAssetsFromConnectedPartyRequest)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/assets-from-connected-party?journeyType=Standard&fbNumber=$fbNumber",
        aResponse().withStatus(413)
      )

      val result = connector.submitAssetsFromConnectedParty(testAssetsFromConnectedPartyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testAssetsFromConnectedPartyRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(assetsFromConnectedPartyTransaction))
      )
      val result = connector.submitAssetsFromConnectedParty(fullSizeRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }

  "Tangible Moveable Property" - {

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/tangible-moveable-property?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitTangibleMoveableProperty(testTangibleMoveablePropertyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/tangible-moveable-property?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitTangibleMoveableProperty(testTangibleMoveablePropertyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/tangible-moveable-property?journeyType=Standard&fbNumber=$fbNumber",
        journeySubmissionCreatedResponse
      )

      val result = connector.submitTangibleMoveableProperty(testTangibleMoveablePropertyRequest)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/tangible-moveable-property?journeyType=Standard&fbNumber=$fbNumber",
        aResponse().withStatus(413)
      )

      val result = connector.submitTangibleMoveableProperty(testTangibleMoveablePropertyRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testTangibleMoveablePropertyRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(tangibleMoveablePropertyTransaction))
      )
      val result = connector.submitTangibleMoveableProperty(fullSizeRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }

  "Unquoted Shares" - {

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/unquoted-shares?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitUnquotedShares(testUnquotedShareRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/unquoted-shares?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitUnquotedShares(testUnquotedShareRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/unquoted-shares?journeyType=Standard&fbNumber=$fbNumber", journeySubmissionCreatedResponse)

      val result = connector.submitUnquotedShares(testUnquotedShareRequest)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/unquoted-shares?journeyType=Standard&fbNumber=$fbNumber", aResponse().withStatus(413))

      val result = connector.submitUnquotedShares(testUnquotedShareRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testUnquotedShareRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(unquotedShareTransaction))
      )
      val result = connector.submitUnquotedShares(fullSizeRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }
}
