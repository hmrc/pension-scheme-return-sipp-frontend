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

import com.google.inject.ImplementedBy
import config.FrontendAppConfig
import models.PensionSchemeId.{PsaId, PspId}
import models.SchemeId.Srn
import models.{ListMinimalSchemeDetails, SchemeDetails, SchemeId}
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HttpReads.Implicits.{readFromJson, readOptionOfNotFound}
import uk.gov.hmrc.http.UpstreamErrorResponse.WithStatusCode
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.FutureUtils.tapError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SchemeDetailsConnectorImpl @Inject() (appConfig: FrontendAppConfig, http: HttpClientV2)
    extends SchemeDetailsConnector {

  private def url(relativePath: String) = {
    val urlStr = s"${appConfig.pensionsScheme}$relativePath"
    url"$urlStr"
  }

  // API 1444 (Get scheme details)
  override def details(
    psaId: PsaId,
    schemeId: SchemeId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeDetails]] = {
    val headers = List(
      "idNumber" -> schemeId.value,
      "schemeIdType" -> schemeId.idType,
      "psaId" -> psaId.value
    )

    http
      .get(url(s"/pensions-scheme/scheme/${schemeId.value}"))
      .setHeader(headers*)
      .execute[Option[SchemeDetails]]
      .tapError { t =>
        Future.successful(
          logger.error(s"Failed to fetch scheme details for $schemeId with message ${t.getMessage}")
        )
      }
  }

  // API 1444 (Get scheme details)
  override def details(
    pspId: PspId,
    schemeId: Srn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SchemeDetails]] = {
    val headers = List("pspId" -> pspId.value, "srn" -> schemeId.value)

    http
      .get(url(s"/pensions-scheme/psp-scheme/${schemeId.value}"))
      .setHeader(headers*)
      .execute[Option[SchemeDetails]]
      .tapError { t =>
        Future.successful(
          logger.error(s"Failed to fetch scheme details for $schemeId with message ${t.getMessage}", t)
        )
      }
  }

  def listSchemeDetails(
    psaId: PsaId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ListMinimalSchemeDetails]] =
    listSchemeDetails(psaId.value, "PSA")

  def listSchemeDetails(
    pspId: PspId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ListMinimalSchemeDetails]] =
    listSchemeDetails(pspId.value, "PSP")

  private def listSchemeDetails(
    idValue: String,
    idType: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ListMinimalSchemeDetails]] = {
    val headers = List("idValue" -> idValue, "idType" -> idType)

    http
      .get(url("/pensions-scheme/list-of-schemes-self"))
      .setHeader(headers*)
      .execute[ListMinimalSchemeDetails]
      .map(Some(_))
      .recover { case WithStatusCode(NOT_FOUND) => None }
      .tapError { t =>
        Future.successful(
          logger.error(s"Failed list scheme details for $idType $idValue with message ${t.getMessage}", t)
        )
      }
  }
}

@ImplementedBy(classOf[SchemeDetailsConnectorImpl])
trait SchemeDetailsConnector {

  protected val logger: Logger = Logger(classOf[SchemeDetailsConnector])

  def details(psaId: PsaId, schemeId: SchemeId)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[SchemeDetails]]

  def details(pspId: PspId, schemeId: Srn)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[SchemeDetails]]

  def listSchemeDetails(
    psaId: PsaId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ListMinimalSchemeDetails]]

  def listSchemeDetails(
    pspId: PspId
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ListMinimalSchemeDetails]]
}
