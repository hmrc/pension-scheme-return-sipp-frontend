/*
 * Copyright 2024 HM Revenue & Customs
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

package services.validation

import cats.implicits._
import models.{CsvHeaderKey, CsvValue}

import scala.math.Integral.Implicits.infixIntegralOps

trait Validator {
  private val aToZ: List[Char] = ('a' to 'z').toList.map(_.toUpper)

  // Replace missing csv value with blank string so form validation can return a `value required` instead of returning a format error
  protected def getCSVValue(
    key: String,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[CsvValue[String]] =
    getOptionalCSVValue(key, headerKeys, csvData) match {
      case Some(CsvValue(key, Some(value))) => Some(CsvValue(key, value))
      case Some(CsvValue(key, None)) => Some(CsvValue(key, ""))
      case _ => None
    }

  protected def getOptionalCSVValue(
    key: String,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[CsvValue[Option[String]]] =
    headerKeys
      .find(_.key.toLowerCase() == key.toLowerCase())
      .map(foundKey => CsvValue(foundKey, csvData.get(foundKey.index).flatMap(s => if (s.isEmpty) None else Some(s))))

  def indexToCsvKey(index: Int): String =
    if (index == 0) aToZ.head.toString
    else {
      val (quotient, remainder) = index /% (aToZ.size)
      if (quotient == 0) aToZ(remainder).toString
      else indexToCsvKey(quotient - 1) + indexToCsvKey(remainder)
    }
}

object Validator extends Validator
