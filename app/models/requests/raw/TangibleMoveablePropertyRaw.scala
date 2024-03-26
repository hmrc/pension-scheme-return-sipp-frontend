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

import models.CsvValue
import play.api.libs.json._

object TangibleMoveablePropertyRaw {

  case class RawAcquiredFrom(
    whoAcquiredFromName: CsvValue[String],
    acquiredFromType: CsvValue[String],
    acquirerNinoForIndividual: CsvValue[Option[String]],
    acquirerCrnForCompany: CsvValue[Option[String]],
    acquirerUtrForPartnership: CsvValue[Option[String]],
    whoAcquiredFromTypeReasonAsset: CsvValue[Option[String]]
  )

  case class RawTransactionDetail(
    row: Int,
    firstNameOfSchemeMember: CsvValue[String],
    lastNameOfSchemeMember: CsvValue[String],
    memberDateOfBirth: CsvValue[String],
    countOfTangiblePropertyTransactions: CsvValue[String],
    rawAsset: RawAsset
  )

  case class RawAsset(
    descriptionOfAsset: CsvValue[String],
    dateOfAcquisitionAsset: CsvValue[String],
    totalCostAsset: CsvValue[String],
    rawAcquiredFromType: RawAcquiredFrom,
    isTxSupportedByIndependentValuation: CsvValue[String],
    totalAmountIncomeReceiptsTaxYear: CsvValue[String],
    isTotalCostValueOrMarketValue: CsvValue[String],
    totalCostValueTaxYearAsset: CsvValue[String],
    rawDisposal: RawDisposal
  )

  case class RawDisposal(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    totalConsiderationAmountSaleIfAnyDisposal: CsvValue[Option[String]],
    wereAnyDisposals: CsvValue[String],
    first: RawPurchaser,
    second: RawPurchaser,
    third: RawPurchaser,
    fourth: RawPurchaser,
    fifth: RawPurchaser,
    sixth: RawPurchaser,
    seventh: RawPurchaser,
    eighth: RawPurchaser,
    ninth: RawPurchaser,
    tenth: RawPurchaser,
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
      /*  B */ firstNameOfSchemeMember: CsvValue[String],
      /*  C */ lastNameOfSchemeMember: CsvValue[String],
      /*  D */ memberDateOfBirth: CsvValue[String],
      /*  E */ countOfTangiblePropertyTransactions: CsvValue[String],
      /*  F */ descriptionOfAsset: CsvValue[String],
      /*  G */ dateOfAcquisitionAsset: CsvValue[String],
      /*  H */ totalCostAsset: CsvValue[String],
      /*  I */ whoAcquiredFromName: CsvValue[String],
      /*  J */ acquiredFromType: CsvValue[String],
      /*  K */ acquirerNinoForIndividual: CsvValue[Option[String]],
      /*  L */ acquirerCrnForCompany: CsvValue[Option[String]],
      /*  M */ acquirerUtrForPartnership: CsvValue[Option[String]],
      /*  N */ whoAcquiredFromTypeReasonAsset: CsvValue[Option[String]],
      /*  O */ isTxSupportedByIndependentValuation: CsvValue[String],
      /*  P */ totalAmountIncomeReceiptsTaxYear: CsvValue[String],
      /*  Q */ isTotalCostValueOrMarketValue: CsvValue[String],
      /*  R */ totalCostValueTaxYearAsset: CsvValue[String],
      /*  S */ wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
      /*  T */ totalConsiderationAmountSaleIfAnyDisposal: CsvValue[Option[String]],
      /*  U */ wereAnyDisposals: CsvValue[String],
      /*  V ... AO */
      purchaser1: CsvValue[Option[String]],
      connection1: CsvValue[Option[String]],
      purchaser2: CsvValue[Option[String]],
      connection2: CsvValue[Option[String]],
      purchaser3: CsvValue[Option[String]],
      connection3: CsvValue[Option[String]],
      purchaser4: CsvValue[Option[String]],
      connection4: CsvValue[Option[String]],
      purchaser5: CsvValue[Option[String]],
      connection5: CsvValue[Option[String]],
      purchaser6: CsvValue[Option[String]],
      connection6: CsvValue[Option[String]],
      purchaser7: CsvValue[Option[String]],
      connection7: CsvValue[Option[String]],
      purchaser8: CsvValue[Option[String]],
      connection8: CsvValue[Option[String]],
      purchaser9: CsvValue[Option[String]],
      connection9: CsvValue[Option[String]],
      purchaser10: CsvValue[Option[String]],
      connection10: CsvValue[Option[String]],
      /*  AP */ isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
      /*  AQ */ isAnyPartAssetStillHeld: CsvValue[Option[String]]
    ): RawTransactionDetail = RawTransactionDetail(
      row,
      firstNameOfSchemeMember,
      lastNameOfSchemeMember,
      memberDateOfBirth,
      countOfTangiblePropertyTransactions,
      RawAsset(
        descriptionOfAsset = descriptionOfAsset,
        dateOfAcquisitionAsset = dateOfAcquisitionAsset,
        totalCostAsset = totalCostAsset,
        rawAcquiredFromType = RawAcquiredFrom(
          whoAcquiredFromName = whoAcquiredFromName,
          acquiredFromType = acquiredFromType,
          acquirerNinoForIndividual = acquirerNinoForIndividual,
          acquirerCrnForCompany = acquirerCrnForCompany,
          acquirerUtrForPartnership = acquirerUtrForPartnership,
          whoAcquiredFromTypeReasonAsset = whoAcquiredFromTypeReasonAsset
        ),
        isTxSupportedByIndependentValuation = isTxSupportedByIndependentValuation,
        totalAmountIncomeReceiptsTaxYear = totalAmountIncomeReceiptsTaxYear,
        isTotalCostValueOrMarketValue = isTotalCostValueOrMarketValue,
        totalCostValueTaxYearAsset = totalCostValueTaxYearAsset,
        rawDisposal = RawDisposal(
          wereAnyDisposalOnThisDuringTheYear = wereAnyDisposalOnThisDuringTheYear,
          totalConsiderationAmountSaleIfAnyDisposal = totalConsiderationAmountSaleIfAnyDisposal,
          wereAnyDisposals = wereAnyDisposals,
          first = RawPurchaser(purchaser1, connection1),
          second = RawPurchaser(purchaser2, connection2),
          third = RawPurchaser(purchaser3, connection3),
          fourth = RawPurchaser(purchaser4, connection4),
          fifth = RawPurchaser(purchaser5, connection5),
          sixth = RawPurchaser(purchaser6, connection6),
          seventh = RawPurchaser(purchaser7, connection7),
          eighth = RawPurchaser(purchaser8, connection8),
          ninth = RawPurchaser(purchaser9, connection9),
          tenth = RawPurchaser(purchaser10, connection10),
          isTransactionSupportedByIndependentValuation = isTransactionSupportedByIndependentValuation,
          isAnyPartAssetStillHeld = isAnyPartAssetStillHeld
        )
      )
    )

  }

  implicit val formatRawAcquiredFrom: OFormat[RawAcquiredFrom] = Json.format[RawAcquiredFrom]
  implicit val formatRawPurchaser: OFormat[RawPurchaser] = Json.format[RawPurchaser]
  implicit val formatRawDisposal: OFormat[RawDisposal] = Json.format[RawDisposal]
  implicit val formatRawAsset: OFormat[RawAsset] = Json.format[RawAsset]
  implicit val formatTransactionRawDetails: OFormat[RawTransactionDetail] = Json.format[RawTransactionDetail]
}
