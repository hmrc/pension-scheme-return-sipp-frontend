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

import forms.*
import generators.Generators
import models.ValidationErrorType.*
import models.requests.common.YesNo
import models.requests.common.{DisposalDetails, LesseeDetails, RegistryDetails}
import models.{CsvHeaderKey, CsvValue}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import utils.ValidationSpecUtils.{checkError, checkSuccess, genErr}

import java.time.LocalDate

class LandOrPropertyValidationsServiceSpec
    extends AnyFreeSpec
    with ScalaCheckPropertyChecks
    with Generators
    with Matchers {

  private val nameDOBFormProvider = NameDOBFormProvider()
  private val textFormProvider = TextFormProvider()
  private val datePageFormProvider = DatePageFormProvider()
  private val moneyFormProvider = MoneyFormProvider()
  private val intFormProvider = IntFormProvider()
  private val doubleFormProvider = DoubleFormProvider()
  private val validator = LandOrPropertyValidationsService(
    nameDOBFormProvider,
    textFormProvider,
    datePageFormProvider,
    moneyFormProvider,
    intFormProvider,
    doubleFormProvider
  )

  val row = 1
  val csvKey: CsvHeaderKey = CsvHeaderKey(key = "test", cell = "A", index = 1)
  val formKey = "key"
  val name = "fullName"
  val freeTextWith161Chars =
    "LoremipsumdolorsitametconsecteturadipisicingelitseddoeiusmodtemporincididuntutlaboreetdoloremagnaaliquaUtencoadminimveniamquisnostrudexercitationullamcolaborisnisiutaliquipexeacommodoconsequatDuisauteiruredolorinreprehenderitinvoluptatevelitessecillumdoloreeufugiatnullapariaturExcepteursintoccaecatcupidatatnonproidentsuntinculpaquiofficiadeseruntmollitanimidestlaboruma"

  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())

  "LandOrPropertyValidationsServiceSpec" - {

    "validateIsThereARegistryReference" - {
      "return required error if no isThereARegistryReference entered" in {
        val validation =
          validator.validateIsThereARegistryReference(CsvValue(csvKey, ""), CsvValue(csvKey, ""), name, row)

        checkError(
          validation,
          List(genErr(YesNoQuestion, "landOrProperty.isThereARegistryReference.upload.error.required"))
        )
      }

      "return invalid error if isThereARegistryReference entered other than YES/NO" in {
        val validation =
          validator.validateIsThereARegistryReference(CsvValue(csvKey, "ASD"), CsvValue(csvKey, ""), name, row)

        checkError(
          validation,
          List(genErr(YesNoQuestion, "landOrProperty.isThereARegistryReference.upload.error.invalid"))
        )
      }

      "return required noRegistryReference error if isThereARegistryReference entered as Yes but no reference" in {
        val validation =
          validator.validateIsThereARegistryReference(CsvValue(csvKey, "YES"), CsvValue(csvKey, ""), name, row)

        checkError(
          validation,
          List(genErr(FreeText, "landOrProperty.landRegistryReferenceOrReason.upload.error.required"))
        )
      }

      "return required noRegistryReference error if isThereARegistryReference entered as NO but no reason" in {
        val validation =
          validator.validateIsThereARegistryReference(CsvValue(csvKey, "NO"), CsvValue(csvKey, ""), name, row)

        checkError(
          validation,
          List(genErr(FreeText, "landOrProperty.landRegistryReferenceOrReason.upload.error.required"))
        )
      }

      "return tooLong noRegistryReference error if isThereARegistryReference entered as NO but and noRegistryReference is more than 160 char" in {
        val validation =
          validator.validateIsThereARegistryReference(
            CsvValue(csvKey, "NO"),
            CsvValue(csvKey, freeTextWith161Chars),
            name,
            row
          )

        checkError(
          validation,
          List(genErr(FreeText, "landOrProperty.landRegistryReferenceOrReason.upload.error.tooLong"))
        )
      }

      "return successfully noRegistryReference if isThereARegistryReference is YES but no registry reference" in {
        val validation =
          validator.validateIsThereARegistryReference(CsvValue(csvKey, "YES"), CsvValue(csvKey, ""), name, row)

        checkError(
          validation,
          List(genErr(FreeText, "landOrProperty.landRegistryReferenceOrReason.upload.error.required"))
        )
      }

      "return successfully RegistryDetails if isThereARegistryReference is YES and enter registry reference" in {

        val validation = validator.validateIsThereARegistryReference(
          CsvValue(csvKey, "YES"),
          CsvValue(csvKey, "Some Reference"),
          name,
          row
        )

        checkSuccess(
          validation,
          RegistryDetails(YesNo.Yes, Some("Some Reference"), None)
        )
      }

      "return successfully RegistryDetails if isThereARegistryReference is NO and valid noRegistryReference" in {

        val validation =
          validator.validateIsThereARegistryReference(
            CsvValue(csvKey, "NO"),
            CsvValue(csvKey, "Reason to not have"),
            name,
            row
          )

        checkSuccess(
          validation,
          RegistryDetails(YesNo.No, None, Some("Reason to not have"))
        )
      }
    }

    "validateJointlyHeld" - {
      // ERROR TESTS
      "get errors for validateJointlyHeld" - {
        "return required isPropertyHeldJointly if isPropertyHeldJointly is empty" in {
          val validation = validator.validateJointlyHeld(
            isPropertyHeldJointly = CsvValue(csvKey, ""),
            percentageHeldByMember = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(YesNoQuestion, "landOrProperty.isPropertyHeldJointly.upload.error.required"))
          )
        }

        "return invalid isPropertyHeldJointly if isPropertyHeldJointly is invalid" in {
          val validation = validator.validateJointlyHeld(
            isPropertyHeldJointly = CsvValue(csvKey, "ASD"),
            percentageHeldByMember = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(YesNoQuestion, "landOrProperty.isPropertyHeldJointly.upload.error.invalid"))
          )
        }

        "return required percentageHeldByMember required if isPropertyHeldJointly is YES and percentageHeldByMember is not entered" in {
          val validation = validator.validateJointlyHeld(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            percentageHeldByMember = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(Count, "landOrProperty.percentageHeldByMember.upload.error.required"))
          )
        }

        "return required percentageHeldByMember invalid if isPropertyHeldJointly is YES and percentageHeldByMember is entered some random character" in {
          val validation = validator.validateJointlyHeld(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            percentageHeldByMember = CsvValue(csvKey, Some("ASD")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(Count, "landOrProperty.percentageHeldByMember.upload.error.invalid"))
          )
        }

        "return required percentageHeldByMember tooLong if isPropertyHeldJointly is YES and percentageHeldByMember is entered bigger than 9999999" in {
          val validation = validator.validateJointlyHeld(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            percentageHeldByMember = CsvValue(csvKey, Some("99999999")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(Count, "landOrProperty.percentageHeldByMember.upload.error.tooBig"))
          )
        }
      }

      // SUCCESS TESTS
      "get success results for validateJointlyHeld" - {
        "return successfully NO if isPropertyHeldJointly flag marked as false" in {
          val validation = validator.validateJointlyHeld(
            isPropertyHeldJointly = CsvValue(csvKey, "NO"),
            percentageHeldByMember = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (YesNo.No, None)
          )
        }

        "return successfully YES and isPropertyHeldJointly if isPropertyHeldJointly flag marked as true and entered count and first person with nino correctly" in {
          val validation = validator.validateJointlyHeld(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            percentageHeldByMember = CsvValue(csvKey, Some("1")),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (
              YesNo.Yes,
              Some(1)
            )
          )
        }
      }
    }

    "validateLease" - {
      // ERROR TESTS
      "get errors for validateLease" - {
        "return required isLeased error if isLeased not entered" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, ""),
            numberOfLessees = CsvValue(csvKey, None),
            anyLesseeConnectedParty = CsvValue(csvKey, None),
            leaseDate = CsvValue(csvKey, None),
            annualLeaseAmount = CsvValue(csvKey, None),
            isCountEntered = false,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(YesNoQuestion, "landOrProperty.isLeased.upload.error.required"))
          )
        }

        "return invalid isLeased error if isLeased entered other than YES/NO" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, "ASD"),
            numberOfLessees = CsvValue(csvKey, None),
            anyLesseeConnectedParty = CsvValue(csvKey, None),
            leaseDate = CsvValue(csvKey, None),
            annualLeaseAmount = CsvValue(csvKey, None),
            isCountEntered = false,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(YesNoQuestion, "landOrProperty.isLeased.upload.error.invalid"))
          )
        }

        "return countOfLessees and other fields are required errors if isLeased Yes and nothing entered (isCountEntered = true)" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, "Yes"),
            numberOfLessees = CsvValue(csvKey, None),
            anyLesseeConnectedParty = CsvValue(csvKey, None),
            leaseDate = CsvValue(csvKey, None),
            annualLeaseAmount = CsvValue(csvKey, None),
            isCountEntered = true,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(Count, "landOrProperty.lesseePersonCount.upload.error.required"),
              genErr(YesNoQuestion, "landOrProperty.anyLesseeConnected.upload.error.required"),
              genErr(LocalDateFormat, "landOrProperty.leaseDate.upload.error.required"),
              genErr(Price, "landOrProperty.leaseAmount.upload.error.required")
            )
          )
        }

        "return countOfLessees is invalid error if isLeased Yes and countOfLessees is not correct (isCountEntered = true)" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, "Yes"),
            numberOfLessees = CsvValue(csvKey, Some("asd1")),
            anyLesseeConnectedParty = CsvValue(csvKey, Some("Yes")),
            leaseDate = CsvValue(csvKey, Some("12/12/2022")),
            annualLeaseAmount = CsvValue(csvKey, Some("123.33")),
            isCountEntered = true,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(Count, "landOrProperty.lesseePersonCount.upload.error.invalid")
            )
          )
        }

        "return countOfLessees is too small error if isLeased Yes and countOfLessees is negative (isCountEntered = true)" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, "Yes"),
            numberOfLessees = CsvValue(csvKey, Some("-99")),
            anyLesseeConnectedParty = CsvValue(csvKey, Some("Yes")),
            leaseDate = CsvValue(csvKey, Some("12/12/2022")),
            annualLeaseAmount = CsvValue(csvKey, Some("123.33")),
            isCountEntered = true,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(Count, "landOrProperty.lesseePersonCount.upload.error.tooSmall")
            )
          )
        }

        "return countOfLessees is too big error if isLeased Yes and countOfLessees is bigger than 9.999.999 (isCountEntered = true)" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, "Yes"),
            numberOfLessees = CsvValue(csvKey, Some("1222222222")),
            anyLesseeConnectedParty = CsvValue(csvKey, Some("Yes")),
            leaseDate = CsvValue(csvKey, Some("12/12/2022")),
            annualLeaseAmount = CsvValue(csvKey, Some("123.33")),
            isCountEntered = true,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(Count, "landOrProperty.lesseePersonCount.upload.error.tooBig")
            )
          )
        }

        "return anyOfLesseesConnected is not valid if isLeased Yes (isCountEntered = true)" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, "Yes"),
            numberOfLessees = CsvValue(csvKey, Some("22")),
            anyLesseeConnectedParty = CsvValue(csvKey, Some("X")),
            leaseDate = CsvValue(csvKey, Some("12/12/2022")),
            annualLeaseAmount = CsvValue(csvKey, Some("123.33")),
            isCountEntered = true,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(YesNoQuestion, "landOrProperty.anyLesseeConnected.upload.error.invalid")
            )
          )
        }

        "return leaseDate is not valid if isLeased Yes (isCountEntered = true)" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, "Yes"),
            numberOfLessees = CsvValue(csvKey, Some("22")),
            anyLesseeConnectedParty = CsvValue(csvKey, Some("No")),
            leaseDate = CsvValue(csvKey, Some("12/111/2022")),
            annualLeaseAmount = CsvValue(csvKey, Some("123.33")),
            isCountEntered = true,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(LocalDateFormat, "landOrProperty.leaseDate.upload.error.invalid.date")
            )
          )
        }

        "return numericValueRequired for annualLeaseAmount if isLeased Yes (isCountEntered = true)" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, "Yes"),
            numberOfLessees = CsvValue(csvKey, Some("22")),
            anyLesseeConnectedParty = CsvValue(csvKey, Some("YES")),
            leaseDate = CsvValue(csvKey, Some("12/11/2022")),
            annualLeaseAmount = CsvValue(csvKey, Some("A11.33")),
            isCountEntered = true,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(Price, "landOrProperty.leaseAmount.upload.error.numericValueRequired")
            )
          )
        }
      }

      // SUCCESS TESTS
      "get success results for validateLease" - {
        "return successfully Yes, None if isLeased selected as NO" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, "No"),
            numberOfLessees = CsvValue(csvKey, None),
            anyLesseeConnectedParty = CsvValue(csvKey, None),
            leaseDate = CsvValue(csvKey, None),
            annualLeaseAmount = CsvValue(csvKey, None),
            isCountEntered = false,
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(validation, (YesNo.No, None))
        }

        "return successfully LesseeDetail if isRequired Yes and all details entered correctly (isCountEntered = true)" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, "Yes"),
            numberOfLessees = CsvValue(csvKey, Some("1")),
            anyLesseeConnectedParty = CsvValue(csvKey, Some("No")),
            leaseDate = CsvValue(csvKey, Some("12/12/2022")),
            annualLeaseAmount = CsvValue(csvKey, Some("123.00")),
            isCountEntered = true,
            memberFullNameDob = name,
            row = row
          )
          checkSuccess(
            validation,
            (
              YesNo.Yes,
              Some(
                LesseeDetails(
                  numberOfLessees = 1,
                  anyLesseeConnectedParty = YesNo.No,
                  leaseGrantedDate = LocalDate.of(2022, 12, 12),
                  annualLeaseAmount = 123.0
                )
              )
            )
          )
        }

        "return successfully LesseeDetail if isRequired Yes and all details entered correctly (isCountEntered = false)" in {
          val validation = validator.validateLease(
            isLeased = CsvValue(csvKey, "Yes"),
            numberOfLessees = CsvValue(csvKey, None),
            anyLesseeConnectedParty = CsvValue(csvKey, Some("Yes")),
            leaseDate = CsvValue(csvKey, Some("12/12/2022")),
            annualLeaseAmount = CsvValue(csvKey, Some("125.00")),
            isCountEntered = false,
            memberFullNameDob = name,
            row = row
          )
          checkSuccess(
            validation,
            (
              YesNo.Yes,
              Some(
                LesseeDetails(
                  numberOfLessees = 1,
                  anyLesseeConnectedParty = YesNo.Yes,
                  leaseGrantedDate = LocalDate.of(2022, 12, 12),
                  annualLeaseAmount = 125.0
                )
              )
            )
          )
        }
      }
    }

    "validateDisposals" - {
      // ERROR TESTS
      "get errors for validateDisposals" - {
        "return required error if wereAnyDisposalOnThisDuringTheYear not entered" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, ""),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, None),
            nameOfPurchasers = CsvValue(csvKey, None),
            isAnyPurchaserConnected = CsvValue(csvKey, None),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, None),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(YesNoQuestion, "landOrProperty.isAnyDisposalMade.upload.error.required"))
          )
        }

        "return invalid error if wereAnyDisposalOnThisDuringTheYear is not valid" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "ADS"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, None),
            nameOfPurchasers = CsvValue(csvKey, None),
            isAnyPurchaserConnected = CsvValue(csvKey, None),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, None),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(YesNoQuestion, "landOrProperty.isAnyDisposalMade.upload.error.invalid"))
          )
        }

        "return all required errors if wereAnyDisposalOnThisDuringTheYear is YES but others not entered" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "YES"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, None),
            nameOfPurchasers = CsvValue(csvKey, None),
            isAnyPurchaserConnected = CsvValue(csvKey, None),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, None),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(Price, "landOrProperty.disposedAmount.upload.error.required"),
              genErr(FreeText, "landOrProperty.disposedNames.upload.error.required"),
              genErr(YesNoQuestion, "landOrProperty.anyConnectedPurchaser.upload.error.required"),
              genErr(YesNoQuestion, "landOrProperty.isTransactionSupported.upload.error.required"),
              genErr(YesNoQuestion, "landOrProperty.isFullyDisposedOf.upload.error.required")
            )
          )
        }

        "return numericValueRequired error for totalSaleProceedIfAnyDisposal" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("ASD")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("No")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("No")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(Price, "landOrProperty.disposedAmount.upload.error.numericValueRequired")
            )
          )
        }

        "return required error for nameOfPurchasers if not entered" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some("")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("No")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("No")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(FreeText, "landOrProperty.disposedNames.upload.error.required")
            )
          )
        }

        "return tooLong error for nameOfPurchasers if entered more than 160 chars" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some(freeTextWith161Chars)),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("No")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("No")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(FreeText, "landOrProperty.disposedNames.upload.error.tooLong")
            )
          )
        }

        "return invalid error for isAnyPurchaserConnected if wrong" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("ASD")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("No")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(YesNoQuestion, "landOrProperty.anyConnectedPurchaser.upload.error.invalid")
            )
          )
        }

        "return invalid error for isTransactionSupportedByIndependentValuation if wrong" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("Yes")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("X")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(YesNoQuestion, "landOrProperty.isTransactionSupported.upload.error.invalid")
            )
          )
        }

        "return invalid error for hasLandOrPropertyFullyDisposedOf if wrong" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("Yes")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("yes")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("nx")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(YesNoQuestion, "landOrProperty.isFullyDisposedOf.upload.error.invalid")
            )
          )
        }

        "return multiple errors" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.2")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("a")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("nx")),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(YesNoQuestion, "landOrProperty.anyConnectedPurchaser.upload.error.required"),
              genErr(YesNoQuestion, "landOrProperty.isTransactionSupported.upload.error.invalid"),
              genErr(YesNoQuestion, "landOrProperty.isFullyDisposedOf.upload.error.invalid")
            )
          )
        }
      }

      // SUCCESS TESTS
      "get success results for validateDisposals" - {
        "return successfully Yes, None if wereAnyDisposalOnThisDuringTheYear entered as NO" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "No"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, None),
            nameOfPurchasers = CsvValue(csvKey, None),
            isAnyPurchaserConnected = CsvValue(csvKey, None),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, None),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (YesNo.No, None)
          )
        }

        "return successfully DisposalDetails if wereAnyDisposalOnThisDuringTheYear Yes and all details entered correctly" in {
          val validation = validator.validateDisposals(
            wereAnyDisposalOnThisDuringTheYear = CsvValue(csvKey, "Yes"),
            totalSaleProceedIfAnyDisposal = CsvValue(csvKey, Some("123.22")),
            nameOfPurchasers = CsvValue(csvKey, Some("Name 1, Name 2")),
            isAnyPurchaserConnected = CsvValue(csvKey, Some("No")),
            isTransactionSupportedByIndependentValuation = CsvValue(csvKey, Some("No")),
            hasLandOrPropertyFullyDisposedOf = CsvValue(csvKey, Some("Yes")),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (
              YesNo.Yes,
              Some(
                DisposalDetails(
                  disposedPropertyProceedsAmt = 123.22,
                  purchasersNames = "Name 1, Name 2",
                  anyPurchaserConnectedParty = YesNo.No,
                  independentValuationDisposal = YesNo.No,
                  propertyFullyDisposed = YesNo.Yes
                )
              )
            )
          )
        }
      }
    }
  }
}
