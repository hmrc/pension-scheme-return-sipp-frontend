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
import forms._
import generators.Generators
import models.ValidationErrorType._
import models.requests.common.YesNo.{No, Yes}
import models.requests.common.ShareDisposalDetail
import models.{CsvHeaderKey, CsvValue, ValidationError}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi

class AssetsFromConnectedPartyValidationsServiceSpec
    extends AnyFreeSpec
    with ScalaCheckPropertyChecks
    with Generators
    with Matchers {

  private val nameDOBFormProvider = new NameDOBFormProvider {}
  private val textFormProvider = new TextFormProvider {}
  private val datePageFormProvider = new DatePageFormProvider {}
  private val moneyFormProvider = new MoneyFormProvider {}
  private val intFormProvider = new IntFormProvider {}
  private val doubleFormProvider = new DoubleFormProvider {}
  private val validator = new AssetsFromConnectedPartyValidationsService(
    nameDOBFormProvider,
    textFormProvider,
    datePageFormProvider,
    moneyFormProvider,
    intFormProvider,
    doubleFormProvider
  )

  val row = 1
  val csvKey = CsvHeaderKey(key = "test", cell = "A", index = 1)
  val formKey = "key"
  val name = "fullName"
  val freeTextWith161Chars =
    "LoremipsumdolorsitametconsecteturadipisicingelitseddoeiusmodtemporincididuntutlaboreetdoloremagnaaliquaUtencoadminimveniamquisnostrudexercitationullamcolaborisnisiutaliquipexeacommodoconsequatDuisauteiruredolorinreprehenderitinvoluptatevelitessecillumdoloreeufugiatnullapariaturExcepteursintoccaecatcupidatatnonproidentsuntinculpaquiofficiadeseruntmollitanimidestlaboruma"

  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())

  "assetConnectedPartyValidationsServiceSpec" - {

    "validateShareCompanyDetails" - {

      "return required error if acquisitionOfShares is Yes, but no companySharesName entered" in {
        val validation = validator.validateShareCompanyDetails(
          acquisitionOfShares = CsvValue(csvKey, "Yes"),
          companySharesName = CsvValue(csvKey, None),
          companySharesCRN = CsvValue(csvKey, None),
          reasonNoCRN = CsvValue(csvKey, Some("test")),
          sharesClass = CsvValue(csvKey, Some("test")),
          noOfShares = CsvValue(csvKey, Some("123")),
          memberFullNameDob = name,
          row
        )

        checkError(
          validation,
          List(genErr(FreeText, s"assetConnectedParty.shareCompanyName.error.required"))
        )
      }

      "return required error if acquisitionOfShares is Yes, but no sharesClass entered" in {
        val validation = validator.validateShareCompanyDetails(
          acquisitionOfShares = CsvValue(csvKey, "Yes"),
          companySharesName = CsvValue(csvKey, Some("name")),
          companySharesCRN = CsvValue(csvKey, None),
          reasonNoCRN = CsvValue(csvKey, Some("test")),
          sharesClass = CsvValue(csvKey, None),
          noOfShares = CsvValue(csvKey, Some("123")),
          memberFullNameDob = name,
          row
        )

        checkError(
          validation,
          List(genErr(FreeText, s"assetConnectedParty.shareClass.error.required"))
        )
      }

      "return required error if acquisitionOfShares is Yes, but no noOfShares entered" in {
        val validation = validator.validateShareCompanyDetails(
          acquisitionOfShares = CsvValue(csvKey, "Yes"),
          companySharesName = CsvValue(csvKey, Some("name")),
          companySharesCRN = CsvValue(csvKey, None),
          reasonNoCRN = CsvValue(csvKey, Some("test")),
          sharesClass = CsvValue(csvKey, Some("test")),
          noOfShares = CsvValue(csvKey, None),
          memberFullNameDob = name,
          row
        )

        checkError(
          validation,
          List(genErr(Count, s"assetConnectedParty.numberOfShares.error.required"))
        )
      }

      "return required error if acquisitionOfShares is Yes, but no companySharesCRN or reasonNoCRN entered" in {
        val validation = validator.validateShareCompanyDetails(
          acquisitionOfShares = CsvValue(csvKey, "Yes"),
          companySharesName = CsvValue(csvKey, Some("test")),
          companySharesCRN = CsvValue(csvKey, None),
          reasonNoCRN = CsvValue(csvKey, None),
          sharesClass = CsvValue(csvKey, Some("test")),
          noOfShares = CsvValue(csvKey, Some("1")),
          memberFullNameDob = name,
          row
        )

        checkError(
          validation,
          List(genErr(FreeText, s"assetConnectedParty.reasonNoCRN.error.required"))
        )
      }

      "return required error if acquisitionOfShares is Yes, but noOfShares is invalid" in {
        val validation = validator.validateShareCompanyDetails(
          acquisitionOfShares = CsvValue(csvKey, "Yes"),
          companySharesName = CsvValue(csvKey, Some("test")),
          companySharesCRN = CsvValue(csvKey, None),
          reasonNoCRN = CsvValue(csvKey, Some("test")),
          sharesClass = CsvValue(csvKey, Some("test")),
          noOfShares = CsvValue(csvKey, Some("test")),
          memberFullNameDob = name,
          row
        )

        checkError(
          validation,
          List(genErr(Count, s"assetConnectedParty.noOfShares.upload.error.invalid"))
        )
      }

    }

    "validateDisposals" - {

      "return valid when disposals and all details are entered" in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, Some("100")),
          namesOfPurchasers = CsvValue(csvKey, Some("test")),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, Some("Yes")),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("Yes")),
          fullyDisposed = CsvValue(csvKey, Some("Yes")),
          disposalOfShares = CsvValue(csvKey, Some("Yes")),
          noOfSharesHeld = CsvValue(csvKey, Some("0")),
          memberFullNameDob = name,
          row
        )

        checkSuccess(
          validation,
          (Yes, Some(ShareDisposalDetail(100.0, "test", Yes, Yes, Yes, Yes, Some(0))))
        )
      }

      "return valid when wereAnyDisposalOnThisDuringTheYear is NO and details are not present " in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "No"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, None),
          namesOfPurchasers = CsvValue(csvKey, None),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, None),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, None),
          fullyDisposed = CsvValue(csvKey, None),
          disposalOfShares = CsvValue(csvKey, None),
          noOfSharesHeld = CsvValue(csvKey, None),
          memberFullNameDob = name,
          row
        )

        checkSuccess(
          validation,
          (No, None)
        )
      }

      "return valid when fullyDisposed is Yes and disposalOfShares is Yes and noOfSharesHeld is 0" in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, Some("100")),
          namesOfPurchasers = CsvValue(csvKey, Some("test")),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, Some("Yes")),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("Yes")),
          fullyDisposed = CsvValue(csvKey, Some("Yes")),
          disposalOfShares = CsvValue(csvKey, Some("Yes")),
          noOfSharesHeld = CsvValue(csvKey, Some("0")),
          memberFullNameDob = name,
          row
        )

        checkSuccess(
          validation,
          (Yes, Some(ShareDisposalDetail(100.0, "test", Yes, Yes, Yes, Yes, Some(0))))
        )
      }

      "return has to be zero error if fullyDisposed is Yes and disposalOfShares is Yes but noOfSharesHeld is not 0" in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, Some("100")),
          namesOfPurchasers = CsvValue(csvKey, Some("test")),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, Some("Yes")),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("Yes")),
          fullyDisposed = CsvValue(csvKey, Some("Yes")),
          disposalOfShares = CsvValue(csvKey, Some("Yes")),
          noOfSharesHeld = CsvValue(csvKey, Some("123")),
          memberFullNameDob = name,
          row
        )

        checkError(
          validation,
          List(
            genErr(
              Count,
              s"assetConnectedParty.noOfSharesHeld.upload.error.hasToBeZero"
            )
          )
        )
      }

      "return valid when fullyDisposed is No and disposalOfShares is Yes and noOfSharesHeld is 0" in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, Some("100")),
          namesOfPurchasers = CsvValue(csvKey, Some("test")),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, Some("Yes")),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("Yes")),
          fullyDisposed = CsvValue(csvKey, Some("No")),
          disposalOfShares = CsvValue(csvKey, Some("Yes")),
          noOfSharesHeld = CsvValue(csvKey, Some("0")),
          memberFullNameDob = name,
          row
        )

        checkSuccess(
          validation,
          (Yes, Some(ShareDisposalDetail(100.0, "test", Yes, Yes, No, Yes, Some(0))))
        )
      }

      "return valid when fullyDisposed is No and disposalOfShares is Yes and noOfSharesHeld is bigger than 0" in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, Some("100")),
          namesOfPurchasers = CsvValue(csvKey, Some("test")),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, Some("Yes")),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("Yes")),
          fullyDisposed = CsvValue(csvKey, Some("No")),
          disposalOfShares = CsvValue(csvKey, Some("Yes")),
          noOfSharesHeld = CsvValue(csvKey, Some("123")),
          memberFullNameDob = name,
          row
        )

        checkSuccess(
          validation,
          (Yes, Some(ShareDisposalDetail(100.0, "test", Yes, Yes, No, Yes, Some(123))))
        )
      }

      "return valid when fullyDisposed is Yes and disposalOfShares is No and noOfSharesHeld is some valid number" in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, Some("100")),
          namesOfPurchasers = CsvValue(csvKey, Some("test")),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, Some("Yes")),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("Yes")),
          fullyDisposed = CsvValue(csvKey, Some("Yes")),
          disposalOfShares = CsvValue(csvKey, Some("No")),
          noOfSharesHeld = CsvValue(csvKey, Some("222")),
          memberFullNameDob = name,
          row
        )

        checkSuccess(
          validation,
          (Yes, Some(ShareDisposalDetail(100.0, "test", Yes, Yes, Yes, No, Some(222))))
        )
      }

      "return valid when fullyDisposed is Yes and disposalOfShares is No and noOfSharesHeld is none" in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, Some("100")),
          namesOfPurchasers = CsvValue(csvKey, Some("test")),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, Some("Yes")),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("Yes")),
          fullyDisposed = CsvValue(csvKey, Some("Yes")),
          disposalOfShares = CsvValue(csvKey, Some("No")),
          noOfSharesHeld = CsvValue(csvKey, None),
          memberFullNameDob = name,
          row
        )

        checkSuccess(
          validation,
          (Yes, Some(ShareDisposalDetail(100.0, "test", Yes, Yes, Yes, No, None)))
        )
      }

      "return valid when fullyDisposed is No and disposalOfShares is No and noOfSharesHeld is some valid number" in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, Some("100")),
          namesOfPurchasers = CsvValue(csvKey, Some("test")),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, Some("Yes")),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("Yes")),
          fullyDisposed = CsvValue(csvKey, Some("No")),
          disposalOfShares = CsvValue(csvKey, Some("No")),
          noOfSharesHeld = CsvValue(csvKey, Some("111")),
          memberFullNameDob = name,
          row
        )

        checkSuccess(
          validation,
          (Yes, Some(ShareDisposalDetail(100.0, "test", Yes, Yes, No, No, Some(111))))
        )
      }

      "return valid when fullyDisposed is No and disposalOfShares is No and noOfSharesHeld is none" in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, Some("100")),
          namesOfPurchasers = CsvValue(csvKey, Some("test")),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, Some("Yes")),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("Yes")),
          fullyDisposed = CsvValue(csvKey, Some("No")),
          disposalOfShares = CsvValue(csvKey, Some("No")),
          noOfSharesHeld = CsvValue(csvKey, None),
          memberFullNameDob = name,
          row
        )

        checkSuccess(
          validation,
          (Yes, Some(ShareDisposalDetail(100.0, "test", Yes, Yes, No, No, None)))
        )
      }

      "return malformed error for noOfSharesHeld when fullyDisposed is No and disposalOfShares is No and noOfSharesHeld is wrong" in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, Some("100")),
          namesOfPurchasers = CsvValue(csvKey, Some("test")),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, Some("Yes")),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("Yes")),
          fullyDisposed = CsvValue(csvKey, Some("No")),
          disposalOfShares = CsvValue(csvKey, Some("No")),
          noOfSharesHeld = CsvValue(csvKey, Some("ADS")),
          memberFullNameDob = name,
          row
        )

        checkError(
          validation,
          List(
            genErr(
              Count,
              s"assetConnectedParty.noOfSharesHeld.upload.error.invalid"
            )
          )
        )
      }

      "return required error if wereAnyDisposalOnThisDuringTheYear is Yes, but totalConsiderationAmountSaleIfAnyDisposal is malformed " in {
        val validation = validator.validateDisposals(
          wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
          totalConsiderationAmountSaleIfAnyDisposal = CsvValue(csvKey, Some("aaaa")),
          namesOfPurchasers = CsvValue(csvKey, Some("test")),
          areAnyPurchasersConnectedParty = CsvValue(csvKey, Some("Yes")),
          isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("Yes")),
          fullyDisposed = CsvValue(csvKey, Some("Yes")),
          disposalOfShares = CsvValue(csvKey, Some("Yes")),
          noOfSharesHeld = CsvValue(csvKey, Some("0")),
          memberFullNameDob = name,
          row
        )

        checkError(
          validation,
          List(
            genErr(
              Price,
              s"assetConnectedParty.totalConsiderationAmountSaleIfAnyDisposal.upload.error.numericValueRequired"
            )
          )
        )
      }

    }

  }

  private def genErr(errType: ValidationErrorType, errKey: String) =
    ValidationError(row, errType, errKey)

  private def checkError[T](
    validation: Option[ValidatedNel[ValidationError, T]],
    expectedErrors: List[ValidationError]
  ) = {
    validation.get.isInvalid mustBe true
    validation.get match {
      case Validated.Invalid(errors) =>
        val errorList: NonEmptyList[ValidationError] = errors
        errorList.toList mustBe expectedErrors
      case _ =>
        fail("Expected to get invalid")
    }
  }

  private def checkSuccess[T](validation: Option[ValidatedNel[ValidationError, T]], expectedObject: T) = {
    validation.get.isValid mustBe true
    validation.get match {
      case Validated.Valid(success) =>
        success mustBe expectedObject
      case _ =>
        fail("Expected to get valid object")
    }
  }
}
