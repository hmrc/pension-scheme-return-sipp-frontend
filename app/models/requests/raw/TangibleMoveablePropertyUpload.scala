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

import models.{Money, NameDOB, NinoType}
import models.requests.YesNo
import models.requests.common.{CostValueOrMarketValueType, DisposalDetail}
import play.api.libs.json._

import java.time.LocalDate

object TangibleMoveablePropertyUpload {

  case class Asset(
    descriptionOfAsset: String,
    dateOfAcquisitionAsset: LocalDate,
    totalCostAsset: Money,
    acquiredFrom: String,
    isTxSupportedByIndependentValuation: YesNo,
    totalAmountIncomeReceiptsTaxYear: Money,
    isTotalCostValueOrMarketValue: CostValueOrMarketValueType,
    totalCostValueTaxYearAsset: Money,
    wereAnyDisposalOnThisDuringTheYear: YesNo,
    disposal: Option[DisposalDetail]
  )

  case class TangibleMoveablePropertyUpload(
    nameDob: NameDOB,
    nino: NinoType,
    countOfTangiblePropertyTransactions: Int,
    asset: Asset
  )

  implicit val formatAsset: OFormat[Asset] = Json.format[Asset]
  implicit val formatTangibleMoveablePropertyUpload: OFormat[TangibleMoveablePropertyUpload] =
    Json.format[TangibleMoveablePropertyUpload]
}
