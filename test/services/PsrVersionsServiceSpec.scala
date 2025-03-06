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

package services

import connectors.PSRConnector
import models.ReportStatus.SubmittedAndSuccessfullyProcessed
import models.requests.{AllowedAccessRequest, DataRequest}
import models.{PsrVersionsResponse, ReportSubmitterDetails, UserAnswers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PsrVersionsServiceSpec extends BaseSpec with Matchers with MockitoSugar with ScalaFutures {

  private val mockPsrConnector = mock[PSRConnector]
  private val service = PsrVersionsService(mockPsrConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private val defaultUserAnswers: UserAnswers = UserAnswers("id")
  private val allowedAccessRequest: AllowedAccessRequest[AnyContent] =
    allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val dataRequest: DataRequest[AnyContent] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  "getPsrVersions should return versions when connector call is successful" in {
    val pstr = "test-pstr"
    val startDate = LocalDate.now()
    val response = Seq(
      PsrVersionsResponse(
        reportFormBundleNumber = "123456",
        reportVersion = 1,
        reportStatus = SubmittedAndSuccessfullyProcessed,
        compilationOrSubmissionDate = ZonedDateTime.now,
        reportSubmitterDetails = Some(ReportSubmitterDetails("John", None, None)),
        psaDetails = None
      )
    )

    when(mockPsrConnector.getPsrVersions(pstr, startDate)).thenReturn(Future.successful(response))

    val result = service.getPsrVersions(pstr, startDate).futureValue

    result mustEqual response
  }

  "getPsrVersions should return empty list when connector call returns empty list" in {
    val pstr = "test-pstr"
    val startDate = LocalDate.now()
    val response = Seq.empty[PsrVersionsResponse]

    when(mockPsrConnector.getPsrVersions(pstr, startDate)).thenReturn(Future.successful(response))

    val result = service.getPsrVersions(pstr, startDate).futureValue

    result mustEqual response
  }

  "getPsrVersions should fail when connector call fails" in {
    val pstr = "test-pstr"
    val startDate = LocalDate.now()

    when(mockPsrConnector.getPsrVersions(pstr, startDate))
      .thenReturn(Future.failed(Exception("Connector call failed")))

    val result = service.getPsrVersions(pstr, startDate).failed.futureValue

    result mustBe a[Exception]
    result.getMessage mustEqual "Connector call failed"
  }
}
