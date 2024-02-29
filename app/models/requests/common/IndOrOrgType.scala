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

// 01 = Individual, 02 = Company, 03 = Partnership, 04 = Other
sealed trait IndOrOrgType {
  val value: String
  val definition: String
}

object IndOrOrgType {
  case object Individual extends IndOrOrgType {
    val value = "01"
    val definition = "INDIVIDUAL"
  }
  case object Company extends IndOrOrgType {
    val value = "02"
    val definition = "COMPANY"
  }

  case object Partnership extends IndOrOrgType {
    val value = "03"
    val definition = "PARTNERSHIP"
  }

  case object Other extends IndOrOrgType {
    val value = "04"
    val definition = "OTHER"
  }

  def apply(definition: String): IndOrOrgType = definition match {
    case Individual.definition => Individual
    case Company.definition => Company
    case Partnership.definition => Partnership
    case Other.definition => Other
    case _ => throw new RuntimeException("Couldn't match the type for EtmpSippIndOrOrgType!")
  }

  implicit val writes: Writes[IndOrOrgType] = invOrOrgType => JsString(invOrOrgType.value)
  implicit val reads: Reads[IndOrOrgType] = Reads {
    case JsString(Individual.value) => JsSuccess(Individual)
    case JsString(Company.value) => JsSuccess(Company)
    case JsString(Partnership.value) => JsSuccess(Partnership)
    case JsString(Other.value) => JsSuccess(Other)
    case unknown => JsError(s"Unknown value for YesNo: $unknown")
  }
}
