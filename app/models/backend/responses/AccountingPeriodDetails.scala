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

package models.backend.responses

import cats.data.NonEmptyList
import models.DateRange
import models.CustomFormats.nonEmptyListFormat
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class AccountingPeriodDetails(
  version: Option[String],
  accountingPeriods: Option[NonEmptyList[AccountingPeriod]]
)

case class AccountingPeriod(
  accPeriodStart: LocalDate,
  accPeriodEnd: LocalDate
)

object AccountingPeriod {
  implicit val format: OFormat[AccountingPeriod] = Json.format[AccountingPeriod]

  def apply(dateRange: DateRange): AccountingPeriod = AccountingPeriod(
    dateRange.from,
    dateRange.to
  )
}

object AccountingPeriodDetails {
  implicit val format: OFormat[AccountingPeriodDetails] = Json.format[AccountingPeriodDetails]
  
  def apply(dateRanges: List[DateRange]): AccountingPeriodDetails =
    AccountingPeriodDetails(
      None,
      NonEmptyList.fromList(dateRanges.map(AccountingPeriod(_)))
    )
}
