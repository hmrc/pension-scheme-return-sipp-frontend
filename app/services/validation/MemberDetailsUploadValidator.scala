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

package services.validation

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import cats.effect.IO
import cats.implicits._
import models.ValidationErrorType.InvalidRowFormat
import models._
import models.csv.{CsvDocumentState, CsvRowState}
import models.csv.CsvRowState._
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import javax.inject.Inject

class MemberDetailsUploadValidator @Inject()(
  uploadValidatorFs2: UploadValidator,
  validations: LandOrPropertyValidationsService
) extends Validator {

  def validateUpload(
    uploadKey: UploadKey,
    stream: fs2.Stream[IO, String],
    validDateThreshold: Option[LocalDate]
  )(implicit messages: Messages): IO[CsvDocumentState] =
    uploadValidatorFs2.validateUpload[MemberDetailsUpload](
      stream,
      memberDetailsValidator(validDateThreshold),
      uploadKey
    )

  private def memberDetailsValidator(
    validDateThreshold: Option[LocalDate]
  ): CsvRowValidator[MemberDetailsUpload] =
    new CsvRowValidator[MemberDetailsUpload] {
      override def validate(line: Int, values: NonEmptyList[String], headers: List[CsvHeaderKey])(
        implicit messages: Messages
      ): CsvRowState[MemberDetailsUpload] =
        validateJourney(line, values, headers, validDateThreshold, List.empty)
    }

  private def validateJourney(
    row: Int,
    data: NonEmptyList[String],
    headerKeys: List[CsvHeaderKey],
    validDateThreshold: Option[LocalDate],
    previousNinos: List[Nino]
  )(
    implicit messages: Messages
  ): CsvRowState[MemberDetailsUpload] =
    (for {
      raw <- readCSV(row, headerKeys, data.toList)
      memberFullName = s"${raw.firstName} ${raw.lastName}"
      validatedNameDOB <- validations.validateNameDOB(
        raw.firstName,
        raw.lastName,
        raw.dateOfBirth,
        row,
        validDateThreshold
      )
      maybeValidatedNino = raw.nino.value.flatMap { nino =>
        validations.validateNinoWithDuplicationControl(
          raw.nino.as(nino.toUpperCase),
          memberFullName,
          previousNinos,
          row
        )
      }
      maybeValidatedNoNinoReason = raw.ninoReason.value.flatMap(
        reason => validations.validateNoNino(raw.ninoReason.as(reason), memberFullName, row)
      )
      validatedNinoOrNoNinoReason <- (maybeValidatedNino, maybeValidatedNoNinoReason) match {
        case (Some(validatedNino), None) => Some(Right(validatedNino))
        case (None, Some(validatedNoNinoReason)) => Some(Left(validatedNoNinoReason))
        case (_, _) =>
          Some(
            Left(
              ValidationError
                .fromCell(
                  row,
                  ValidationErrorType.NoNinoReason,
                  messages("noNINO.upload.error.required")
                )
                .invalidNel
            )
          )
      }
      validatedAddress <- validations.validateUKOrROWAddress(
        raw.isUK,
        raw.ukAddressLine1,
        raw.ukAddressLine2,
        raw.ukAddressLine3,
        raw.ukCity,
        raw.ukPostCode,
        raw.addressLine1,
        raw.addressLine2,
        raw.addressLine3,
        raw.addressLine4,
        raw.country,
        memberFullName,
        row
      )
    } yield (
      raw,
      (validatedAddress, validatedNameDOB, validatedNinoOrNoNinoReason.bisequence).mapN(
        (_, _, _) => MemberDetailsUpload.fromRaw(raw)
      )
    )) match {
      case None =>
        CsvRowInvalid(
          row,
          NonEmptyList.of(
            ValidationError(row, InvalidRowFormat, "Invalid file format, please format file as per provided template")
          ),
          data
        )
      case Some((_, Valid(memberDetails))) =>
        CsvRowValid(row, memberDetails, data)
      case Some((_, Invalid(errs))) =>
        CsvRowInvalid[MemberDetailsUpload](row, errs, data)
    }

  private def readCSV(
    row: Int,
    headerKeys: List[CsvHeaderKey],
    csvData: List[String]
  ): Option[RawMemberDetails] =
    for {
      firstName <- getCSVValue(UploadKeys.firstName, headerKeys, csvData)
      lastName <- getCSVValue(UploadKeys.lastName, headerKeys, csvData)
      dob <- getCSVValue(UploadKeys.dateOfBirth, headerKeys, csvData)
      maybeNino <- getOptionalCSVValue(UploadKeys.nino, headerKeys, csvData)
      maybeNoNinoReason <- getOptionalCSVValue(UploadKeys.reasonForNoNino, headerKeys, csvData)
      isUKAddress <- getCSVValue(UploadKeys.isUKAddress, headerKeys, csvData)
      ukAddressLine1 <- getOptionalCSVValue(UploadKeys.ukAddressLine1, headerKeys, csvData)
      ukAddressLine2 <- getOptionalCSVValue(UploadKeys.ukAddressLine2, headerKeys, csvData)
      ukAddressLine3 <- getOptionalCSVValue(UploadKeys.ukAddressLine3, headerKeys, csvData)
      ukTownOrCity <- getOptionalCSVValue(UploadKeys.ukTownOrCity, headerKeys, csvData)
      ukPostcode <- getOptionalCSVValue(UploadKeys.ukPostcode, headerKeys, csvData)
      addressLine1 <- getOptionalCSVValue(UploadKeys.addressLine1, headerKeys, csvData)
      addressLine2 <- getOptionalCSVValue(UploadKeys.addressLine2, headerKeys, csvData)
      addressLine3 <- getOptionalCSVValue(UploadKeys.addressLine3, headerKeys, csvData)
      addressLine4 <- getOptionalCSVValue(UploadKeys.addressLine4, headerKeys, csvData)
      country <- getOptionalCSVValue(UploadKeys.country, headerKeys, csvData)
    } yield RawMemberDetails(
      row,
      firstName,
      lastName,
      dob,
      maybeNino,
      maybeNoNinoReason,
      isUKAddress,
      ukAddressLine1,
      ukAddressLine2,
      ukAddressLine3,
      ukTownOrCity,
      ukPostcode,
      addressLine1,
      addressLine2,
      addressLine3,
      addressLine4,
      country
    )
}

object MemberDetailsUploadValidator {

  sealed trait UploadAddress

  case class UKAddress(
    line1: String,
    line2: Option[String],
    line3: Option[String],
    city: Option[String],
    postcode: String
  ) extends UploadAddress

  case class ROWAddress(
    line1: String,
    line2: Option[String],
    line3: Option[String],
    line4: Option[String],
    country: String
  ) extends UploadAddress

}
