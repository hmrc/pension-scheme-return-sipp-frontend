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

package models

import config.Constants
import play.api.mvc.Session

import java.time.LocalDate

case class TaxYear(taxYear: String, taxYearDateRange: DateRange)

object TaxYear {
  def optFromSession(session: Session): Option[TaxYear] =
    for {
      taxYear <- session.get(Constants.taxYear)
      yearFrom = LocalDate.parse(taxYear)
      yearTo = yearFrom.plusYears(1).minusDays(1)
    } yield TaxYear(taxYear, DateRange(yearFrom, yearTo))
}
