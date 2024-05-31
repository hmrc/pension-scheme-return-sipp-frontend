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
import models.requests.{AssetsFromConnectedPartyRequest, LandOrConnectedPropertyRequest, OutstandingLoanRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PSRConnector @Inject()(appConfig: FrontendAppConfig, http: HttpClient)(implicit ec: ExecutionContext) {

  private val baseUrl = s"${appConfig.pensionSchemeReturn.baseUrl}/pension-scheme-return-sipp/psr"

  def submitLandArmsLength(request: LandOrConnectedPropertyRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http.PUT[LandOrConnectedPropertyRequest, Unit](s"$baseUrl/land-arms-length", request, headers)

  def submitLandOrConnectedProperty(request: LandOrConnectedPropertyRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http.PUT[LandOrConnectedPropertyRequest, Unit](s"$baseUrl/land-or-connected-property", request, headers)

  def submitOutstandingLoans(request: OutstandingLoanRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http.PUT[OutstandingLoanRequest, Unit](s"$baseUrl/outstanding-loans", request, headers)

  def submitAssetsFromConnectedParty(
    request: AssetsFromConnectedPartyRequest
  )(implicit hc: HeaderCarrier): Future[Unit] =
    http.PUT[AssetsFromConnectedPartyRequest, Unit](s"$baseUrl/assets-from-connected-party", request, headers)

  private def headers: Seq[(String, String)] = Seq(
    "CorrelationId" -> UUID.randomUUID().toString
  )
}
