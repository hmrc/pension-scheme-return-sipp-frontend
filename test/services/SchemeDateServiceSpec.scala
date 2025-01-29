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
import config.RefinedTypes.Max3
import connectors.PSRConnector
import models.SchemeId.Pstr
import models.backend.responses.{AccountingPeriod, AccountingPeriodDetails, PSRSubmissionResponse, Versions}
import models.requests.common.YesNo
import models.requests.psr.EtmpPsrStatus.Compiled
import models.requests.psr.{EtmpPsrStatus, ReportDetails}
import models.requests.{AllowedAccessRequest, DataRequest}
import models.{BasicDetails, DateRange, FormBundleNumber, NormalMode, SchemeId, UserAnswers, VersionTaxYear}
import org.scalacheck.Gen
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.WhichTaxYearPage
import pages.accountingperiod.AccountingPeriodPage
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import utils.BaseSpec
import utils.UserAnswersUtils.*

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
      val request = DataRequest(allowedAccessRequest, defaultUserAnswers)
      val result = service.returnAccountingPeriods(srn)(request)

      result mustBe None
    }

    s"return period from AccountingPeriodPage answer when 1 period present" in {

      forAll(dateRangeGen, dateRangeGen) { (whichTaxYearPage, accountingPeriod) =>
        val userAnswers = defaultUserAnswers
          .unsafeSet(WhichTaxYearPage(srn), whichTaxYearPage)
          .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), accountingPeriod)

        val request = DataRequest(allowedAccessRequest, userAnswers)
        val result = service.returnAccountingPeriods(srn)(request)

        result mustBe Some(NonEmptyList.one((accountingPeriod, Max3.ONE)))
      }
    }

    s"choose periods from AccountingPeriodPage answer when multiple exist" in {

      forAll(dateRangeGen, oldestDateRange, newestDateRange) {

        (accountingPeriod1, accountingPeriod2, accountingPeriod3) =>
          val userAnswers = defaultUserAnswers
            .unsafeSet(AccountingPeriodPage(srn, Max3.ONE, NormalMode), accountingPeriod1)
            .unsafeSet(AccountingPeriodPage(srn, Max3.TWO, NormalMode), accountingPeriod2)
            .unsafeSet(AccountingPeriodPage(srn, Max3.THREE, NormalMode), accountingPeriod3)

          val request = DataRequest(allowedAccessRequest, userAnswers)
          val result = service.returnAccountingPeriods(srn)(request)

          result mustBe Some(
            NonEmptyList.of(
              (accountingPeriod1, Max3.ONE),
              (accountingPeriod2, Max3.ONE),
              (accountingPeriod3, Max3.ONE)
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
            accountingPeriods = Some(List(AccountingPeriod(accountingPeriod.from, accountingPeriod.to)))
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
