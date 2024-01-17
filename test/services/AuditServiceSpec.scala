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

package services

import config.FrontendAppConfig
import controllers.TestValues
import models.audit.PSRStartAuditEvent
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditServiceSpec extends BaseSpec with TestValues {

  private val mockConfig = mock[FrontendAppConfig]
  private val mockAuditConnector = mock[AuditConnector]

  implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val service = new AuditService(mockConfig, mockAuditConnector)

  private val testAppName = "test-app-name"

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockConfig.appName).thenReturn(testAppName)
    reset(mockAuditConnector)
  }

  "AuditService" - {
    "PSRStartAuditEvent PSA" in {

      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      when(mockAuditConnector.sendEvent(captor.capture())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val auditEvent = PSRStartAuditEvent(
        psaId,
        defaultMinimalDetails,
        defaultSchemeDetails,
        dateRange
      )

      service.sendEvent(auditEvent).futureValue

      val dataEvent = captor.getValue
      val expectedDataEvent = Map(
        "SchemeName" -> defaultSchemeDetails.schemeName,
        "SchemeAdministratorName" -> "testFirstName testLastName",
        "PensionSchemeAdministratorId" -> psaId.value,
        "PensionSchemeTaxReference" -> defaultSchemeDetails.pstr,
        "AffinityGroup" -> "Organisation",
        "CredentialRole(PSA/PSP)" -> "PSA",
        "TaxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "Date" -> LocalDate.now().toString
      )

      dataEvent.auditSource mustEqual testAppName
      dataEvent.auditType mustEqual "PsrSippStart"
      dataEvent.detail mustEqual expectedDataEvent
    }

    "PSRStartAuditEvent PSP" in {

      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      when(mockAuditConnector.sendEvent(captor.capture())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val auditEvent = PSRStartAuditEvent(
        pspId,
        defaultMinimalDetails,
        defaultSchemeDetails,
        dateRange
      )

      service.sendEvent(auditEvent).futureValue

      val dataEvent = captor.getValue
      val expectedDataEvent = Map(
        "SchemeName" -> defaultSchemeDetails.schemeName,
        "SchemePractitionerName" -> "testFirstName testLastName",
        "PensionSchemePractitionerId" -> pspId.value,
        "PensionSchemeTaxReference" -> defaultSchemeDetails.pstr,
        "AffinityGroup" -> "Organisation",
        "CredentialRole(PSA/PSP)" -> "PSP",
        "TaxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "Date" -> LocalDate.now().toString
      )

      dataEvent.auditSource mustEqual testAppName
      dataEvent.auditType mustEqual "PsrSippStart"
      dataEvent.detail mustEqual expectedDataEvent
    }
  }
}
