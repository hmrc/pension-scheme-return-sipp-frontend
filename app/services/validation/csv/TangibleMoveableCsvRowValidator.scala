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
import models.requests.raw.TangibleMoveablePropertyRaw.RawTransactionDetail
import models.requests.raw.TangibleMoveablePropertyUpload.{fromRaw, TangibleMoveablePropertyUpload}
import play.api.i18n.Messages
import services.validation.{TangibleMoveablePropertyValidationsService, Validator}

import javax.inject.Inject

class TangibleMoveableCsvRowValidator @Inject()(
  validations: TangibleMoveablePropertyValidationsService
) extends CsvRowValidator[TangibleMoveablePropertyUpload]
    with Validator {

  override def validate(
    line: Int,
    values: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(
    implicit messages: Messages
  ): CsvRowState[TangibleMoveablePropertyUpload] = {
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

      validatePropertyCount <- validations.validateCount(
        raw.countOfTangiblePropertyTransactions,
        key = "tangibleMoveableProperty.transactionCount",
        memberFullName = memberFullNameDob,
        row = line,
        maxCount = 50
      )

      /*  F */
      validateDescriptionOfAsset <- validations.validateFreeText(
        raw.rawAsset.descriptionOfAsset,
        "tangibleMoveableProperty.descriptionOfAsset",
        memberFullNameDob,
        line
      )

      /*  G */
      validatedDateOfAcquisitionAsset <- validations.validateDate(
        date = raw.rawAsset.dateOfAcquisitionAsset,
        key = "tangibleMoveableProperty.dateOfAcquisitionAsset",
        row = line,
        validDateThreshold = validDateThreshold
      )

      /*  H */
      validatedTotalCostAsset <- validations.validatePrice(
        raw.rawAsset.totalCostAsset,
        "tangibleMoveableProperty.totalCostAsset",
        memberFullNameDob,
        line
      )

      /*  I ... N */
      validatedWhoAcquiredFromName <- validations.validateFreeText(
        raw.rawAsset.rawAcquiredFromType.whoAcquiredFromName,
        "tangibleMoveableProperty.whoAcquiredFromName",
        memberFullNameDob,
        line
      )

      validatedAcquiredFromType <- validations.validateAcquiredFrom(
        raw.rawAsset.rawAcquiredFromType.acquiredFromType,
        raw.rawAsset.rawAcquiredFromType.acquirerNinoForIndividual,
        raw.rawAsset.rawAcquiredFromType.acquirerCrnForCompany,
        raw.rawAsset.rawAcquiredFromType.acquirerUtrForPartnership,
        raw.rawAsset.rawAcquiredFromType.whoAcquiredFromTypeReasonAsset,
        memberFullNameDob,
        line
      )

      /*  O */
      validatedIsTxSupportedByIndependentValuation <- validations.validateYesNoQuestion(
        raw.rawAsset.isTxSupportedByIndependentValuation,
        "tangibleMoveableProperty.isTxSupportedByIndependentValuation",
        memberFullNameDob,
        line
      )

      /*  P */
      validatedTotalAmountIncomeReceiptsTaxYear <- validations.validatePrice(
        raw.rawAsset.totalAmountIncomeReceiptsTaxYear,
        "tangibleMoveableProperty.totalAmountIncomeReceiptsTaxYear",
        memberFullNameDob,
        line
      )

      /*  Q */
      validatedIsTotalCostValueOrMarketValue <- validations.validateMarketValueOrCostValue(
        raw.rawAsset.isTotalCostValueOrMarketValue,
        "tangibleMoveableProperty.isTotalCostValueOrMarketValue",
        memberFullNameDob,
        line
      )

      /*  R */
      validatedTotalCostValueTaxYearAsset <- validations.validatePrice(
        raw.rawAsset.totalCostValueTaxYearAsset,
        "tangibleMoveableProperty.totalCostValueTaxYearAsset",
        memberFullNameDob,
        line
      )

      validatedDisposals <- validations.validateDisposals(
        raw.rawAsset.rawDisposal.wereAnyDisposalOnThisDuringTheYear,
        raw.rawAsset.rawDisposal.totalConsiderationAmountSaleIfAnyDisposal,
        raw.rawAsset.rawDisposal.wereAnyDisposals,
        List(
          (raw.rawAsset.rawDisposal.first.name, raw.rawAsset.rawDisposal.first.connection),
          (raw.rawAsset.rawDisposal.second.name, raw.rawAsset.rawDisposal.second.connection),
          (raw.rawAsset.rawDisposal.third.name, raw.rawAsset.rawDisposal.third.connection),
          (raw.rawAsset.rawDisposal.fourth.name, raw.rawAsset.rawDisposal.fourth.connection),
          (raw.rawAsset.rawDisposal.fifth.name, raw.rawAsset.rawDisposal.fifth.connection),
          (raw.rawAsset.rawDisposal.sixth.name, raw.rawAsset.rawDisposal.sixth.connection),
          (raw.rawAsset.rawDisposal.seventh.name, raw.rawAsset.rawDisposal.seventh.connection),
          (raw.rawAsset.rawDisposal.eighth.name, raw.rawAsset.rawDisposal.eighth.connection),
          (raw.rawAsset.rawDisposal.ninth.name, raw.rawAsset.rawDisposal.ninth.connection),
          (raw.rawAsset.rawDisposal.tenth.name, raw.rawAsset.rawDisposal.tenth.connection)
        ),
        raw.rawAsset.rawDisposal.isTransactionSupportedByIndependentValuation,
        raw.rawAsset.rawDisposal.isAnyPartAssetStillHeld,
        memberFullNameDob,
        line
      )

    } yield (
      raw,
      (
        validatedNameDOB,
        validatePropertyCount,
        validateDescriptionOfAsset,
        validatedDateOfAcquisitionAsset,
        validatedTotalCostAsset,
        validatedWhoAcquiredFromName,
        validatedAcquiredFromType,
        validatedIsTxSupportedByIndependentValuation,
        validatedTotalAmountIncomeReceiptsTaxYear,
        validatedTotalCostValueTaxYearAsset,
        validatedIsTotalCostValueOrMarketValue,
        validatedDisposals
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
      case Some((raw, Valid(tangibleMoveablePropertyUpload))) =>
        CsvRowValid(line, tangibleMoveablePropertyUpload, raw.toNonEmptyList)
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
      firstNameOfSchemeMemberTangible <- getCSVValue(UploadKeys.firstNameOfSchemeMemberTangible, headerKeys, csvData)
      /*  C */
      lastNameOfSchemeMemberTangible <- getCSVValue(UploadKeys.lastNameOfSchemeMemberTangible, headerKeys, csvData)
      /*  D */
      memberDateOfBirthTangible <- getCSVValue(UploadKeys.memberDateOfBirthTangible, headerKeys, csvData)
      /*  E */
      countOfTangiblePropertyTransactions <- getCSVValue(
        UploadKeys.countOfTangiblePropertyTransactions,
        headerKeys,
        csvData
      )
      /*  F */
      descriptionOfAssetTangible <- getCSVValue(UploadKeys.descriptionOfAssetTangible, headerKeys, csvData)
      /*  G */
      dateOfAcquisitionTangible <- getCSVValue(UploadKeys.dateOfAcquisitionTangible, headerKeys, csvData)
      /*  H */
      totalCostOfAssetTangible <- getCSVValue(UploadKeys.totalCostOfAssetTangible, headerKeys, csvData)
      /*  I */
      acquiredFromTangible <- getCSVValue(UploadKeys.acquiredFromTangible, headerKeys, csvData)
      /*  J */
      acquiredFromTypeTangible <- getCSVValue(UploadKeys.acquiredFromTypeTangible, headerKeys, csvData)
      /*  K */
      acquiredFromIndividualTangible <- getOptionalCSVValue(
        UploadKeys.acquiredFromIndividualTangible,
        headerKeys,
        csvData
      )
      /*  L */
      acquiredFromCompanyTangible <- getOptionalCSVValue(UploadKeys.acquiredFromCompanyTangible, headerKeys, csvData)
      /*  M */
      acquiredFromPartnershipTangible <- getOptionalCSVValue(
        UploadKeys.acquiredFromPartnershipTangible,
        headerKeys,
        csvData
      )
      /*  N */
      acquiredFromReasonTangible <- getOptionalCSVValue(UploadKeys.acquiredFromReasonTangible, headerKeys, csvData)
      /*  O */
      isIndependentEvaluationTangible <- getCSVValue(UploadKeys.isIndependentEvaluationTangible, headerKeys, csvData)
      /*  P */
      totalIncomeInTaxYearTangible <- getCSVValue(UploadKeys.totalIncomeInTaxYearTangible, headerKeys, csvData)
      /*  Q */
      isTotalCostOrMarketValueTangible <- getCSVValue(UploadKeys.isTotalCostOrMarketValueTangible, headerKeys, csvData)
      /*  R */
      totalCostOrMarketValueTangible <- getCSVValue(UploadKeys.totalCostOrMarketValueTangible, headerKeys, csvData)
      /*  S */
      areAnyDisposalsTangible <- getCSVValue(UploadKeys.areAnyDisposalsTangible, headerKeys, csvData)
      /*  T */
      disposalsAmountTangible <- getOptionalCSVValue(UploadKeys.disposalsAmountTangible, headerKeys, csvData)
      /*  U */
      areJustDisposalsTangible <- getCSVValue(UploadKeys.areJustDisposalsTangible, headerKeys, csvData)
      /*  V ... O */
      disposalNameOfPurchaser1Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser1Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser1Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser1Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser2Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser2Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser2Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser2Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser3Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser3Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser3Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser3Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser4Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser4Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser4Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser4Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser5Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser5Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser5Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser5Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser6Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser6Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser6Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser6Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser7Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser7Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser7Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser7Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser8Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser8Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser8Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser8Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser9Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser9Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser9Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser9Tangible,
        headerKeys,
        csvData
      )
      disposalNameOfPurchaser10Tangible <- getOptionalCSVValue(
        UploadKeys.disposalNameOfPurchaser10Tangible,
        headerKeys,
        csvData
      )
      disposalTypeOfPurchaser10Tangible <- getOptionalCSVValue(
        UploadKeys.disposalTypeOfPurchaser10Tangible,
        headerKeys,
        csvData
      )
      /*  AP */
      wasTransactionSupportedIndValuationTangible <- getOptionalCSVValue(
        UploadKeys.wasTransactionSupportedIndValuationTangible,
        headerKeys,
        csvData
      )
      /*  AQ */
      isAnyPartStillHeldTangible <- getOptionalCSVValue(UploadKeys.isAnyPartStillHeldTangible, headerKeys, csvData)
    } yield RawTransactionDetail.create(
      row,
      firstNameOfSchemeMemberTangible,
      lastNameOfSchemeMemberTangible,
      memberDateOfBirthTangible,
      countOfTangiblePropertyTransactions,
      descriptionOfAssetTangible,
      dateOfAcquisitionTangible,
      totalCostOfAssetTangible,
      acquiredFromTangible,
      acquiredFromTypeTangible,
      acquiredFromIndividualTangible,
      acquiredFromCompanyTangible,
      acquiredFromPartnershipTangible,
      acquiredFromReasonTangible,
      isIndependentEvaluationTangible,
      totalIncomeInTaxYearTangible,
      isTotalCostOrMarketValueTangible,
      totalCostOrMarketValueTangible,
      areAnyDisposalsTangible,
      disposalsAmountTangible,
      areJustDisposalsTangible,
      disposalNameOfPurchaser1Tangible,
      disposalTypeOfPurchaser1Tangible,
      disposalNameOfPurchaser2Tangible,
      disposalTypeOfPurchaser2Tangible,
      disposalNameOfPurchaser3Tangible,
      disposalTypeOfPurchaser3Tangible,
      disposalNameOfPurchaser4Tangible,
      disposalTypeOfPurchaser4Tangible,
      disposalNameOfPurchaser5Tangible,
      disposalTypeOfPurchaser5Tangible,
      disposalNameOfPurchaser6Tangible,
      disposalTypeOfPurchaser6Tangible,
      disposalNameOfPurchaser7Tangible,
      disposalTypeOfPurchaser7Tangible,
      disposalNameOfPurchaser8Tangible,
      disposalTypeOfPurchaser8Tangible,
      disposalNameOfPurchaser9Tangible,
      disposalTypeOfPurchaser9Tangible,
      disposalNameOfPurchaser10Tangible,
      disposalTypeOfPurchaser10Tangible,
      wasTransactionSupportedIndValuationTangible,
      isAnyPartStillHeldTangible
    )

}
