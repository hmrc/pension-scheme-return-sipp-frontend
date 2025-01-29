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

import cats.data.NonEmptyList
import connectors.PSRConnector
import models.SchemeId.Pstr
import models.backend.responses.{AccountingPeriod, AccountingPeriodDetails, PSRSubmissionResponse, Versions}
import models.requests.common.YesNo
import models.requests.psr.EtmpPsrStatus.Compiled
import models.requests.psr.{EtmpPsrStatus, ReportDetails}
import models.requests.{
  AllowedAccessRequest,
  DataRequest,
  FormBundleOrVersionTaxYearRequest,
  LandOrConnectedPropertyApi
}
import models.{BasicDetails, DateRange, FormBundleNumber, NormalMode, SchemeId, UserAnswers, VersionTaxYear}
import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SchemeDateServiceSpec extends BaseSpec with ScalaCheckPropertyChecks {

  val connector: PSRConnector = mock[PSRConnector]

  val service = SchemeDateServiceImpl(connector)

  override def beforeEach(): Unit = reset(connector)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val defaultUserAnswers: UserAnswers = UserAnswers("id")
  val srn: SchemeId.Srn = srnGen.sample.value
  val fbNumber: FormBundleNumber = FormBundleNumber("test")
  val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] =
    allowedAccessRequestGen(FakeRequest()).sample.value

  val oldestDateRange: Gen[DateRange] =
    dateRangeWithinRangeGen(
      DateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2010, 1, 1))
    )

  val newestDateRange: Gen[DateRange] =
    dateRangeWithinRangeGen(
      DateRange(LocalDate.of(2011, 1, 1), LocalDate.of(2020, 1, 1))
    )

  private val mockReportDetails: ReportDetails =
    ReportDetails("test", Compiled, earliestDate, latestDate, None, None, YesNo.Yes)
  private val emptyVersions: Versions = Versions(None, None, None, None, None, None, None)

  "returnAccountingPeriods" - {

    "return None when nothing is in cache" in {
      val request = FormBundleOrVersionTaxYearRequest(
        Some(fbNumber),
        None,
        DataRequest(allowedAccessRequest, defaultUserAnswers)
      )

      when(connector.getPSRSubmission(any, any, any, any)(any))
        .thenReturn(
          Future.successful(
            PSRSubmissionResponse(
              mockReportDetails,
              None,
              None,
              None,
              None,
              None,
              None,
              None,
              emptyVersions
            )
          )
        )
      val result = service.returnAccountingPeriods(request)(
        scala.concurrent.ExecutionContext.Implicits.global,
        HeaderCarrier()
      )

      result.futureValue mustBe None
    }

    s"choose periods from the psr response" in {
      forAll(dateRangeGen, oldestDateRange, newestDateRange) {

        (accountingPeriod1, accountingPeriod2, accountingPeriod3) =>

          val mockAccPeriodDetails: AccountingPeriodDetails =
            AccountingPeriodDetails(
              None,
              accountingPeriods = Some(
                NonEmptyList
                  .of(
                    accountingPeriod1,
                    accountingPeriod2,
                    accountingPeriod3
                  )
                  .map(AccountingPeriod(_))
              )
            )

          when(connector.getPSRSubmission(any, any, any, any)(any))
            .thenReturn(
              Future.successful(
                PSRSubmissionResponse(
                  mockReportDetails,
                  Some(mockAccPeriodDetails),
                  None,
                  None,
                  None,
                  None,
                  None,
                  None,
                  emptyVersions
                )
              )
            )

          val request = FormBundleOrVersionTaxYearRequest(
            Some(fbNumber),
            None,
            DataRequest(allowedAccessRequest, defaultUserAnswers)
          )
          val result = service.returnAccountingPeriods(request)(
            scala.concurrent.ExecutionContext.Implicits.global,
            HeaderCarrier()
          )

          result.futureValue mustBe Some(
            NonEmptyList.of(
              accountingPeriod1,
              accountingPeriod2,
              accountingPeriod3
            )
          )
      }
    }

  }

  "returnAccountingPeriodsFromEtmp" - {

    "return empty PSRSubmissionResponse when accounting periods do not exist" in {
      val psrt = Pstr("test")
      val fbNumber = FormBundleNumber("test")
      val mockAccPeriodDetails: AccountingPeriodDetails =
        AccountingPeriodDetails(None, accountingPeriods = None)

      when(connector.getPSRSubmission(any, any, any, any)(any))
        .thenReturn(
          Future.successful(
            PSRSubmissionResponse(
              mockReportDetails,
              Some(mockAccPeriodDetails),
              None,
              None,
              None,
              None,
              None,
              None,
              emptyVersions
            )
          )
        )

      val result = service.returnBasicDetails(psrt, fbNumber).futureValue

      result.value mustBe BasicDetails(None, mockReportDetails.taxYearDateRange, YesNo.Yes, Compiled, YesNo.No)
    }

    "return empty PSRSubmissionResponse when accounting periods do not exist with version and tax year" in {
      val psrt = Pstr("test")
      val versionTaxYear = VersionTaxYear("001", "2003", DateRange(LocalDate.now(), LocalDate.now()))
      val mockAccPeriodDetails: AccountingPeriodDetails =
        AccountingPeriodDetails(None, accountingPeriods = None)

      when(connector.getPSRSubmission(any, any, any, any)(any))
        .thenReturn(
          Future.successful(
            PSRSubmissionResponse(
              mockReportDetails,
              Some(mockAccPeriodDetails),
              Some(NonEmptyList.fromListUnsafe(Gen.listOfN(5, landOrPropertyGen).sample.value)),
              None,
              None,
              None,
              None,
              None,
              emptyVersions
            )
          )
        )

      val result = service
        .returnBasicDetails(psrt, versionTaxYear)
        .futureValue

      result.value mustBe BasicDetails(
        None,
        mockReportDetails.taxYearDateRange,
        YesNo.Yes,
        EtmpPsrStatus.Compiled,
        YesNo.Yes
      )
    }

    "return None when data does not exist in ETMP with version and tax year" in {
      val psrt = Pstr("test")
      val fbNumber = FormBundleNumber("test")

      when(connector.getPSRSubmission(any, any, any, any)(any))
        .thenReturn(
          Future.failed(new NotFoundException("test"))
        )

      val result = service
        .returnBasicDetails(psrt, fbNumber)
        .futureValue

      result mustBe None
    }

    "return None when data does not exist in ETMP with Form Bundle number" in {
      val psrt = Pstr("test")
      val versionTaxYear = VersionTaxYear("001", "2003", DateRange(LocalDate.now(), LocalDate.now()))

      when(connector.getPSRSubmission(any, any, any, any)(any))
        .thenReturn(
          Future.failed(new NotFoundException("test"))
        )

      val result = service
        .returnBasicDetails(psrt, versionTaxYear)
        .futureValue

      result mustBe None
    }

    s"return period from ETMP response when a period is present" in {

      forAll(dateRangeGen) { accountingPeriod =>
        val psrt = Pstr("test")
        val fbNumber = FormBundleNumber("test")
        val mockAccPeriodDetails: AccountingPeriodDetails =
          AccountingPeriodDetails(
            None,
            accountingPeriods = Some(NonEmptyList.one(AccountingPeriod(accountingPeriod.from, accountingPeriod.to)))
          )

        when(connector.getPSRSubmission(any, any, any, any)(any))
          .thenReturn(
            Future.successful(
              PSRSubmissionResponse(
                mockReportDetails,
                Some(mockAccPeriodDetails),
                None,
                None,
                None,
                None,
                None,
                None,
                emptyVersions
              )
            )
          )

        val result = service.returnBasicDetails(psrt, fbNumber).futureValue

        result.value mustBe BasicDetails(
          Some(NonEmptyList.one(accountingPeriod)),
          mockReportDetails.taxYearDateRange,
          YesNo.Yes,
          EtmpPsrStatus.Compiled,
          YesNo.No
        )
      }
    }

  }

}
