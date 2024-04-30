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
import config.RefinedTypes.OneToThree
import eu.timepit.refined.refineMV
import models.requests.DataRequest
import models.{DateRange, NormalMode, UserAnswers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.WhichTaxYearPage
import pages.accountingperiod.AccountingPeriodPage
import play.api.test.FakeRequest
import utils.BaseSpec
import utils.UserAnswersUtils._

import java.time.LocalDate

class SchemeDateServiceSpec extends BaseSpec with ScalaCheckPropertyChecks {

  val service = new SchemeDateServiceImpl()

  val defaultUserAnswers = UserAnswers("id")
  val srn = srnGen.sample.value
  val allowedAccessRequest = allowedAccessRequestGen(FakeRequest()).sample.value

  val oldestDateRange =
    dateRangeWithinRangeGen(
      DateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2010, 1, 1))
    )

  val newestDateRange =
    dateRangeWithinRangeGen(
      DateRange(LocalDate.of(2011, 1, 1), LocalDate.of(2020, 1, 1))
    )

  "getMinimalDetails" - {

    "return None when nothing is in cache" in {
      val request = DataRequest(allowedAccessRequest, defaultUserAnswers)
      val result = service.returnAccountingPeriods(srn)(request)

      result mustBe None
    }

    s"return period from AccountingPeriodPage answer when 1 period present" in {

      forAll(dateRangeGen, dateRangeGen) { (whichTaxYearPage, accountingPeriod) =>
        val userAnswers = defaultUserAnswers
          .unsafeSet(WhichTaxYearPage(srn), whichTaxYearPage)
          .unsafeSet(AccountingPeriodPage(srn, refineMV(1), NormalMode), accountingPeriod)

        val request = DataRequest(allowedAccessRequest, userAnswers)
        val result = service.returnAccountingPeriods(srn)(request)

        result mustBe Some(NonEmptyList.one((accountingPeriod, refineMV[OneToThree](1))))
      }
    }

    s"choose periods from AccountingPeriodPage answer when multiple exist" in {

      forAll(dateRangeGen, oldestDateRange, newestDateRange) {

        (accountingPeriod1, accountingPeriod2, accountingPeriod3) =>
          val userAnswers = defaultUserAnswers
            .unsafeSet(AccountingPeriodPage(srn, refineMV(1), NormalMode), accountingPeriod1)
            .unsafeSet(AccountingPeriodPage(srn, refineMV(2), NormalMode), accountingPeriod2)
            .unsafeSet(AccountingPeriodPage(srn, refineMV(3), NormalMode), accountingPeriod3)

          val request = DataRequest(allowedAccessRequest, userAnswers)
          val result = service.returnAccountingPeriods(srn)(request)

          result mustBe Some(
            NonEmptyList.of(
              (accountingPeriod1, refineMV[OneToThree](1)),
              (accountingPeriod2, refineMV[OneToThree](1)),
              (accountingPeriod3, refineMV[OneToThree](1))
            )
          )
      }
    }

  }

}
