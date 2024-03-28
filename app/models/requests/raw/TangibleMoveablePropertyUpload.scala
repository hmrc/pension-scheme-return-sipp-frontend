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

import models.requests.raw.TangibleMoveablePropertyRaw.RawTransactionDetail
import play.api.libs.json._

//TODO: Add more strict types for the TangibleMoveablePropertyUpload
object TangibleMoveablePropertyUpload {

  case class AcquiredFrom(
    whoAcquiredFromName: String,
    acquiredFromType: String,
    acquirerNinoForIndividual: Option[String],
    acquirerCrnForCompany: Option[String],
    acquirerUtrForPartnership: Option[String],
    whoAcquiredFromTypeReasonAsset: Option[String]
  )

  case class Purchaser(
    name: Option[String],
    connection: Option[String]
  )

  case class Disposal(
    wereAnyDisposalOnThisDuringTheYear: String,
    totalConsiderationAmountSaleIfAnyDisposal: Option[String],
    wereAnyDisposals: String,
    purchaserDetails: List[Purchaser],
    isTransactionSupportedByIndependentValuation: Option[String],
    isAnyPartAssetStillHeld: Option[String]
  )

  case class Asset(
    descriptionOfAsset: String,
    dateOfAcquisitionAsset: String,
    totalCostAsset: String,
    acquiredFromType: AcquiredFrom,
    isTxSupportedByIndependentValuation: String,
    totalAmountIncomeReceiptsTaxYear: String,
    isTotalCostValueOrMarketValue: String,
    totalCostValueTaxYearAsset: String,
    disposal: Disposal
  )

  case class TangibleMoveablePropertyUpload(
    firstNameOfSchemeMember: String,
    lastNameOfSchemeMember: String,
    memberDateOfBirth: String,
    countOfTangiblePropertyTransactions: String,
    asset: Asset
  )

  def fromRaw(raw: RawTransactionDetail) =
    TangibleMoveablePropertyUpload(
      raw.firstNameOfSchemeMember.value,
      raw.lastNameOfSchemeMember.value,
      raw.memberDateOfBirth.value,
      raw.countOfTangiblePropertyTransactions.value,
      Asset(
        descriptionOfAsset = raw.rawAsset.descriptionOfAsset.value,
        dateOfAcquisitionAsset = raw.rawAsset.dateOfAcquisitionAsset.value,
        totalCostAsset = raw.rawAsset.totalCostAsset.value,
        acquiredFromType = AcquiredFrom(
          whoAcquiredFromName = raw.rawAsset.rawAcquiredFromType.whoAcquiredFromName.value,
          acquiredFromType = raw.rawAsset.rawAcquiredFromType.acquiredFromType.value,
          acquirerNinoForIndividual = raw.rawAsset.rawAcquiredFromType.acquirerNinoForIndividual.value,
          acquirerCrnForCompany = raw.rawAsset.rawAcquiredFromType.acquirerCrnForCompany.value,
          acquirerUtrForPartnership = raw.rawAsset.rawAcquiredFromType.acquirerUtrForPartnership.value,
          whoAcquiredFromTypeReasonAsset = raw.rawAsset.rawAcquiredFromType.whoAcquiredFromTypeReasonAsset.value
        ),
        isTxSupportedByIndependentValuation = raw.rawAsset.isTxSupportedByIndependentValuation.value,
        totalAmountIncomeReceiptsTaxYear = raw.rawAsset.totalAmountIncomeReceiptsTaxYear.value,
        isTotalCostValueOrMarketValue = raw.rawAsset.isTotalCostValueOrMarketValue.value,
        totalCostValueTaxYearAsset = raw.rawAsset.totalCostValueTaxYearAsset.value,
        disposal = Disposal(
          wereAnyDisposalOnThisDuringTheYear = raw.rawAsset.rawDisposal.wereAnyDisposalOnThisDuringTheYear.value,
          totalConsiderationAmountSaleIfAnyDisposal =
            raw.rawAsset.rawDisposal.totalConsiderationAmountSaleIfAnyDisposal.value,
          wereAnyDisposals = raw.rawAsset.rawDisposal.wereAnyDisposals.value,
          purchaserDetails = List(
            Purchaser(raw.rawAsset.rawDisposal.first.name.value, raw.rawAsset.rawDisposal.first.connection.value),
            Purchaser(raw.rawAsset.rawDisposal.second.name.value, raw.rawAsset.rawDisposal.second.connection.value),
            Purchaser(raw.rawAsset.rawDisposal.third.name.value, raw.rawAsset.rawDisposal.third.connection.value),
            Purchaser(raw.rawAsset.rawDisposal.fourth.name.value, raw.rawAsset.rawDisposal.fourth.connection.value),
            Purchaser(raw.rawAsset.rawDisposal.fifth.name.value, raw.rawAsset.rawDisposal.fifth.connection.value),
            Purchaser(raw.rawAsset.rawDisposal.sixth.name.value, raw.rawAsset.rawDisposal.sixth.connection.value),
            Purchaser(raw.rawAsset.rawDisposal.seventh.name.value, raw.rawAsset.rawDisposal.seventh.connection.value),
            Purchaser(raw.rawAsset.rawDisposal.eighth.name.value, raw.rawAsset.rawDisposal.eighth.connection.value),
            Purchaser(raw.rawAsset.rawDisposal.ninth.name.value, raw.rawAsset.rawDisposal.ninth.connection.value),
            Purchaser(raw.rawAsset.rawDisposal.tenth.name.value, raw.rawAsset.rawDisposal.tenth.connection.value)
          ).filter(p => p.name.isEmpty && p.connection.isEmpty),
          isTransactionSupportedByIndependentValuation =
            raw.rawAsset.rawDisposal.isTransactionSupportedByIndependentValuation.value,
          isAnyPartAssetStillHeld = raw.rawAsset.rawDisposal.isAnyPartAssetStillHeld.value
        )
      )
    )

  implicit val formatAcquiredFrom: OFormat[AcquiredFrom] = Json.format[AcquiredFrom]
  implicit val formatPurchaser: OFormat[Purchaser] = Json.format[Purchaser]
  implicit val formatDisposal: OFormat[Disposal] = Json.format[Disposal]
  implicit val formatAsset: OFormat[Asset] = Json.format[Asset]
  implicit val formatTangibleMoveablePropertyUpload: OFormat[TangibleMoveablePropertyUpload] =
    Json.format[TangibleMoveablePropertyUpload]
}
