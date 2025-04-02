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
import models.Journey
import models.audit.{FileUploadAuditEvent, PSRStartAuditEvent}
import org.mockito.ArgumentCaptor
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditServiceSpec extends BaseSpec with TestValues {

  private val mockConfig = mock[FrontendAppConfig]
  private val mockAuditConnector = mock[AuditConnector]

  implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val service = AuditService(mockConfig, mockAuditConnector, stubMessagesApi())

  private val testAppName = "test-app-name"

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockConfig.appName).thenReturn(testAppName)
    reset(mockAuditConnector)
  }

  "AuditService" - {
    "PSRStartAuditEvent PSA" in {

      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      when(mockAuditConnector.sendEvent(captor.capture())(any, any))
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
        "schemeName" -> defaultSchemeDetails.schemeName,
        "schemeAdministratorName" -> "testFirstName testLastName",
        "pensionSchemeAdministratorId" -> psaId.value,
        "pensionSchemeTaxReference" -> defaultSchemeDetails.pstr,
        "affinityGroup" -> "Organisation",
        "credentialRolePsaPsp" -> "PSA",
        "taxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "date" -> LocalDate.now().toString
      )

      dataEvent.auditSource mustEqual testAppName
      dataEvent.auditType mustEqual "PensionSchemeReturnStarted"
      dataEvent.detail mustEqual expectedDataEvent
    }

    "PSRStartAuditEvent PSP" in {

      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      when(mockAuditConnector.sendEvent(captor.capture())(any, any))
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
        "schemeName" -> defaultSchemeDetails.schemeName,
        "schemePractitionerName" -> "testFirstName testLastName",
        "pensionSchemePractitionerId" -> pspId.value,
        "pensionSchemeTaxReference" -> defaultSchemeDetails.pstr,
        "affinityGroup" -> "Organisation",
        "credentialRolePsaPsp" -> "PSP",
        "taxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "date" -> LocalDate.now().toString
      )

      dataEvent.auditSource mustEqual testAppName
      dataEvent.auditType mustEqual "PensionSchemeReturnStarted"
      dataEvent.detail mustEqual expectedDataEvent
    }

    "FileUpload for success case" in {

      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      when(mockAuditConnector.sendEvent(captor.capture())(any, any))
        .thenReturn(Future.successful(AuditResult.Success))

      val auditEvent = FileUploadAuditEvent(
        fileUploadType = Journey.OutstandingLoans.entryName,
        fileUploadStatus = "Success",
        fileName = "xxx.csv",
        fileReference = "123123123123",
        typeOfError = None,
        fileSize = 1223,
        validationCompleted = LocalDate.parse("2024-03-24"),
        pensionSchemeId = pspId,
        minimalDetails = defaultMinimalDetails,
        schemeDetails = defaultSchemeDetails,
        taxYear = dateRange
      )

      service.sendEvent(auditEvent).futureValue

      val dataEvent = captor.getValue
      val expectedDataEvent = Map(
        "fileUploadType" -> "outstandingLoans",
        "fileUploadStatus" -> "Success",
        "fileSize" -> "1223",
        "validationCompleted" -> "2024-03-24",
        "fileName" -> "xxx.csv",
        "fileReference" -> "123123123123",
        "schemeName" -> defaultSchemeDetails.schemeName,
        "schemePractitionerName" -> "testFirstName testLastName",
        "pensionSchemePractitionerId" -> pspId.value,
        "pensionSchemeTaxReference" -> defaultSchemeDetails.pstr,
        "affinityGroup" -> "Organisation",
        "credentialRolePsaPsp" -> "PSP",
        "taxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "date" -> LocalDate.now().toString
      )

      dataEvent.auditSource mustEqual testAppName
      dataEvent.auditType mustEqual "PensionSchemeReturnFileUpload"
      dataEvent.detail mustEqual expectedDataEvent
    }

    "FileUpload for success case including error details" in {

      val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      when(mockAuditConnector.sendExtendedEvent(captor.capture())(any, any))
        .thenReturn(Future.successful(AuditResult.Success))

      val auditEvent = FileUploadAuditEvent(
        fileUploadType = Journey.OutstandingLoans.entryName,
        fileUploadStatus = "Success",
        fileName = "xxx.csv",
        fileReference = "123123123123",
        typeOfError = None,
        fileSize = 1223,
        validationCompleted = LocalDate.parse("2024-03-24"),
        pensionSchemeId = pspId,
        minimalDetails = defaultMinimalDetails,
        schemeDetails = defaultSchemeDetails,
        taxYear = dateRange,
        errorDetails = Some(listOfValidationErrors)
      )

      service.sendExtendedEvent(auditEvent).futureValue

      val dataEvent = captor.getValue
      val expectedDataEvent = Json.obj(
        "fileUploadType" -> "outstandingLoans",
        "fileUploadStatus" -> "Success",
        "fileSize" -> "1223",
        "validationCompleted" -> "2024-03-24",
        "fileName" -> "xxx.csv",
        "fileReference" -> "123123123123",
        "schemeName" -> defaultSchemeDetails.schemeName,
        "schemePractitionerName" -> "testFirstName testLastName",
        "pensionSchemePractitionerId" -> pspId.value,
        "pensionSchemeTaxReference" -> defaultSchemeDetails.pstr,
        "affinityGroup" -> "Organisation",
        "credentialRolePsaPsp" -> "PSP",
        "taxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "date" -> LocalDate.now().toString,
        "errorDetails" -> Json.toJson(listOfValidationErrors.toList)
      )

      dataEvent.detail mustEqual expectedDataEvent
    }

    "FileUpload for fail case" in {

      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      when(mockAuditConnector.sendEvent(captor.capture())(any, any))
        .thenReturn(Future.successful(AuditResult.Success))

      val auditEvent = FileUploadAuditEvent(
        fileUploadType = Journey.OutstandingLoans.entryName,
        fileUploadStatus = "Error",
        fileName = "xxx.csv",
        fileReference = "123123123123",
        typeOfError = Some("Over 25"),
        fileSize = 1223,
        validationCompleted = LocalDate.parse("2024-03-24"),
        pensionSchemeId = pspId,
        minimalDetails = defaultMinimalDetails,
        schemeDetails = defaultSchemeDetails,
        taxYear = dateRange
      )

      service.sendEvent(auditEvent).futureValue

      val dataEvent = captor.getValue
      val expectedDataEvent = Map(
        "fileUploadType" -> "outstandingLoans",
        "fileUploadStatus" -> "Error",
        "fileSize" -> "1223",
        "validationCompleted" -> "2024-03-24",
        "typeOfError" -> "Over 25",
        "fileName" -> "xxx.csv",
        "fileReference" -> "123123123123",
        "schemeName" -> defaultSchemeDetails.schemeName,
        "schemePractitionerName" -> "testFirstName testLastName",
        "pensionSchemePractitionerId" -> pspId.value,
        "pensionSchemeTaxReference" -> defaultSchemeDetails.pstr,
        "affinityGroup" -> "Organisation",
        "credentialRolePsaPsp" -> "PSP",
        "taxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "date" -> LocalDate.now().toString
      )

      dataEvent.auditSource mustEqual testAppName
      dataEvent.auditType mustEqual "PensionSchemeReturnFileUpload"
      dataEvent.detail mustEqual expectedDataEvent
    }
  }
}
