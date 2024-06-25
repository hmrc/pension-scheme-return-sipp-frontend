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
import fs2.data.csv.RowEncoder
import models._
import models.requests.common._
import models.requests.psr.ReportDetails
import play.api.libs.json._
import CustomFormats._

import java.time.LocalDate

case class LandOrConnectedPropertyRequest(
  reportDetails: ReportDetails,
  transactions: Option[NonEmptyList[LandOrConnectedPropertyApi.TransactionDetail]]
)

case class LandOrConnectedPropertyResponse(
  transactions: List[LandOrConnectedPropertyApi.TransactionDetail]
)

object LandOrConnectedPropertyApi {

  case class TransactionDetail(
    row: Option[Int],
    nameDOB: NameDOB,
    nino: NinoType,
    acquisitionDate: LocalDate,
    landOrPropertyinUK: YesNo,
    addressDetails: AddressDetails,
    registryDetails: RegistryDetails,
    acquiredFromName: String,
    totalCost: Double,
    independentValuation: YesNo,
    jointlyHeld: YesNo,
    noOfPersons: Option[Int],
    residentialSchedule29A: YesNo,
    isLeased: YesNo,
    lesseeDetails: Option[LesseeDetail],
    totalIncomeOrReceipts: Double,
    isPropertyDisposed: YesNo,
    disposalDetails: Option[DisposalDetail],
    transactionCount: Option[Int] = None // TODO -> Should not be needed! In Backend side we are counting with transactions.length
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
  implicit val formatLandConnectedPartyReq: OFormat[LandOrConnectedPropertyRequest] =
    Json.format[LandOrConnectedPropertyRequest]
  implicit val formatLandConnectedPartyRes: OFormat[LandOrConnectedPropertyResponse] =
    Json.format[LandOrConnectedPropertyResponse]


  implicit val landOrConnectedPropertyApiCsvRowEncoder: RowEncoder[TransactionDetail] = RowEncoder.instance { tx =>
    NonEmptyList.of(
      "",
      tx.nameDOB.firstName,
      tx.nameDOB.lastName,
      tx.nameDOB.dob.toString,
      tx.nino.nino.getOrElse(""),
      tx.nino.reasonNoNino.getOrElse(""),
      tx.transactionCount.map(_.toString).getOrElse(""),
      tx.acquisitionDate.toString,
      tx.landOrPropertyinUK.toString,
      if (tx.landOrPropertyinUK == YesNo.Yes) tx.addressDetails.addressLine1 else "",
      if (tx.landOrPropertyinUK == YesNo.Yes) tx.addressDetails.addressLine2.getOrElse("") else "",
      if (tx.landOrPropertyinUK == YesNo.Yes) tx.addressDetails.addressLine3.getOrElse("") else "",
      if (tx.landOrPropertyinUK == YesNo.Yes) tx.addressDetails.addressLine4.getOrElse("") else "",
      if (tx.landOrPropertyinUK == YesNo.Yes) tx.addressDetails.ukPostCode.getOrElse("") else "",
      if (tx.landOrPropertyinUK == YesNo.No) tx.addressDetails.addressLine1 else "",
      if (tx.landOrPropertyinUK == YesNo.No) tx.addressDetails.addressLine2.getOrElse("") else "",
      if (tx.landOrPropertyinUK == YesNo.No) tx.addressDetails.addressLine3.getOrElse("") else "",
      if (tx.landOrPropertyinUK == YesNo.No) tx.addressDetails.addressLine4.getOrElse("") else "",
      if (tx.landOrPropertyinUK == YesNo.No) tx.addressDetails.countryCode else "",
      tx.registryDetails.registryRefExist.toString,
      tx.registryDetails.noRegistryRefReason.getOrElse(""),
      tx.acquiredFromName,
      tx.totalCost.toString,
      tx.independentValuation.toString,
      tx.noOfPersons.map(_.toString).getOrElse(""),
      tx.residentialSchedule29A.toString,
      tx.isLeased.toString,
      tx.lesseeDetails.flatMap(_.countOfLessees.map(_.toString)).getOrElse(""),
      tx.lesseeDetails.map(_.anyOfLesseesConnected.toString).getOrElse(""),
      tx.lesseeDetails.map(_.leaseGrantedDate.toString).getOrElse(""),
      tx.lesseeDetails.map(_.annualLeaseAmount.toString).getOrElse(""),
      tx.totalIncomeOrReceipts.toString,
      tx.isPropertyDisposed.toString,
      tx.disposalDetails.map(_.disposedPropertyProceedsAmt.toString).getOrElse(""),
      tx.disposalDetails.map(_.namesOfPurchasers).getOrElse(""),
      tx.disposalDetails.map(_.anyPurchaserConnected.toString).getOrElse(""),
      tx.disposalDetails.map(_.independentValuationDisposal.toString).getOrElse(""),
      tx.disposalDetails.map(_.propertyFullyDisposed.toString).getOrElse("")
    )
  }

}
