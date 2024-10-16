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

import cats.implicits.toShow
import utils.Transform.to

import java.time.{LocalDate, LocalDateTime}

class DateTimeUtilsTest extends BaseSpec {
  "DateTimeUtil implicits" - {
    "return a LocalDateTime as Day Month(As String) Year" in {
      import DateTimeUtils.localDateTimeShow

      val year = 2024
      val month = 1
      val day = 1
      val hour = 11
      val minute = 10

      val time = LocalDateTime.of(year, month, day, hour, minute)
      val show = time.to.show
      show must be(s"1 January 2024")
    }

    "return a LocalDate as Day Month(As String) Year" in {
      import DateTimeUtils.localDateShow

      val year = 2024
      val month = 2
      val day = 2

      val time = LocalDate.of(year, month, day)
      val show = time.to.show
      show must be(s"2 February 2024")
    }
  }
}
