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

package models

import cats.Order
import cats.data.NonEmptyList
import models.csv.CsvDocumentState
import play.api.libs.json.*
import uk.gov.hmrc.domain.Nino
import utils.ListUtils.ListOps
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

import java.time.Instant

case class ValidationError(row: Int, errorType: ValidationErrorType, message: String)

sealed trait ValidationErrorType extends EnumEntry

object ValidationErrorType extends Enum[ValidationErrorType] with PlayJsonEnum[ValidationErrorType] {
  case object FirstName extends ValidationErrorType
  case object LastName extends ValidationErrorType
  case object DateOfBirth extends ValidationErrorType
  case object NinoFormat extends ValidationErrorType
  case object CrnFormat extends ValidationErrorType
  case object UtrFormat extends ValidationErrorType
  case object FreeText extends ValidationErrorType

  case object DuplicateNino extends ValidationErrorType
  case object NoNinoReason extends ValidationErrorType
  case object AddressLine extends ValidationErrorType
  case object TownOrCity extends ValidationErrorType
  case object Country extends ValidationErrorType
  case object UKPostcode extends ValidationErrorType
  case object YesNoAddress extends ValidationErrorType
  case object YesNoQuestion extends ValidationErrorType
  case object Formatting extends ValidationErrorType
  case object LocalDateFormat extends ValidationErrorType
  case object Count extends ValidationErrorType
  case object Price extends ValidationErrorType
  case object AcquiredFromType extends ValidationErrorType
  case object ConnectedUnconnectedType extends ValidationErrorType
  case object InvalidRowFormat extends ValidationErrorType
  case object MarketOrCostType extends ValidationErrorType
  case object Percentage extends ValidationErrorType

  override def values: IndexedSeq[ValidationErrorType] = findValues
}

object ValidationError {
  implicit val format: Format[ValidationError] = Json.format[ValidationError]
  implicit val order: Order[ValidationError] = Order.by(vE => (vE.row, vE.message))

  def fromCell(row: Int, errorType: ValidationErrorType, errorMessage: String): ValidationError =
    ValidationError(row, errorType: ValidationErrorType, errorMessage)
}

case class UploadInternalState(row: Int, previousNinos: List[Nino]) {
  def next(nino: Option[Nino] = None): UploadInternalState =
    UploadInternalState(row + 1, previousNinos :?+ nino)
}

object UploadInternalState {
  val init: UploadInternalState = UploadInternalState(3, Nil) // first 2 rows are not 'data' rows and are ignored
}

sealed trait Upload

sealed trait UploadSuccess[T] extends Upload {
  def rows: List[T]
}

// UploadError should not extend Upload as the nested inheritance causes issues with the play Json macros
sealed trait UploadError

case class UploadFormatError(detail: ValidationError) extends Upload with UploadError
case class UploadErrors(errors: NonEmptyList[ValidationError]) extends UploadError

sealed trait UploadState

object UploadState {
  case object Uploaded extends UploadState
  case class UploadValidating(since: Instant) extends UploadState
  case class UploadValidated(state: CsvDocumentState) extends UploadState
  case object ValidationException extends UploadState
  case class SavingToEtmpException(errUrl: String) extends UploadState

  implicit val uploadedFormat: OFormat[Uploaded.type] = Json.format[Uploaded.type]
  implicit val uploadValidatingFormat: OFormat[UploadValidating] = Json.format[UploadValidating]
  implicit val uploadValidatedFormat: OFormat[UploadValidated] = Json.format[UploadValidated]
  implicit val validationExceptionFormat: OFormat[ValidationException.type] = Json.format[ValidationException.type]
  implicit val savingToEtmpExceptionFormat: OFormat[SavingToEtmpException] = Json.format[SavingToEtmpException]
  implicit val uploadStateFormat: OFormat[UploadState] = Json.format[UploadState]
}

/**
 * @param key
 *   csv header key e.g. First name
 * @param cell
 *   letter identifying column e.g A,B,C ... BA,BB ...
 * @param index
 *   column number
 */
case class CsvHeaderKey(key: String, cell: String, index: Int)

case class CsvValue[A](key: CsvHeaderKey, value: A) {
  def map[B](f: A => B): CsvValue[B] = CsvValue[B](key, f(value))

  def as[B](b: B): CsvValue[B] = map(_ => b)
}

object CsvValue {

  implicit val formatHeader: OFormat[CsvHeaderKey] = Json.format[CsvHeaderKey]
  implicit def formatCsvValue[A: Format]: OFormat[CsvValue[A]] = new OFormat[CsvValue[A]] {
    override def reads(json: JsValue): JsResult[CsvValue[A]] =
      for {
        key <- (json \ "key").validate[CsvHeaderKey]
        value <- (json \ "value").validate[A]
      } yield CsvValue(key, value)

    override def writes(o: CsvValue[A]): JsObject = Json.obj(
      "key" -> Json.toJson(o.key),
      "value" -> Json.toJson(o.value)
    )
  }

  // Additional implicit format for CsvValue[Option[A]]
  implicit def formatCsvValueOption[A: Format]: OFormat[CsvValue[Option[A]]] = new OFormat[CsvValue[Option[A]]] {
    override def reads(json: JsValue): JsResult[CsvValue[Option[A]]] =
      for {
        key <- (json \ "key").validate[CsvHeaderKey]
        value <- (json \ "value").validateOpt[A]
      } yield CsvValue(key, value)

    override def writes(o: CsvValue[Option[A]]): JsObject = Json.obj(
      "key" -> Json.toJson(o.key),
      "value" -> Json.toJson(o.value)
    )
  }
}
