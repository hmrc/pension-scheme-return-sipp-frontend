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

import models.SelectInput
import play.api.libs.json.Json

import java.io.{File, FileInputStream, InputStream}
import scala.io.Source

case class Country(countryCode: String, country: String)

object Country {

  private val locationCanonicalList = "location-autocomplete-canonical-list.json"

  lazy val countries: List[SelectInput] = {
    val jsonFile = File(getClass.getClassLoader.getResource(locationCanonicalList).toURI.getPath)
    val inputStream = FileInputStream(jsonFile)
    val locationJsValue = Json.parse(readStreamToString(inputStream))

    Json
      .fromJson[Array[(String, String)]](locationJsValue)
      .asOpt
      .map {
        _.map { case (name, code) =>
          SelectInput(code.split(":")(1).trim, name)
        }.toList
      }
      .get
  }

  def getCountry(countryCode: String): Option[String] =
    countries
      .find(_.value == countryCode)
      .map(_.label)

  def getCountryCode(country: String): Option[String] =
    countries
      .find(_.label == country)
      .map(_.value)

  private def readStreamToString(is: InputStream): String =
    try Source.fromInputStream(is).mkString
    finally is.close()
}
