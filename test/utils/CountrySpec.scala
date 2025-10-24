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

package utils

class CountrySpec extends BaseSpec {

  "Country getCountry" - {
    "return a country from a country code" in {
      Country.getCountry("AD") must be(Some("andorra"))
      Country.getCountry("FR") must be(Some("france"))
      Country.getCountry("DE") must be(Some("germany"))
      Country.getCountry("TR") must be(Some("turkey"))

      Country.getCountry("ZZ") must be(None)
    }
  }

  "Country getCountryCode" - {
    "return a country code from a country" in {
      Country.getCountryCode("andorra") must be(Some("AD"))
      Country.getCountryCode("germany") must be(Some("DE"))
      Country.getCountryCode("france") must be(Some("FR"))
      Country.getCountryCode("turkey") must be(Some("TR"))

      Country.getCountryCode("ZZ") must be(None)
    }
  }
}
