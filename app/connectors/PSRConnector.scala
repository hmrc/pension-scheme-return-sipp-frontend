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

import config.FrontendAppConfig
import models.backend.responses.{MemberDetails, MemberDetailsResponse, PSRSubmissionResponse}
import models.error.{EtmpRequestDataSizeExceedError, EtmpServerError}
import models.requests.AssetsFromConnectedPartyApi._
import models.requests.LandOrConnectedPropertyApi._
import models.requests.OutstandingLoanApi._
import models.requests.PsrSubmissionRequest.PsrSubmittedResponse
import models.requests.TangibleMoveablePropertyApi._
import models.requests.UnquotedShareApi._
import models.requests._
import models.{DateRange, JourneyType, PsrVersionsResponse}
import play.api.Logging
import play.api.http.Status
import play.api.http.Status.{NOT_FOUND, REQUEST_ENTITY_TOO_LARGE}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{
  HeaderCarrier,
  HttpClient,
  HttpResponse,
  InternalServerException,
  NotFoundException,
  UpstreamErrorResponse
}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PSRConnector @Inject()(appConfig: FrontendAppConfig, http: HttpClient)(
  implicit ec: ExecutionContext
) extends Logging {

  private val baseUrl = s"${appConfig.pensionSchemeReturn.baseUrl}/pension-scheme-return-sipp/psr"

  def submitLandArmsLength(
    request: LandOrConnectedPropertyRequest,
  )(implicit hc: HeaderCarrier): Future[Unit] =
    submitRequest(request, s"$baseUrl/land-arms-length??journeyType=${JourneyType.Standard}") //TODO: pass correct journey type for amend journey

  def getLandArmsLength(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[LandOrConnectedPropertyResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    http
      .GET[LandOrConnectedPropertyResponse](s"$baseUrl/land-arms-length/$pstr", queryParams, headers)
      .recoverWith(handleError)
  }

  def submitLandOrConnectedProperty(
    request: LandOrConnectedPropertyRequest,
  )(implicit hc: HeaderCarrier): Future[Unit] =
    submitRequest(request, s"$baseUrl/land-or-connected-property?journeyType=${JourneyType.Standard}") //TODO: pass correct journey type for amend journey

  def getLandOrConnectedProperty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[LandOrConnectedPropertyResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    http
      .GET[LandOrConnectedPropertyResponse](s"$baseUrl/land-or-connected-property/$pstr", queryParams, headers)
      .recoverWith(handleError)
  }

  def submitOutstandingLoans(
    request: OutstandingLoanRequest,
  )(implicit hc: HeaderCarrier): Future[Unit] =
    submitRequest(request, s"$baseUrl/outstanding-loans?journeyType=${JourneyType.Standard}") //TODO: pass correct journey type for amend journey

  def getOutstandingLoans(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[OutstandingLoanResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    http
      .GET[OutstandingLoanResponse](s"$baseUrl/outstanding-loans/$pstr", queryParams, headers)
      .recoverWith(handleError)
  }

  def submitAssetsFromConnectedParty(
    request: AssetsFromConnectedPartyRequest,
  )(implicit hc: HeaderCarrier): Future[Unit] =
    submitRequest(request, s"$baseUrl/assets-from-connected-party?journeyType=${JourneyType.Standard}") //TODO: pass correct journey type for amend journey

  def getAssetsFromConnectedParty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[AssetsFromConnectedPartyResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    http
      .GET[AssetsFromConnectedPartyResponse](s"$baseUrl/assets-from-connected-party/$pstr", queryParams, headers)
      .recoverWith(handleError)
  }

  def submitTangibleMoveableProperty(
    request: TangibleMoveablePropertyRequest,
  )(implicit hc: HeaderCarrier): Future[Unit] =
    submitRequest(request, s"$baseUrl/tangible-moveable-property?journeyType=${JourneyType.Standard}") //TODO: pass correct journey type for amend journey

  def getTangibleMoveableProperty(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[TangibleMoveablePropertyResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    http
      .GET[TangibleMoveablePropertyResponse](s"$baseUrl/tangible-moveable-property/$pstr", queryParams, headers)
      .recoverWith(handleError)
  }

  def submitUnquotedShares(
    request: UnquotedShareRequest,
  )(implicit hc: HeaderCarrier): Future[Unit] =
    submitRequest(request, s"$baseUrl/unquoted-shares?journeyType=${JourneyType.Standard}") //TODO: pass correct journey type for amend journey

  def getUnquotedShares(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[UnquotedShareResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    http
      .GET[UnquotedShareResponse](s"$baseUrl/unquoted-shares/$pstr", queryParams, headers)
      .recoverWith(handleError)
  }

  def getPSRSubmission(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[PSRSubmissionResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)

    http
      .GET[PSRSubmissionResponse](s"$baseUrl/sipp/$pstr", queryParams)
      .recoverWith(handleError)
  }

  def getMemberDetails(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit headerCarrier: HeaderCarrier): Future[MemberDetailsResponse] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)

    http
      .GET[MemberDetailsResponse](s"$baseUrl/member-details/$pstr", queryParams)
      .recoverWith(handleError)

  }

  def deleteMember(
    pstr: String,
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    memberDetails: MemberDetails
  )(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val fullUrl = s"$baseUrl/delete-member/$pstr?journeyType=$journeyType" + queryParams
      .map { case (k, v) => s"$k=$v" }
      .mkString("&", "&", "")
    submitRequest(memberDetails, fullUrl)
  }

  def submitPsr(
    pstr: String,
    journeyType: JourneyType,
    fbNumber: Option[String],
    periodStartDate: Option[String],
    psrVersion: Option[String],
    taxYear: DateRange,
    schemeName: Option[String]
  )(implicit headerCarrier: HeaderCarrier): Future[PsrSubmittedResponse] = {

    val request = PsrSubmissionRequest(
      pstr,
      fbNumber,
      periodStartDate,
      psrVersion,
      isPsa = false,
      taxYear,
      schemeName
    )

    http
      .POST[PsrSubmissionRequest, PsrSubmittedResponse](s"$baseUrl/sipp?journeyType=$journeyType", request, headers)
      .recoverWith(handleError)
  }

  def getPsrVersions(pstr: String, startDate: LocalDate)(implicit hc: HeaderCarrier): Future[Seq[PsrVersionsResponse]] =
    http
      .GET[Seq[PsrVersionsResponse]](
        s"$baseUrl/versions/$pstr",
        Seq("startDate" -> startDate.format(DateTimeFormatter.ISO_DATE))
      )
      .recoverWith(handleError)

  private def headers: Seq[(String, String)] = Seq(
    "CorrelationId" -> UUID.randomUUID().toString
  )

  private def createQueryParams(
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  ) =
    (optPeriodStartDate, optPsrVersion, optFbNumber) match {
      case (Some(startDate), Some(version), _) =>
        Seq("periodStartDate" -> startDate, "psrVersion" -> version)
      case (_, _, Some(fbNumber)) =>
        Seq("fbNumber" -> fbNumber)
      case _ =>
        throw new RuntimeException("Query Parameters not correct!") //TODO how can we handle that part??
    }

  private def submitRequest[T](request: T, url: String)(
    implicit hc: HeaderCarrier,
    w: Writes[T]
  ): Future[Unit] = {
    val jsonRequest = Json.toJson(request)
    val jsonSizeInBytes = jsonRequest.toString().getBytes("UTF-8").length

    if (jsonSizeInBytes > appConfig.maxRequestSize) {
      val errorMessage = s"Request body size exceeds maximum limit of ${appConfig.maxRequestSize} bytes"
      logger.error(errorMessage)
      Future.failed(new EtmpRequestDataSizeExceedError(errorMessage))
    } else {
      http
        .PUT[T, HttpResponse](url, request, headers)
        .flatMap {
          case response if response.status == Status.NO_CONTENT || response.status == Status.OK =>
            Future.successful(())
          case response =>
            Future.failed(UpstreamErrorResponse(response.body, response.status))
        }
        .recoverWith(handleError)
    }
  }

  def updateMemberDetails(
    pstr: String,
    journeyType: JourneyType,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    request: UpdateMemberDetailsRequest
  )(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    val queryParams = createQueryParams(optFbNumber, optPeriodStartDate, optPsrVersion)
    val fullUrl = s"$baseUrl/member-details/$pstr?journeyType=$journeyType" + queryParams
      .map { case (k, v) => s"$k=$v" }
      .mkString("&", "&", "")
    submitRequest(request, fullUrl)
  }

  private def handleError: PartialFunction[Throwable, Future[Nothing]] = {
    case UpstreamErrorResponse(message, NOT_FOUND, _, _) =>
      logger.error(s"PSR backend call failed with code 404 and message $message")
      Future.failed(new NotFoundException(message))
    case UpstreamErrorResponse(message, REQUEST_ENTITY_TOO_LARGE, _, _) =>
      logger.error(s"PSR backend call failed with code 413 and message $message")
      Future.failed(new EtmpRequestDataSizeExceedError(message))
    case UpstreamErrorResponse(message, statusCode, _, _) =>
      logger.error(s"PSR backend call failed with code $statusCode and message $message")
      Future.failed(new EtmpServerError(message))
    case e: Exception =>
      logger.error(s"PSR backend call failed with exception $e")
      Future.failed(new InternalServerException(e.getMessage))
  }
}
