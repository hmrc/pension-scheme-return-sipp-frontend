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
import cats.syntax.option.*
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.Journey.{ArmsLengthLandOrProperty, InterestInLandOrProperty}
import models.ReportStatus.SubmittedAndSuccessfullyProcessed
import models.SchemeId.Srn
import models.backend.responses.*
import models.error.{EtmpRequestDataSizeExceedError, EtmpServerError}
import models.requests.*
import models.requests.AssetsFromConnectedPartyApi.formatAssetsFromConnectedResponse
import models.requests.LandOrConnectedPropertyApi.formatLandConnectedResponse
import models.requests.OutstandingLoanApi.formatOutstandingResponse
import models.requests.PsrSubmissionRequest.PsrSubmittedResponse
import models.requests.TangibleMoveablePropertyApi.formatTangibleResponse
import models.requests.UnquotedShareApi.formatUnquotedResponse
import models.requests.common.YesNo
import models.requests.psr.EtmpPsrStatus.Compiled
import models.requests.psr.ReportDetails
import models.{DateRange, Journey, JourneyType, PsrVersionsResponse, ReportSubmitterDetails}
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import util.TestTransactions.*

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

class PSRConnectorSpec extends BaseConnectorSpec {

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
  val testPstr: String = "00000042IN"
  val testStartDay: LocalDate = LocalDate.of(2020, 4, 6)
  val testReportDetails: ReportDetails =
    ReportDetails("test", Compiled, earliestDate, latestDate, None, None, YesNo.Yes)
  val testRequest: LandOrConnectedPropertyRequest =
    LandOrConnectedPropertyRequest(reportDetails = testReportDetails, transactions = None)
  val testOutstandingRequest: OutstandingLoanRequest =
    OutstandingLoanRequest(reportDetails = testReportDetails, transactions = None)
  val testAssetsFromConnectedPartyRequest: AssetsFromConnectedPartyRequest =
    AssetsFromConnectedPartyRequest(reportDetails = testReportDetails, transactions = None)
  val testTangibleMoveablePropertyRequest: TangibleMoveablePropertyRequest =
    TangibleMoveablePropertyRequest(reportDetails = testReportDetails, transactions = None)
  val testUnquotedShareRequest: UnquotedShareRequest =
    UnquotedShareRequest(reportDetails = testReportDetails, transactions = None)

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

  val jsonPsrSubmittedResponse = jsonResponse(Json.stringify(Json.toJson(psrSubmittedResponse)), 201)

  val psaId: String = psaIdGen.sample.get.value
  val srn: Srn = Srn(srnGen.sample.get.value).value

  private val mockAccPeriodDetails: AccountingPeriodDetails =
    AccountingPeriodDetails(None, accountingPeriods = None)

  def connector(implicit app: Application): PSRConnector = injected[PSRConnector]

  "Land Arms Length" - {
    "fetch land arms length" in runningApplication { implicit app =>
      val response = LandOrConnectedPropertyResponse(List(landConnectedPropertyTrx1, landOrConnectedPropertyTrx2))
      stubGet(
        s"$baseUrl/land-arms-length/$testPstr?fbNumber=$fbNumber",
        jsonResponse(Json.toJson(response).toString, OK)
      )
      whenReady(connector.getLandArmsLength(testPstr, fbNumber.some, none, none)) { result =>
        result mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-arms-length?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitLandArmsLength(testRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-arms-length?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitLandArmsLength(testRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-arms-length?journeyType=Standard&fbNumber=$fbNumber", journeySubmissionCreatedResponse)

      val result = connector.submitLandArmsLength(testRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-arms-length?journeyType=Standard&fbNumber=$fbNumber", aResponse().withStatus(413))

      val result = connector.submitLandArmsLength(testRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(landConnectedPropertyTrx1))
      )
      val result = connector.submitLandArmsLength(fullSizeRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }

  "Land or Connected Property" - {

    "fetch land or connected property successfully" in runningApplication { implicit app =>
      val response = LandOrConnectedPropertyResponse(List(landConnectedPropertyTrx1, landOrConnectedPropertyTrx2))
      stubGet(
        s"$baseUrl/land-or-connected-property/$testPstr?periodStartDate=$testStartDay&psrVersion=1",
        jsonResponse(Json.toJson(response).toString, OK)
      )
      whenReady(connector.getLandOrConnectedProperty(testPstr, None, testStartDay.toString.some, "1".some)) { result =>
        result mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-or-connected-property?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitLandOrConnectedProperty(testRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/land-or-connected-property?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitLandOrConnectedProperty(testRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/land-or-connected-property?journeyType=Standard&fbNumber=$fbNumber",
        journeySubmissionCreatedResponse
      )

      val result = connector.submitLandOrConnectedProperty(testRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/land-or-connected-property?journeyType=Standard&fbNumber=$fbNumber",
        aResponse().withStatus(413)
      )

      val result = connector.submitLandOrConnectedProperty(testRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(landConnectedPropertyTrx1))
      )
      val result = connector.submitLandOrConnectedProperty(fullSizeRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }

  "Outstanding Loans" - {

    "fetch outstanding loans" in runningApplication { implicit app =>
      val response = OutstandingLoanResponse(List(outstandingLoanTransaction))
      stubGet(
        s"$baseUrl/outstanding-loans/$testPstr?fbNumber=$fbNumber",
        jsonResponse(Json.toJson(response).toString, OK)
      )
      whenReady(connector.getOutstandingLoans(testPstr, fbNumber.some, None, None)) { result =>
        result mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/outstanding-loans?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitOutstandingLoans(testOutstandingRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/outstanding-loans?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitOutstandingLoans(testOutstandingRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/outstanding-loans?journeyType=Standard&fbNumber=$fbNumber", journeySubmissionCreatedResponse)

      val result = connector.submitOutstandingLoans(testOutstandingRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/outstanding-loans?journeyType=Standard&fbNumber=$fbNumber", aResponse().withStatus(413))

      val result = connector.submitOutstandingLoans(testOutstandingRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testOutstandingRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(outstandingLoanTransaction))
      )
      val result = connector.submitOutstandingLoans(fullSizeRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }

  "Assets From Connected Party" - {
    "fetch assets from connected party" in runningApplication { implicit app =>
      val response = AssetsFromConnectedPartyResponse(List(assetsFromConnectedPartyTransaction))
      stubGet(
        s"$baseUrl/assets-from-connected-party/$testPstr?fbNumber=$fbNumber",
        jsonResponse(Json.toJson(response).toString, OK)
      )
      whenReady(connector.getAssetsFromConnectedParty(testPstr, fbNumber.some, None, None)) { result =>
        result mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/assets-from-connected-party?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitAssetsFromConnectedParty(testAssetsFromConnectedPartyRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/assets-from-connected-party?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitAssetsFromConnectedParty(testAssetsFromConnectedPartyRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/assets-from-connected-party?journeyType=Standard&fbNumber=$fbNumber",
        journeySubmissionCreatedResponse
      )

      val result = connector.submitAssetsFromConnectedParty(testAssetsFromConnectedPartyRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/assets-from-connected-party?journeyType=Standard&fbNumber=$fbNumber",
        aResponse().withStatus(413)
      )

      val result = connector.submitAssetsFromConnectedParty(testAssetsFromConnectedPartyRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testAssetsFromConnectedPartyRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(assetsFromConnectedPartyTransaction))
      )
      val result = connector.submitAssetsFromConnectedParty(fullSizeRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }

  "Tangible Moveable Property" - {
    "fetch tangible moveable properties" in runningApplication { implicit app =>
      val response = TangibleMoveablePropertyResponse(List(tangibleMoveablePropertyTransaction))
      stubGet(
        s"$baseUrl/tangible-moveable-property/$testPstr?fbNumber=$fbNumber",
        jsonResponse(Json.toJson(response).toString, OK)
      )
      whenReady(connector.getTangibleMoveableProperty(testPstr, fbNumber.some, None, None)) { result =>
        result mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/tangible-moveable-property?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitTangibleMoveableProperty(testTangibleMoveablePropertyRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/tangible-moveable-property?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitTangibleMoveableProperty(testTangibleMoveablePropertyRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/tangible-moveable-property?journeyType=Standard&fbNumber=$fbNumber",
        journeySubmissionCreatedResponse
      )

      val result = connector.submitTangibleMoveableProperty(testTangibleMoveablePropertyRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/tangible-moveable-property?journeyType=Standard&fbNumber=$fbNumber",
        aResponse().withStatus(413)
      )

      val result = connector.submitTangibleMoveableProperty(testTangibleMoveablePropertyRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testTangibleMoveablePropertyRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(tangibleMoveablePropertyTransaction))
      )
      val result = connector.submitTangibleMoveableProperty(fullSizeRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }

  "Unquoted Shares" - {
    "fetch unquoted shares" in runningApplication { implicit app =>
      val response = UnquotedShareResponse(List(unquotedShareTransaction))
      stubGet(
        s"$baseUrl/unquoted-shares/$testPstr?fbNumber=$fbNumber",
        jsonResponse(Json.toJson(response).toString, OK)
      )
      whenReady(connector.getUnquotedShares(testPstr, fbNumber.some, None, None)) { result =>
        result mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/unquoted-shares?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.submitUnquotedShares(testUnquotedShareRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/unquoted-shares?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.submitUnquotedShares(testUnquotedShareRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/unquoted-shares?journeyType=Standard&fbNumber=$fbNumber", journeySubmissionCreatedResponse)

      val result = connector.submitUnquotedShares(testUnquotedShareRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpRequestDataSizeExceedError from server" in runningApplication { implicit app =>
      stubPut(s"$baseUrl/unquoted-shares?journeyType=Standard&fbNumber=$fbNumber", aResponse().withStatus(413))

      val result = connector.submitUnquotedShares(testUnquotedShareRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }

    "return an EtmpRequestDataSizeExceedError from fe" in runningApplication { implicit app =>
      val fullSizeRequest = testUnquotedShareRequest.copy(
        transactions = NonEmptyList.fromList(List.fill(100)(unquotedShareTransaction))
      )
      val result = connector.submitUnquotedShares(fullSizeRequest, JourneyType.Standard, Journey.InterestInLandOrProperty, srn)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpRequestDataSizeExceedError]
      }
    }
  }

  "submitPsr" - {

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPost(s"$baseUrl/sipp?journeyType=Standard", serverError)

      val result = connector.submitPsr(
        testPstr,
        JourneyType.Standard,
        Some(fbNumber),
        Some(testStartDay.toString),
        None,
        DateRange(testStartDay, testStartDay.plusYears(1)),
        Some("Test Scheme"),
        psaId
      )

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPost(s"$baseUrl/sipp?journeyType=Standard", notFound)

      val result = connector.submitPsr(
        testPstr,
        JourneyType.Standard,
        Some(fbNumber),
        Some(testStartDay.toString),
        None,
        DateRange(testStartDay, testStartDay.plusYears(1)),
        Some("Test Scheme"),
        psaId
      )

      whenReady(result.failed) { exception =>
        exception mustBe an[NotFoundException]
      }
    }

    "return a successful response" in runningApplication { implicit app =>
      stubPost(s"$baseUrl/sipp?journeyType=Standard", jsonPsrSubmittedResponse)

      val result = connector.submitPsr(
        testPstr,
        JourneyType.Standard,
        Some(fbNumber),
        Some(testStartDay.toString),
        None,
        DateRange(testStartDay, testStartDay.plusYears(1)),
        Some("Test Scheme"),
        psaId
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
        s"$baseUrl/versions/$testPstr?startDate=${testStartDay.format(DateTimeFormatter.ISO_DATE)}",
        jsonResponse(Json.stringify(Json.toJson(response)), 200)
      )

      val result = connector.getPsrVersions(testPstr, testStartDay)

      whenReady(result) { res =>
        res mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/versions/$testPstr?startDate=${testStartDay.format(DateTimeFormatter.ISO_DATE)}", serverError)

      val result = connector.getPsrVersions(testPstr, testStartDay)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubGet(
        s"$baseUrl/versions/$testPstr?startDate=${testStartDay.format(DateTimeFormatter.ISO_DATE)}",
        notFound
      )

      val result = connector.getPsrVersions(testPstr, testStartDay)

      whenReady(result.failed) { exception =>
        exception mustBe a[NotFoundException]
      }
    }
  }

  "getPSRSubmission" - {

    "return a successful response" in runningApplication { implicit app =>
      val response = PSRSubmissionResponse(
        testReportDetails,
        Some(mockAccPeriodDetails),
        None,
        None,
        None,
        None,
        None,
        None,
        emptyVersions
      )
      stubGet(s"$baseUrl/sipp/$testPstr?fbNumber=$fbNumber", jsonResponse(Json.stringify(Json.toJson(response)), 200))

      val result = connector.getPSRSubmission(testPstr, Some(fbNumber), None, None)

      whenReady(result) { res =>
        res mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/sipp/$testPstr?fbNumber=$fbNumber", serverError)

      val result = connector.getPSRSubmission(testPstr, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/sipp/$testPstr?fbNumber=$fbNumber", notFound)

      val result = connector.getPSRSubmission(testPstr, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe a[NotFoundException]
      }
    }
  }

  "getMemberDetails" - {

    "return a successful response" in runningApplication { implicit app =>
      val response = MemberDetailsResponse(memberDetailsGen.sample.toList)

      stubGet(
        s"$baseUrl/member-details/$testPstr?fbNumber=$fbNumber",
        jsonResponse(Json.stringify(Json.toJson(response)), 200)
      )

      val result = connector.getMemberDetails(testPstr, Some(fbNumber), None, None)

      whenReady(result) { res =>
        res mustBe response
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/member-details/$testPstr?fbNumber=$fbNumber", serverError)

      val result = connector.getMemberDetails(testPstr, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/member-details/$testPstr?fbNumber=$fbNumber", notFound)

      val result = connector.getMemberDetails(testPstr, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe a[NotFoundException]
      }
    }
  }

  "deleteMember" - {

    "return a successful response" in runningApplication { implicit app =>
      val response = memberDetailsGen.sample.get

      stubPut(
        s"$baseUrl/delete-member/$testPstr?journeyType=Standard&fbNumber=$fbNumber",
        journeySubmissionCreatedResponse
      )

      val result = connector.deleteMember(testPstr, JourneyType.Standard, Some(fbNumber), None, None, response)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      val response = memberDetailsGen.sample.get

      stubPut(s"$baseUrl/delete-member/$testPstr?journeyType=Standard&fbNumber=$fbNumber", serverError)

      val result = connector.deleteMember(testPstr, JourneyType.Standard, Some(fbNumber), None, None, response)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      val memberDetails = memberDetailsGen.sample.get

      stubPut(s"$baseUrl/delete-member/$testPstr?journeyType=Standard&fbNumber=$fbNumber", notFound)

      val result = connector.deleteMember(testPstr, JourneyType.Standard, Some(fbNumber), None, None, memberDetails)

      whenReady(result.failed) { exception =>
        exception mustBe a[NotFoundException]
      }
    }
  }

  "deleteAssets" - {

    "return a successful response" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/delete-assets/$testPstr?journey=ArmsLengthLandOrProperty&journeyType=Standard&fbNumber=$fbNumber",
        journeySubmissionCreatedResponse
      )

      val result =
        connector.deleteAssets(testPstr, ArmsLengthLandOrProperty, JourneyType.Standard, Some(fbNumber), None, None)

      whenReady(result) { res =>
        res mustBe sippPsrJourneySubmissionEtmpResponse
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/delete-assets/$testPstr?journey=ArmsLengthLandOrProperty&journeyType=Standard&fbNumber=$fbNumber",
        serverError
      )

      val result =
        connector.deleteAssets(testPstr, ArmsLengthLandOrProperty, JourneyType.Standard, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubPut(
        s"$baseUrl/delete-assets/$testPstr?journey=ArmsLengthLandOrProperty&journeyType=Standard&fbNumber=$fbNumber",
        notFound
      )

      val result =
        connector.deleteAssets(testPstr, ArmsLengthLandOrProperty, JourneyType.Standard, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe a[NotFoundException]
      }
    }
  }

  "getPsrAssetCounts" - {

    "return a successful response" in runningApplication { implicit app =>
      implicit val formatter: OFormat[OptionalResponse[PsrAssetCountsResponse]] =
        OptionalResponse.formatter()(using PsrAssetCountsResponse.formatPSRSubmissionResponse)

      val response = OptionalResponse(
        Some(
          PsrAssetCountsResponse(
            1, 2, 3, 4, 5, 6
          )
        )
      )

      stubGet(
        s"$baseUrl/asset-counts/$testPstr?fbNumber=$fbNumber",
        jsonResponse(Json.stringify(Json.toJson(response)), 200)
      )

      val result = connector.getPsrAssetCounts(testPstr, Some(fbNumber), None, None)

      whenReady(result) { res =>
        res.value mustBe response.response.value
      }
    }

    "return a NotFoundException" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/asset-counts/$testPstr?fbNumber=$fbNumber", notFound)

      val result = connector.getPsrAssetCounts(testPstr, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe a[NotFoundException]
      }
    }

    "return an EtmpServerError" in runningApplication { implicit app =>
      stubGet(s"$baseUrl/asset-counts/$testPstr?fbNumber=$fbNumber", serverError)

      val result = connector.getPsrAssetCounts(testPstr, Some(fbNumber), None, None)

      whenReady(result.failed) { exception =>
        exception mustBe an[EtmpServerError]
      }
    }
  }

  "createEmptyPsr" - {
    "is successfully invoked with the right content" in runningApplication { implicit app =>
      wireMockServer.stubFor(
        post(urlEqualTo(s"$baseUrl/empty/sipp"))
          .withRequestBody(equalToJson(Json.toJson(testReportDetails).toString))
          .willReturn(created())
      )
      connector.createEmptyPsr(testReportDetails).futureValue
    }

    "fails when the backend returns a failure" in runningApplication { implicit app =>
      wireMockServer.resetAll()
      val exception = intercept[Exception] {
        connector.createEmptyPsr(testReportDetails).futureValue
      }
      exception.getMessage must include("Create empty PSR failed with status")
    }
  }

  "updateMemberDetails" - {
    "successfully updates" in runningApplication { implicit app =>
      val journeyType = JourneyType.Standard
      val url = s"$baseUrl/member-details/$testPstr?journeyType=$journeyType&fbNumber=$fbNumber"
      val memberDetails = memberDetailsGen.sample.get
      val updatedMemberDetails = memberDetailsGen.sample.get
      val request = UpdateMemberDetailsRequest(memberDetails, updatedMemberDetails)
      val response = SippPsrJourneySubmissionEtmpResponse(fbNumber)
      wireMockServer
        .stubFor(
          put(urlEqualTo(url))
            .withRequestBody(equalToJson(Json.toJson(request).toString))
            .willReturn(jsonResponse(Json.toJson(response).toString, 201))
        )

      whenReady(connector.updateMemberDetails(testPstr, journeyType, fbNumber, None, None, request)) { actual =>
        actual mustBe response
      }
    }
  }
}
