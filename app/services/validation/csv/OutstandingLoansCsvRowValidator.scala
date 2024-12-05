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

package services.validation.csv

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.*
import models.*
import models.csv.CsvRowState
import models.csv.CsvRowState.*
import models.requests.OutstandingLoanApi
import models.requests.common.YesNo
import models.requests.raw.OutstandingLoansRaw.RawTransactionDetail
import play.api.i18n.Messages
import services.validation.{ValidationsService, Validator}
import models.keys.{OutstandingLoansKeys => Keys}

import javax.inject.Inject

class OutstandingLoansCsvRowValidator @Inject() (
  validations: ValidationsService
) extends CsvRowValidator[OutstandingLoanApi.TransactionDetail]
    with Validator {

  override def validate(
    line: Int,
    data: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(implicit
    messages: Messages
  ): CsvRowState[OutstandingLoanApi.TransactionDetail] = {
    val validDateThreshold = csvRowValidationParameters.schemeWindUpDate

    (for {
      raw <- readCSV(line, headers, data.toList)
      memberFullNameDob =
        s"${raw.firstNameOfSchemeMember.value} ${raw.lastNameOfSchemeMember.value} ${raw.memberDateOfBirth.value}"

      // Validations
      /* B - D */
      validatedNameDOB <- validations.validateNameDOB(
        firstName = raw.firstNameOfSchemeMember,
        lastName = raw.lastNameOfSchemeMember,
        dob = raw.memberDateOfBirth,
        row = line,
        validDateThreshold = validDateThreshold
      )

      /* E - F */
      validatedNino <- validations.validateNinoWithNoReason(
        nino = raw.memberNino,
        noNinoReason = raw.memberReasonNoNino,
        memberFullName = memberFullNameDob,
        row = line
      )

      /*  G */
      validatedTransactionCount <- validations.validateCount(
        raw.countOfTransactions,
        key = "outstandingLoans.transactionCount",
        memberFullName = memberFullNameDob,
        row = line,
        maxCount = 50
      )

      /*  H */
      validatedLoanRecipientName <- validations.validateFreeText(
        raw.rawAsset.loanRecipientName,
        "outstandingLoans.loanRecipientName",
        memberFullNameDob,
        line
      )

      /*  I */
      validatedDateOfLoan <- validations.validateDate(
        date = raw.rawAsset.dateOfLoan,
        key = "outstandingLoans.dateOfLoan",
        row = line
      )

      /*  J */
      validatedAmountOfLoan <- validations.validatePrice(
        price = raw.rawAsset.amountOfLoan,
        key = "outstandingLoans.amountOfLoan",
        memberFullName = memberFullNameDob,
        row = line
      )

      /*  K */
      validatedLoanConnectedParty <- validations.validateYesNoQuestion(
        raw.rawAsset.loanConnectedParty,
        "outstandingLoans.loanConnectedParty",
        memberFullNameDob,
        line
      )

      /*  L */
      validatedRepaymentDate <- validations.validateDate(
        raw.rawAsset.repayDate,
        "outstandingLoans.repayDate",
        line
      )

      /*  M */
      validatedInterestRate <- validations.validatePercentage(
        raw.rawAsset.interestRate,
        "outstandingLoans.interestRate",
        memberFullNameDob,
        line
      )

      /*  N */
      validatedLoanSecurity <- validations.validateYesNoQuestion(
        raw.rawAsset.loanSecurity,
        "outstandingLoans.loanSecurity",
        memberFullNameDob,
        line
      )

      /*  O */
      validatedCapitalPayment <- validations.validatePrice(
        price = raw.rawAsset.capitalRepayments,
        key = "outstandingLoans.capitalPayment",
        memberFullName = memberFullNameDob,
        row = line
      )

      /*  P */
      validatedAnyArrears <- validations.validateYesNoQuestion(
        raw.rawAsset.arrearsOutstandingPrYears,
        "outstandingLoans.arrearsOutstandingPrYears",
        memberFullNameDob,
        line
      )

      /*  Q */
      validatedOutstandingYearEndAmount <- validations.validatePrice(
        raw.rawAsset.outstandingYearEndAmount,
        "outstandingLoans.outstandingYearEndAmount",
        memberFullNameDob,
        line
      )

      /*  R */
      validatedArrearsOutstandingPrYearsAmt <- raw.rawAsset.arrearsOutstandingPrYearsAmt.value match {
        case None => none.validNel.some
        case Some(arrearsOutstandingPrYearsAmt) =>
          validations
            .validatePrice(
              price = raw.rawAsset.arrearsOutstandingPrYearsAmt.as(arrearsOutstandingPrYearsAmt),
              key = "outstandingLoans.arrearsOutstandingPrYearsAmt",
              memberFullName = memberFullNameDob,
              row = line
            )
            .map(_.map(_.some))
      }
    } yield (
      raw,
      (
        validatedNameDOB,
        validatedNino,
        validatedTransactionCount,
        validatedLoanRecipientName,
        validatedDateOfLoan,
        validatedAmountOfLoan,
        validatedLoanConnectedParty,
        validatedRepaymentDate,
        validatedInterestRate,
        validatedLoanSecurity,
        validatedCapitalPayment,
        validatedAnyArrears,
        validatedOutstandingYearEndAmount,
        validatedArrearsOutstandingPrYearsAmt
      ).mapN {
        (
          nameDob,
          nino,
          _, // Backend is deciding, future plan is removing that from csv
          loanRecipientName,
          dateOfLoan,
          amountOfLoan,
          loanConnectedParty,
          repaymentDate,
          interestRate,
          loanSecurity,
          capitalPayment,
          anyArrears,
          outstandingYearEndAmount,
          arrearsOutstandingPrYearsAmt
        ) =>
          OutstandingLoanApi.TransactionDetail(
            row = Some(line),
            nameDOB = nameDob,
            nino = nino,
            loanRecipientName = loanRecipientName,
            dateOfLoan = dateOfLoan,
            amountOfLoan = amountOfLoan.value,
            loanConnectedParty = YesNo.withNameInsensitive(loanConnectedParty),
            repayDate = repaymentDate,
            interestRate = interestRate,
            loanSecurity = YesNo.withNameInsensitive(loanSecurity),
            capitalRepayments = capitalPayment.value,
            arrearsOutstandingPrYears = YesNo.withNameInsensitive(anyArrears),
            outstandingYearEndAmount = outstandingYearEndAmount.value,
            arrearsOutstandingPrYearsAmt = arrearsOutstandingPrYearsAmt.map(_.value)
          )
      }
    )) match {
      case None =>
        invalidFileFormat(line, data)
      case Some((raw, Valid(outstandingLoanRequest))) =>
        CsvRowValid(line, outstandingLoanRequest, raw.toNonEmptyList)
      case Some((raw, Invalid(errors))) =>
        CsvRowInvalid(line, errors, raw.toNonEmptyList)
    }
  }

  private def readCSV(
    row: Int,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[RawTransactionDetail] =
    for {
      /*  B */
      firstNameOfSchemeMember <- getCSVValue(Keys.firstName, headerKeys, csvData)
      /*  C */
      lastNameOfSchemeMember <- getCSVValue(Keys.lastName, headerKeys, csvData)
      /*  D */
      memberDateOfBirth <- getCSVValue(Keys.memberDateOfBirth, headerKeys, csvData)
      /*  E */
      memberNino <- getOptionalCSVValue(Keys.memberNino, headerKeys, csvData)
      /*  F */
      memberReasonNoNino <- getOptionalCSVValue(Keys.memberReasonNoNino, headerKeys, csvData)
      /*  G */
      countOfTransactions <- getCSVValue(Keys.countOfPropertyTransactions, headerKeys, csvData)
      /*  H */
      recipientName <- getCSVValue(Keys.recipientNameOfLoan, headerKeys, csvData)
      /*  I */
      dateOfLoan <- getCSVValue(Keys.dateOfOutstandingLoan, headerKeys, csvData)
      /*  J */
      amountOfLoan <- getCSVValue(Keys.amountOfLoan, headerKeys, csvData)
      /*  K */
      isAssociated <- getCSVValue(Keys.isLoanAssociatedWithConnectedParty, headerKeys, csvData)
      /*  L */
      repaymentDate <- getCSVValue(Keys.repaymentDate, headerKeys, csvData)
      /*  M */
      interestRate <- getCSVValue(Keys.interestRate, headerKeys, csvData)
      /*  N */
      hasSecurity <- getCSVValue(Keys.hasSecurity, headerKeys, csvData)
      /*  O */
      capitalPayment <- getCSVValue(Keys.capitalAndInterestPaymentForYear, headerKeys, csvData)
      /*  P */
      anyArrears <- getCSVValue(Keys.anyArrears, headerKeys, csvData)
      /*  Q */
      arrearsOutstandingPrYearsAmt <- getOptionalCSVValue(Keys.arrearsOutstandingPrYearsAmt, headerKeys, csvData)
      /*  R */
      outstandingAmount <- getCSVValue(Keys.outstandingAmount, headerKeys, csvData)
    } yield RawTransactionDetail.create(
      row,
      firstNameOfSchemeMember,
      lastNameOfSchemeMember,
      memberDateOfBirth,
      memberNino,
      memberReasonNoNino,
      countOfTransactions,
      recipientName,
      dateOfLoan,
      amountOfLoan,
      isAssociated,
      repaymentDate,
      interestRate,
      hasSecurity,
      capitalPayment,
      anyArrears,
      outstandingAmount,
      arrearsOutstandingPrYearsAmt
    )

}
