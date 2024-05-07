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

import cats.data.NonEmptyList
import models.ValidationError
import models.ValidationErrorType.InvalidRowFormat
import models.csv.CsvRowState.CsvRowInvalid

package object csv {
  def invalidFileFormat[A](line: Int, values: NonEmptyList[String]): CsvRowInvalid[A] = CsvRowInvalid[A](
    line,
    NonEmptyList.of(
      ValidationError(line, InvalidRowFormat, "Invalid file format, please format file as per provided template")
    ),
    values
  )
}
