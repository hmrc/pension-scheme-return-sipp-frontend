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
import cats.implicits._
import models._
import models.csv.CsvRowState
import models.csv.CsvRowState._
import models.requests.raw.OutstandingLoansRaw.RawTransactionDetail
import models.requests.{OutstandingLoanRequest, YesNo}
import play.api.i18n.Messages
import services.validation.{OutstandingLoansValidationsService, Validator}

import javax.inject.Inject

class OutstandingLoansCsvRowValidator @Inject()(
  validations: OutstandingLoansValidationsService
) extends CsvRowValidator[OutstandingLoanRequest.TransactionDetail]
    with Validator {

  override def validate(
    line: Int,
    data: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(
    implicit
    messages: Messages
  ): CsvRowState[OutstandingLoanRequest.TransactionDetail] = {
    val validDateThreshold = csvRowValidationParameters.schemeWindUpDate

    (for {
      raw <- readCSV(line, headers, data.toList)
      memberFullNameDob = s"${raw.firstNameOfSchemeMember.value} ${raw.lastNameOfSchemeMember.value} ${raw.memberDateOfBirth.value}"

      // Validations
      /* B ... D */
      validatedNameDOB <- validations.validateNameDOB(
        firstName = raw.firstNameOfSchemeMember,
        lastName = raw.lastNameOfSchemeMember,
        dob = raw.memberDateOfBirth,
        row = line,
        validDateThreshold = validDateThreshold
      )

      /*  E */
      validatedTransactionCount <- validations.validateCount(
        raw.countOfTransactions,
        key = "outstandingLoans.transactionCount",
        memberFullName = memberFullNameDob,
        row = line,
        maxCount = 50
      )

      /*  F */
      validatedLoanRecipientName <- validations.validateFreeText(
        raw.rawAsset.loanRecipientName,
        "outstandingLoans.loanRecipientName",
        memberFullNameDob,
        line
      )

      /* G ... K */
      validatedAcquiredFrom <- validations.validateAcquiredFrom(
        acquiredFromType = raw.rawAsset.acquiredFrom.acquiredFromType,
        acquirerNinoForIndividual = raw.rawAsset.acquiredFrom.acquirerNinoForIndividual,
        acquirerCrnForCompany = raw.rawAsset.acquiredFrom.acquirerCrnForCompany,
        acquirerUtrForPartnership = raw.rawAsset.acquiredFrom.acquirerUtrForPartnership,
        whoAcquiredFromTypeReasonAsset = raw.rawAsset.acquiredFrom.whoAcquiredFromTypeReasonAsset,
        memberFullNameDob = memberFullNameDob,
        row = line
      )

      /*  L */
      validatedDateOfLoan <- validations.validateDate(
        date = raw.rawAsset.dateOfLoan,
        key = "outstandingLoans.dateOfLoan",
        row = line
      )

      /*  M */
      validatedAmountOfLoan <- validations.validatePrice(
        price = raw.rawAsset.amountOfLoan,
        key = "outstandingLoans.amountOfLoan",
        memberFullName = memberFullNameDob,
        row = line
      )

      /*  N */
      validatedLoanConnectedParty <- validations.validateYesNoQuestion(
        raw.rawAsset.loanConnectedParty,
        "outstandingLoans.loanConnectedParty",
        memberFullNameDob,
        line
      )

      /*  O */
      validatedRepaymentDate <- validations.validateDate(
        raw.rawAsset.repayDate,
        "outstandingLoans.repayDate",
        line
      )

      /*  P */
      validatedInterestRate <- validations.validatePercentage(
        raw.rawAsset.interestRate,
        "outstandingLoans.interestRate",
        memberFullNameDob,
        line
      )

      /*  Q */
      validatedLoanSecurity <- validations.validateYesNoQuestion(
        raw.rawAsset.loanSecurity,
        "outstandingLoans.loanSecurity",
        memberFullNameDob,
        line
      )

      /*  R */
      validatedCapitalPayment <- validations.validatePrice(
        price = raw.rawAsset.capitalRepayments,
        key = "outstandingLoans.capitalPayment",
        memberFullName = memberFullNameDob,
        row = line
      )

      /*  S */
      validatedInterestPayments <- validations.validatePrice(
        price = raw.rawAsset.interestPayments,
        key = "outstandingLoans.interestPayments",
        memberFullName = memberFullNameDob,
        row = line
      )

      /*  T */
      validatedAnyArrears <- validations.validateYesNoQuestion(
        raw.rawAsset.arrearsOutstandingPrYears,
        "outstandingLoans.arrearsOutstandingPrYears",
        memberFullNameDob,
        line
      )

      /*  U */
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
        validatedTransactionCount,
        validatedLoanRecipientName,
        validatedAcquiredFrom,
        validatedDateOfLoan,
        validatedAmountOfLoan,
        validatedLoanConnectedParty,
        validatedRepaymentDate,
        validatedInterestRate,
        validatedLoanSecurity,
        validatedCapitalPayment,
        validatedInterestPayments,
        validatedAnyArrears,
        validatedOutstandingYearEndAmount
      ).mapN {
        (
          nameDob,
          transactionCount, // TODO Fix that later!
          loanRecipientName,
          acquiredFrom,
          dateOfLoan,
          amountOfLoan,
          loanConnectedParty,
          repaymentDate,
          interestRate,
          loanSecurity,
          capitalPayment,
          interestPayments,
          anyArrears,
          outstandingYearEndAmount
        ) =>
          OutstandingLoanRequest.TransactionDetail(
            row = line,
            nameDOB = nameDob,
            loanRecipientName = loanRecipientName,
            indivOrOrgIdentityDetails = acquiredFrom,
            dateOfLoan = dateOfLoan,
            amountOfLoan = amountOfLoan.value,
            loanConnectedParty = YesNo.uploadYesNoToRequestYesNo(loanConnectedParty),
            repayDate = repaymentDate,
            interestRate = interestRate,
            loanSecurity = YesNo.uploadYesNoToRequestYesNo(loanSecurity),
            capitalRepayments = capitalPayment.value,
            interestPayments = interestPayments.value,
            arrearsOutstandingPrYears = YesNo.uploadYesNoToRequestYesNo(anyArrears),
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
      firstNameOfSchemeMember <- getCSVValue(UploadKeys.firstNameOfSchemeMemberOutstanding, headerKeys, csvData)
      /*  C */
      lastNameOfSchemeMember <- getCSVValue(UploadKeys.lastNameOfSchemeMemberOutstanding, headerKeys, csvData)
      /*  D */
      memberDateOfBirth <- getCSVValue(UploadKeys.memberDateOfBirthOutstanding, headerKeys, csvData)
      /*  E */
      countOfTransactions <- getCSVValue(UploadKeys.countOfOutstandingLoansPropertyTransactions, headerKeys, csvData)
      /*  F */
      recipientName <- getCSVValue(UploadKeys.recipientNameOfOutstanding, headerKeys, csvData)
      /*  G */
      acquiredFromType <- getCSVValue(UploadKeys.acquiredFromTypeOutstanding, headerKeys, csvData)
      /*  H */
      acquiredFromIndividual <- getOptionalCSVValue(UploadKeys.acquiredFromIndividualOutstanding, headerKeys, csvData)
      /*  I */
      acquiredFromCompany <- getOptionalCSVValue(UploadKeys.acquiredFromCompanyOutstanding, headerKeys, csvData)
      /*  J */
      acquiredFromPartnership <- getOptionalCSVValue(UploadKeys.acquiredFromPartnershipOutstanding, headerKeys, csvData)
      /*  K */
      acquiredFromReason <- getOptionalCSVValue(UploadKeys.acquiredFromReasonOutstanding, headerKeys, csvData)
      /*  L */
      dateOfLoan <- getCSVValue(UploadKeys.dateOfOutstandingLoan, headerKeys, csvData)
      /*  M */
      amountOfLoan <- getCSVValue(UploadKeys.amountOfOutstandingLoan, headerKeys, csvData)
      /*  N */
      isAssociated <- getCSVValue(UploadKeys.isOutstandingLoanAssociatedWithConnectedParty, headerKeys, csvData)
      /*  O */
      repaymentDate <- getCSVValue(UploadKeys.repaymentDateOfOutstandingLoan, headerKeys, csvData)
      /*  P */
      interestRate <- getCSVValue(UploadKeys.interestRateOfOutstandingLoan, headerKeys, csvData)
      /*  Q */
      hasSecurity <- getCSVValue(UploadKeys.hasSecurityForOutstandingLoan, headerKeys, csvData)
      /*  R */
      capitalPayment <- getCSVValue(UploadKeys.capitalPaymentOfOutstandingLoanForYear, headerKeys, csvData)
      /*  S */
      interestRateForYear <- getCSVValue(UploadKeys.interestRateOfOutstandingLoanForYear, headerKeys, csvData)
      /*  T */
      anyArrears <- getCSVValue(UploadKeys.anyArrearsForOutstandingLoan, headerKeys, csvData)
      /*  U */
      outstandingAmount <- getCSVValue(UploadKeys.outstandingAmountForOutstandingLoan, headerKeys, csvData)
    } yield RawTransactionDetail.create(
      row,
      firstNameOfSchemeMember,
      lastNameOfSchemeMember,
      memberDateOfBirth,
      countOfTransactions,
      recipientName,
      acquiredFromType,
      acquiredFromIndividual,
      acquiredFromCompany,
      acquiredFromPartnership,
      acquiredFromReason,
      dateOfLoan,
      amountOfLoan,
      isAssociated,
      repaymentDate,
      interestRate,
      hasSecurity,
      capitalPayment,
      interestRateForYear,
      anyArrears,
      outstandingAmount
    )

}
