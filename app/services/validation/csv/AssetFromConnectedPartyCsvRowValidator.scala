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
import models.requests.AssetsFromConnectedPartyApi
import models.requests.common.{DisposalDetail, SharesCompanyDetails, YesNo}
import models.requests.raw.AssetConnectedPartyRaw.RawTransactionDetail
import play.api.i18n.Messages
import services.validation.{AssetsFromConnectedPartyValidationsService, Validator}

import javax.inject.Inject

class AssetFromConnectedPartyCsvRowValidator @Inject()(
  validations: AssetsFromConnectedPartyValidationsService
) extends CsvRowValidator[AssetsFromConnectedPartyApi.TransactionDetail]
    with Validator {

  override def validate(
    line: Int,
    values: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(
    implicit messages: Messages
  ): CsvRowState[AssetsFromConnectedPartyApi.TransactionDetail] = {
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

      validatedNino <- validations.validateNinoWithNoReason(
        nino = raw.memberNino,
        noNinoReason = raw.memberNoNinoReason,
        memberFullName = memberFullNameDob,
        row = line
      )

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
        validatedNino,
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
          nameDob,
          nino,
          totalPropertyCount,
          acquisitionDate,
          description,
          acquisitionOfShares,
          acquiredFromName,
          totalCost,
          independentValuation,
          tangibleSchedule29A,
          totalIncomeOrReceipts,
          disposals
        ) => {
          AssetsFromConnectedPartyApi.TransactionDetail(
            row = line,
            nameDOB = nameDob,
            nino = nino,
            acquisitionDate = acquisitionDate,
            assetDescription = description,
            acquisitionOfShares = if (acquisitionOfShares.isDefined) YesNo.Yes else YesNo.No,
            shareCompanyDetails = acquisitionOfShares.map { share =>
              SharesCompanyDetails(
                companySharesName = share.companySharesName,
                companySharesCRN = share.companySharesCRN,
                reasonNoCRN = share.reasonNoCRN,
                sharesClass = share.sharesClass,
                noOfShares = share.noOfShares
              )
            },
            acquiredFromName = acquiredFromName,
            totalCost = totalCost.value,
            independentValuation = YesNo.withNameInsensitive(independentValuation),
            tangibleSchedule29A = YesNo.withNameInsensitive(tangibleSchedule29A),
            totalIncomeOrReceipts = totalIncomeOrReceipts.value,
            isPropertyDisposed = disposals._1,
            disposalDetails = disposals._2.map { disposal =>
              DisposalDetail(
                disposedPropertyProceedsAmt = disposal.totalConsiderationAmountSaleIfAnyDisposal,
                namesOfPurchasers = disposal.namesOfPurchasers,
                anyPurchaserConnected = disposal.areAnyPurchasersConnectedParty,
                independentValuationDisposal = disposal.independentValuationDisposal,
                propertyFullyDisposed = disposal.fullyDisposed.getOrElse(YesNo.No)
              )
            },
            disposalOfShares = disposals._2.map(_.disposalOfShares).getOrElse(YesNo.No),
            noOfSharesHeld = disposals._2.flatMap(_.noOfSharesHeld)
          )
        }
      )
    )) match {
      case Some((raw, Valid(assetConnectedPartyUpload))) =>
        CsvRowValid(line, assetConnectedPartyUpload, raw.toNonEmptyList)
      case Some((raw, Invalid(errors))) =>
        CsvRowInvalid(line, errors, raw.toNonEmptyList)
      case None =>
        invalidFileFormat(line, values)
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
