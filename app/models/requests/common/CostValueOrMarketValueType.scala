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

import play.api.libs.json._

// 01 = COST VALUE, 02 = MARKET VALUE
sealed trait CostValueOrMarketValueType {
  val value: String
  val definition: String
}

object CostValueOrMarketValueType {
  def uploadStringToRequestCostValueOrMarketValue(s: String): CostValueOrMarketValueType =
    if (s.toUpperCase.equals("COST VALUE")) {
      CostValueOrMarketValueType.CostValue
    } else {
      CostValueOrMarketValueType.MarketValue
    }
  case object CostValue extends CostValueOrMarketValueType {
    val value = "01"
    val definition = "COST VALUE"
  }
  case object MarketValue extends CostValueOrMarketValueType {
    val value = "02"
    val definition = "MARKET VALUE"
  }

  def apply(definition: String): CostValueOrMarketValueType = definition match {
    case CostValue.definition => CostValue
    case MarketValue.definition => MarketValue
    case _ => throw new RuntimeException("Couldn't match the type for CostValueOrMarketValueType!")
  }

  implicit val writes: Writes[CostValueOrMarketValueType] = invOrOrgType => JsString(invOrOrgType.value)
  implicit val reads: Reads[CostValueOrMarketValueType] = Reads {
    case JsString(CostValue.value) => JsSuccess(CostValue)
    case JsString(MarketValue.value) => JsSuccess(MarketValue)
    case unknown => JsError(s"Unknown value for CostValueOrMarketValueType: $unknown")
  }
}
