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

package services.validation.csv

import cats.data.NonEmptyList
import cats.syntax.option.*
import forms.*
import models.csv.CsvRowState.CsvRowValid
import models.keys.UnquotedSharesKeys
import models.requests.common.UnquotedShareDisposalDetail
import models.requests.common.SharesCompanyDetails
import models.requests.common.YesNo.Yes
import models.requests.UnquotedShareApi
import models.{Crn, CsvHeaderKey, NameDOB, NinoType}
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import services.validation.UnquotedSharesValidationsService
import utils.BaseSpec

import java.time.LocalDate

class UnquotedSharesCsvRowValidatorSpec extends BaseSpec {
  val nameDobProvider = NameDOBFormProvider()
  private val textFormProvider = TextFormProvider()
  private val datePageFormProvider = DatePageFormProvider()
  private val moneyFormProvider = MoneyFormProvider()
  private val intFormProvider = IntFormProvider()
  private val doubleFormProvider = DoubleFormProvider()

  implicit val messages: Messages = stubMessages()
  val service = UnquotedSharesValidationsService(
    nameDobProvider,
    textFormProvider,
    datePageFormProvider,
    moneyFormProvider,
    intFormProvider,
    doubleFormProvider
  )
  val validator = UnquotedSharesCsvRowValidator(service)

  val headers = UnquotedSharesKeys.headers.zipWithIndex.map { case (key, i) => CsvHeaderKey(key, key, i) }.toList
  val validData = NonEmptyList.of(
    "name", // firstName
    "lastName", // lastName
    dateToString(LocalDate.now().minusYears(20)), // memberDateOfBirth
    "AB123456C", // memberNino
    "", // memberReasonNoNino
    "company", // companyName
    "12345678", // companyCRN
    "", // companyNoCRNReason
    "class", // companyClassOfShares
    "30", // companyNumberOfShares
    "acquiredFrom", // acquiredFrom
    "1002", // totalCost
    "Yes", // transactionIndependentValuation
    "10", // totalDividends
    "Yes", // disposalMade
    "1010", // totalSaleValue
    "purchaser", // disposalPurchaserName
    "Yes", // disposalConnectedParty
    "Yes", // disposalIndependentValuation
    "10", // noOfSharesSold
    "10" // noOfSharesHeld
  )
  val validationParams = CsvRowValidationParameters(Some(LocalDate.now().plusDays(15)))

  "UnquotedSharesCsvRowValidator" - {
    "validate correct row" in {
      val actual = validator.validate(1, validData, headers, validationParams)
      val expected = CsvRowValid(
        1,
        UnquotedShareApi.TransactionDetail(
          row = 1.some,
          nameDOB = NameDOB("name", "lastName", LocalDate.now().minusYears(20)),
          nino = NinoType("AB123456C".some, None),
          sharesCompanyDetails = SharesCompanyDetails("company", Crn("12345678").some, None, "class", 30),
          acquiredFromName = "acquiredFrom",
          totalCost = 1002,
          independentValuation = Yes,
          totalDividendsIncome = 10,
          sharesDisposed = Yes,
          sharesDisposalDetails = UnquotedShareDisposalDetail(1010, "purchaser", Yes, Yes, 10, 10).some
        ),
        validData
      )
      actual mustBe expected
    }
  }

}
