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
import cats.data.{NonEmptyList, ValidatedNel}
import cats.implicits.*
import forms.*
import models.*
import models.requests.common.YesNo.{No, Yes}
import models.requests.common.*
import play.api.i18n.Messages

import javax.inject.Inject

class LandOrPropertyValidationsService @Inject() (
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

  def validateIsThereARegistryReference(
    isThereARegistryReference: CsvValue[String],
    landRegistryRefOrReason: CsvValue[String],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, RegistryDetails]] =
    for {
      validatedIsThereARegistryReference <- validateYesNoQuestion(
        isThereARegistryReference,
        "landOrProperty.isThereARegistryReference",
        memberFullNameDob,
        row
      )
      validatedReferenceNumberOrReason <- validateFreeText(
        landRegistryRefOrReason,
        "landOrProperty.landRegistryReferenceOrReason",
        memberFullNameDob,
        row
      )
      referenceDetails <- (
        validatedIsThereARegistryReference,
        validatedReferenceNumberOrReason
      ) match {
        case (Valid(isThereARegistryReference), Valid(referenceNumberOrReason)) =>
          if (isThereARegistryReference.equalsIgnoreCase("YES"))
            Some(RegistryDetails(Yes, Some(referenceNumberOrReason), None).valid)
          else
            Some(RegistryDetails(No, None, Some(referenceNumberOrReason)).valid)
        case (e @ Invalid(_), _) => Some(e)
        case (_, e @ Invalid(_)) => Some(e)
        case _ => None
      }
    } yield referenceDetails

  def validateJointlyHeld(
    isPropertyHeldJointly: CsvValue[String],
    howManyPersonsJointlyOwnProperty: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, (YesNo, Option[Int])]] =
    for {
      validatedIsPropertyHeldJointly <- validateYesNoQuestion(
        isPropertyHeldJointly,
        "landOrProperty.isPropertyHeldJointly",
        memberFullNameDob,
        row
      )

      maybeCount = howManyPersonsJointlyOwnProperty.value.flatMap(count =>
        validateCount(
          howManyPersonsJointlyOwnProperty.as(count),
          "landOrProperty.personCount",
          memberFullNameDob,
          row
        )
      )

      jointlyHeld <- (
        validatedIsPropertyHeldJointly,
        maybeCount
      ) match {
        case (Valid(isPropertyHeldJointly), mCount) if isPropertyHeldJointly.toUpperCase == "YES" =>
          mCount match {
            case Some(count) =>
              count match {
                case Valid(c) => Some((Yes, Some(c)).validNel)
                case e @ Invalid(_) => Some(e)
              }
            case _ =>
              Some(
                ValidationError(
                  row,
                  errorType = ValidationErrorType.Count,
                  "landOrProperty.personCount.upload.error.required"
                ).invalidNel
              )
          }

        case (Valid(isPropertyHeldJointly), _) if isPropertyHeldJointly.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _) => Some(e)

        case _ => None
      }
    } yield jointlyHeld

  def validateLease(
    isLeased: CsvValue[String],
    numberOfLessees: CsvValue[Option[String]],
    anyLesseeConnectedParty: CsvValue[Option[String]],
    leaseDate: CsvValue[Option[String]],
    annualLeaseAmount: CsvValue[Option[String]],
    isCountEntered: Boolean,
    memberFullNameDob: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, (YesNo, Option[LesseeDetails])]] =
    for {
      validatedIsLeased <- validateYesNoQuestionTyped(
        isLeased,
        "landOrProperty.isLeased",
        memberFullNameDob,
        row
      )

      maybeCount = numberOfLessees.value.flatMap(count =>
        validateCount(
          numberOfLessees.as(count),
          "landOrProperty.lesseePersonCount",
          memberFullNameDob,
          row,
          maxCount = 50
        )
      )

      maybeConnected = anyLesseeConnectedParty.value.flatMap(yesNo =>
        validateYesNoQuestionTyped(
          anyLesseeConnectedParty.as(yesNo),
          "landOrProperty.anyLesseeConnected",
          memberFullNameDob,
          row
        )
      )

      maybeDate = leaseDate.value.flatMap(d =>
        validateDate(
          leaseDate.as(d),
          "landOrProperty.leaseDate",
          row
        )
      )

      maybeAmount = annualLeaseAmount.value.flatMap(amount =>
        validatePrice(
          annualLeaseAmount.as(amount),
          "landOrProperty.leaseAmount",
          memberFullNameDob,
          row
        )
      )

      lesseeDetails <- (
        validatedIsLeased,
        maybeCount,
        maybeConnected,
        maybeDate,
        maybeAmount
      ) match {
        case (Valid(isLeased), mCount, mConnected, mDate, mAmount) if isLeased.boolean =>
          (isCountEntered, mCount, mConnected, mDate, mAmount) match {
            case (countEntered, Some(count), Some(connected), Some(date), Some(amount)) if countEntered =>
              Some((count, connected, date, amount).mapN { (_count, _connected, _date, _amount) =>
                (Yes, Some(LesseeDetails(_count, _connected, _date, _amount.value)))
              })
            case (countEntered, _, Some(connected), Some(date), Some(amount)) if !countEntered =>
              Some((connected, date, amount).mapN { (_connected, _date, _amount) =>
                (Yes, Some(LesseeDetails(1, _connected, _date, _amount.value)))
              })
            case _ =>
              val listEmpty = List.empty[Option[ValidationError]]
              val optCount = if (isCountEntered && mCount.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Count,
                    "landOrProperty.lesseePersonCount.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optConnected = if (mConnected.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.YesNoQuestion,
                    "landOrProperty.anyLesseeConnected.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optDate = if (mDate.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.LocalDateFormat,
                    "landOrProperty.leaseDate.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optAmount = if (mAmount.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Price,
                    "landOrProperty.leaseAmount.upload.error.required"
                  )
                )
              } else {
                None
              }
              val errors = listEmpty :+ optCount :+ optConnected :+ optDate :+ optAmount
              Some(Invalid(NonEmptyList.fromListUnsafe(errors.flatten)))
          }

        case (Valid(isLeased), _, _, _, _) if !isLeased.boolean =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _) => Some(e)

        case _ => None
      }
    } yield lesseeDetails

  def validateDisposals(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    totalSaleProceedIfAnyDisposal: CsvValue[Option[String]],
    nameOfPurchasers: CsvValue[Option[String]],
    isAnyPurchaserConnected: CsvValue[Option[String]],
    isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
    hasLandOrPropertyFullyDisposedOf: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, (YesNo, Option[DisposalDetails])]] =
    for {
      validatedWereAnyDisposalOnThisDuringTheYear <- validateYesNoQuestion(
        wereAnyDisposalOnThisDuringTheYear,
        "landOrProperty.isAnyDisposalMade",
        memberFullNameDob,
        row
      )

      maybeDisposalAmount = totalSaleProceedIfAnyDisposal.value.flatMap(p =>
        validatePrice(
          totalSaleProceedIfAnyDisposal.as(p),
          s"landOrProperty.disposedAmount",
          memberFullNameDob,
          row
        )
      )

      maybeNames = nameOfPurchasers.value.flatMap(n =>
        validateFreeText(
          nameOfPurchasers.as(n),
          s"landOrProperty.disposedNames",
          memberFullNameDob,
          row
        )
      )

      maybeConnected = isAnyPurchaserConnected.value.flatMap(yN =>
        validateYesNoQuestion(
          isAnyPurchaserConnected.as(yN),
          s"landOrProperty.anyConnectedPurchaser",
          memberFullNameDob,
          row
        )
      )

      maybeIsTransactionSupportedByIndependentValuation = isTransactionSupportedByIndependentValuation.value.flatMap(
        p =>
          validateYesNoQuestion(
            totalSaleProceedIfAnyDisposal.as(p),
            s"landOrProperty.isTransactionSupported",
            memberFullNameDob,
            row
          )
      )

      maybeHasLandOrPropertyFullyDisposedOf = hasLandOrPropertyFullyDisposedOf.value.flatMap(p =>
        validateYesNoQuestion(
          hasLandOrPropertyFullyDisposedOf.as(p),
          s"landOrProperty.isFullyDisposedOf",
          memberFullNameDob,
          row
        )
      )

      disposalDetails <- (
        validatedWereAnyDisposalOnThisDuringTheYear,
        maybeDisposalAmount,
        maybeNames,
        maybeConnected,
        maybeIsTransactionSupportedByIndependentValuation,
        maybeHasLandOrPropertyFullyDisposedOf
      ) match {
        case (Valid(isLeased), mAmount, mNames, mConnected, mIndependent, mFully) if isLeased.toUpperCase == "YES" =>
          (mAmount, mNames, mConnected, mIndependent, mFully) match {
            case (Some(amount), Some(names), Some(connected), Some(independent), Some(fully)) =>
              Some((amount, names, connected, independent, fully).mapN {
                (_amount, _names, _connected, _independent, _fully) =>
                  (
                    Yes,
                    Some(
                      DisposalDetails(
                        _amount.value,
                        _names,
                        YesNo.withNameInsensitive(_connected),
                        YesNo.withNameInsensitive(_independent),
                        YesNo.withNameInsensitive(_fully)
                      )
                    )
                  )
              })
            case _ =>
              val listEmpty = List.empty[Option[ValidationError]]
              val optAmount = if (mAmount.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Price,
                    "landOrProperty.disposedAmount.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optNames = if (mNames.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.FreeText,
                    "landOrProperty.disposedNames.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optConnected = if (mConnected.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.YesNoQuestion,
                    "landOrProperty.anyConnectedPurchaser.upload.error.required"
                  )
                )
              } else {
                None
              }

              val optIndependent = if (mIndependent.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Price,
                    "landOrProperty.isTransactionSupported.upload.error.required"
                  )
                )
              } else {
                None
              }
              val optFully = if (mFully.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Price,
                    "landOrProperty.isFullyDisposedOf.upload.error.required"
                  )
                )
              } else {
                None
              }

              val errors = listEmpty :+ optAmount :+ optNames :+ optConnected :+ optIndependent :+ optFully
              Some(Invalid(NonEmptyList.fromListUnsafe(errors.flatten)))
          }

        case (Valid(isLeased), _, _, _, _, _) if isLeased.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield disposalDetails

}
