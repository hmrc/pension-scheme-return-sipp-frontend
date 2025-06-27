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
      validatedLoanRecipientName <- validations.validateFreeText(
        raw.rawAsset.loanRecipientName,
        "outstandingLoans.loanRecipientName",
        memberFullNameDob,
        line
      )

      /*  H */
      validatedDateOfLoan <- validations.validateDate(
        date = raw.rawAsset.dateOfLoan,
        key = "outstandingLoans.dateOfLoan",
        row = line
      )

      /*  I */
      validatedAmountOfLoan <- validations.validatePrice(
        price = raw.rawAsset.amountOfLoan,
        key = "outstandingLoans.amountOfLoan",
        memberFullName = memberFullNameDob,
        row = line
      )

      /*  J */
      validatedLoanConnectedParty <- validations.validateYesNoQuestion(
        raw.rawAsset.loanConnectedParty,
        "outstandingLoans.loanConnectedParty",
        memberFullNameDob,
        line
      )

      /*  K */
      validatedRepaymentDate <- validations.validateDate(
        raw.rawAsset.repayDate,
        "outstandingLoans.repayDate",
        line
      )

      /*  L */
      validatedInterestRate <- validations.validatePercentage(
        raw.rawAsset.interestRate,
        "outstandingLoans.interestRate",
        memberFullNameDob,
        line
      )

      /*  M */
      validatedLoanSecurity <- validations.validateYesNoQuestion(
        raw.rawAsset.loanSecurity,
        "outstandingLoans.loanSecurity",
        memberFullNameDob,
        line
      )

      /*  N */
      validatedCapitalPayment <- validations.validatePrice(
        price = raw.rawAsset.capitalRepayments,
        key = "outstandingLoans.capitalPayment",
        memberFullName = memberFullNameDob,
        row = line
      )

      /*  O */
      validatedAnyArrears <- validations.validateYesNoQuestion(
        raw.rawAsset.arrearsOutstandingPrYears,
        "outstandingLoans.arrearsOutstandingPrYears",
        memberFullNameDob,
        line
      )

      /*  P */
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

      /*  Q */
      validatedOutstandingYearEndAmount <- validations.validatePrice(
        raw.rawAsset.outstandingYearEndAmount,
        "outstandingLoans.outstandingYearEndAmount",
        memberFullNameDob,
        line
      )
    } yield (
      raw,
      (
        validatedNameDOB,
        validatedNino,
        validatedLoanRecipientName,
        validatedDateOfLoan,
        validatedAmountOfLoan,
        validatedLoanConnectedParty,
        validatedRepaymentDate,
        validatedInterestRate,
        validatedLoanSecurity,
        validatedCapitalPayment,
        validatedAnyArrears,
        validatedArrearsOutstandingPrYearsAmt,
        validatedOutstandingYearEndAmount
      ).mapN {
        (
          nameDob,
          nino,
          loanRecipientName,
          dateOfLoan,
          amountOfLoan,
          loanConnectedParty,
          repaymentDate,
          interestRate,
          loanSecurity,
          capitalPayment,
          anyArrears,
          arrearsOutstandingPrYearsAmt,
          outstandingYearEndAmount
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
            arrearsOutstandingPrYearsAmt = arrearsOutstandingPrYearsAmt.map(_.value),
            outstandingYearEndAmount = outstandingYearEndAmount.value
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
      recipientName <- getCSVValue(Keys.recipientNameOfLoan, headerKeys, csvData)
      /*  H */
      dateOfLoan <- getCSVValue(Keys.dateOfOutstandingLoan, headerKeys, csvData)
      /*  I */
      amountOfLoan <- getCSVValue(Keys.amountOfLoan, headerKeys, csvData)
      /*  J */
      isAssociated <- getCSVValue(Keys.isLoanAssociatedWithConnectedParty, headerKeys, csvData)
      /*  K */
      repaymentDate <- getCSVValue(Keys.repaymentDate, headerKeys, csvData)
      /*  L */
      interestRate <- getCSVValue(Keys.interestRate, headerKeys, csvData)
      /*  M */
      hasSecurity <- getCSVValue(Keys.hasSecurity, headerKeys, csvData)
      /*  N */
      capitalPayment <- getCSVValue(Keys.capitalAndInterestPaymentForYear, headerKeys, csvData)
      /*  O */
      anyArrears <- getCSVValue(Keys.anyArrears, headerKeys, csvData)
      /*  P */
      arrearsOutstandingPrYearsAmt <- getOptionalCSVValue(Keys.arrearsOutstandingPrYearsAmt, headerKeys, csvData)
      /*  Q */
      outstandingAmount <- getCSVValue(Keys.outstandingAmount, headerKeys, csvData)
    } yield RawTransactionDetail.create(
      row,
      firstNameOfSchemeMember,
      lastNameOfSchemeMember,
      memberDateOfBirth,
      memberNino,
      memberReasonNoNino,
      recipientName,
      dateOfLoan,
      amountOfLoan,
      isAssociated,
      repaymentDate,
      interestRate,
      hasSecurity,
      capitalPayment,
      anyArrears,
      arrearsOutstandingPrYearsAmt,
      outstandingAmount
    )

}
