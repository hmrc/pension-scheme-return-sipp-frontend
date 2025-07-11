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
import cats.syntax.all.*
import forms.*
import models.*
import models.requests.common.YesNo.{No, Yes}
import models.requests.common.*

import javax.inject.Inject

class UnquotedSharesValidationsService @Inject() (
  nameDOBFormProvider: NameDOBFormProvider,
  textFormProvider: TextFormProvider,
  datePageFormProvider: DatePageFormProvider,
  moneyFormProvider: MoneyFormProvider,
  intFormProvider: IntFormProvider,
  doubleFormProvider: DoubleFormProvider
) extends ValidationsService(
      nameDOBFormProvider,
      textFormProvider,
      datePageFormProvider,
      moneyFormProvider,
      intFormProvider,
      doubleFormProvider
    ) {

  def validateShareCompanyDetails(
    companySharesName: CsvValue[String],
    companySharesCRN: CsvValue[Option[String]],
    reasonNoCRN: CsvValue[Option[String]],
    sharesClass: CsvValue[Option[String]],
    noOfSharesHeld: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, SharesCompanyDetails]] = {
    val companyName = validateFreeText(
      text = companySharesName,
      key = "unquotedShares.companySharesName",
      memberFullName = memberFullNameDob,
      row = row
    )

    val maybeCrn = companySharesCRN.value.flatMap(crn =>
      validateCrn(companySharesCRN.as(crn), memberFullNameDob, row, "unquotedShares.companySharesCrn")
    )

    val maybeValidatedReasonNoCRN = reasonNoCRN.value.flatMap(reason =>
      validateFreeText(
        reasonNoCRN.as(reason),
        "unquotedShares.reasonNoCRN",
        memberFullNameDob,
        row
      )
    )

    val maybeValidatedShareClass = sharesClass.value.flatMap(sClass =>
      validateFreeText(
        sharesClass.as(sClass),
        "unquotedShares.sharesClass",
        memberFullNameDob,
        row
      )
    )

    val maybeValidatedNoOfShares = noOfSharesHeld.value.flatMap(number =>
      validateCount(
        noOfSharesHeld.as(number),
        "unquotedShares.noOfShares",
        memberFullNameDob,
        row,
        maxCount = 9999999
      )
    )

    (companyName, maybeCrn, maybeValidatedReasonNoCRN, maybeValidatedShareClass, maybeValidatedNoOfShares) match {
      case (Some(name), Some(crn), _, Some(shareClass), Some(noOfShares)) =>
        Some(
          (name, crn, shareClass, noOfShares).mapN((validName, validCrn, validShareClass, validNoOfShares) =>
            SharesCompanyDetails(
              companySharesName = validName,
              companySharesCRN = Some(validCrn),
              reasonNoCRN = None,
              sharesClass = validShareClass,
              noOfShares = validNoOfShares
            )
          )
        )

      case (Some(name), None, Some(reason), Some(shareClass), Some(noOfShares)) =>
        Some(
          (name, reason, shareClass, noOfShares).mapN((validName, validReason, validShareClass, validNoOfShares) =>
            SharesCompanyDetails(
              companySharesName = validName,
              companySharesCRN = None,
              reasonNoCRN = Some(validReason),
              sharesClass = validShareClass,
              noOfShares = validNoOfShares
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
              ValidationErrorType.FreeText,
              "unquotedShares.shareCompanyName.error.required"
            ).invalidNel
          )
        } else if (mShareClass.isEmpty) {
          Some(
            ValidationError(row, ValidationErrorType.FreeText, "unquotedShares.shareClass.error.required").invalidNel
          )
        } else if (noOfShares.isEmpty) {
          Some(
            ValidationError(
              row,
              ValidationErrorType.Count,
              "unquotedShares.noOfShares.upload.error.required"
            ).invalidNel
          )
        } else
          None
    }
  }

  def validateDisposals(
    wereAnyDisposalOnThisDuringTheYear: CsvValue[String],
    disposedSharesAmt: CsvValue[Option[String]],
    purchaserNames: CsvValue[Option[String]],
    disposalConnectedParty: CsvValue[Option[String]],
    independentValuation: CsvValue[Option[String]],
    noOfSharesSold: CsvValue[Option[String]],
    noOfSharesHeld: CsvValue[Option[String]],
    memberFullNameDob: String,
    row: Int
  ): Option[ValidatedNel[ValidationError, (YesNo, Option[UnquotedShareDisposalDetail])]] =
    for {
      validatedWereAnyDisposalOnThisDuringTheYear <- validateYesNoQuestionTyped(
        wereAnyDisposalOnThisDuringTheYear,
        "unquotedShares.wereAnyDisposalOnThisDuringTheYear",
        memberFullNameDob,
        row
      )

      maybeDisposalAmount = disposedSharesAmt.value.flatMap(p =>
        validatePrice(
          disposedSharesAmt.as(p),
          "unquotedShares.totalConsiderationAmountSaleIfAnyDisposal",
          memberFullNameDob,
          row
        )
      )

      maybeNamesOfPurchasers = purchaserNames.value.flatMap(n =>
        validateFreeText(
          purchaserNames.as(n),
          "unquotedShares.namesOfPurchasers",
          memberFullNameDob,
          row
        )
      )

      maybeConnected = disposalConnectedParty.value.flatMap { connected =>
        validateYesNoQuestionTyped(
          disposalConnectedParty.copy(value = connected),
          "unquotedShares.areAnyDisposalsConnectedParty",
          memberFullNameDob,
          row
        )
      }

      maybeIsTransactionSupportedByIndependentValuation = independentValuation.value.flatMap(p =>
        validateYesNoQuestionTyped(
          independentValuation.as(p),
          "unquotedShares.isTransactionSupportedByIndependentValuation",
          memberFullNameDob,
          row
        )
      )

      maybeNoOfSharesSold = noOfSharesSold.value.flatMap { s =>
        validateCount(
          noOfSharesSold.as(s),
          "unquotedShares.noOfSharesSold",
          memberFullNameDob,
          row = row,
          maxCount = 999999999,
          minCount = 0
        )
      }

      maybeNoOfSharesHeld = noOfSharesHeld.value.flatMap { s =>
        validateCount(
          noOfSharesHeld.as(s),
          "unquotedShares.noOfSharesHeld",
          memberFullNameDob,
          row = row,
          maxCount = 999999999,
          minCount = 0
        )
      }

      disposalDetails <- (
        validatedWereAnyDisposalOnThisDuringTheYear,
        maybeDisposalAmount,
        maybeNamesOfPurchasers,
        maybeConnected,
        maybeIsTransactionSupportedByIndependentValuation,
        maybeNoOfSharesSold,
        maybeNoOfSharesHeld
      ) match {
        case (Valid(Yes), mAmount, mPurchasers, mConnected, mIndependent, mSold, mHeld) =>
          doValidateDisposals(mAmount, mPurchasers, mConnected, mIndependent, mSold, mHeld, row)

        case (Valid(No), _, _, _, _, _, _) =>
          Some((No, None).validNel)

        case (e @ Invalid(_), _, _, _, _, _, _) => Some(e)

        case _ => None
      }
    } yield disposalDetails

  private def doValidateDisposals(
    mAmount: Option[ValidatedNel[ValidationError, Money]],
    mPurchasers: Option[ValidatedNel[ValidationError, String]],
    mConnected: Option[ValidatedNel[ValidationError, YesNo]],
    mIndependent: Option[ValidatedNel[ValidationError, YesNo]],
    mSharesSold: Option[ValidatedNel[ValidationError, Int]],
    mSharesHeld: Option[ValidatedNel[ValidationError, Int]],
    row: Int
  ): Option[ValidatedNel[ValidationError, (YesNo, Option[UnquotedShareDisposalDetail])]] =
    (mAmount, mPurchasers, mConnected, mIndependent, mSharesSold, mSharesHeld) match {
      case (Some(amount), Some(purchasers), Some(connected), Some(independent), Some(sold), Some(held)) =>
        Some(
          (amount.map(_.value), purchasers, connected, independent, sold, held)
            .mapN(UnquotedShareDisposalDetail.apply)
            .map(Yes -> _.some)
        )

      case _ =>
        def checkRequiredV = checkRequired(row, "unquotedShares")
        import ValidationErrorType.{Price, FreeText, YesNoQuestion}
        mergeErrors(
          checkRequiredV(mAmount, "totalConsiderationAmountSaleIfAnyDisposal", Price),
          checkRequiredV(mPurchasers, "namesOfPurchasers", FreeText),
          checkRequiredV(mConnected, "areAnyPurchasersConnectedParty", YesNoQuestion),
          checkRequiredV(mIndependent, "isDisposalSupportedByIndependentValuation", YesNoQuestion)
        )
    }

  def validateShareTransaction(
    totalCost: CsvValue[String],
    independentValuation: CsvValue[String],
    totalDividendsIncome: CsvValue[String],
    memberFullNameDob: String,
    row: Int
  ): Option[Validated[NonEmptyList[ValidationError], UnquotedShareTransactionDetail]] =
    for {
      maybeTotalCost <- validatePrice(
        totalCost,
        "unquotedShares.totalConsiderationAmount",
        memberFullNameDob,
        row
      )

      maybeSupportedByIndependentValuation <- validateYesNoQuestionTyped(
        independentValuation,
        "unquotedShares.isTransactionSupportedByIndependentValuation",
        memberFullNameDob,
        row
      )

      maybeDividendsIncome <- validatePrice(
        totalDividendsIncome,
        "unquotedShares.totalDividendsIncome",
        memberFullNameDob,
        row
      )
    } yield (
      maybeTotalCost.map(_.value),
      maybeSupportedByIndependentValuation,
      maybeDividendsIncome.map(_.value)
    ).mapN(UnquotedShareTransactionDetail(_, _, _))
}
