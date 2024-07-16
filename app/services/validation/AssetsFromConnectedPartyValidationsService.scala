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
import cats.implicits._
import forms._
import models._
import models.requests.common.YesNo
import models.requests.common.YesNo.{No, Yes}
import models.requests.common._

import javax.inject.Inject

class AssetsFromConnectedPartyValidationsService @Inject()(
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

  def validateShareCompanyDetails(
    acquisitionOfShares: CsvValue[String],
    companySharesName: CsvValue[Option[String]],
    companySharesCRN: CsvValue[Option[String]],
    reasonNoCRN: CsvValue[Option[String]],
    sharesClass: CsvValue[Option[String]],
    noOfShares: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, Option[SharesCompanyDetails]]] = {

    for {

      validatedAcquisitionOfShares <- validateYesNoQuestion(
        acquisitionOfShares,
        "assetConnectedParty.isAcquisitionOfShares",
        memberFullNameDob,
        row
      )

      maybeValidatedCompanyName = companySharesName.value.flatMap(
        name =>
          validateFreeText(
            companySharesName.as(name),
            "assetConnectedParty.companySharesName",
            memberFullNameDob,
            row
          )
      )

      maybeCrn = companySharesCRN.value.flatMap(
        crn => validateCrn(companySharesCRN.as(crn), memberFullNameDob, row, "assetConnectedParty.companySharesCrn")
      )

      maybeValidatedReasonNoCRN = reasonNoCRN.value.flatMap(
        reason =>
          validateFreeText(
            reasonNoCRN.as(reason),
            "assetConnectedParty.reasonNoCRN",
            memberFullNameDob,
            row
          )
      )

      maybeValidatedShareClass = sharesClass.value.flatMap(
        sClass =>
          validateFreeText(
            sharesClass.as(sClass),
            "assetConnectedParty.sharesClass",
            memberFullNameDob,
            row
          )
      )

      maybeValidatedNoOfShares = noOfShares.value.flatMap(
        number =>
          validateCount(
            noOfShares.as(number),
            "assetConnectedParty.noOfShares",
            memberFullNameDob,
            row,
            maxCount = 9999999,
            minCount = 0
          )
      )

      validatedSharesCompanyDetails <- (
        validatedAcquisitionOfShares,
        maybeValidatedCompanyName,
        maybeCrn,
        maybeValidatedReasonNoCRN,
        maybeValidatedShareClass,
        maybeValidatedNoOfShares
      ) match {
        case (Valid(acquisitionOfShares), mCompanyName, mCrn, mReason, mShareClass, mValidatedNoOfShares)
            if acquisitionOfShares.toUpperCase == "YES" =>
          (mCompanyName, mCrn, mReason, mShareClass, mValidatedNoOfShares) match {

            case (Some(name), Some(crn), _, Some(shareClass), Some(noOfShares)) =>
              Some(
                (name, crn, shareClass, noOfShares).mapN(
                  (validName, validCrn, validShareClass, validNoOfShares) =>
                    Some(
                      SharesCompanyDetails(
                        companySharesName = validName,
                        companySharesCRN = Some(validCrn),
                        reasonNoCRN = None,
                        sharesClass = validShareClass,
                        noOfShares = validNoOfShares
                      )
                    )
                )
              )

            case (Some(name), None, Some(reason), Some(shareClass), Some(noOfShares)) =>
              Some(
                (name, reason, shareClass, noOfShares).mapN(
                  (validName, validReason, validShareClass, validNoOfShares) =>
                    Some(
                      SharesCompanyDetails(
                        companySharesName = validName,
                        companySharesCRN = None,
                        reasonNoCRN = Some(validReason),
                        sharesClass = validShareClass,
                        noOfShares = validNoOfShares
                      )
                    )
                )
              )

            case (_, None, None, _, _) =>
              Some(
                ValidationError(
                  row,
                  ValidationErrorType.FreeText,
                  message = "assetConnectedParty.reasonNoCRN.error.required"
                ).invalidNel
              )

            case (name, _, _, mShareClass, noOfShares) =>
              if (name.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.FreeText,
                    "assetConnectedParty.shareCompanyName.error.required"
                  ).invalidNel
                )
              } else if (mShareClass.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.FreeText,
                    "assetConnectedParty.shareClass.error.required"
                  ).invalidNel
                )
              } else if (noOfShares.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Count,
                    "assetConnectedParty.numberOfShares.error.required"
                  ).invalidNel
                )
              } else
                None

          }

        case (Valid(_), _, _, _, _, _) =>
          Some(None.validNel)

        case (e @ Invalid(_), _, _, _, _, _) => Some(e)

      }

    } yield validatedSharesCompanyDetails
  }

  def validateDisposals(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    totalConsiderationAmountSaleIfAnyDisposal: CsvValue[Option[String]],
    namesOfPurchasers: CsvValue[Option[String]],
    areAnyPurchasersConnectedParty: CsvValue[Option[String]],
    isTransactionSupportedByIndependentValuation: CsvValue[Option[String]],
    disposalOfShares: CsvValue[Option[String]],
    noOfSharesHeld: CsvValue[Option[String]],
    fullyDisposed: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, (YesNo, Option[ShareDisposalDetail])]] =
    for {
      validatedWereAnyDisposalOnThisDuringTheYear <- validateYesNoQuestionTyped(
        wereAnyDisposalOnThisDuringTheYear,
        "assetConnectedParty.wereAnyDisposalOnThisDuringTheYear",
        memberFullNameDob,
        row
      )

      maybeDisposalAmount = totalConsiderationAmountSaleIfAnyDisposal.value.flatMap(
        p =>
          validatePrice(
            totalConsiderationAmountSaleIfAnyDisposal.as(p),
            s"assetConnectedParty.totalConsiderationAmountSaleIfAnyDisposal",
            memberFullNameDob,
            row
          )
      )

      maybeNamesOfPurchasers = namesOfPurchasers.value.flatMap(
        n =>
          validateFreeText(
            namesOfPurchasers.as(n),
            s"assetConnectedParty.namesOfPurchasers",
            memberFullNameDob,
            row
          )
      )

      maybeConnected = areAnyPurchasersConnectedParty.value.flatMap(
        yN =>
          validateYesNoQuestionTyped(
            areAnyPurchasersConnectedParty.as(yN),
            s"assetConnectedParty.areAnyPurchasersConnectedParty",
            memberFullNameDob,
            row
          )
      )

      maybeIsTransactionSupportedByIndependentValuation = isTransactionSupportedByIndependentValuation.value.flatMap(
        p =>
          validateYesNoQuestionTyped(
            isTransactionSupportedByIndependentValuation.as(p),
            s"assetConnectedParty.isTransactionSupportedByIndependentValuation",
            memberFullNameDob,
            row
          )
      )

      maybeDisposalOfShares = disposalOfShares.value.flatMap(
        p =>
          validateYesNoQuestionTyped(
            disposalOfShares.as(p),
            s"assetConnectedParty.disposalOfShares",
            memberFullNameDob,
            row
          )
      )

      maybeNoOfSharesHeld = noOfSharesHeld.value.flatMap(
        p =>
          validateCount(
            disposalOfShares.as(p),
            s"assetConnectedParty.noOfSharesHeld",
            memberFullNameDob,
            row,
            maxCount = 999999999,
            minCount = 0
          )
      )

      maybeFullyDisposed = fullyDisposed.value.flatMap(
        p =>
          validateYesNoQuestionTyped(
            fullyDisposed.as(p),
            s"assetConnectedParty.fullyDisposed",
            memberFullNameDob,
            row
          )
      )

      disposalDetails <- (
        validatedWereAnyDisposalOnThisDuringTheYear,
        maybeDisposalAmount,
        maybeNamesOfPurchasers,
        maybeConnected,
        maybeIsTransactionSupportedByIndependentValuation,
        maybeDisposalOfShares,
        maybeNoOfSharesHeld,
        maybeFullyDisposed
      ) match {
        case (
            Valid(wereDisposals),
            mAmount,
            mPurchasers,
            mConnected,
            mIndependent,
            mDisposalOfShares,
            mNumShares,
            mFully
            ) if wereDisposals.boolean =>
          doValidateDisposals(
            mAmount,
            mPurchasers,
            mConnected,
            mIndependent,
            mDisposalOfShares,
            mNumShares,
            mFully,
            row
          )

        case (Valid(wereDisposals), _, _, _, _, _, _, _) if !wereDisposals.boolean =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield disposalDetails

  private def doValidateDisposals(
    mAmount: Option[ValidatedNel[ValidationError, Money]],
    mPurchasers: Option[ValidatedNel[ValidationError, String]],
    mConnected: Option[ValidatedNel[ValidationError, YesNo]],
    mIndependent: Option[ValidatedNel[ValidationError, YesNo]],
    mDisposalOfShares: Option[ValidatedNel[ValidationError, YesNo]],
    mNumShares: Option[ValidatedNel[ValidationError, Int]],
    mFully: Option[ValidatedNel[ValidationError, YesNo]],
    row: Int
  ) = {
    (mAmount, mPurchasers, mConnected, mIndependent, mDisposalOfShares, mNumShares, mFully) match {

      case (
          Some(amount),
          Some(purchasers),
          Some(connected),
          Some(independent),
          Some(disposal),
          mNumShares,
          mFully
          ) =>
        (disposal, mNumShares, mFully) match {

          case (Valid(isDisposal), Some(numShares), Some(fully)) if isDisposal.boolean =>
            Some(
              (amount, purchasers, connected, independent, numShares, fully).mapN {
                (_amount, _purchasers, _connected, _independent, _numShares, _fully) =>
                  (
                    Yes,
                    Some(
                      ShareDisposalDetail(
                        _amount.value,
                        _purchasers,
                        _connected,
                        _independent,
                        isDisposal,
                        Some(_numShares),
                        Some(_fully)
                      )
                    )
                  )
              }
            )

          case (Valid(isDisposal), _, _) if !isDisposal.boolean =>
            Some(
              (amount, purchasers, connected, independent).mapN { (_amount, _purchasers, _connected, _independent) =>
                (
                  Yes,
                  Some(
                    ShareDisposalDetail(
                      _amount.value,
                      _purchasers,
                      _connected,
                      _independent,
                      isDisposal,
                      None,
                      None
                    )
                  )
                )
              }
            )

          case (Valid(_), _, _) =>
            val listEmpty = List.empty[Option[ValidationError]]

            val optNumShares = if (disposal.map(_.boolean).getOrElse(false) && mNumShares.isEmpty) {
              Some(
                ValidationError(
                  row,
                  errorType = ValidationErrorType.Price,
                  "assetConnectedParty.noOfSharesHeld.upload.error.required"
                )
              )
            } else {
              None
            }

            val optFullyDisposed = if (disposal.map(_.boolean).getOrElse(false) && mFully.isEmpty) {
              Some(
                ValidationError(
                  row,
                  errorType = ValidationErrorType.Price,
                  "assetConnectedParty.fullyDisposed.upload.error.required"
                )
              )
            } else {
              None
            }

            val errors = listEmpty :+ optNumShares :+ optFullyDisposed
            Some(Invalid(NonEmptyList.fromListUnsafe(errors.flatten)))

          case (e @ Invalid(_), _, _) => Some(e)
        }

      case _ =>
        val listEmpty = List.empty[Option[ValidationError]]

        val optTotalConsideration = if (mAmount.isEmpty) {
          Some(
            ValidationError(
              row,
              errorType = ValidationErrorType.Price,
              "assetConnectedParty.totalConsiderationAmountSaleIfAnyDisposal.upload.error.required"
            )
          )
        } else {
          None
        }

        val optPurchasers = if (mPurchasers.isEmpty) {
          Some(
            ValidationError(
              row,
              errorType = ValidationErrorType.FreeText,
              "assetConnectedParty.namesOfPurchasers.upload.error.required"
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
              "assetConnectedParty.areAnyPurchasersConnectedParty.upload.error.required"
            )
          )
        } else {
          None
        }

        val optIndependent = if (mIndependent.isEmpty) {
          Some(
            ValidationError(
              row,
              errorType = ValidationErrorType.YesNoQuestion,
              "assetConnectedParty.isTransactionSupportedByIndependentValuation.upload.error.required"
            )
          )
        } else {
          None
        }

        val optDisposalShares = if (mDisposalOfShares.isEmpty) {
          Some(
            ValidationError(
              row,
              errorType = ValidationErrorType.YesNoQuestion,
              "assetConnectedParty.disposalOfShares.upload.error.required"
            )
          )
        } else {
          None
        }

        val errors = listEmpty :+ optTotalConsideration :+ optPurchasers :+ optConnected :+ optIndependent :+ optDisposalShares
        Some(Invalid(NonEmptyList.fromListUnsafe(errors.flatten)))
    }
  }
}
