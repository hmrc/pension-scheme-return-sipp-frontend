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
import play.api.libs.json.*

object LandOrConnectedPropertyRaw {

  case class RawAddressDetail(
    isLandOrPropertyInUK: CsvValue[String],
    landOrPropertyUkAddressLine1: CsvValue[Option[String]],
    landOrPropertyUkAddressLine2: CsvValue[Option[String]],
    landOrPropertyUkAddressLine3: CsvValue[Option[String]],
    landOrPropertyUkTownOrCity: CsvValue[Option[String]],
    landOrPropertyUkPostCode: CsvValue[Option[String]],
    landOrPropertyAddressLine1: CsvValue[Option[String]],
    landOrPropertyAddressLine2: CsvValue[Option[String]],
    landOrPropertyAddressLine3: CsvValue[Option[String]],
    landOrPropertyAddressLine4: CsvValue[Option[String]],
    landOrPropertyCountry: CsvValue[Option[String]]
  )

  case class RawJointlyHeld(
    isPropertyHeldJointly: CsvValue[String],
    whatPercentageOfPropertyOwnedByMember: CsvValue[Option[String]]
  )

  case class RawDisposal(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    totalSaleProceedIfAnyDisposal: CsvValue[Option[String]],
    nameOfPurchasers: CsvValue[Option[String]],
    isAnyPurchaserConnected: CsvValue[Option[String]],
    isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
    hasLandOrPropertyFullyDisposedOf: CsvValue[Option[String]]
  )

  implicit val formatRawAddressDetails: OFormat[RawAddressDetail] = Json.format[RawAddressDetail]
  implicit val formatRawJointlyHeld: OFormat[RawJointlyHeld] = Json.format[RawJointlyHeld]
  implicit val formatRawDisposal: OFormat[RawDisposal] = Json.format[RawDisposal]
}
