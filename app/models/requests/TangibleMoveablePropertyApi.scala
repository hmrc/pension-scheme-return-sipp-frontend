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

package models.requests

import cats.data.NonEmptyList
import models.*
import models.requests.common.*
import models.requests.psr.ReportDetails
import CustomFormats.*
import play.api.libs.json.*
import controllers.DownloadCsvController.RowEncoder

import java.time.LocalDate

case class TangibleMoveablePropertyRequest(
  reportDetails: ReportDetails,
  transactions: Option[NonEmptyList[TangibleMoveablePropertyApi.TransactionDetail]]
)

case class TangibleMoveablePropertyResponse(
  transactions: List[TangibleMoveablePropertyApi.TransactionDetail]
)

object TangibleMoveablePropertyApi {

  case class TransactionDetail(
    row: Option[Int],
    nameDOB: NameDOB,
    nino: NinoType,
    assetDescription: String,
    acquisitionDate: LocalDate,
    totalCost: Double,
    acquiredFromName: String,
    independentValuation: YesNo,
    totalIncomeOrReceipts: Double,
    costMarketValue: Double,
    costOrMarket: CostOrMarketType,
    isPropertyDisposed: YesNo,
    disposalDetails: Option[DisposalDetails],
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
  implicit val formatTangibleRequest: OFormat[TangibleMoveablePropertyRequest] =
    Json.format[TangibleMoveablePropertyRequest]
  implicit val formatTangibleResponse: OFormat[TangibleMoveablePropertyResponse] =
    Json.format[TangibleMoveablePropertyResponse]

  implicit val tangibleTrxDetailRowEncoder: RowEncoder[TransactionDetail] = { trx =>
    NonEmptyList.of(
      "",
      trx.nameDOB.firstName,
      trx.nameDOB.lastName,
      trx.nameDOB.dob.toString,
      trx.nino.nino.mkString,
      trx.nino.reasonNoNino.mkString,
      trx.assetDescription,
      trx.acquisitionDate.format(CSV_DATE_TIME),
      trx.totalCost.toString,
      trx.acquiredFromName,
      trx.independentValuation.toString,
      trx.totalIncomeOrReceipts.toString,
      trx.costMarketValue.toString,
      trx.costOrMarket.toString,
      trx.isPropertyDisposed.toString,
      trx.disposalDetails.map(_.disposedPropertyProceedsAmt.toString).getOrElse(""),
      trx.disposalDetails.map(_.purchasersNames).getOrElse(""),
      trx.disposalDetails.map(_.anyPurchaserConnectedParty.toString).getOrElse(""),
      trx.disposalDetails.map(_.independentValuationDisposal.toString).getOrElse(""),
      trx.disposalDetails.map(_.propertyFullyDisposed.toString).getOrElse("")
    )
  }
}
