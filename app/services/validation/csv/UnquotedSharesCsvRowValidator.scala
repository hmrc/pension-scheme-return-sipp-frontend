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
import models.ValidationErrorType.InvalidRowFormat
import models.*
import models.csv.CsvRowState
import models.csv.CsvRowState.*
import models.requests.UnquotedShareApi
import models.requests.raw.UnquotedShareRaw.RawTransactionDetail
import play.api.i18n.Messages
import services.validation.{UnquotedSharesValidationsService, Validator}
import models.keys.{UnquotedSharesKeys => Keys}

import javax.inject.Inject

class UnquotedSharesCsvRowValidator @Inject() (
  validations: UnquotedSharesValidationsService
) extends CsvRowValidator[UnquotedShareApi.TransactionDetail]
    with Validator {

  override def validate(
    line: Int,
    data: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(implicit
    messages: Messages
  ): CsvRowState[UnquotedShareApi.TransactionDetail] = {
    val validDateThreshold = csvRowValidationParameters.schemeWindUpDate

    (for {
      raw <- readCSV(line, headers, data.toList)

      memberFullNameDob =
        s"${raw.firstNameOfSchemeMember.value} ${raw.lastNameOfSchemeMember.value} ${raw.memberDateOfBirth.value}"

      validatedNameDOB <- validations.validateNameDOB(
        firstName = raw.firstNameOfSchemeMember,
        lastName = raw.lastNameOfSchemeMember,
        dob = raw.memberDateOfBirth,
        row = line,
        validDateThreshold = validDateThreshold
      )

      validatedNino <- validations.validateNinoWithNoReason(
        nino = raw.memberNino,
        noNinoReason = raw.memberNoNinoReason,
        memberFullName = memberFullNameDob,
        row = line
      )

      validatedTransactionCount <- validations.validateCount(
        raw.countOfTransactions,
        key = "unquotedShares.transactionCount",
        memberFullName = memberFullNameDob,
        row = line,
        minCount = 0,
        maxCount = 50
      )

      validatedShareCompanyDetails <- validations.validateShareCompanyDetails(
        companySharesName = raw.shareCompanyDetails.companySharesName,
        companySharesCRN = raw.shareCompanyDetails.companySharesCRN,
        reasonNoCRN = raw.shareCompanyDetails.reasonNoCRN,
        sharesClass = raw.shareCompanyDetails.sharesClass,
        noOfSharesHeld = raw.shareCompanyDetails.noOfShares,
        memberFullNameDob = memberFullNameDob,
        line
      )

      validatedWhoAcquiredFromName <- validations.validateFreeText(
        raw.acquiredFromName,
        "unquotedShares.whoAcquiredFromName",
        memberFullNameDob,
        line
      )

      validatedTransactionDetail <- validations.validateShareTransaction(
        raw.rawSharesTransactionDetail.totalCost,
        raw.rawSharesTransactionDetail.independentValuation,
        raw.rawSharesTransactionDetail.totalDividendsIncome,
        memberFullNameDob,
        line
      )

      validatedDisposal <- validations.validateDisposals(
        raw.rawSharesTransactionDetail.sharesDisposed,
        raw.rawDisposal.disposedSharesAmt,
        raw.rawDisposal.purchaserName,
        raw.rawDisposal.disposalConnectedParty,
        raw.rawDisposal.independentValuation,
        raw.rawDisposal.noOfSharesSold,
        raw.rawDisposal.noOfSharesHeld,
        memberFullNameDob,
        line
      )

    } yield (
      raw,
      (
        validatedNameDOB,
        validatedNino,
        validatedTransactionCount,
        validatedShareCompanyDetails,
        validatedWhoAcquiredFromName,
        validatedTransactionDetail,
        validatedDisposal
      ).mapN(
        (
          validatedNameDOB,
          validatedNinoOrNoNinoReason,
          _, // Backend is deciding, future plan is removing that from csv
          validatedShareCompanyDetails,
          validatedWhoAcquiredFromName,
          validatedTransactionDetail,
          validatedDisposal
        ) =>
          UnquotedShareApi.TransactionDetail(
            row = Some(line),
            nameDOB = validatedNameDOB,
            nino = validatedNinoOrNoNinoReason,
            sharesCompanyDetails = validatedShareCompanyDetails,
            acquiredFromName = validatedWhoAcquiredFromName,
            totalCost = validatedTransactionDetail.totalCost,
            independentValuation = validatedTransactionDetail.independentValuation,
            totalDividendsIncome = validatedTransactionDetail.totalDividendsIncome,
            sharesDisposed = validatedDisposal._1,
            sharesDisposalDetails = validatedDisposal._2
          )
      )
    )) match {
      case None =>
        CsvRowInvalid(
          line,
          NonEmptyList.of(
            ValidationError(line, InvalidRowFormat, "Invalid file format, please format file as per provided template")
          ),
          data
        )
      case Some((raw, Valid(unquotedShareUpload))) =>
        CsvRowValid(line, unquotedShareUpload, raw.toNonEmptyList)
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
      firstNameOfSchemeMember <- getCSVValue(Keys.firstName, headerKeys, csvData)
      lastNameOfSchemeMember <- getCSVValue(Keys.lastName, headerKeys, csvData)
      memberDateOfBirth <- getCSVValue(Keys.memberDateOfBirth, headerKeys, csvData)
      nino <- getOptionalCSVValue(Keys.memberNino, headerKeys, csvData)
      reasonForNoNino <- getOptionalCSVValue(Keys.memberReasonNoNino, headerKeys, csvData)
      countOfTransactions <- getCSVValue(Keys.countOfTransactions, headerKeys, csvData)
      companyName <- getCSVValue(Keys.companyName, headerKeys, csvData)
      companyCRN <- getOptionalCSVValue(Keys.companyCRN, headerKeys, csvData)
      companyNoCRNReason <- getOptionalCSVValue(Keys.companyNoCRNReason, headerKeys, csvData)
      companyClassOfShares <- getOptionalCSVValue(Keys.companyClassOfShares, headerKeys, csvData)
      companyNumberOfShares <- getOptionalCSVValue(Keys.companyNumberOfShares, headerKeys, csvData)
      acquiredFromName <- getCSVValue(Keys.acquiredFrom, headerKeys, csvData)
      totalCost <- getCSVValue(Keys.totalCost, headerKeys, csvData)
      transactionIndependentValuation <- getCSVValue(Keys.transactionIndependentValuation, headerKeys, csvData)
      totalDividends <- getCSVValue(Keys.totalDividends, headerKeys, csvData)
      disposalMade <- getCSVValue(Keys.disposalMade, headerKeys, csvData)
      totalSaleValue <- getOptionalCSVValue(Keys.totalSaleValue, headerKeys, csvData)
      disposalConnectedParty <- getOptionalCSVValue(Keys.disposalConnectedParty, headerKeys, csvData)
      disposalPurchaserName <- getOptionalCSVValue(Keys.disposalPurchaserName, headerKeys, csvData)
      disposalIndependentValuation <- getOptionalCSVValue(Keys.disposalIndependentValuation, headerKeys, csvData)
      noOfSharesSold <- getOptionalCSVValue(Keys.noOfSharesSold, headerKeys, csvData)
      noOfSharesHeld <- getOptionalCSVValue(Keys.noOfSharesHeld, headerKeys, csvData)
    } yield RawTransactionDetail.create(
      row,
      firstNameOfSchemeMember,
      lastNameOfSchemeMember,
      memberDateOfBirth,
      nino,
      reasonForNoNino,
      countOfTransactions,
      companyName,
      companyCRN,
      companyNoCRNReason,
      companyClassOfShares,
      companyNumberOfShares,
      acquiredFromName,
      totalCost,
      transactionIndependentValuation,
      totalDividends,
      disposalMade,
      totalSaleValue,
      disposalConnectedParty,
      disposalPurchaserName,
      disposalIndependentValuation,
      noOfSharesSold,
      noOfSharesHeld
    )

}
