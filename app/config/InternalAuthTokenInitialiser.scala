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

package config

import play.api.Logging
import play.api.http.Status.{CREATED, NOT_FOUND, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import services.RetryService
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class InternalAuthTokenInitialiser {
  val initialised: Future[Unit]
}

@Singleton
class NoOpInternalAuthTokenInitialiser @Inject() extends InternalAuthTokenInitialiser {
  override val initialised: Future[Unit] = Future.successful((): Unit)
}

@Singleton
class InternalAuthTokenInitialiserImpl @Inject() (
  config: FrontendAppConfig,
  httpClient: HttpClientV2,
  retryService: RetryService
)(implicit ec: ExecutionContext)
    extends InternalAuthTokenInitialiser
    with Logging {

  override val initialised: Future[Unit] = retryService.retry(ensureAuthToken())

  Await.result(initialised, 5.minutes)

  private def ensureAuthToken(): Future[Unit] =
    authTokenIsValid.flatMap { isValid =>
      if (isValid) {
        logger.info("Auth token is already valid")
        Future.successful((): Unit)
      } else {
        createClientAuthToken()
      }
    }

  private def createClientAuthToken(): Future[Unit] = {
    logger.info("Initialising auth token")
    httpClient
      .post(url"${config.internalAuthService.baseUrl}/test-only/token")(HeaderCarrier())
      .withBody(
        Json.obj(
          "token" -> config.internalAuthToken,
          "principal" -> config.appName,
          "permissions" -> Seq(
            Json.obj(
              "resourceType" -> "object-store",
              "resourceLocation" -> "pension-scheme-return-sipp-frontend",
              "actions" -> List("READ", "WRITE", "DELETE")
            )
          )
        )
      )
      .execute
      .flatMap { response =>
        if (response.status == CREATED) {
          logger.info(
            "Auth token initialised"
          )
          Future.successful((): Unit)
        } else {
          logger.error(
            "Unable to initialise internal-auth token"
          )
          Future.failed(new RuntimeException("Unable to initialise internal-auth token"))
        }
      }
  }

  private def authTokenIsValid: Future[Boolean] = {
    logger.info("Checking auth token")
    httpClient
      .get(url"${config.internalAuthService.baseUrl}/test-only/token")(HeaderCarrier())
      .setHeader("Authorization" -> config.internalAuthToken)
      .execute
      .flatMap {
        _.status match {
          case OK        => Future.successful(true)
          case NOT_FOUND => Future.successful(false)
          case _         => Future.failed(new RuntimeException("Unexpected response"))
        }
      }
  }
}
