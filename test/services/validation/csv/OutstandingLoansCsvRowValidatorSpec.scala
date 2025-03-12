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
import forms.{
  DatePageFormProvider,
  DoubleFormProvider,
  IntFormProvider,
  MoneyFormProvider,
  NameDOBFormProvider,
  TextFormProvider
}
import models.{CsvHeaderKey, NameDOB, NinoType}
import models.csv.CsvRowState.CsvRowValid
import models.keys.OutstandingLoansKeys
import models.requests.OutstandingLoanApi
import utils.BaseSpec
import services.validation.ValidationsService

import java.time.LocalDate
import play.api.test.Helpers.stubMessages
import play.api.i18n.Messages
import cats.syntax.option.*
import models.requests.common.YesNo.Yes

class OutstandingLoansCsvRowValidatorSpec extends BaseSpec {
  val nameDobProvider = NameDOBFormProvider()
  private val textFormProvider = TextFormProvider()
  private val datePageFormProvider = DatePageFormProvider()
  private val moneyFormProvider = MoneyFormProvider()
  private val intFormProvider = IntFormProvider()
  private val doubleFormProvider = DoubleFormProvider()

  implicit val messages: Messages = stubMessages()
  val service = ValidationsService(
    nameDobProvider,
    textFormProvider,
    datePageFormProvider,
    moneyFormProvider,
    intFormProvider,
    doubleFormProvider
  )
  val validator = OutstandingLoansCsvRowValidator(service)

  val headers = OutstandingLoansKeys.headers.zipWithIndex.map { case (key, i) => CsvHeaderKey(key, key, i) }.toList
  val validData = NonEmptyList.of(
    "name", // firstName
    "lastName", // lastName
    dateToString(LocalDate.now().minusYears(20)), // memberDateOfBirth
    "AB123456C", // memberNino
    "", // memberReasonNoNino
    "10", // countOfPropertyTransactions
    "recipient", // recipientNameOfLoan
    dateToString(LocalDate.now()), // dateOfOutstandingLoan
    "1000", // amountOfLoan
    "Yes", // isLoanAssociatedWithConnectedParty
    dateToString(LocalDate.now().plusMonths(1)), // repaymentDate
    "5", // interestRate
    "Yes", // hasSecurity
    "50", // capitalAndInterestPaymentForYear
    "Yes", // anyArrears
    "100", // arrearsOutstandingPrYearsAmt
    "1000" // outstandingAmount
  )
  val validationParams = CsvRowValidationParameters(Some(LocalDate.now().plusDays(15)))

  "OutstandingLoansCsvRowValidator" - {
    "validate correct row" in {
      val actual = validator.validate(1, validData, headers, validationParams)
      val expected = CsvRowValid(
        1,
        OutstandingLoanApi.TransactionDetail(
          row = 1.some,
          nameDOB = NameDOB("name", "lastName", LocalDate.now().minusYears(20)),
          nino = NinoType("AB123456C".some, None),
          loanRecipientName = "recipient",
          dateOfLoan = LocalDate.now(),
          amountOfLoan = 1000,
          loanConnectedParty = Yes,
          repayDate = LocalDate.now().plusMonths(1),
          interestRate = 5,
          loanSecurity = Yes,
          capitalRepayments = 50,
          arrearsOutstandingPrYears = Yes,
          outstandingYearEndAmount = 1000,
          arrearsOutstandingPrYearsAmt = 100.0.some,
          transactionCount = None
        ),
        validData
      )
      actual mustBe expected
    }
  }

}
