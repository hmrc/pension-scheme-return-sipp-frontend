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
import models.requests.raw.TangibleMoveablePropertyRaw.RawTransactionDetail
import models.requests.raw.TangibleMoveablePropertyUpload.TangibleMoveablePropertyUpload
import play.api.i18n.Messages
import services.validation.Validator
import services.validation.TangibleMoveablePropertyValidationsService

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

    val validationResult = for {
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

      validatedNino <- validations.validateNinoWithNoReason(
        nino = raw.memberNino,
        noNinoReason = raw.memberReasonNoNino,
        memberFullName = memberFullNameDob,
        row = line
      )

      validatePropCount <- validations.validateCount(
        raw.countOfTangiblePropertyTransactions,
        key = "tangibleMoveableProperty.transactionCount",
        memberFullName = memberFullNameDob,
        row = line,
        maxCount = 50
      )

      validatedAsset <- validations.validateAsset(raw.rawAsset, memberFullNameDob, line)

    } yield (
      raw.toNonEmptyList,
      (validatedNameDOB, validatedNino, validatePropCount, validatedAsset).mapN(TangibleMoveablePropertyUpload.apply)
    )
    validationResult match {
      case Some(raw -> Valid(request)) => CsvRowValid(line, request, raw)
      case Some(raw -> Invalid(errors)) => CsvRowInvalid(line, errors, raw)
      case None => invalidFileFormat(line, values)
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
      /* B */ firstNameOfSchemeMemberTangible <- csvValue(UploadKeys.firstNameOfSchemeMemberTangible)
      /* C */ lastNameOfSchemeMemberTangible <- csvValue(UploadKeys.lastNameOfSchemeMemberTangible)
      /* D */ memberDateOfBirthTangible <- csvValue(UploadKeys.memberDateOfBirthTangible)
      /* E */ memberNino <- optCsvValue(UploadKeys.memberNinoTangible)
      /* F */ memberReasonNoNino <- optCsvValue(UploadKeys.memberReasonNoNino)
      /* G */ countOfTangiblePropertyTransactions <- csvValue(UploadKeys.countOfTangiblePropertyTransactions)
      /* H */ descriptionOfAssetTangible <- csvValue(UploadKeys.descriptionOfAssetTangible)
      /* I */ dateOfAcquisitionTangible <- csvValue(UploadKeys.dateOfAcquisitionTangible)
      /* J */ totalCostOfAssetTangible <- csvValue(UploadKeys.totalCostOfAssetTangible)
      /* K */ acquiredFromTangible <- csvValue(UploadKeys.acquiredFromTangible)
      /* L */ isIndependentEvaluationTangible <- csvValue(UploadKeys.isIndependentEvaluationTangible)
      /* M */ totalIncomeInTaxYearTangible <- csvValue(UploadKeys.totalIncomeInTaxYearTangible)
      /* N */ isTotalCostOrMarketValueTangible <- csvValue(UploadKeys.isTotalCostOrMarketValueTangible)
      /* O */ totalCostOrMarketValueTangible <- csvValue(UploadKeys.totalCostOrMarketValueTangible)
      /* P */ areAnyDisposalsTangible <- csvValue(UploadKeys.areAnyDisposalsTangible)
      /* Q */ disposalsAmountTangible <- optCsvValue(UploadKeys.disposalsAmountTangible)
      /* R */ namesOfPurchasersTangible <- optCsvValue(UploadKeys.namesOfPurchasersTangible)
      /* S */ areAnyPurchasersConnected <- optCsvValue(UploadKeys.areAnyPurchasersConnectedTangible)
      /* T */ wasTxSupportedIndValuationTangible <- optCsvValue(UploadKeys.wasTxSupportedIndValuationTangible)
      /* U */ isAnyPartStillHeldTangible <- optCsvValue(UploadKeys.isAnyPartStillHeldTangible)
      // format: on
    } yield RawTransactionDetail.create(
      row,
      firstNameOfSchemeMemberTangible,
      lastNameOfSchemeMemberTangible,
      memberDateOfBirthTangible,
      memberNino,
      memberReasonNoNino,
      countOfTangiblePropertyTransactions,
      descriptionOfAssetTangible,
      dateOfAcquisitionTangible,
      totalCostOfAssetTangible,
      acquiredFromTangible,
      isIndependentEvaluationTangible,
      totalIncomeInTaxYearTangible,
      isTotalCostOrMarketValueTangible,
      totalCostOrMarketValueTangible,
      areAnyDisposalsTangible,
      disposalsAmountTangible,
      namesOfPurchasersTangible,
      areAnyPurchasersConnected,
      wasTxSupportedIndValuationTangible,
      isAnyPartStillHeldTangible
    )
  }

}
