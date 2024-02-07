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

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits._
import config.Constants
import forms.mappings.errors.DateFormErrors
import forms.{NameDOBFormProvider, TextFormProvider}
import models.ValidationErrorType.ValidationErrorType
import models._
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, FormatStyle}
import javax.inject.Inject

class ValidationsService @Inject()(
  nameDOBFormProvider: NameDOBFormProvider,
  textFormProvider: TextFormProvider
) {

  private def ninoForm(memberFullName: String): Form[Nino] =
    textFormProvider.nino(
      "memberDetailsNino.upload.error.required",
      "memberDetailsNino.upload.error.invalid",
      memberFullName
    )

  private def noNinoForm(memberFullName: String): Form[String] =
    textFormProvider.textArea(
      "noNINO.upload.error.required",
      "noNINO.upload.upload.error.length",
      "noNINO.upload.upload.error.invalid",
      memberFullName
    )

  private def isUkAddressForm(memberFullName: String): Form[String] =
    textFormProvider.yesNo(
      "TODO key for required value",
      "TODO key for too long value",
      "TODO key for invalid value",
      memberFullName
    )

  private def addressLineForm(memberFullName: String): Form[String] =
    textFormProvider.addressLine(
      "TODO key for required value",
      "TODO key for too long value",
      "TODO key for invalid value",
      memberFullName
    )

  private def postcodeForm(memberFullName: String): Form[String] =
    textFormProvider.postcode(
      "TODO key for required value",
      "TODO key for invalid chars",
      "TODO key for invalid format value",
      memberFullName
    )

  def validateNameDOB(
    firstName: CsvValue[String],
    lastName: CsvValue[String],
    dob: CsvValue[String],
    row: Int,
    validDateThreshold: Option[LocalDate]
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, NameDOB]] = {
    val dobDayKey = s"${nameDOBFormProvider.dateOfBirth}.day"
    val dobMonthKey = s"${nameDOBFormProvider.dateOfBirth}.month"
    val dobYearKey = s"${nameDOBFormProvider.dateOfBirth}.year"

    dob.value.split("-").toList match {
      case day :: month :: year :: Nil =>
        val memberDetailsForm = {
          val dateThreshold: LocalDate = validDateThreshold.getOrElse(LocalDate.now())
          nameDOBFormProvider(
            "memberDetails.firstName.upload.error.required",
            "memberDetails.firstName.upload.error.invalid",
            "memberDetails.firstName.upload.error.length",
            "memberDetails.lastName.upload.error.required",
            "memberDetails.lastName.upload.error.invalid",
            "memberDetails.lastName.upload.error.length",
            DateFormErrors(
              "memberDetails.dateOfBirth.upload.error.required.all",
              "memberDetails.dateOfBirth.upload.error.required.day",
              "memberDetails.dateOfBirth.upload.error.required.month",
              "memberDetails.dateOfBirth.upload.error.required.year",
              "memberDetails.dateOfBirth.upload.error.required.two",
              "memberDetails.dateOfBirth.upload.error.invalid.date",
              "memberDetails.dateOfBirth.upload.error.invalid.characters",
              List(
                DateFormErrors
                  .failIfDateAfter(
                    dateThreshold,
                    messages(
                      "memberDetails.dateOfBirth.upload.error.future",
                      dateThreshold.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                    )
                  ),
                DateFormErrors
                  .failIfDateBefore(
                    Constants.earliestDate,
                    messages(
                      "memberDetails.dateOfBirth.upload.error.after",
                      Constants.earliestDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                    )
                  )
              )
            )
          )
        }.bind(
          Map(
            nameDOBFormProvider.firstName -> firstName.value,
            nameDOBFormProvider.lastName -> lastName.value,
            dobDayKey -> day,
            dobMonthKey -> month,
            dobYearKey -> year
          )
        )

        val errorTypeMapping: FormError => ValidationErrorType = _.key match {
          case nameDOBFormProvider.firstName => ValidationErrorType.FirstName
          case nameDOBFormProvider.lastName => ValidationErrorType.LastName
          case nameDOBFormProvider.dateOfBirth => ValidationErrorType.DateOfBirth
          case `dobDayKey` => ValidationErrorType.DateOfBirth
          case `dobMonthKey` => ValidationErrorType.DateOfBirth
          case `dobYearKey` => ValidationErrorType.DateOfBirth
        }

        val cellMapping: FormError => Option[String] = {
          case err if err.key == nameDOBFormProvider.firstName => Some(firstName.key.cell)
          case err if err.key == nameDOBFormProvider.lastName => Some(lastName.key.cell)
          case err if err.key == nameDOBFormProvider.dateOfBirth => Some(dob.key.cell)
          case err if err.key == dobDayKey => Some(dob.key.cell)
          case err if err.key == dobMonthKey => Some(dob.key.cell)
          case err if err.key == dobYearKey => Some(dob.key.cell)
          case _ => None
        }

        formToResult(memberDetailsForm, row, errorTypeMapping, cellMapping)

      case _ =>
        Some(
          ValidationError
            .fromCell(
              dob.key.cell,
              row,
              ValidationErrorType.DateOfBirth,
              messages("memberDetails.dateOfBirth.error.format")
            )
            .invalidNel
        )
    }
  }

  def validateNino(
    nino: CsvValue[String],
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, Nino]] = {
    val boundForm = ninoForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> nino.value
        )
      )

    formToResult(boundForm, row, _ => ValidationErrorType.NinoFormat, cellMapping = _ => Some(nino.key.cell))
  }

  def validateNoNino(
    noNinoReason: CsvValue[String],
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = noNinoForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> noNinoReason.value
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.NoNinoReason,
      cellMapping = _ => Some(noNinoReason.key.cell)
    )
  }


  def validateAddressLine(
                      inputAddressLine: CsvValue[String],
                      memberFullName: String,
                      row: Int
                    ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = addressLineForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> inputAddressLine.value
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.AddressLine,
      cellMapping = _ => Some(inputAddressLine.key.cell)
    )
  }

  def validateUkPostcode(
                           postcode: CsvValue[String],
                           memberFullName: String,
                           row: Int
                         ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = postcodeForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> postcode.value
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.UKPostcode,
      cellMapping = _ => Some(postcode.key.cell)
    )
  }

  def validateIsUkAddress(
    isUkAddress: CsvValue[String],
    memberFullName: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = isUkAddressForm(memberFullName)
      .bind(
        Map(
          textFormProvider.formKey -> isUkAddress.value
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.YesNoAddress,
      cellMapping = _ => Some(isUkAddress.key.cell)
    )
  }

  private def formToResult[A](
    form: Form[A],
    row: Int,
    errorTypeMapping: FormError => ValidationErrorType,
    cellMapping: FormError => Option[String]
  ): Option[Validated[NonEmptyList[ValidationError], A]] =
    form.fold(
      // unchecked is used as there will always be form errors here and theres no need to exhaustively pattern match and throw an unreachable exception
      hasErrors = form =>
        (form.errors: @unchecked) match {
          case head :: rest =>
            NonEmptyList
              .of[FormError](head, rest: _*)
              .map(
                err =>
                  cellMapping(err).map(cell => ValidationError.fromCell(cell, row, errorTypeMapping(err), err.message))
              )
              .sequence
              .map(_.invalid)
        },
      success = _.valid.some
    )

}
