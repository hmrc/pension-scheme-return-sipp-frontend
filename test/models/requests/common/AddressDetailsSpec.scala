/*
 * Copyright 2025 HM Revenue & Customs
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

import models.UKAddress
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class AddressDetailsSpec extends AnyFreeSpec with Matchers {

  private val addressMappings = List(
    UKAddress(line1 = "line1", line2 = None, line3 = None, city = "London", postcode = "PO57CD") ->
      AddressDetails("line1", "London", None, None, None, Some("PO57CD"), "GB"),

    UKAddress(line1 = "line1", line2 = Some("line2"), line3 = None, city = "London", postcode = "PO57CD") ->
      AddressDetails("line1", "line2", None, Some("London"), None, Some("PO57CD"), "GB"),

    UKAddress(line1 = "line1", line2 = Some("line2"), line3 = Some("line3"), city = "London", postcode = "PO57CD") ->
      AddressDetails("line1", "line2", Some("line3"), Some("London"), None, Some("PO57CD"), "GB"),

    UKAddress(line1 = "line1", line2 = None, line3 = Some("line3"), city = "London", postcode = "PO57CD") ->
      AddressDetails("line1", "line3", None, Some("London"), None, Some("PO57CD"), "GB")
  )

  "uploadAddressToRequestAddressDetails" - {
    "should correctly map UK addresses to AddressDetails" in {
      addressMappings.foreach { case (input, expectedOutput) =>
        val (yesNo, result) = AddressDetails.uploadAddressToRequestAddressDetails(input)

        yesNo mustBe YesNo.Yes
        result mustBe expectedOutput
      }
    }
  }
}