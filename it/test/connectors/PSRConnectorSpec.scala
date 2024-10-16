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
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, jsonResponse, notFound, serverError}
import models.Journey.ArmsLengthLandOrProperty
import models.ReportStatus.SubmittedAndSuccessfullyProcessed
import models.backend.responses.*
import models.error.{EtmpRequestDataSizeExceedError, EtmpServerError}
import models.requests.PsrSubmissionRequest.PsrSubmittedResponse
import models.requests.*
import models.requests.psr.EtmpPsrStatus.Compiled
import models.requests.psr.ReportDetails
import models.{DateRange, JourneyType, PsrVersionsResponse, ReportSubmitterDetails}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import util.TestTransactions

import java.time.{LocalDate, ZonedDateTime}
import java.time.format.DateTimeFormatter

class PSRConnectorSpec extends BaseConnectorSpec with TestTransactions {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val fbNumber = "TestFbNumber"
  implicit val req: DataRequest[AnyContentAsEmpty.type] =
    DataRequest(
      allowedAccessRequestGen(FakeRequest().withSession("fbNumber" -> fbNumber)).sample.value,
      arbitraryUserData.arbitrary.sample.get
    )
  private val emptyVersions: Versions = Versions(None, None, None, None, None, None, None)
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

  val journeySubmissionCreatedResponse: ResponseDefinitionBuilder =
    jsonResponse(Json.stringify(Json.toJson(sippPsrJourneySubmissionEtmpResponse)), 201)
  val psrVersionResponse1 = PsrVersionsResponse(
    reportFormBundleNumber = "123456",
    reportVersion = 1,
    reportStatus = SubmittedAndSuccessfullyProcessed,
    compilationOrSubmissionDate = ZonedDateTime.now,
    reportSubmitterDetails = Some(ReportSubmitterDetails("John", None, None)),
    psaDetails = None
  )

  val psrSubmittedResponse = PsrSubmittedResponse(emailSent = true)

  val jsonPsrSubmittedResponse =
    jsonResponse(Json.stringify(Json.toJson(psrSubmittedResponse)), 201)

  private val mockAccPeriodDetails: AccountingPeriodDetails =
    AccountingPeriodDetails(None, accountingPeriods = None)

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

  "submitPsr" - {

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPost(s"$baseUrl/sipp?journeyType=Standard", serverError)

      val result = connector.submitPsr(
        mockPstr,
        JourneyType.Standard,
        Some(fbNumber),
        Some(mockStartDay.toString),
        None,
        DateRange(mockStartDay, mockStartDay.plusYears(1)),
        Some("Test Scheme")
      )

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPost(s"$baseUrl/sipp?journeyType=Standard", notFound)

      val result = connector.submitPsr(
        mockPstr,
        JourneyType.Standard,
        Some(fbNumber),
        Some(mockStartDay.toString),
        None,
        DateRange(mockStartDay, mockStartDay.plusYears(1)),
        Some("Test Scheme")
      )

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPost(s"$baseUrl/sipp?journeyType=Standard", jsonPsrSubmittedResponse)

      val result = connector.submitPsr(
        mockPstr,
        JourneyType.Standard,
        Some(fbNumber),
        Some(mockStartDay.toString),
        None,
        DateRange(mockStartDay, mockStartDay.plusYears(1)),
        Some("Test Scheme")
      )

      whenReady(result) { res =>
        res mustBe psrSubmittedResponse
      }
    }
  }

  "getPsrVersions" - {

    "return a successful response" in runningApplication { implicit app =>
      val response = Seq(psrVersionResponse1)
      stubGet(
        s"$baseUrl/versions/$mockPstr?startDate=${mockStartDay.format(DateTimeFormatter.ISO_DATE)}",
        jsonResponse(Json.stringify(Json.toJson(response)), 200)
      )

      val result = connector.getPsrVersions(mockPstr, mockStartDay)

      whenReady(result) { res =>
        res mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/versions/$mockPstr?startDate=${mockStartDay.format(DateTimeFormatter.ISO_DATE)}", serverError)

      val result = connector.getPsrVersions(mockPstr, mockStartDay)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }
  }

  "getPSRSubmission" - {

    "return a successful response" in runningApplication { implicit app =>
      val response = PSRSubmissionResponse(
        mockReportDetails,
        Some(mockAccPeriodDetails),
        None,
        None,
        None,
        None,
        None,
        None,
        emptyVersions
      )
      stubGet(s"$baseUrl/sipp/$mockPstr?fbNumber=$fbNumber", jsonResponse(Json.stringify(Json.toJson(response)), 200))

      val result = connector.getPSRSubmission(mockPstr, Some(fbNumber), None, None)

      whenReady(result) { res =>
        res mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/sipp/$mockPstr?fbNumber=$fbNumber", serverError)

      val result = connector.getPSRSubmission(mockPstr, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }
  }

  "getMemberDetails" - {

    "return a successful response" in runningApplication { implicit app =>
      val response = MemberDetailsResponse(memberDetailsGen.sample.toList)

      stubGet(
        s"$baseUrl/member-details/$mockPstr?fbNumber=$fbNumber",
        jsonResponse(Json.stringify(Json.toJson(response)), 200)
      )

      val result = connector.getMemberDetails(mockPstr, Some(fbNumber), None, None)

      whenReady(result) { res =>
        res mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/member-details/$mockPstr?fbNumber=$fbNumber", serverError)

      val result = connector.getMemberDetails(mockPstr, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }
  }

  "deleteMember" - {

    "return a successful response" in runningApplication { implicit app =>
      val response = memberDetailsGen.sample.get

      stubPut(
        s"$baseUrl/delete-member/$mockPstr?journeyType=Standard&fbNumber=$fbNumber",
        journeySubmissionCreatedResponse
      )

      val result = connector.deleteMember(mockPstr, JourneyType.Standard, Some(fbNumber), None, None, response)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      val response = memberDetailsGen.sample.get

      stubPut(s"$baseUrl/delete-member/$mockPstr?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.deleteMember(mockPstr, JourneyType.Standard, Some(fbNumber), None, None, response)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }
  }

  "deleteAssets" - {

    "return a successful response" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/delete-assets/$mockPstr?journey=ArmsLengthLandOrProperty&journeyType=Standard&fbNumber=$fbNumber",
        journeySubmissionCreatedResponse
      )

      val result =
        connector.deleteAssets(mockPstr, ArmsLengthLandOrProperty, JourneyType.Standard, Some(fbNumber), None, None)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/delete-assets/$mockPstr?journey=ArmsLengthLandOrProperty&journeyType=Standard&fbNumber=$fbNumber",
        serverError
      )

      val result =
        connector.deleteAssets(mockPstr, ArmsLengthLandOrProperty, JourneyType.Standard, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }
  }

  "getPsrAssetCounts" - {

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/asset-counts/$mockPstr?fbNumber=$fbNumber", serverError)

      val result = connector.getPsrAssetCounts(mockPstr, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }
  }
}
