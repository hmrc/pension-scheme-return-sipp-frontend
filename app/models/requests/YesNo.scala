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

import models.requests.YesNo.{No, Yes}
import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}

sealed trait YesNo {
  val value: String
  val boolean: Boolean

  def negate: YesNo = this match {
    case Yes => No
    case No => Yes
  }
}

object YesNo {
  def uploadYesNoToRequestYesNo(yesNo: String): YesNo =
    if (yesNo.toLowerCase().equals("yes")) {
      YesNo.Yes
    } else {
      YesNo.No
    }

  case object Yes extends YesNo {
    val value = "Yes"
    val boolean = true
  }
  case object No extends YesNo {
    val value = "No"
    val boolean = false
  }

  def apply(bool: Boolean): YesNo = if (bool) Yes else No

  def unapply(yesNo: YesNo): Boolean = yesNo == Yes

  implicit val writes: Writes[YesNo] = yesNo => JsString(yesNo.value)
  implicit val reads: Reads[YesNo] = Reads {
    case JsString(Yes.value) => JsSuccess(Yes)
    case JsString(No.value) => JsSuccess(No)
    case unknown => JsError(s"Unknown value for YesNo: $unknown")
  }
}
