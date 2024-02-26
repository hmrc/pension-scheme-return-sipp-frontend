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
import models.ValidationErrorType.ValidationErrorType
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.domain.Nino
import utils.ListUtils.ListOps

import java.time.Instant

case class ValidationError(row: Int, errorType: ValidationErrorType, message: String)

object ValidationErrorType {

  sealed trait ValidationErrorType
  case object FirstName extends ValidationErrorType
  case object LastName extends ValidationErrorType
  case object DateOfBirth extends ValidationErrorType
  case object NinoFormat extends ValidationErrorType
  case object DuplicateNino extends ValidationErrorType
  case object NoNinoReason extends ValidationErrorType
  case object AddressLine extends ValidationErrorType
  case object TownOrCity extends ValidationErrorType
  case object Country extends ValidationErrorType
  case object UKPostcode extends ValidationErrorType
  case object YesNoAddress extends ValidationErrorType
  case object Formatting extends ValidationErrorType
}

object ValidationError {
  import ValidationErrorType._

  implicit val firstNameFormat: Format[ValidationErrorType.FirstName.type] =
    Json.format[ValidationErrorType.FirstName.type]
  implicit val lastNameFormat: Format[ValidationErrorType.LastName.type] =
    Json.format[ValidationErrorType.LastName.type]
  implicit val dobFormat: Format[ValidationErrorType.DateOfBirth.type] =
    Json.format[ValidationErrorType.DateOfBirth.type]
  implicit val ninoFormat: Format[ValidationErrorType.NinoFormat.type] =
    Json.format[ValidationErrorType.NinoFormat.type]
  implicit val duplicateNinoFormat: Format[ValidationErrorType.DuplicateNino.type] =
    Json.format[ValidationErrorType.DuplicateNino.type]
  implicit val noNinoReasonFormat: Format[ValidationErrorType.NoNinoReason.type] =
    Json.format[ValidationErrorType.NoNinoReason.type]
  implicit val yesNoFormat: Format[ValidationErrorType.YesNoAddress.type] =
    Json.format[ValidationErrorType.YesNoAddress.type]
  implicit val addressLineFormat: Format[ValidationErrorType.AddressLine.type] =
    Json.format[ValidationErrorType.AddressLine.type]
  implicit val townOrCityFormat: Format[ValidationErrorType.TownOrCity.type] =
    Json.format[ValidationErrorType.TownOrCity.type]
  implicit val ukPostcodeFormat: Format[ValidationErrorType.UKPostcode.type] =
    Json.format[ValidationErrorType.UKPostcode.type]
  implicit val countryFormat: Format[ValidationErrorType.Country.type] =
    Json.format[ValidationErrorType.Country.type]
  implicit val fFormat: Format[ValidationErrorType.Formatting.type] =
    Json.format[ValidationErrorType.Formatting.type]
  implicit val errorTypeFormat: Format[ValidationErrorType] = Json.format[ValidationErrorType]
  implicit val format: Format[ValidationError] = Json.format[ValidationError]
  implicit val order: Order[ValidationError] = Order.by(_.message)
  implicit val ordering: Order[ValidationErrorType] = Order.by(_.toString)

  def fromCell(row: Int, errorType: ValidationErrorType, errorMessage: String): ValidationError =
    ValidationError(row, errorType: ValidationErrorType, errorMessage)
}

case class UploadState(row: Int, previousNinos: List[Nino]) {
  def next(nino: Option[Nino] = None): UploadState =
    UploadState(row + 1, previousNinos :?+ nino)
}

object UploadState {
  val init: UploadState = UploadState(3, Nil) //first 2 rows are not 'data' rows and are ignored
}

sealed trait Upload
case object Uploaded extends Upload
case class UploadValidating(since: Instant) extends Upload
case class UploadSuccess(memberDetails: List[MemberDetailsUpload]) extends Upload

// UploadError should not extend Upload as the nested inheritance causes issues with the play Json macros
sealed trait UploadError

case class UploadFormatError(detail: ValidationError) extends Upload with UploadError

case class UploadErrors(
  nonValidatedMemberDetails: NonEmptyList[MemberDetailsUpload],
  errors: NonEmptyList[ValidationError]
) extends Upload
    with UploadError

case class RawMemberDetails(
  row: Int,
  firstName: CsvValue[String],
  lastName: CsvValue[String],
  dateOfBirth: CsvValue[String],
  nino: CsvValue[Option[String]],
  ninoReason: CsvValue[Option[String]],
  isUK: CsvValue[String],
  ukAddressLine1: CsvValue[Option[String]],
  ukAddressLine2: CsvValue[Option[String]],
  ukAddressLine3: CsvValue[Option[String]],
  ukCity: CsvValue[Option[String]],
  ukPostCode: CsvValue[Option[String]],
  addressLine1: CsvValue[Option[String]],
  addressLine2: CsvValue[Option[String]],
  addressLine3: CsvValue[Option[String]],
  addressLine4: CsvValue[Option[String]],
  country: CsvValue[Option[String]]
)
case class MemberDetailsUpload(
  row: Int,
  firstName: String,
  lastName: String,
  dateOfBirth: String,
  nino: Option[String],
  ninoReason: Option[String],
  isUK: String,
  ukAddressLine1: Option[String],
  ukAddressLine2: Option[String],
  ukAddressLine3: Option[String],
  ukCity: Option[String],
  ukPostCode: Option[String],
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  country: Option[String]
)

object MemberDetailsUpload {
  def fromRaw(raw: RawMemberDetails): MemberDetailsUpload =
    MemberDetailsUpload(
      row = raw.row,
      firstName = raw.firstName.value,
      lastName = raw.lastName.value,
      dateOfBirth = raw.dateOfBirth.value,
      nino = raw.nino.value,
      ninoReason = raw.ninoReason.value,
      isUK = raw.isUK.value,
      ukAddressLine1 = raw.ukAddressLine1.value,
      ukAddressLine2 = raw.ukAddressLine2.value,
      ukAddressLine3 = raw.ukAddressLine3.value,
      ukCity = raw.ukCity.value,
      ukPostCode = raw.ukPostCode.value,
      addressLine1 = raw.addressLine1.value,
      addressLine2 = raw.addressLine2.value,
      addressLine3 = raw.addressLine3.value,
      addressLine4 = raw.addressLine4.value,
      country = raw.country.value
    )

  implicit val eitherWrites: Writes[Either[String, Nino]] = e =>
    Json.obj(
      e.fold(
        noNinoReason => "noNinoReason" -> Json.toJson(noNinoReason),
        nino => "nino" -> Json.toJson(nino)
      )
    )

  implicit val eitherReads: Reads[Either[String, Nino]] =
    (__ \ "noNinoReason").read[String].map(noNinoReason => Left(noNinoReason)) |
      (__ \ "nino").read[Nino].map(nino => Right(nino))

  implicit val format: Format[MemberDetailsUpload] = Json.format[MemberDetailsUpload]
}

/**
 * @param key = csv header key e.g. First name
 * @param cell = letter identifying column e.g A,B,C ... BA,BB ...
 * @param index = column number
 */
case class CsvHeaderKey(key: String, cell: String, index: Int)

case class CsvValue[A](key: CsvHeaderKey, value: A) {
  def map[B](f: A => B): CsvValue[B] = CsvValue[B](key, f(value))

  def as[B](b: B): CsvValue[B] = map(_ => b)
}
