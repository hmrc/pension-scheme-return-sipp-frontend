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

package models.csv

import cats.Semigroup
import cats.data.NonEmptyList
import cats.implicits.catsSyntaxSemigroup
import models.{CustomFormats, ValidationError}
import models.csv.CsvRowState._
import play.api.libs.json.{Format, Json, OFormat}

sealed trait CsvDocumentState

case object CsvDocumentEmpty extends CsvDocumentState
case object CsvDocumentValid extends CsvDocumentState

case object CsvDocumentValidAndSaved extends CsvDocumentState

case class CsvDocumentInvalid(errorCount: Int, errors: NonEmptyList[ValidationError]) extends CsvDocumentState

object CsvDocumentState {
  def combine[T](csvDocumentState: CsvDocumentState, csvRowState: CsvRowState[T]): CsvDocumentState =
    csvDocumentState.combine(CsvDocumentState[T](csvRowState))

  def apply[T](csvRowState: CsvRowState[T]): CsvDocumentState = csvRowState match {
    case CsvRowValid(_, _, _) => CsvDocumentValid
    case CsvRowInvalid(_, errors, _) => CsvDocumentInvalid(errors.size, errors)
  }

  implicit val CsvValidationStateSemiGroup: Semigroup[CsvDocumentState] = {
    case (x @ CsvDocumentInvalid(_, _), y @ CsvDocumentInvalid(_, _)) =>
      x.copy(
        errorCount = x.errorCount + y.errorCount,
        errors = x.errors ::: y.errors
      )
    case (invalid: CsvDocumentInvalid, _) => invalid
    case (_, invalid: CsvDocumentInvalid) => invalid

    case (CsvDocumentValid, CsvDocumentValid) => CsvDocumentValid
    case (CsvDocumentEmpty, state) => state
    case (state, CsvDocumentEmpty) => state
    case (CsvDocumentValidAndSaved, state) => state
    case (state, CsvDocumentValidAndSaved) => state
  }

  implicit class CsvDocumentStateOps(val csvDocumentState: CsvDocumentState) extends AnyVal {
    def count: Int = csvDocumentState match {
      case CsvDocumentValid | CsvDocumentValidAndSaved | CsvDocumentEmpty => 0
      case c: CsvDocumentInvalid => c.errorCount
    }
  }

  implicit val nonEmptyListFormat: Format[NonEmptyList[ValidationError]] =
    CustomFormats.nonEmptyListFormat[ValidationError]

  implicit val emptyFormat: OFormat[CsvDocumentEmpty.type] = Json.format[CsvDocumentEmpty.type]
  implicit val validFormat: OFormat[CsvDocumentValid.type] = Json.format[CsvDocumentValid.type]
  implicit val validSavedFormat: OFormat[CsvDocumentValidAndSaved.type] = Json.format[CsvDocumentValidAndSaved.type]
  implicit val invalidFormat: OFormat[CsvDocumentInvalid] = Json.format[CsvDocumentInvalid]
  implicit val format: OFormat[CsvDocumentState] = Json.format[CsvDocumentState]
}
