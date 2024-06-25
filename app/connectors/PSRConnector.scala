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

import config.FrontendAppConfig
import models.backend.responses.PSRSubmissionResponse
import models.requests.LandOrConnectedPropertyApi._
import models.requests._
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, InternalServerException, NotFoundException, UpstreamErrorResponse}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PSRConnector @Inject()(appConfig: FrontendAppConfig, http: HttpClient)(implicit ec: ExecutionContext)
    extends Logging {

  private val baseUrl = s"${appConfig.pensionSchemeReturn.baseUrl}/pension-scheme-return-sipp/psr"

  def submitLandArmsLength(request: LandOrConnectedPropertyRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http
      .PUT[LandOrConnectedPropertyRequest, Unit](s"$baseUrl/land-arms-length", request, headers)
      .recoverWith(handleError)

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

  def submitLandOrConnectedProperty(request: LandOrConnectedPropertyRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http
      .PUT[LandOrConnectedPropertyRequest, Unit](s"$baseUrl/land-or-connected-property", request, headers)
      .recoverWith(handleError)

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

  def submitOutstandingLoans(request: OutstandingLoanRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http.PUT[OutstandingLoanRequest, Unit](s"$baseUrl/outstanding-loans", request, headers).recoverWith(handleError)

  def submitAssetsFromConnectedParty(
    request: AssetsFromConnectedPartyRequest
  )(implicit hc: HeaderCarrier): Future[Unit] =
    http
      .PUT[AssetsFromConnectedPartyRequest, Unit](s"$baseUrl/assets-from-connected-party", request, headers)
      .recoverWith(handleError)

  def submitTangibleMoveableProperty(
    request: TangibleMoveablePropertyRequest
  )(implicit hc: HeaderCarrier): Future[Unit] =
    http
      .PUT[TangibleMoveablePropertyRequest, Unit](s"$baseUrl/tangible-moveable-property", request, headers)
      .recoverWith(handleError)

  def submitUnquotedShares(
    request: UnquotedShareRequest
  )(implicit hc: HeaderCarrier): Future[Unit] =
    http.PUT[UnquotedShareRequest, Unit](s"$baseUrl/unquoted-shares", request, headers).recoverWith(handleError)

  def getPSRSubmission(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[PSRSubmissionResponse] = {

    val queryParams = (optPeriodStartDate, optPsrVersion, optFbNumber) match {
      case (Some(startDate), Some(version), _) =>
        Seq(
          "periodStartDate" -> startDate,
          "psrVersion" -> version
        )
      case (_, _, Some(fbNumber)) =>
        Seq("fbNumber" -> fbNumber)
      case _ =>
        Seq.empty[(String, String)]
    }

    http
      .GET[PSRSubmissionResponse](s"$baseUrl/sipp/$pstr", queryParams)
      .recoverWith(handleError)
  }

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

  private def headers: Seq[(String, String)] = Seq(
    "CorrelationId" -> UUID.randomUUID().toString
  )

  private def createQueryParams(
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  ) = {
    val queryParams = (optPeriodStartDate, optPsrVersion, optFbNumber) match {
      case (Some(startDate), Some(version), _) =>
        Seq(
          "periodStartDate" -> startDate,
          "psrVersion" -> version
        )
      case (_, _, Some(fbNumber)) =>
        Seq(
          "fbNumber" -> fbNumber
        )
      case _ =>
        throw new RuntimeException("Query Parameters not correct!") //TODO how can we handle that part??
    }
    queryParams
  }

  private def handleError: PartialFunction[Throwable, Future[Nothing]] = {
    case UpstreamErrorResponse(message, statusCode, _, _)
        if (statusCode >= 400 && statusCode < 500 && statusCode != 404) || (statusCode >= 500) =>
      logger.error(s"PSR backend call failed with code $statusCode and message $message")
      Future.failed(new InternalServerException(message))
    case UpstreamErrorResponse(message, statusCode, _, _) if statusCode == 404 =>
      logger.error(s"PSR backend call failed with code $statusCode and message $message")
      Future.failed(new NotFoundException(message))
    case e: Exception =>
      logger.error(s"PSR backend call failed with code exception $e")
      Future.failed(new InternalServerException(e.getMessage))
  }
}
