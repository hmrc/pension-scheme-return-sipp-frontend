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

import models.requests.raw.AssetConnectedPartyRaw.RawTransactionDetail
import play.api.libs.json.{Json, OFormat}

object AssetConnectedPartyUpload {

  case class AssetConnectedPartyUpload(
    row: Int,
    firstNameOfSchemeMember: String,
    lastNameOfSchemeMember: String,
    memberDateOfBirth: String,
    memberNino: Option[String],
    memberNoNinoReason: Option[String],
    countOfTransactions: String,
    acquisitionDate: String,
    assetDescription: String,
    acquisitionOfShares: String,
    shareCompanyDetails: ShareCompanyDetails,
    acquiredFromName: String,
    totalCost: String,
    independentValuation: String,
    tangibleSchedule29A: String,
    totalIncomeOrReceipts: String,
    isAssetDisposed: String,
    disposal: Disposal
  )

  case class ShareCompanyDetails(
    companySharesName: String,
    companySharesCRN: Option[String],
    reasonNoCRN: Option[String],
    sharesClass: Option[String],
    noOfShares: Option[String]
  )

  case class Disposal(
    disposedPropertyProceedsAmt: Option[String],
    namesOfPurchasers: Option[String],
    areAnyPurchasersConnectedParty: Option[String],
    independentEvalTx: Option[String],
    disposalOfShares: Option[String],
    noOfSharesHeld: Option[String],
    fullyDisposed: Option[String]
  )

  def fromRaw(raw: RawTransactionDetail): AssetConnectedPartyUpload =
    AssetConnectedPartyUpload(
      row = raw.row,
      firstNameOfSchemeMember = raw.firstNameOfSchemeMember.value,
      lastNameOfSchemeMember = raw.lastNameOfSchemeMember.value,
      memberDateOfBirth = raw.memberDateOfBirth.value,
      memberNino = raw.memberNino.value,
      memberNoNinoReason = raw.memberNoNinoReason.value,
      countOfTransactions = raw.countOfTransactions.value,
      acquisitionDate = raw.acquisitionDate.value,
      assetDescription = raw.assetDescription.value,
      acquisitionOfShares = raw.acquisitionOfShares.value,
      shareCompanyDetails = ShareCompanyDetails(
        companySharesName = raw.shareCompanyDetails.companySharesName.value,
        companySharesCRN = raw.shareCompanyDetails.companySharesCRN.value,
        reasonNoCRN = raw.shareCompanyDetails.reasonNoCRN.value,
        sharesClass = raw.shareCompanyDetails.sharesClass.value,
        noOfShares = raw.shareCompanyDetails.noOfShares.value
      ),
      acquiredFromName = raw.acquiredFromName.value,
      totalCost = raw.totalCost.value,
      independentValuation = raw.independentValuation.value,
      tangibleSchedule29A = raw.tangibleSchedule29A.value,
      totalIncomeOrReceipts = raw.totalIncomeOrReceipts.value,
      isAssetDisposed = raw.isAssetDisposed.value,
      disposal = Disposal(
        disposedPropertyProceedsAmt = raw.rawDisposal.disposedPropertyProceedsAmt.value,
        namesOfPurchasers = raw.rawDisposal.namesOfPurchasers.value,
        areAnyPurchasersConnectedParty = raw.rawDisposal.areAnyPurchasersConnectedParty.value,
        independentEvalTx = raw.rawDisposal.independentEvalTx.value,
        disposalOfShares = raw.rawDisposal.disposalOfShares.value,
        noOfSharesHeld = raw.rawDisposal.noOfSharesHeld.value,
        fullyDisposed = raw.rawDisposal.fullyDisposed.value
      )
    )

  implicit val formatDisposal: OFormat[Disposal] = Json.format[Disposal]
  implicit val formatShareCompanyDetails: OFormat[ShareCompanyDetails] = Json.format[ShareCompanyDetails]
  implicit val formatAssetConnectedPartyUpload: OFormat[AssetConnectedPartyUpload] =
    Json.format[AssetConnectedPartyUpload]
}
