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

package models.requests.common

import play.api.libs.json.{Json, OFormat}
import models.{ROWAddress, UKAddress, UploadAddress}

case class AddressDetails(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  addressLine5: Option[String],
  ukPostCode: Option[String],
  countryCode: String
)

object AddressDetails {
  def uploadAddressToRequestAddressDetails(address: UploadAddress): (YesNo, AddressDetails) =
    address match {
      case UKAddress(line1, line2, line3, city, postcode) =>
        (YesNo.Yes, AddressDetails(line1, line2, line3, city, None, Some(postcode), "GB"))
      case ROWAddress(line1, line2, line3, line4, country) =>
        (YesNo.No, AddressDetails(line1, Some(line2), line3, line4, None, None, country))
    }

  implicit val format: OFormat[AddressDetails] = Json.format[AddressDetails]
}
