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
import models.csv.CsvRowState
import models.csv.CsvRowState.*
import models.requests.TangibleMoveablePropertyApi
import models.requests.raw.TangibleMoveablePropertyRaw.RawTransactionDetail
import models.*
import play.api.i18n.Messages
import services.validation.{TangibleMoveablePropertyValidationsService, Validator}
import models.keys.{TangibleKeys => Keys}

import javax.inject.Inject

class TangibleMoveableCsvRowValidator @Inject() (
  validations: TangibleMoveablePropertyValidationsService
) extends CsvRowValidator[TangibleMoveablePropertyApi.TransactionDetail]
    with Validator {

  override def validate(
    line: Int,
    data: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(implicit
    messages: Messages
  ): CsvRowState[TangibleMoveablePropertyApi.TransactionDetail] = {
    val validDateThreshold = csvRowValidationParameters.schemeWindUpDate

    (for {
      raw <- readCSV(line, headers, data.toList)
      memberFullNameDob =
        s"${raw.firstNameOfSchemeMember.value} ${raw.lastNameOfSchemeMember.value} ${raw.memberDateOfBirth.value}"

      // Validations
      validatedNameDOB <- validations.validateNameDOB(
        firstName = raw.firstNameOfSchemeMember,
        lastName = raw.lastNameOfSchemeMember,
        dob = raw.memberDateOfBirth,
        row = line,
        validDateThreshold = validDateThreshold
      )

      validatedNino <- validations.validateNinoWithNoReason(
        nino = raw.memberNino,
        noNinoReason = raw.memberReasonNoNino,
        memberFullName = memberFullNameDob,
        row = line
      )

      validatedDescriptionOfAsset <- validations.validateFreeText(
        raw.rawAsset.descriptionOfAsset,
        "tangibleMoveableProperty.descriptionOfAsset",
        memberFullNameDob,
        line
      )

      validatedDateOfAcquisitionAsset <- validations.validateDate(
        date = raw.rawAsset.dateOfAcquisitionAsset,
        key = "tangibleMoveableProperty.dateOfAcquisitionAsset",
        row = line
      )

      validatedTotalCostAsset <- validations.validatePrice(
        raw.rawAsset.totalCostAsset,
        "tangibleMoveableProperty.totalCostAsset",
        memberFullNameDob,
        line
      )

      validatedAcquiredFrom <- validations.validateFreeText(
        raw.rawAsset.acquiredFrom,
        "tangibleMoveableProperty.whoAcquiredFromName",
        memberFullNameDob,
        line
      )

      validatedIsTxSupportedByIndependentValuation <- validations.validateYesNoQuestionTyped(
        raw.rawAsset.isTxSupportedByIndependentValuation,
        "tangibleMoveableProperty.isTxSupportedByIndependentValuation",
        memberFullNameDob,
        line
      )

      validatedTotalAmountIncomeReceiptsTaxYear <- validations.validatePrice(
        raw.rawAsset.totalAmountIncomeReceiptsTaxYear,
        "tangibleMoveableProperty.totalAmountIncomeReceiptsTaxYear",
        memberFullNameDob,
        line
      )

      validatedTotalCostValueTaxYearAsset <- validations.validatePrice(
        raw.rawAsset.totalCostValueTaxYearAsset,
        "tangibleMoveableProperty.totalCostValueTaxYearAsset",
        memberFullNameDob,
        line
      )

      validatedIsTotalCostValueOrMarketValue <- validations.validateMarketValueOrCostValue(
        raw.rawAsset.isTotalCostValueOrMarketValue,
        "tangibleMoveableProperty.isTotalCostValueOrMarketValue",
        memberFullNameDob,
        line
      )

      validatedDisposals <- validations.validateDisposals(
        raw.rawAsset.rawDisposal.wereAnyDisposalOnThisDuringTheYear,
        raw.rawAsset.rawDisposal.totalConsiderationAmountSaleIfAnyDisposal,
        raw.rawAsset.rawDisposal.purchaserNames,
        raw.rawAsset.rawDisposal.areAnyPurchasersConnected,
        raw.rawAsset.rawDisposal.isTransactionSupportedByIndependentValuation,
        raw.rawAsset.rawDisposal.isAnyPartAssetStillHeld,
        memberFullNameDob,
        line
      )

    } yield (
      raw,
      (
        validatedNameDOB,
        validatedNino,
        validatedDescriptionOfAsset,
        validatedDateOfAcquisitionAsset,
        validatedTotalCostAsset,
        validatedAcquiredFrom,
        validatedIsTxSupportedByIndependentValuation,
        validatedTotalAmountIncomeReceiptsTaxYear,
        validatedTotalCostValueTaxYearAsset,
        validatedIsTotalCostValueOrMarketValue,
        validatedDisposals
      ).mapN {
        (
          nameDOB,
          nino,
          descriptionOfAsset,
          dateOfAcquisitionAsset,
          totalCostAsset,
          acquiredFrom,
          isTxSupportedByIndependentValuation,
          totalAmountIncomeReceiptsTaxYear,
          totalCostValueTaxYearAsset,
          isTotalCostValueOrMarketValue,
          disposals
        ) =>
          TangibleMoveablePropertyApi.TransactionDetail(
            row = Some(line),
            nameDOB = nameDOB,
            nino = nino,
            assetDescription = descriptionOfAsset,
            acquisitionDate = dateOfAcquisitionAsset,
            totalCost = totalCostAsset.value,
            acquiredFromName = acquiredFrom,
            independentValuation = isTxSupportedByIndependentValuation,
            totalIncomeOrReceipts = totalAmountIncomeReceiptsTaxYear.value,
            costMarketValue = totalCostValueTaxYearAsset.value,
            costOrMarket = isTotalCostValueOrMarketValue,
            isPropertyDisposed = disposals._1,
            disposalDetails = disposals._2
          )
      }
    )) match {
      case None =>
        invalidFileFormat(line, data)
      case Some((raw, Valid(landConnectedProperty))) =>
        CsvRowValid(line, landConnectedProperty, raw.toNonEmptyList)
      case Some((raw, Invalid(errs))) =>
        CsvRowInvalid[requests.TangibleMoveablePropertyApi.TransactionDetail](line, errs, raw.toNonEmptyList)
    }
  }

  private def readCSV(
    row: Int,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[RawTransactionDetail] = {
    val csvValue = getCSVValue(_, headerKeys, csvData)
    val optCsvValue = getOptionalCSVValue(_, headerKeys, csvData)
    for {
      // format: off
      /* B */ firstName <- csvValue(Keys.firstName)
      /* C */ lastName <- csvValue(Keys.lastName)
      /* D */ memberDateOfBirth <- csvValue(Keys.memberDateOfBirth)
      /* E */ memberNino <- optCsvValue(Keys.memberNino)
      /* F */ memberReasonNoNino <- optCsvValue(Keys.memberReasonNoNino)
      /* G */ descriptionOfAsset <- csvValue(Keys.descriptionOfAsset)
      /* H */ dateOfAcquisition <- csvValue(Keys.dateOfAcquisition)
      /* I */ totalCostOfAsset <- csvValue(Keys.totalCostOfAsset)
      /* J */ acquiredFrom <- csvValue(Keys.acquiredFrom)
      /* K */ isIndependentEvaluation <- csvValue(Keys.isIndependentValuation)
      /* L */ totalIncomeInTaxYear <- csvValue(Keys.totalIncomeInTaxYear)
      /* M */ totalCostOrMarketValue <- csvValue(Keys.totalCostOrMarketValue)
      /* N */ isTotalCostOrMarketValue <- csvValue(Keys.isTotalCostOrMarketValue)
      /* O */ areAnyDisposals <- csvValue(Keys.areAnyDisposals)
      /* P */ disposalsAmount <- optCsvValue(Keys.disposalsAmount)
      /* Q */ namesOfPurchasers <- optCsvValue(Keys.namesOfPurchasers)
      /* R */ areAnyPurchasersConnected <- optCsvValue(Keys.areAnyPurchasersConnected)
      /* S */ wasTxSupportedIndValuation <- optCsvValue(Keys.wasTxSupportedIndValuation)
      /* T */ isAnyPartStillHeld <- optCsvValue(Keys.isAnyPartStillHeld)
      // format: on
    } yield RawTransactionDetail.create(
      row,
      firstName,
      lastName,
      memberDateOfBirth,
      memberNino,
      memberReasonNoNino,
      descriptionOfAsset,
      dateOfAcquisition,
      totalCostOfAsset,
      acquiredFrom,
      isIndependentEvaluation,
      totalIncomeInTaxYear,
      totalCostOrMarketValue,
      isTotalCostOrMarketValue,
      areAnyDisposals,
      disposalsAmount,
      namesOfPurchasers,
      areAnyPurchasersConnected,
      wasTxSupportedIndValuation,
      isAnyPartStillHeld
    )
  }

}
