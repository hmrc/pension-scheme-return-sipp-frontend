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
import play.api.libs.json._

object TangibleMoveablePropertyRaw {

  case class RawTransactionDetail(
    row: Int,
    firstNameOfSchemeMember: CsvValue[String],
    lastNameOfSchemeMember: CsvValue[String],
    memberDateOfBirth: CsvValue[String],
    memberNino: CsvValue[Option[String]],
    memberReasonNoNino: CsvValue[Option[String]],
    countOfTangiblePropertyTransactions: CsvValue[String],
    rawAsset: RawAsset
  )

  case class RawAsset(
    descriptionOfAsset: CsvValue[String],
    dateOfAcquisitionAsset: CsvValue[String],
    totalCostAsset: CsvValue[String],
    acquiredFrom: CsvValue[String],
    isTxSupportedByIndependentValuation: CsvValue[String],
    totalAmountIncomeReceiptsTaxYear: CsvValue[String],
    isTotalCostValueOrMarketValue: CsvValue[String],
    totalCostValueTaxYearAsset: CsvValue[String],
    rawDisposal: RawDisposal
  )

  case class RawDisposal(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    totalConsiderationAmountSaleIfAnyDisposal: CsvValue[Option[String]],
    purchaserNames: CsvValue[Option[String]],
    areAnyPurchasersConnected: CsvValue[Option[String]],
    isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
    isAnyPartAssetStillHeld: CsvValue[Option[String]]
  )

  case class RawPurchaser(
    name: CsvValue[Option[String]],
    connection: CsvValue[Option[String]]
  )

  object RawTransactionDetail {
    def create(
      row: Int,
      /* B */ firstNameOfSchemeMember: CsvValue[String],
      /* C */ lastNameOfSchemeMember: CsvValue[String],
      /* D */ memberDateOfBirth: CsvValue[String],
      /* E */ memberNino: CsvValue[Option[String]],
      /* F */ memberReasonNoNino: CsvValue[Option[String]],
      /* G */ countOfTangiblePropertyTransactions: CsvValue[String],
      /* H */ descriptionOfAsset: CsvValue[String],
      /* I */ dateOfAcquisitionAsset: CsvValue[String],
      /* J */ totalCostAsset: CsvValue[String],
      /* K */ whoAcquiredFromName: CsvValue[String],
      /* L */ isTxSupportedByIndependentValuation: CsvValue[String],
      /* M */ totalAmountIncomeReceiptsTaxYear: CsvValue[String],
      /* N */ isTotalCostValueOrMarketValue: CsvValue[String],
      /* O */ totalCostValueTaxYearAsset: CsvValue[String],
      /* P */ wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
      /* Q */ totalConsiderationAmountSaleIfAnyDisposal: CsvValue[Option[String]],
      /* R */ purchaserNames: CsvValue[Option[String]],
      /* S */ areAnyPurchasersConnected: CsvValue[Option[String]],
      /* T */ isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
      /* U */ isAnyPartAssetStillHeld: CsvValue[Option[String]]
    ): RawTransactionDetail = RawTransactionDetail(
      row,
      firstNameOfSchemeMember,
      lastNameOfSchemeMember,
      memberDateOfBirth,
      memberNino,
      memberReasonNoNino,
      countOfTangiblePropertyTransactions,
      RawAsset(
        descriptionOfAsset = descriptionOfAsset,
        dateOfAcquisitionAsset = dateOfAcquisitionAsset,
        totalCostAsset = totalCostAsset,
        acquiredFrom = whoAcquiredFromName,
        isTxSupportedByIndependentValuation = isTxSupportedByIndependentValuation,
        totalAmountIncomeReceiptsTaxYear = totalAmountIncomeReceiptsTaxYear,
        isTotalCostValueOrMarketValue = isTotalCostValueOrMarketValue,
        totalCostValueTaxYearAsset = totalCostValueTaxYearAsset,
        rawDisposal = RawDisposal(
          wereAnyDisposalOnThisDuringTheYear = wereAnyDisposalOnThisDuringTheYear,
          totalConsiderationAmountSaleIfAnyDisposal = totalConsiderationAmountSaleIfAnyDisposal,
          purchaserNames = purchaserNames,
          areAnyPurchasersConnected = areAnyPurchasersConnected,
          isTransactionSupportedByIndependentValuation = isTransactionSupportedByIndependentValuation,
          isAnyPartAssetStillHeld = isAnyPartAssetStillHeld
        )
      )
    )

    implicit class Ops(val raw: RawTransactionDetail) extends AnyVal {
      def toNonEmptyList: NonEmptyList[String] =
        NonEmptyList.of(
          raw.firstNameOfSchemeMember.value,
          raw.lastNameOfSchemeMember.value,
          raw.memberDateOfBirth.value,
          raw.memberNino.value.mkString,
          raw.memberReasonNoNino.value.mkString,
          raw.countOfTangiblePropertyTransactions.value,
          raw.rawAsset.descriptionOfAsset.value,
          raw.rawAsset.dateOfAcquisitionAsset.value,
          raw.rawAsset.totalCostAsset.value,
          raw.rawAsset.acquiredFrom.value,
          raw.rawAsset.isTxSupportedByIndependentValuation.value,
          raw.rawAsset.totalAmountIncomeReceiptsTaxYear.value,
          raw.rawAsset.isTotalCostValueOrMarketValue.value,
          raw.rawAsset.totalCostValueTaxYearAsset.value,
          raw.rawAsset.rawDisposal.wereAnyDisposalOnThisDuringTheYear.value,
          raw.rawAsset.rawDisposal.totalConsiderationAmountSaleIfAnyDisposal.value.getOrElse(""),
          raw.rawAsset.rawDisposal.purchaserNames.value.mkString,
          raw.rawAsset.rawDisposal.areAnyPurchasersConnected.value.mkString,
          raw.rawAsset.rawDisposal.isTransactionSupportedByIndependentValuation.value.getOrElse(""),
          raw.rawAsset.rawDisposal.isAnyPartAssetStillHeld.value.getOrElse("")
        )
    }

  }

  implicit val formatRawPurchaser: OFormat[RawPurchaser] = Json.format[RawPurchaser]
  implicit val formatRawDisposal: OFormat[RawDisposal] = Json.format[RawDisposal]
  implicit val formatRawAsset: OFormat[RawAsset] = Json.format[RawAsset]
  implicit val formatTransactionRawDetails: OFormat[RawTransactionDetail] = Json.format[RawTransactionDetail]
}
