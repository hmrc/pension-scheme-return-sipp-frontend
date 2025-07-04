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
import models.requests.AssetsFromConnectedPartyApi
import models.requests.common.{DisposalDetails, SharesCompanyDetails, YesNo}
import models.requests.raw.AssetConnectedPartyRaw.RawTransactionDetail
import play.api.i18n.Messages
import services.validation.{AssetsFromConnectedPartyValidationsService, Validator}
import models.keys.{AssetFromConnectedPartyKeys => Keys}

import javax.inject.Inject

class AssetFromConnectedPartyCsvRowValidator @Inject() (
  validations: AssetsFromConnectedPartyValidationsService
) extends CsvRowValidator[AssetsFromConnectedPartyApi.TransactionDetail]
    with Validator {

  override def validate(
    line: Int,
    values: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    csvRowValidationParameters: CsvRowValidationParameters
  )(implicit
    messages: Messages
  ): CsvRowState[AssetsFromConnectedPartyApi.TransactionDetail] = {
    val validDateThreshold = csvRowValidationParameters.schemeWindUpDate

    (for {
      raw <- readCSV(line, headers, values.toList)
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
        noNinoReason = raw.memberNoNinoReason,
        memberFullName = memberFullNameDob,
        row = line
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
        isTransactionSupportedByIndependentValuation = raw.rawDisposal.independentValTx,
        fullyDisposed = raw.rawDisposal.hasFullyDisposed,
        disposalOfShares = raw.rawDisposal.disposalOfShares,
        noOfSharesHeld = raw.rawDisposal.noOfSharesHeld,
        memberFullNameDob = memberFullNameDob,
        line
      )

    } yield (
      raw,
      (
        validatedNameDOB,
        validatedNino,
        validatedDateOfAcquisitionAsset,
        validateDescriptionOfAsset,
        validatedShareCompanyDetails,
        validatedWhoAcquiredFromName,
        validatedTotalCostAsset,
        validatedIsTxSupportedByIndependentValuation,
        validatedIsTangibleSchedule29A,
        validatedTotalCostValueTaxYearAsset,
        validateDisposals
      ).mapN {
        (
          nameDob,
          nino,
          acquisitionDate,
          description,
          acquisitionOfShares,
          acquiredFromName,
          totalCost,
          independentValuation,
          tangibleSchedule29A,
          totalIncomeOrReceipts,
          disposals
        ) =>
          AssetsFromConnectedPartyApi.TransactionDetail(
            row = Some(line),
            nameDOB = nameDob,
            nino = nino,
            acquisitionDate = acquisitionDate,
            assetDescription = description,
            acquisitionOfShares = if (acquisitionOfShares.isDefined) YesNo.Yes else YesNo.No,
            sharesCompanyDetails = acquisitionOfShares.map { share =>
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
              DisposalDetails(
                disposedPropertyProceedsAmt = disposal.totalConsiderationAmountSaleIfAnyDisposal,
                purchasersNames = disposal.namesOfPurchasers,
                anyPurchaserConnectedParty = disposal.areAnyPurchasersConnectedParty,
                independentValuationDisposal = disposal.independentValuationDisposal,
                propertyFullyDisposed = disposal.fullyDisposed
              )
            },
            disposalOfShares = disposals._2.map(_.disposalOfShares),
            noOfSharesHeld = disposals._2.flatMap(_.noOfSharesHeld)
          )
      }
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
  ): Option[RawTransactionDetail] = {
    val csvValue = getCSVValue(_, headerKeys, csvData)
    val csvOptValue = getOptionalCSVValue(_, headerKeys, csvData)
    for {
      /*  B */
      firstName <- csvValue(Keys.firstName)
      /*  C */
      lastName <- csvValue(Keys.lastName)
      /*  D */
      memberDateOfBirth <- csvValue(Keys.memberDateOfBirth)
      /*  E */
      memberNino <- csvOptValue(Keys.memberNino)
      /*  F */
      memberNinoReason <- csvOptValue(Keys.memberReasonNoNino)
      /*  G */
      dateOfAcquisition <- csvValue(Keys.dateOfAcquisition)
      /*  H */
      descriptionOfAsset <- csvValue(Keys.descriptionOfAsset)
      /*  I */
      isAcquisitionOfShares <- csvValue(Keys.isAcquisitionOfShares)
      /*  J */
      companyNameShares <- csvOptValue(Keys.companyNameShares)
      /*  K */
      companyCRNShares <- csvOptValue(Keys.companyCRNShares)
      /*  L */
      companyNoCRNReasonShares <- csvOptValue(Keys.companyNoCRNReasonShares)
      /*  M */
      companyClassShares <- csvOptValue(Keys.companyClassShares)
      /*  N */
      companyNumberOfShares <- csvOptValue(Keys.companyNumberOfShares)
      /*  O */
      acquiredFrom <- csvValue(Keys.acquiredFrom)
      /*  P */
      totalCostOfAsset <- csvValue(Keys.totalCostOfAsset)
      /*  Q */
      isIndependentValuation <- csvValue(Keys.isIndependentValuation)
      /*  R */
      isFinanceAct <- csvValue(Keys.isFinanceAct)
      /*  S */
      totalIncomeInTaxYear <- csvValue(Keys.totalIncomeInTaxYear)
      /*  T */
      areAnyDisposalsYear <- csvValue(Keys.areAnyDisposalsYear)
      /*  U */
      disposalsAmount <- csvOptValue(Keys.disposalsAmount)
      /*  V */
      namesOfPurchasers <- csvOptValue(Keys.namesOfPurchasers)
      /*  W */
      areConnectedPartiesPurchasers <- csvOptValue(Keys.areConnectedPartiesPurchasers)
      /*  X */
      wasTransactionSupportedIndValuation <- csvOptValue(Keys.wasTransactionSupportedIndValuation)
      /*  Y */
      hasFullyDisposedOf <- csvOptValue(Keys.hasFullyDisposedOf)
      /*  Z  */
      wasDisposalOfShares <- csvOptValue(Keys.wasDisposalOfShares)
      /*  AA */
      disposalOfSharesNumberHeld <- csvOptValue(Keys.disposalOfSharesNumberHeld)
    } yield RawTransactionDetail.create(
      row,
      firstName,
      lastName,
      memberDateOfBirth,
      memberNino,
      memberNinoReason,
      dateOfAcquisition,
      descriptionOfAsset,
      isAcquisitionOfShares,
      companyNameShares,
      companyCRNShares,
      companyNoCRNReasonShares,
      companyClassShares,
      companyNumberOfShares,
      acquiredFrom,
      totalCostOfAsset,
      isIndependentValuation,
      isFinanceAct,
      totalIncomeInTaxYear,
      areAnyDisposalsYear,
      disposalsAmount,
      namesOfPurchasers,
      areConnectedPartiesPurchasers,
      wasTransactionSupportedIndValuation,
      hasFullyDisposedOf,
      wasDisposalOfShares,
      disposalOfSharesNumberHeld
    )
  }

}
