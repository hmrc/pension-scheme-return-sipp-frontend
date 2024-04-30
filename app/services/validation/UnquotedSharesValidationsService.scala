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
import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits._
import forms._
import models._
import models.requests.YesNo
import models.requests.YesNo.{No, Yes}
import models.requests.common._
import play.api.data.Form
import play.api.i18n.Messages

import javax.inject.Inject

class UnquotedSharesValidationsService @Inject()(
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
    companySharesName: CsvValue[String],
    companySharesCRN: CsvValue[Option[String]],
    reasonNoCRN: CsvValue[Option[String]],
    sharesClass: CsvValue[Option[String]],
    noOfShares: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, Option[SharesCompanyDetails]]] = {
      val companyName = validateFreeText(
        companySharesName,
        memberFullNameDob,
        "unquotedShares.companySharesName",
        row
      )

      val maybeCrn = companySharesCRN.value.flatMap(
        crn => validateCrn(companySharesCRN.as(crn), memberFullNameDob, row, "unquotedShares.companySharesCrn")
      )

      val maybeValidatedReasonNoCRN = reasonNoCRN.value.flatMap(
        reason =>
          validateFreeText(
            reasonNoCRN.as(reason),
            "unquotedShares.reasonNoCRN",
            memberFullNameDob,
            row
          )
      )

      val maybeValidatedShareClass = sharesClass.value.flatMap(
        sClass =>
          validateFreeText(
            sharesClass.as(sClass),
            "unquotedShares.sharesClas",
            memberFullNameDob,
            row
          )
      )

      val maybeValidatedNoOfShares = noOfShares.value.flatMap(
        number =>
          validateCount(
            noOfShares.as(number),
            "unquotedShares.noOfShare",
            memberFullNameDob,
            row,
            maxCount = 9999999
          )
      )

      (companyName, maybeCrn, maybeValidatedReasonNoCRN, maybeValidatedShareClass, maybeValidatedNoOfShares) match {
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
                  message = "unquotedShares.reasonNoCRN.error.required"
                ).invalidNel
              )

            case (name, _, _, mShareClass, noOfShares) =>
              if (name.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.FreeText,
                    "unquotedShares.shareCompanyName.error.required"
                  ).invalidNel
                )
              } else if (mShareClass.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.FreeText,
                    "unquotedShares.shareClass.error.required"
                  ).invalidNel
                )
              } else if (noOfShares.isEmpty) {
                Some(
                  ValidationError(
                    row,
                    errorType = ValidationErrorType.Count,
                    "unquotedShares.numberOfShares.error.required"
                  ).invalidNel
                )
              } else
                None
          }
  }

  def validateDisposals(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    disposedSharesAmt: CsvValue[Option[String]],
    purchaserName: CsvValue[Option[String]],
    disposalConnectedParty: CsvValue[Option[String]],
    independentValuation: CsvValue[Option[String]],
    noOfSharesHeld: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  )(implicit messages: Messages): Option[ValidatedNel[ValidationError, (YesNo, Option[UnquotedShareDisposalDetail])]] =
    for {
      validatedWereAnyDisposalOnThisDuringTheYear <- validateYesNoQuestion(
        wereAnyDisposalOnThisDuringTheYear,
        "unquotedShares.wereAnyDisposalOnThisDuringTheYear",
        memberFullNameDob,
        row
      )

      maybeDisposalAmount = disposedSharesAmt.value.flatMap(
        p =>
          validatePrice(
            disposedSharesAmt.as(p),
            "unquotedShares.totalConsiderationAmountSaleIfAnyDisposal",
            memberFullNameDob,
            row
          )
      )

      maybeNamesOfPurchasers = purchaserName.value.flatMap(
        n =>
          validateFreeText(
            purchaserName.as(n),
            "unquotedShares.namesOfPurchaser",
            memberFullNameDob,
            row
          )
      )

      maybeConnected = disposalConnectedParty.value.flatMap { connected =>
        validateConnectedOrUnconnected(
          disposalConnectedParty.copy(value = connected),
          "unquotedShares.areAnyDisposalsConnectedParty",
          memberFullNameDob,
          row
        )
      }

      maybeIsTransactionSupportedByIndependentValuation = independentValuation.value.flatMap(
        p =>
          validateYesNoQuestion(
            independentValuation.as(p),
            "unquotedShares.isTransactionSupportedByIndependentValuation",
            memberFullNameDob,
            row
          )
      )

      maybeNoOfSharesHeld = noOfSharesHeld.value.flatMap(
        p =>
          validateCount(
            noOfSharesHeld.as(p),
            "unquotedShares.noOfSharesHeld",
            memberFullNameDob,
            row,
            maxCount = 999999999,
            minCount = 0
          )
      )

      disposalDetails <- (
        validatedWereAnyDisposalOnThisDuringTheYear,
        maybeDisposalAmount,
        maybeNamesOfPurchasers,
        maybeConnected,
        maybeIsTransactionSupportedByIndependentValuation,
        maybeNoOfSharesHeld,
      ) match {
        case (
            Valid(wereDisposals),
            mAmount,
            mPurchasers,
            mConnected,
            mIndependent,
            mNumShares
            ) if wereDisposals.toUpperCase == "YES" =>
          doValidateDisposals(
            mAmount,
            mPurchasers,
            mConnected,
            mIndependent,
            mNumShares,
            row
          )

        case (Valid(wereDisposals), _, _, _, _, _) if wereDisposals.toUpperCase == "NO" =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield disposalDetails

  private def doValidateDisposals(
    mAmount: Option[ValidatedNel[ValidationError, Money]],
    mPurchasers: Option[ValidatedNel[ValidationError, String]],
    mConnected: Option[ValidatedNel[ValidationError, String]],
    mIndependent: Option[ValidatedNel[ValidationError, String]],
    mNumShares: Option[ValidatedNel[ValidationError, Int]],
    row: Int
  ): Option[ValidatedNel[ValidationError, (YesNo, Option[UnquotedShareDisposalDetail])]] = {
    (mAmount, mPurchasers, mConnected, mIndependent, mNumShares) match {

      case (
          Some(amount),
          Some(purchasers),
          Some(connected),
          Some(independent),
          Some(numShares)
          ) =>
        Some(
        (amount, purchasers, connected, independent, numShares).mapN {
                (_amount, _purchasers, _connected, _independent, _numShares) =>
                  (
                    Yes,
                    Some(
                      UnquotedShareDisposalDetail(
                        _amount.value,
                        _purchasers,
                        YesNo.uploadYesNoToRequestYesNo(_connected),
                        YesNo.uploadYesNoToRequestYesNo(_independent),
                        Some(_numShares)
                      )
                    )
                  )
              }
        )

      case _ =>
        val listEmpty = List.empty[Option[ValidationError]]

        val optTotalConsideration = if (mAmount.isEmpty) {
          Some(
            ValidationError(
              row,
              errorType = ValidationErrorType.Price,
              "unquotedShares.totalConsiderationAmountSaleIfAnyDisposal.upload.error.required"
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
              "unquotedShares.namesOfPurchasers.upload.error.required"
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
              "unquotedShares.areAnyPurchasersConnectedParty.upload.error.required"
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
              "unquotedShares.isDisposalSupportedByIndependentValuation.upload.error.required"
            )
          )
        } else {
          None
        }

        val optNoOfShares = if (mNumShares.isEmpty) {
          Some(
            ValidationError(
              row,
              errorType = ValidationErrorType.Count,
              "unquotedShares.isDisposalSupportedByIndependentValuation.upload.error.required"
            )
          )
        } else {
          None
        }

        val errors = listEmpty :+ optTotalConsideration :+ optPurchasers :+ optConnected :+ optIndependent :+ optNoOfShares
        Some(Invalid(NonEmptyList.fromListUnsafe(errors.flatten)))
    }
  }

  def validateConnectedOrUnconnected(
                                      connectedOrUnconnected: CsvValue[String],
                                      key: String,
                                      memberFullName: String,
                                      row: Int
                                    ): Option[ValidatedNel[ValidationError, String]] = {
    val boundForm = connectedOrUnconnectedTypeForm(memberFullName, key)
      .bind(
        Map(
          textFormProvider.formKey -> connectedOrUnconnected.value.toUpperCase
        )
      )

    formToResult(
      boundForm,
      row,
      errorTypeMapping = _ => ValidationErrorType.ConnectedUnconnectedType,
      cellMapping = _ => Some(connectedOrUnconnected.key.cell)
    )
  }

  private def connectedOrUnconnectedTypeForm(memberFullDetails: String, key: String): Form[String] =
    textFormProvider.connectedOrUnconnectedType(
      s"$key.upload.error.required",
      s"$key.upload.error.invalid",
      memberFullDetails
    )

  def validateShareTransaction(
                                totalCost: CsvValue[String],
                                independentValuation: CsvValue[String],
                                noOfSharesSold: CsvValue[Option[String]],
                                totalDividendsIncome: CsvValue[String],
                                memberFullNameDob: String,
                                row: Int
                              ): Option[Validated[NonEmptyList[ValidationError], UnquotedShareTransactionDetail]] = {
    for {
      maybeTotalCost <- validatePrice(
        totalCost,
        "unquotedShares.totalConsiderationAmount",
        memberFullNameDob,
        row
      )

      maybeSupportedByIndependentValuation <- validateYesNoQuestion(
        independentValuation,
        "unquotedShares.isTransactionSupportedByIndependentValuation",
        memberFullNameDob,
        row
      ).map(_.map(YesNo.uploadYesNoToRequestYesNo))

      maybeNoOfSharesSold <- noOfSharesSold.value.flatMap(
        p =>
          validateCount(
            noOfSharesSold.as(p),
            "unquotedShares.noOfSharesSold",
            memberFullNameDob,
            row,
            maxCount = 999999999,
            minCount = 0
          ).map(_.map(_.some))
      ).orElse(Some(Valid(None)))

      maybeDividendsIncome <- validatePrice(
        totalDividendsIncome,
        "unquotedShares.totalDividendsIncome",
        memberFullNameDob,
        row
      )
    } yield {
      (
        maybeTotalCost.map(_.value),
        maybeSupportedByIndependentValuation,
        maybeNoOfSharesSold,
        maybeDividendsIncome.map(_.value)
      ).mapN(UnquotedShareTransactionDetail)
    }
  }
}
