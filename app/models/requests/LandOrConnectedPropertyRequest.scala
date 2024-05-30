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
import models._
import models.requests.common._
import models.requests.psr.ReportDetails
import play.api.libs.json._
import CustomFormats._
import java.time.LocalDate

case class LandOrConnectedPropertyRequest(
  reportDetails: ReportDetails,
  transactions: Option[NonEmptyList[LandOrConnectedPropertyRequest.TransactionDetail]]
)

object LandOrConnectedPropertyRequest {

  case class TransactionDetail(
    row: Int,
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
    disposalDetails: Option[DisposalDetail]
  )

  implicit val formatTransactionDetails: OFormat[TransactionDetail] = Json.format[TransactionDetail]
  implicit val formatLandConnectedParty: OFormat[LandOrConnectedPropertyRequest] =
    Json.format[LandOrConnectedPropertyRequest]
}
