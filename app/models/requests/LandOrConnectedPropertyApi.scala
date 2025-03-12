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
import play.api.libs.json.*
import CustomFormats.*
import controllers.DownloadCsvController.RowEncoder

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
    landOrPropertyInUK: YesNo,
    addressDetails: AddressDetails,
    registryDetails: RegistryDetails,
    acquiredFromName: String,
    totalCost: Double,
    independentValuation: YesNo,
    jointlyHeld: YesNo,
    noOfPersons: Option[Int],
    residentialSchedule29A: YesNo,
    isLeased: YesNo,
    lesseeDetails: Option[LesseeDetails],
    totalIncomeOrReceipts: Double,
    isPropertyDisposed: YesNo,
    disposalDetails: Option[DisposalDetails],
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
  implicit val formatLandConnectedRequest: OFormat[LandOrConnectedPropertyRequest] =
    Json.format[LandOrConnectedPropertyRequest]
  implicit val formatLandConnectedResponse: OFormat[LandOrConnectedPropertyResponse] =
    Json.format[LandOrConnectedPropertyResponse]

  implicit val landOrConnectedPropertyApiCsvRowEncoder: RowEncoder[TransactionDetail] = { tx =>
    NonEmptyList.of(
      "",
      tx.nameDOB.firstName,
      tx.nameDOB.lastName,
      tx.nameDOB.dob.toString,
      tx.nino.nino.mkString,
      tx.nino.reasonNoNino.mkString,
      tx.acquisitionDate.format(CSV_DATE_TIME),
      tx.landOrPropertyInUK.toString,
      if (tx.landOrPropertyInUK == YesNo.Yes) tx.addressDetails.addressLine1 else "",
      if (tx.landOrPropertyInUK == YesNo.Yes) tx.addressDetails.addressLine2 else "",
      if (tx.landOrPropertyInUK == YesNo.Yes) tx.addressDetails.addressLine3.mkString else "",
      if (tx.landOrPropertyInUK == YesNo.Yes) tx.addressDetails.addressLine4.mkString else "",
      if (tx.landOrPropertyInUK == YesNo.Yes) tx.addressDetails.ukPostCode.mkString else "",
      if (tx.landOrPropertyInUK == YesNo.No) tx.addressDetails.addressLine1 else "",
      if (tx.landOrPropertyInUK == YesNo.No) tx.addressDetails.addressLine2 else "",
      if (tx.landOrPropertyInUK == YesNo.No) tx.addressDetails.addressLine3.mkString else "",
      if (tx.landOrPropertyInUK == YesNo.No) tx.addressDetails.addressLine4.mkString else "",
      if (tx.landOrPropertyInUK == YesNo.No) tx.addressDetails.countryCode else "",
      tx.registryDetails.registryRefExist.toString,
      tx.registryDetails.noRegistryRefReason.mkString,
      tx.acquiredFromName,
      tx.totalCost.toString,
      tx.independentValuation.toString,
      tx.jointlyHeld.toString,
      tx.noOfPersons.mkString,
      tx.residentialSchedule29A.toString,
      tx.isLeased.toString,
      tx.lesseeDetails.map(_.numberOfLessees).mkString,
      tx.lesseeDetails.map(_.anyLesseeConnectedParty).mkString,
      tx.lesseeDetails.map(_.leaseGrantedDate.format(CSV_DATE_TIME)).mkString,
      tx.lesseeDetails.map(_.annualLeaseAmount).mkString,
      tx.totalIncomeOrReceipts.toString,
      tx.isPropertyDisposed.toString,
      tx.disposalDetails.map(_.disposedPropertyProceedsAmt).mkString,
      tx.disposalDetails.map(_.purchasersNames).mkString,
      tx.disposalDetails.map(_.anyPurchaserConnectedParty).mkString,
      tx.disposalDetails.map(_.independentValuationDisposal).mkString,
      tx.disposalDetails.map(_.propertyFullyDisposed).mkString
    )
  }

}
