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

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed abstract class IndOrOrgType(override val entryName: String) extends EnumEntry

object IndOrOrgType extends Enum[IndOrOrgType] with PlayJsonEnum[IndOrOrgType] {
  case object Individual extends IndOrOrgType("01")
  case object Company extends IndOrOrgType("02")
  case object Partnership extends IndOrOrgType("03")
  case object Other extends IndOrOrgType("04")

  override def values: IndexedSeq[IndOrOrgType] = findValues
}
