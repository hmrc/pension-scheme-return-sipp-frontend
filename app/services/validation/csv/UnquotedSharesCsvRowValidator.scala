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
import models.ValidationErrorType.InvalidRowFormat
import models._
import models.csv.CsvRowState
import models.csv.CsvRowState._
import models.requests.UnquotedShareApi
import models.requests.raw.UnquotedShareRaw.RawTransactionDetail
import play.api.i18n.Messages
import services.validation.{UnquotedSharesValidationsService, Validator}
import uk.gov.hmrc.domain.Nino

import javax.inject.Inject

class UnquotedSharesCsvRowValidator @Inject()(
  validations: UnquotedSharesValidationsService
) extends CsvRowValidator[UnquotedShareApi.TransactionDetail]
    with Validator {

  override def validate(
    line: Int,
    data: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(
    implicit
    messages: Messages
  ): CsvRowState[UnquotedShareApi.TransactionDetail] = {
    val validDateThreshold = csvRowValidationParameters.schemeWindUpDate

    (for {
      raw <- readCSV(line, headers, data.toList)

      memberFullNameDob = s"${raw.firstNameOfSchemeMember.value} ${raw.lastNameOfSchemeMember.value} ${raw.memberDateOfBirth.value}"

      validatedNameDOB <- validations.validateNameDOB(
        firstName = raw.firstNameOfSchemeMember,
        lastName = raw.lastNameOfSchemeMember,
        dob = raw.memberDateOfBirth,
        row = line,
        validDateThreshold = validDateThreshold
      )

      maybeValidatedNino = raw.memberNino.value.flatMap { nino =>
        validations.validateNinoWithDuplicationControl(
          raw.memberNino.as(nino.toUpperCase),
          memberFullNameDob,
          List.empty[Nino], //TODO: Implement duplicate Nino check
          line
        )
      }

      maybeValidatedNoNinoReason = raw.memberNoNinoReason.value.flatMap(
        reason => validations.validateNoNino(raw.memberNoNinoReason.as(reason), memberFullNameDob, line)
      )

      validatedNinoOrNoNinoReason <- (maybeValidatedNino, maybeValidatedNoNinoReason) match {
        case (Some(validatedNino), None) => Some(Right(validatedNino))
        case (None, Some(validatedNoNinoReason)) => Some(Left(validatedNoNinoReason))
        case (_, _) =>
          Some(
            Left(
              ValidationError
                .fromCell(
                  line,
                  ValidationErrorType.NoNinoReason,
                  messages("noNINO.upload.error.required")
                )
                .invalidNel
            )
          )
      }

      validatedTransactionCount <- validations.validateCount(
        raw.countOfTransactions,
        key = "unquotedShares.transactionCount",
        memberFullName = memberFullNameDob,
        row = line,
        maxCount = 9999999
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
        raw.rawSharesTransactionDetail.noOfIndependentValuationSharesSold,
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
        memberFullNameDob,
        line
      )

      maybeNoOfSharesHeld <- raw.noOfSharesHeld.value
        .flatMap(
          p =>
            validations.validateCount(
              raw.noOfSharesHeld.as(p),
              "unquotedShares.noOfSharesHeld",
              memberFullNameDob,
              row = line,
              maxCount = 999999999,
              minCount = 0
            )
        )
        .orElse(Some(Valid(0)))

    } yield (
      raw,
      (
        validatedNameDOB,
        validatedNinoOrNoNinoReason.bisequence,
        validatedTransactionCount, //TODO: check transaction count
        validatedShareCompanyDetails,
        validatedWhoAcquiredFromName,
        validatedTransactionDetail,
        validatedDisposal,
        maybeNoOfSharesHeld
      ).mapN(
        (
          validatedNameDOB,
          validatedNinoOrNoNinoReason,
          _,
          validatedShareCompanyDetails,
          validatedWhoAcquiredFromName,
          validatedTransactionDetail,
          validatedDisposal,
          maybeNoOfSharesHeld
        ) =>
          UnquotedShareApi.TransactionDetail(
            row = line,
            nameDOB = validatedNameDOB,
            nino = validatedNinoOrNoNinoReason.fold(r => NinoType(None, Some(r)), n => NinoType(Some(n.value), None)),
            shareCompanyDetails = validatedShareCompanyDetails,
            acquiredFromName = validatedWhoAcquiredFromName,
            transactionDetail = validatedTransactionDetail,
            sharesDisposed = validatedDisposal._1,
            sharesDisposalDetails = validatedDisposal._2,
            noOfSharesHeld = maybeNoOfSharesHeld
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
        CsvRowInvalid(
          line,
          errors,
          raw.toNonEmptyList
        )
    }
  }

  private def readCSV(
    row: Int,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[RawTransactionDetail] =
    for {
      firstNameOfSchemeMemberUnquotedShares <- getCSVValue(
        UploadKeys.firstNameOfSchemeMemberUnquotedShares,
        headerKeys,
        csvData
      )
      lastNameOfSchemeMemberUnquotedShares <- getCSVValue(
        UploadKeys.lastNameOfSchemeMemberUnquotedShares,
        headerKeys,
        csvData
      )
      memberDateOfBirthUnquotedShares <- getCSVValue(UploadKeys.memberDateOfBirthUnquotedShares, headerKeys, csvData)
      ninoUnquotedShares <- getOptionalCSVValue(UploadKeys.ninoUnquotedShares, headerKeys, csvData)
      reasonForNoNinoUnquotedShares <- getOptionalCSVValue(
        UploadKeys.reasonForNoNinoUnquotedShares,
        headerKeys,
        csvData
      )
      countOfUnquotedSharesTransactions <- getCSVValue(
        UploadKeys.countOfUnquotedSharesTransactions,
        headerKeys,
        csvData
      )
      companyNameSharesUnquotedShares <- getCSVValue(UploadKeys.companyNameSharesUnquotedShares, headerKeys, csvData)
      companyCRNSharesUnquotedShares <- getOptionalCSVValue(
        UploadKeys.companyCRNSharesUnquotedShares,
        headerKeys,
        csvData
      )
      companyNoCRNReasonSharesUnquotedShares <- getOptionalCSVValue(
        UploadKeys.companyNoCRNReasonSharesUnquotedShares,
        headerKeys,
        csvData
      )
      companyClassSharesUnquotedShares <- getOptionalCSVValue(
        UploadKeys.companyClassSharesUnquotedShares,
        headerKeys,
        csvData
      )
      companyNumberOfSharesUnquotedShares <- getOptionalCSVValue(
        UploadKeys.companyNumberOfSharesUnquotedShares,
        headerKeys,
        csvData
      )
      acquiredFromNameUnquotedShares <- getCSVValue(UploadKeys.acquiredFromUnquotedShares, headerKeys, csvData)
      totalCostUnquotedShares <- getCSVValue(UploadKeys.totalCostUnquotedShares, headerKeys, csvData)
      transactionUnquotedSharesIndependentValuation <- getCSVValue(
        UploadKeys.transactionUnquotedSharesIndependentValuation,
        headerKeys,
        csvData
      )
      transactionIndependentValuationSharesSold <- getOptionalCSVValue(
        UploadKeys.transactionUnquotedNoSharesSold,
        headerKeys,
        csvData
      )
      transactionUnquotedTotalDividends <- getCSVValue(
        UploadKeys.transactionUnquotedTotalDividends,
        headerKeys,
        csvData
      )
      disposalUnquotedSharesDisposalMade <- getCSVValue(
        UploadKeys.disposalUnquotedSharesDisposalMade,
        headerKeys,
        csvData
      )
      disposalUnquotedSharesTotalSaleValue <- getOptionalCSVValue(
        UploadKeys.disposalUnquotedSharesTotalSaleValue,
        headerKeys,
        csvData
      )
      disposalUnquotedSharesConnectedParty <- getOptionalCSVValue(
        UploadKeys.disposalUnquotedSharesConnectedParty,
        headerKeys,
        csvData
      )
      disposalUnquotedSharesPurchaserName <- getOptionalCSVValue(
        UploadKeys.disposalUnquotedSharesPurchaserName,
        headerKeys,
        csvData
      )
      disposalUnquotedSharesIndependentValuation <- getOptionalCSVValue(
        UploadKeys.disposalUnquotedSharesIndependentValuation,
        headerKeys,
        csvData
      )
      noOfSharesHeld <- getOptionalCSVValue(UploadKeys.noOfSharesHeld, headerKeys, csvData)
    } yield RawTransactionDetail.create(
      row,
      firstNameOfSchemeMemberUnquotedShares,
      lastNameOfSchemeMemberUnquotedShares,
      memberDateOfBirthUnquotedShares,
      ninoUnquotedShares,
      reasonForNoNinoUnquotedShares,
      countOfUnquotedSharesTransactions,
      companyNameSharesUnquotedShares,
      companyCRNSharesUnquotedShares,
      companyNoCRNReasonSharesUnquotedShares,
      companyClassSharesUnquotedShares,
      companyNumberOfSharesUnquotedShares,
      acquiredFromNameUnquotedShares,
      totalCostUnquotedShares,
      transactionUnquotedSharesIndependentValuation,
      transactionIndependentValuationSharesSold,
      transactionUnquotedTotalDividends,
      disposalUnquotedSharesDisposalMade,
      disposalUnquotedSharesTotalSaleValue,
      disposalUnquotedSharesConnectedParty,
      disposalUnquotedSharesPurchaserName,
      disposalUnquotedSharesIndependentValuation,
      noOfSharesHeld
    )

}
