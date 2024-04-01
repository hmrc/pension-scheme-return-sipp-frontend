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

import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNel
import cats.implicits._
import forms._
import models._
import models.requests.common._
import play.api.data.Form

import javax.inject.Inject

class OutstandingLoansPropertyValidationsService @Inject()(
  nameDOBFormProvider: NameDOBFormProvider,
  textFormProvider: TextFormProvider,
  dateFormPageProvider: DatePageFormProvider,
  moneyFormProvider: MoneyFormProvider,
  intFormProvider: IntFormProvider,
  doubleFormProvider: DoubleFormProvider
) extends ValidationsService(
      nameDOBFormProvider,
      textFormProvider,
      dateFormPageProvider,
      moneyFormProvider,
      intFormProvider,
      doubleFormProvider
    ) {

  private def acquiredFromTypeForm(memberFullDetails: String, key: String): Form[String] =
    textFormProvider.acquiredFromType(
      s"$key.upload.error.required",
      s"$key.upload.error.invalid",
      memberFullDetails
    )

  def validateAcquiredFromType(
    acquiredFromType: CsvValue[String],
    memberFullName: String,
    row: Int,
    key: String
  ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = acquiredFromTypeForm(memberFullName, key)
      .bind(
        Map(
          textFormProvider.formKey -> acquiredFromType.value.toUpperCase
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.AcquiredFromType,
      cellMapping = _ => Some(acquiredFromType.key.cell)
    )
  }

  def validateAcquiredFrom(
    acquiredFromType: CsvValue[String],
    acquirerNinoForIndividual: CsvValue[Option[String]],
    acquirerCrnForCompany: CsvValue[Option[String]],
    acquirerUtrForPartnership: CsvValue[Option[String]],
    whoAcquiredFromTypeReasonAsset: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, AcquiredFromType]] =
    for {

      validatedAcquiredFromType <- validateAcquiredFromType(
        acquiredFromType,
        memberFullNameDob,
        row,
        "outstandingLoans.acquiredFromType"
      )

      maybeNino = acquirerNinoForIndividual.value.flatMap(
        nino =>
          validateNino(
            acquirerNinoForIndividual.as(nino),
            memberFullNameDob,
            row,
            "outstandingLoans.acquirerNino"
          )
      )
      maybeCrn = acquirerCrnForCompany.value.flatMap(
        crn => validateCrn(acquirerCrnForCompany.as(crn), memberFullNameDob, row, "outstandingLoans.acquirerCrn")
      )
      maybeUtr = acquirerUtrForPartnership.value.flatMap(
        utr => validateUtr(acquirerUtrForPartnership.as(utr), memberFullNameDob, row, "outstandingLoans.acquirerUtr")
      )
      maybeOther = whoAcquiredFromTypeReasonAsset.value.flatMap(
        other =>
          validateFreeText(
            whoAcquiredFromTypeReasonAsset.as(other),
            "outstandingLoans.noWhoAcquiredFromTypeReason",
            memberFullNameDob,
            row
          )
      )

      validatedAcquiredFrom <- (
        validatedAcquiredFromType,
        maybeNino,
        maybeCrn,
        maybeUtr,
        maybeOther
      ) match {
        case (Valid(acquiredFromType), mNino, _, _, mOther) if acquiredFromType.toUpperCase == "INDIVIDUAL" =>
          (mNino, mOther) match {
            case (Some(nino), _) =>
              Some((nino).map { nino =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = Some(nino.value),
                  reasonNoIdNumber = None,
                  otherDescription = None
                )
              })

            case (None, Some(other)) =>
              Some((other).map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = Some(other),
                  otherDescription = None
                )
              })

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "outstandingLoans.acquirerNino.upload.error.required"
                ).invalidNel
              )
          }

        case (Valid(acquiredFromType), _, mCrn, _, mOther) if acquiredFromType.toUpperCase == "COMPANY" =>
          (mCrn, mOther) match {
            case (Some(crn), _) =>
              Some((crn).map { crn =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = Some(crn.value),
                  reasonNoIdNumber = None,
                  otherDescription = None
                )
              })
            case (None, Some(other)) =>
              Some((other).map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = Some(other),
                  otherDescription = None
                )
              })

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "outstandingLoans.acquirerCrn.upload.error.required"
                ).invalidNel
              )
          }

        case (Valid(acquiredFromType), _, _, mUtr, mOther) if acquiredFromType.toUpperCase == "PARTNERSHIP" =>
          (mUtr, mOther) match {
            case (Some(utr), _) =>
              Some((utr).map { utr =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = Some(utr.value),
                  reasonNoIdNumber = None,
                  otherDescription = None
                )
              })

            case (None, Some(other)) =>
              Some((other).map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = Some(other),
                  otherDescription = None
                )
              })

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "outstandingLoans.acquirerUtr.upload.error.required"
                ).invalidNel
              )
          }

        case (Valid(acquiredFromType), _, _, _, mOther) if acquiredFromType.toUpperCase == "OTHER" =>
          mOther match {
            case Some(other) =>
              Some((other).map { other =>
                AcquiredFromType(
                  indivOrOrgType = IndOrOrgType(acquiredFromType.toUpperCase),
                  idNumber = None,
                  reasonNoIdNumber = None,
                  otherDescription = Some(other)
                )
              })

            case _ =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "outstandingLoans.noWhoAcquiredFromTypeReason.upload.error.required"
                ).invalidNel
              )
          }

        case (e @ Invalid(_), _, _, _, _) => Some(e)

        case _ => None
      }
    } yield validatedAcquiredFrom
}
