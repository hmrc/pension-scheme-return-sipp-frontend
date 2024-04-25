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
import models.requests.raw.AssetConnectedPartyRaw.RawTransactionDetail
import models.requests.raw.AssetConnectedPartyUpload._
import play.api.i18n.Messages
import services.validation.{AssetsFromConnectedPartyValidationsService, Validator}
import uk.gov.hmrc.domain.Nino

import javax.inject.Inject

class AssetFromConnectedPartyCsvRowValidator @Inject()(
  validations: AssetsFromConnectedPartyValidationsService
) extends CsvRowValidator[AssetConnectedPartyUpload]
    with Validator {

  override def validate(
    line: Int,
    values: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(
    implicit messages: Messages
  ): CsvRowState[AssetConnectedPartyUpload] = {
    val validDateThreshold = csvRowValidationParameters.schemeWindUpDate

    (for {
      raw <- readCSV(line, headers, values.toList)
      memberFullNameDob = s"${raw.firstNameOfSchemeMember.value} ${raw.lastNameOfSchemeMember.value} ${raw.memberDateOfBirth.value}"

      // Validations
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

      validatePropertyCount <- validations.validateCount(
        raw.countOfTransactions,
        key = "assetConnectedParty.transactionCount",
        memberFullName = memberFullNameDob,
        row = line,
        maxCount = 50
      )

      validatedDateOfAcquisitionAsset <- validations.validateDate(
        date = raw.acquisitionDate,
        key = "assetConnectedParty.dateOfAcquisitionAsset",
        row = line
      )

      validateDescriptionOfAsset <- validations.validateFreeText(
        raw.assetDescription,
        "assetConnectedParty.descriptionOfAsset",
        memberFullNameDob,
        line
      )

      validatedShareCompanyDetails <- validations.validateShareCompanyDetails(
        acquisitionOfShares = raw.acquisitionOfShares,
        companySharesName = raw.shareCompanyDetails.companySharesName,
        companySharesCRN = raw.shareCompanyDetails.companySharesCRN,
        reasonNoCRN = raw.shareCompanyDetails.reasonNoCRN,
        sharesClass = raw.shareCompanyDetails.sharesClass,
        noOfShares = raw.shareCompanyDetails.noOfShares,
        memberFullNameDob = memberFullNameDob,
        line
      )

      validatedWhoAcquiredFromName <- validations.validateFreeText(
        raw.acquiredFromName,
        "assetConnectedParty.whoAcquiredFromName",
        memberFullNameDob,
        line
      )

      validatedTotalCostAsset <- validations.validatePrice(
        raw.totalCost,
        "assetConnectedParty.totalCostAsset",
        memberFullNameDob,
        line
      )

      validatedIsTxSupportedByIndependentValuation <- validations.validateYesNoQuestion(
        raw.independentValuation,
        "assetConnectedParty.isTxSupportedByIndependentValuation",
        memberFullNameDob,
        line
      )

      validatedIsTangibleSchedule29A <- validations.validateYesNoQuestion(
        raw.tangibleSchedule29A,
        "assetConnectedParty.isTangibleSchedule29A",
        memberFullNameDob,
        line
      )

      validatedTotalCostValueTaxYearAsset <- validations.validatePrice(
        raw.totalIncomeOrReceipts,
        "assetConnectedParty.totalAmountIncomeReceiptsTaxYear",
        memberFullNameDob,
        line
      )

      validateDisposals <- validations.validateDisposals(
        wereAnyDisposalOnThisDuringTheYear = raw.isAssetDisposed,
        totalConsiderationAmountSaleIfAnyDisposal = raw.rawDisposal.disposedPropertyProceedsAmt,
        namesOfPurchasers = raw.rawDisposal.namesOfPurchasers,
        areAnyPurchasersConnectedParty = raw.rawDisposal.areAnyPurchasersConnectedParty,
        isTransactionSupportedByIndependentValuation = raw.rawDisposal.independentEvalTx,
        disposalOfShares = raw.rawDisposal.disposalOfShares,
        noOfSharesHeld = raw.rawDisposal.noOfSharesHeld,
        fullyDisposed = raw.rawDisposal.fullyDisposed,
        memberFullNameDob = memberFullNameDob,
        line
      )

    } yield (
      raw,
      (
        validatedNameDOB,
        validatedNinoOrNoNinoReason.bisequence,
        validatePropertyCount,
        validatedDateOfAcquisitionAsset,
        validateDescriptionOfAsset,
        validatedShareCompanyDetails,
        validatedWhoAcquiredFromName,
        validatedTotalCostAsset,
        validatedIsTxSupportedByIndependentValuation,
        validatedIsTangibleSchedule29A,
        validatedTotalCostValueTaxYearAsset,
        validateDisposals
      ).mapN(
        (
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          _
        ) => {
          fromRaw(raw)
        }
      )
    )) match {
      case Some((raw, Valid(assetConnectedPartyUpload))) =>
        CsvRowValid(line, assetConnectedPartyUpload, raw.toNonEmptyList)
      case Some((raw, Invalid(errors))) => CsvRowInvalid(line, errors, raw.toNonEmptyList)
      case None =>
        CsvRowInvalid(
          line,
          NonEmptyList.of(
            ValidationError(line, InvalidRowFormat, "Invalid file format, please format file as per provided template")
          ),
          values
        )
    }
  }

  private def readCSV(
    row: Int,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[RawTransactionDetail] =
    for {
      /*  B */
      firstNameOfSchemeMemberAssetConnectedParty <- getCSVValue(
        UploadKeys.firstNameOfSchemeMemberAssetConnectedParty,
        headerKeys,
        csvData
      )
      /*  C */
      lastNameOfSchemeMemberAssetConnectedParty <- getCSVValue(
        UploadKeys.lastNameOfSchemeMemberAssetConnectedParty,
        headerKeys,
        csvData
      )
      /*  D */
      memberDateOfBirthAssetConnectedParty <- getCSVValue(
        UploadKeys.memberDateOfBirthAssetConnectedParty,
        headerKeys,
        csvData
      )

      /*  E */
      memberNinoAssetConnectedParty <- getOptionalCSVValue(UploadKeys.ninoAssetConnectedParty, headerKeys, csvData)

      /*  F */
      memberNinoReasonAssetConnectedParty <- getOptionalCSVValue(
        UploadKeys.reasonForNoNinoAssetConnectedParty,
        headerKeys,
        csvData
      )

      /*  G */
      countOfAssetConnectedPartyPropertyTransactions <- getCSVValue(
        UploadKeys.countOfAssetConnectedPartyTransactions,
        headerKeys,
        csvData
      )

      /*  H */
      dateOfAcquisitionAssetConnectedParty <- getCSVValue(
        UploadKeys.dateOfAcquisitionAssetConnectedParty,
        headerKeys,
        csvData
      )

      /*  I */
      descriptionOfAssetAssetConnectedParty <- getCSVValue(
        UploadKeys.descriptionOfAssetConnectedParty,
        headerKeys,
        csvData
      )

      /*  J */
      isSharesAssetConnectedParty <- getCSVValue(UploadKeys.isSharesAssetConnectedParty, headerKeys, csvData)

      /*  K */
      companyNameSharesAssetConnectedParty <- getOptionalCSVValue(
        UploadKeys.companyNameSharesAssetConnectedParty,
        headerKeys,
        csvData
      )

      /*  L */
      companyCRNSharesAssetConnectedParty <- getOptionalCSVValue(
        UploadKeys.companyCRNSharesAssetConnectedParty,
        headerKeys,
        csvData
      )

      /*  M */
      companyNoCRNReasonSharesAssetConnectedParty <- getOptionalCSVValue(
        UploadKeys.companyNoCRNReasonSharesAssetConnectedParty,
        headerKeys,
        csvData
      )

      /*  N */
      companyClassSharesAssetConnectedParty <- getOptionalCSVValue(
        UploadKeys.companyClassSharesAssetConnectedParty,
        headerKeys,
        csvData
      )

      /*  O */
      companyNumberOfSharesAssetConnectedParty <- getOptionalCSVValue(
        UploadKeys.companyNumberOfSharesAssetConnectedParty,
        headerKeys,
        csvData
      )

      /*  P */
      acquiredFromAssetConnectedParty <- getCSVValue(UploadKeys.acquiredFromAssetConnectedParty, headerKeys, csvData)

      /*  Q */
      totalCostOfAssetAssetConnectedParty <- getCSVValue(
        UploadKeys.totalCostOfAssetAssetConnectedParty,
        headerKeys,
        csvData
      )

      /*  R */
      isIndependentEvaluationConnectedParty <- getCSVValue(
        UploadKeys.isIndependentEvaluationConnectedParty,
        headerKeys,
        csvData
      )

      /*  S */
      isFinanceActConnectedParty <- getCSVValue(UploadKeys.isFinanceActConnectedParty, headerKeys, csvData)

      /*  T */
      totalIncomeInTaxYearConnectedParty <- getCSVValue(
        UploadKeys.totalIncomeInTaxYearConnectedParty,
        headerKeys,
        csvData
      )

      /*  U */
      areAnyDisposalsYearConnectedParty <- getCSVValue(
        UploadKeys.areAnyDisposalsYearConnectedParty,
        headerKeys,
        csvData
      )

      /*  V */
      disposalsAmountConnectedParty <- getOptionalCSVValue(
        UploadKeys.disposalsAmountConnectedParty,
        headerKeys,
        csvData
      )

      /*  W */
      namesOfPurchasersConnectedParty <- getOptionalCSVValue(
        UploadKeys.namesOfPurchasersConnectedParty,
        headerKeys,
        csvData
      )

      /*  X */
      areConnectedPartiesPurchasersConnectedParty <- getOptionalCSVValue(
        UploadKeys.areConnectedPartiesPurchasersConnectedParty,
        headerKeys,
        csvData
      )

      /*  Y */
      wasTransactionSupportedIndValuationConnectedParty <- getOptionalCSVValue(
        UploadKeys.wasTransactionSupportedIndValuationConnectedParty,
        headerKeys,
        csvData
      )

      /*  Z */
      wasDisposalOfSharesConnectedParty <- getOptionalCSVValue(
        UploadKeys.wasDisposalOfSharesConnectedParty,
        headerKeys,
        csvData
      )

      /*  AA */
      disposalOfSharesNumberHeldConnectedParty <- getOptionalCSVValue(
        UploadKeys.disposalOfSharesNumberHeldConnectedParty,
        headerKeys,
        csvData
      )

      /*  AB */
      noDisposalOfSharesFullyHeldConnectedParty <- getOptionalCSVValue(
        UploadKeys.noDisposalOfSharesFullyHeldConnectedParty,
        headerKeys,
        csvData
      )

    } yield RawTransactionDetail.create(
      row,
      firstNameOfSchemeMemberAssetConnectedParty,
      lastNameOfSchemeMemberAssetConnectedParty,
      memberDateOfBirthAssetConnectedParty,
      memberNinoAssetConnectedParty,
      memberNinoReasonAssetConnectedParty,
      countOfAssetConnectedPartyPropertyTransactions,
      dateOfAcquisitionAssetConnectedParty,
      descriptionOfAssetAssetConnectedParty,
      isSharesAssetConnectedParty,
      companyNameSharesAssetConnectedParty,
      companyCRNSharesAssetConnectedParty,
      companyNoCRNReasonSharesAssetConnectedParty,
      companyClassSharesAssetConnectedParty,
      companyNumberOfSharesAssetConnectedParty,
      acquiredFromAssetConnectedParty,
      totalCostOfAssetAssetConnectedParty,
      isIndependentEvaluationConnectedParty,
      isFinanceActConnectedParty,
      totalIncomeInTaxYearConnectedParty,
      areAnyDisposalsYearConnectedParty,
      disposalsAmountConnectedParty,
      namesOfPurchasersConnectedParty,
      areConnectedPartiesPurchasersConnectedParty,
      wasTransactionSupportedIndValuationConnectedParty,
      wasDisposalOfSharesConnectedParty,
      disposalOfSharesNumberHeldConnectedParty,
      noDisposalOfSharesFullyHeldConnectedParty
    )

}
