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
import models.requests.{LandOrConnectedPropertyRequest, OutstandingLoanRequest}
import models.requests.psr.PsrSubmission
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PSRConnector @Inject()(appConfig: FrontendAppConfig, http: HttpClient)(implicit ec: ExecutionContext) {

  private val baseUrl = s"${appConfig.pensionSchemeReturn.baseUrl}/pension-scheme-return/psr/"

  def submitLandArmsLength(request: LandOrConnectedPropertyRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http.POST[LandOrConnectedPropertyRequest, Unit](s"$baseUrl/land-arms-length", request, headers)

  def submitLandOrConnectedProperty(request: LandOrConnectedPropertyRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http.POST[LandOrConnectedPropertyRequest, Unit](s"$baseUrl/land-or-connected-property", request, headers)

  def submitOutstandingLoans(request: OutstandingLoanRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http.POST[OutstandingLoanRequest, Unit](s"$baseUrl/outstanding-loans", request, headers)


  def submitPsrDetails(
    psrSubmission: PsrSubmission
  )(implicit hc: HeaderCarrier): Future[Unit] =
    http.POST[PsrSubmission, Unit](s"$baseUrl/standard", psrSubmission)

  def getStandardPsrDetails(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String]
  )(implicit hc: HeaderCarrier): Future[Option[PsrSubmission]] = {
    val queryParams = (optPeriodStartDate, optPsrVersion, optFbNumber) match {
      case (Some(startDate), Some(version), _) =>
        Seq("periodStartDate" -> startDate, "psrVersion" -> version)
      case (_, _, Some(fbNumber)) =>
        Seq("fbNumber" -> fbNumber)
      case _ =>
        Seq.empty
    }

    http
      .GET[HttpResponse](s"$baseUrl/standard/$pstr", queryParams)
      .map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[PsrSubmission] match {
              case JsSuccess(data, _) => Some(data)
              case JsError(errors) => throw JsResultException(errors)
            }
          case NOT_FOUND => None
          case other => throw new Exception(s"Unexpected response status $other for $pstr")
        }
      }
  }

  private def headers: Seq[(String, String)] = Seq(
    "CorrelationId" -> UUID.randomUUID().toString
  )
}
