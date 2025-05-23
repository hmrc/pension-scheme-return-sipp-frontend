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

package models.requests.raw

import cats.data.NonEmptyList
import models.CsvValue
import play.api.libs.json.*

object AssetConnectedPartyRaw {

  case class RawTransactionDetail(
    row: Int,
    firstNameOfSchemeMember: CsvValue[String],
    lastNameOfSchemeMember: CsvValue[String],
    memberDateOfBirth: CsvValue[String],
    memberNino: CsvValue[Option[String]],
    memberNoNinoReason: CsvValue[Option[String]],
    acquisitionDate: CsvValue[String],
    assetDescription: CsvValue[String],
    acquisitionOfShares: CsvValue[String],
    shareCompanyDetails: RawShareCompanyDetails,
    acquiredFromName: CsvValue[String],
    totalCost: CsvValue[String],
    independentValuation: CsvValue[String],
    tangibleSchedule29A: CsvValue[String],
    totalIncomeOrReceipts: CsvValue[String],
    isAssetDisposed: CsvValue[String],
    rawDisposal: RawDisposal
  )

  case class RawShareCompanyDetails(
    companySharesName: CsvValue[Option[String]],
    companySharesCRN: CsvValue[Option[String]],
    reasonNoCRN: CsvValue[Option[String]],
    sharesClass: CsvValue[Option[String]],
    noOfShares: CsvValue[Option[String]]
  )

  case class RawDisposal(
    disposedPropertyProceedsAmt: CsvValue[Option[String]],
    namesOfPurchasers: CsvValue[Option[String]],
    areAnyPurchasersConnectedParty: CsvValue[Option[String]],
    independentValTx: CsvValue[Option[String]],
    hasFullyDisposed: CsvValue[Option[String]],
    disposalOfShares: CsvValue[Option[String]],
    noOfSharesHeld: CsvValue[Option[String]]
  )

  object RawTransactionDetail {
    def create(
      row: Int,
      /*  B */ firstNameOfSchemeMemberAssetConnectedParty: CsvValue[String],
      /*  C */ lastNameOfSchemeMemberAssetConnectedParty: CsvValue[String],
      /*  D */ memberDateOfBirthAssetConnectedParty: CsvValue[String],
      /*  E */ memberNinoAssetConnectedParty: CsvValue[Option[String]],
      /*  F */ memberNinoReasonAssetConnectedParty: CsvValue[Option[String]],
      /*  G */ dateOfAcquisitionAssetConnectedParty: CsvValue[String],
      /*  H */ descriptionOfAssetAssetConnectedParty: CsvValue[String],
      /*  I */ isSharesAssetConnectedParty: CsvValue[String],
      /*  J */ companyNameSharesAssetConnectedParty: CsvValue[Option[String]],
      /*  K */ companyCRNSharesAssetConnectedParty: CsvValue[Option[String]],
      /*  L */ companyNoCRNReasonSharesAssetConnectedParty: CsvValue[Option[String]],
      /*  M */ companyClassSharesAssetConnectedParty: CsvValue[Option[String]],
      /*  N */ companyNumberOfSharesAssetConnectedParty: CsvValue[Option[String]],
      /*  O */ acquiredFromAssetConnectedParty: CsvValue[String],
      /*  P */ totalCostOfAssetAssetConnectedParty: CsvValue[String],
      /*  Q */ isIndependentEvaluationConnectedParty: CsvValue[String],
      /*  R */ isFinanceActConnectedParty: CsvValue[String],
      /*  S */ totalIncomeInTaxYearConnectedParty: CsvValue[String],
      /*  T */ areAnyDisposalsYearConnectedParty: CsvValue[String],
      /*  U */ disposalsAmountConnectedParty: CsvValue[Option[String]],
      /*  V */ namesOfPurchasersConnectedParty: CsvValue[Option[String]],
      /*  W */ areConnectedPartiesPurchasersConnectedParty: CsvValue[Option[String]],
      /*  X */ wasTransactionSupportedIndValuationConnectedParty: CsvValue[Option[String]],
      /*  Y */ hasFullyDisposedOf: CsvValue[Option[String]],
      /*  Z */ wasThereADisposalOfShares: CsvValue[Option[String]],
      /*  AA */ disposalOfSharesNumberHeld: CsvValue[Option[String]]
    ): RawTransactionDetail = RawTransactionDetail(
      row,
      firstNameOfSchemeMemberAssetConnectedParty,
      lastNameOfSchemeMemberAssetConnectedParty,
      memberDateOfBirthAssetConnectedParty,
      memberNinoAssetConnectedParty,
      memberNinoReasonAssetConnectedParty,
      dateOfAcquisitionAssetConnectedParty,
      descriptionOfAssetAssetConnectedParty,
      isSharesAssetConnectedParty,
      RawShareCompanyDetails(
        companyNameSharesAssetConnectedParty,
        companyCRNSharesAssetConnectedParty,
        companyNoCRNReasonSharesAssetConnectedParty,
        companyClassSharesAssetConnectedParty,
        companyNumberOfSharesAssetConnectedParty
      ),
      acquiredFromAssetConnectedParty,
      totalCostOfAssetAssetConnectedParty,
      isIndependentEvaluationConnectedParty,
      isFinanceActConnectedParty,
      totalIncomeInTaxYearConnectedParty,
      areAnyDisposalsYearConnectedParty,
      RawDisposal(
        disposalsAmountConnectedParty,
        namesOfPurchasersConnectedParty,
        areConnectedPartiesPurchasersConnectedParty,
        wasTransactionSupportedIndValuationConnectedParty,
        hasFullyDisposedOf,
        wasThereADisposalOfShares,
        disposalOfSharesNumberHeld
      )
    )

    implicit class Ops(val raw: RawTransactionDetail) extends AnyVal {
      def toNonEmptyList: NonEmptyList[String] =
        NonEmptyList.of(
          raw.firstNameOfSchemeMember.value,
          raw.lastNameOfSchemeMember.value,
          raw.memberDateOfBirth.value,
          raw.memberNino.value.getOrElse(""),
          raw.memberNoNinoReason.value.getOrElse(""),
          raw.acquisitionDate.value,
          raw.assetDescription.value,
          raw.acquisitionOfShares.value,
          raw.shareCompanyDetails.companySharesName.value.getOrElse(""),
          raw.shareCompanyDetails.companySharesCRN.value.getOrElse(""),
          raw.shareCompanyDetails.reasonNoCRN.value.getOrElse(""),
          raw.shareCompanyDetails.sharesClass.value.getOrElse(""),
          raw.shareCompanyDetails.noOfShares.value.getOrElse(""),
          raw.acquiredFromName.value,
          raw.totalCost.value,
          raw.independentValuation.value,
          raw.tangibleSchedule29A.value,
          raw.totalIncomeOrReceipts.value,
          raw.isAssetDisposed.value,
          raw.rawDisposal.disposedPropertyProceedsAmt.value.getOrElse(""),
          raw.rawDisposal.namesOfPurchasers.value.getOrElse(""),
          raw.rawDisposal.areAnyPurchasersConnectedParty.value.getOrElse(""),
          raw.rawDisposal.independentValTx.value.getOrElse(""),
          raw.rawDisposal.hasFullyDisposed.value.getOrElse(""),
          raw.rawDisposal.disposalOfShares.value.getOrElse(""),
          raw.rawDisposal.noOfSharesHeld.value.getOrElse("")
        )

    }

  }

  implicit val formatRawShareCompanyDetails: OFormat[RawShareCompanyDetails] = Json.format[RawShareCompanyDetails]
  implicit val formatRawDisposal: OFormat[RawDisposal] = Json.format[RawDisposal]
  implicit val formatTransactionRawDetails: OFormat[RawTransactionDetail] = Json.format[RawTransactionDetail]
}
