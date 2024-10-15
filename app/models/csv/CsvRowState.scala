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

import cats.data.NonEmptyList
import models.CustomFormats.nonEmptyListFormat
import models.ValidationError
import play.api.libs.json.*

sealed trait CsvRowState[A] {
  def raw: NonEmptyList[String]
}

object CsvRowState {
  case class CsvRowValid[A](line: Long, validated: A, raw: NonEmptyList[String]) extends CsvRowState[A]

  object CsvRowValid {

    implicit def csvRowValidFormat[A: Format]: OFormat[CsvRowValid[A]] = new OFormat[CsvRowValid[A]] {
      override def reads(json: JsValue): JsResult[CsvRowValid[A]] =
        for {
          line <- (json \ "line").validate[Long]
          validated <- (json \ "validated").validate[A]
          raw <- (json \ "raw").validate[NonEmptyList[String]]
        } yield CsvRowValid(line, validated, raw)

      override def writes(o: CsvRowValid[A]): JsObject = Json.obj(
        "line" -> Json.toJson(o.line),
        "validated" -> Json.toJson(o.validated),
        "raw" -> Json.toJson(o.raw)
      )
    }
  }
  case class CsvRowInvalid[A](line: Long, errors: NonEmptyList[ValidationError], raw: NonEmptyList[String])
      extends CsvRowState[A]

  implicit def csvRowStateFormat[A: Format]: OFormat[CsvRowState[A]] = new OFormat[CsvRowState[A]] {
    override def writes(o: CsvRowState[A]): JsObject = o match {
      case valid: CsvRowValid[A] => CsvRowValid.csvRowValidFormat[A].writes(valid)
      case invalid: CsvRowInvalid[A] => Json.format[CsvRowInvalid[A]].writes(invalid)
    }

    override def reads(json: JsValue): JsResult[CsvRowState[A]] = json match {
      case JsObject(underlying) =>
        if (underlying.contains("validated")) {
          CsvRowValid.csvRowValidFormat[A].reads(json)
        } else {
          Json.format[CsvRowInvalid[A]].reads(json)
        }
      case other => JsError(s"expected a JsObject for CsvRowState, but got $other")
    }

  }

}
