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
import models.SchemeId.Pstr
import models.backend.responses.PsrAssetCountsResponse
import models.FormBundleNumber
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReportDetailsServiceSpec extends BaseSpec with Matchers with MockitoSugar with ScalaFutures {

  private val mockTaxYearService = mock[TaxYearService]
  private val mockConnector = mock[PSRConnector]
  private val service = ReportDetailsService(mockTaxYearService, mockConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "getAssetCounts should return asset counts when connector call is successful" in {
    val fbNumber = Some(FormBundleNumber("test-fb-number"))
    val taxYear = Some("2023")
    val version = Some("1")
    val pstr = Pstr("test-pstr")
    val response = Some(
      PsrAssetCountsResponse(
        interestInLandOrPropertyCount = 1,
        landArmsLengthCount = 1,
        assetsFromConnectedPartyCount = 1,
        tangibleMoveablePropertyCount = 1,
        outstandingLoansCount = 1,
        unquotedSharesCount = 1
      )
    )

    when(mockConnector.getPsrAssetCounts(pstr.value, fbNumber.map(_.value), taxYear, version))
      .thenReturn(Future.successful(response))

    val result = service.getAssetCounts(fbNumber, taxYear, version, pstr).futureValue

    result mustEqual response
  }

  "getAssetCounts should return None when connector call returns None" in {
    val fbNumber = Some(FormBundleNumber("test-fb-number"))
    val taxYear = Some("2023")
    val version = Some("1")
    val pstr = Pstr("test-pstr")

    when(mockConnector.getPsrAssetCounts(pstr.value, fbNumber.map(_.value), taxYear, version))
      .thenReturn(Future.successful(None))

    val result = service.getAssetCounts(fbNumber, taxYear, version, pstr).futureValue

    result mustEqual None
  }
}
