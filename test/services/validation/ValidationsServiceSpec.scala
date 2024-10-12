/*
 * Copyright 2024 HM Revenue & Customs
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
import models.{Crn, CsvHeaderKey, CsvValue, Money, NameDOB, NinoType, ROWAddress, UKAddress, Utr}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.domain.Nino
import utils.ValidationSpecUtils.{checkError, checkSuccess, genErr}

import java.time.LocalDate

class ValidationsServiceSpec extends AnyFreeSpec with ScalaCheckPropertyChecks with Generators with Matchers {

  private val nameDOBFormProvider = new NameDOBFormProvider {}
  private val textFormProvider = new TextFormProvider {}
  private val datePageFormProvider = new DatePageFormProvider {}
  private val moneyFormProvider = new MoneyFormProvider {}
  private val intFormProvider = new IntFormProvider {}
  private val doubleFormProvider = new DoubleFormProvider {}

  private val validator = new ValidationsService(
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

  "ValidationsServiceSpec" - {
    "validateNameDOB" - {
      // ERROR TESTS
      "return DateOfBirth format error if nothing entered" in {
        val validation = validator.validateNameDOB(
          CsvValue(csvKey, ""),
          CsvValue(csvKey, ""),
          CsvValue(csvKey, ""),
          row,
          None
        )

        checkError(
          validation,
          List(genErr(DateOfBirth, "memberDetails.dateOfBirth.error.format"))
        )
      }

      "return DateOfBirth format error if date is wrong" in {
        val validation = validator.validateNameDOB(
          CsvValue(csvKey, ""),
          CsvValue(csvKey, ""),
          CsvValue(csvKey, "ASD"),
          row,
          None
        )

        checkError(
          validation,
          List(genErr(DateOfBirth, "memberDetails.dateOfBirth.error.format"))
        )
      }

      "return required errors for name and last name and DateOfBirth invalid error if date is wrong" in {
        val validation = validator.validateNameDOB(
          CsvValue(csvKey, ""),
          CsvValue(csvKey, ""),
          CsvValue(csvKey, "1221/12/2019"),
          row,
          None
        )

        checkError(
          validation,
          List(
            genErr(FirstName, "memberDetails.firstName.upload.error.required"),
            genErr(LastName, "memberDetails.lastName.upload.error.required"),
            genErr(DateOfBirth, "memberDetails.dateOfBirth.upload.error.invalid.date")
          )
        )
      }

      "return tooLong error for name" in {
        val validation = validator.validateNameDOB(
          CsvValue(csvKey, freeTextWith161Chars),
          CsvValue(csvKey, "LastName"),
          CsvValue(csvKey, "12/12/2019"),
          row,
          None
        )

        checkError(
          validation,
          List(
            genErr(FirstName, "memberDetails.firstName.upload.error.length")
          )
        )
      }

      "return tooLong error for last name" in {
        val validation = validator.validateNameDOB(
          CsvValue(csvKey, "firstName"),
          CsvValue(csvKey, freeTextWith161Chars),
          CsvValue(csvKey, "12/12/2019"),
          row,
          None
        )

        checkError(
          validation,
          List(
            genErr(LastName, "memberDetails.lastName.upload.error.length")
          )
        )
      }

      "return invalid error for name" in {
        val validation = validator.validateNameDOB(
          CsvValue(csvKey, "firstName, ads12"),
          CsvValue(csvKey, "lastName"),
          CsvValue(csvKey, "12/12/2019"),
          row,
          None
        )

        checkError(
          validation,
          List(
            genErr(FirstName, "memberDetails.firstName.upload.error.invalid")
          )
        )
      }

      // SUCCESS TESTS
      "return NameDOB if it is valid" in {
        val validation = validator.validateNameDOB(
          CsvValue(csvKey, "firstName"),
          CsvValue(csvKey, "lastName"),
          CsvValue(csvKey, "12/12/2019"),
          row,
          None
        )

        checkSuccess(
          validation,
          NameDOB(
            "firstName",
            "lastName",
            LocalDate.of(2019, 12, 12)
          )
        )
      }
    }

    "validateNinoControlWithNoNinoReason" - {
      // ERROR TESTS
      "return nino not valid" in {

        val validation = validator.validateNinoWithNoReason(
          CsvValue(csvKey, Some("ASDASDAS")),
          CsvValue(csvKey, None),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(NinoFormat, "nino.upload.error.invalid"))
        )
      }

      "return required" in {
        val validation = validator.validateNinoWithNoReason(
          CsvValue(csvKey, None),
          CsvValue(csvKey, None),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(NoNinoReason, "nino.upload.error.required"))
        )
      }

      "return too long" in {
        val validation = validator.validateNinoWithNoReason(
          CsvValue(csvKey, None),
          CsvValue(csvKey, Some(freeTextWith161Chars)),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(FreeText, "nino.reason.upload.error.tooLong"))
        )
      }

      // SUCCESS TESTS
      "return nino if it is valid" in {
        val validation = validator.validateNinoWithNoReason(
          CsvValue(csvKey, Some("AB123456C")),
          CsvValue(csvKey, None),
          name,
          row
        )

        checkSuccess(
          validation,
          NinoType(Some("AB123456C"), None)
        )
      }

      "return reason if it is valid" in {
        val validation = validator.validateNinoWithNoReason(
          CsvValue(csvKey, None),
          CsvValue(csvKey, Some("VALID_REASON")),
          name,
          row
        )

        checkSuccess(
          validation,
          NinoType(None, Some("VALID_REASON"))
        )
      }
    }

    "validateNinoWithDuplicationControl" - {
      // ERROR TESTS
      "return nino not valid" in {
        val validation = validator.validateNinoWithDuplicationControl(
          CsvValue(csvKey, "ASDASDAS"),
          name,
          List.empty,
          row
        )

        checkError(
          validation,
          List(genErr(NinoFormat, "memberDetailsNino.upload.error.invalid"))
        )
      }

      "return required" in {
        val validation = validator.validateNinoWithDuplicationControl(
          CsvValue(csvKey, ""),
          name,
          List.empty,
          row
        )

        checkError(
          validation,
          List(genErr(NinoFormat, "memberDetailsNino.upload.error.required"))
        )
      }

      "return duplicate" in {
        val validation = validator.validateNinoWithDuplicationControl(
          CsvValue(csvKey, "AB123456C"),
          name,
          List(
            Nino("AB123456C")
          ),
          row
        )

        checkError(
          validation,
          List(genErr(NinoFormat, "memberDetailsNino.upload.error.duplicate"))
        )
      }

      // SUCCESS TESTS
      "return nino if previous nino list empty" in {
        val validation = validator.validateNinoWithDuplicationControl(
          CsvValue(csvKey, "AB123456C"),
          name,
          List.empty,
          row
        )

        checkSuccess(
          validation,
          Nino("AB123456C")
        )
      }

      "return nino if previous nino doesn't have current one" in {
        val validation = validator.validateNinoWithDuplicationControl(
          CsvValue(csvKey, "AB123456C"),
          name,
          List(
            Nino("AB123456D"),
            Nino("AB123451C"),
            Nino("AB123422C")
          ),
          row
        )

        checkSuccess(
          validation,
          Nino("AB123456C")
        )
      }
    }

    "validateNino" - {
      // ERROR TESTS
      "return nino not valid" in {
        val validation = validator.validateNino(
          CsvValue(csvKey, "ASDASDAS"),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(NinoFormat, "nino.upload.error.invalid"))
        )
      }

      "return required" in {
        val validation = validator.validateNino(
          CsvValue(csvKey, ""),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(NinoFormat, "nino.upload.error.required"))
        )
      }

      // SUCCESS TESTS
      "return nino" in {
        val validation = validator.validateNino(
          CsvValue(csvKey, "AB123456C"),
          name,
          row
        )

        checkSuccess(
          validation,
          Nino("AB123456C")
        )
      }
    }

    "validateCrn" - {
      // ERROR TESTS
      "return not valid" in {
        val validation = validator.validateCrn(
          CsvValue(csvKey, "ASDASDASADSA"),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(CrnFormat, "crn.upload.error.invalid"))
        )
      }

      "return required" in {
        val validation = validator.validateCrn(
          CsvValue(csvKey, ""),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(CrnFormat, "crn.upload.error.required"))
        )
      }

      // SUCCESS TESTS
      "return crn" in {
        val validation = validator.validateCrn(
          CsvValue(csvKey, "AB123456"),
          name,
          row
        )

        checkSuccess(
          validation,
          Crn("AB123456")
        )
      }
    }

    "validateUtr" - {
      // ERROR TESTS
      "return not valid" in {
        val validation = validator.validateUtr(
          CsvValue(csvKey, "ASDASDASADSA"),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(UtrFormat, "utr.upload.error.invalid"))
        )
      }

      "return required" in {
        val validation = validator.validateUtr(
          CsvValue(csvKey, ""),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(UtrFormat, "utr.upload.error.required"))
        )
      }

      // SUCCESS TESTS
      "return utr" in {
        val validation = validator.validateUtr(
          CsvValue(csvKey, "12345 67890"),
          name,
          row
        )

        checkSuccess(
          validation,
          Utr("12345 67890")
        )
      }
    }

    "validateFreeText" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validateFreeText(
          text = CsvValue(csvKey, ""),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(FreeText, "other.upload.error.required"))
        )
      }

      "return too long" in {
        val validation = validator.validateFreeText(
          text = CsvValue(csvKey, freeTextWith161Chars),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(FreeText, "other.upload.error.tooLong"))
        )
      }

      // SUCCESS TESTS
      "return free text" in {
        val validation = validator.validateFreeText(
          text = CsvValue(csvKey, "Test Text"),
          memberFullName = name,
          row = row
        )

        checkSuccess(
          validation,
          "Test Text"
        )
      }
    }

    "validateNoNino" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validateNoNino(
          CsvValue(csvKey, ""),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(NoNinoReason, "noNINO.upload.error.required"))
        )
      }

      "return too long" in {
        val validation = validator.validateNoNino(
          CsvValue(csvKey, freeTextWith161Chars),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(NoNinoReason, "noNINO.upload.error.length"))
        )
      }

      "return not valid" in {
        val validation = validator.validateNoNino(
          CsvValue(csvKey, "ASD %%"),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(NoNinoReason, "noNINO.upload.error.invalid"))
        )
      }

      // SUCCESS TESTS
      "return no nino" in {
        val validation = validator.validateNoNino(
          CsvValue(csvKey, "AB123456C"),
          name,
          row
        )

        checkSuccess(
          validation,
          "AB123456C"
        )
      }
    }

    "validateAddressLine" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validateAddressLine(
          CsvValue(csvKey, ""),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(AddressLine, "address-line.upload.error.required"))
        )
      }

      "return too long" in {
        val validation = validator.validateAddressLine(
          CsvValue(csvKey, "More than 35 characters line it is and it will be not valid"),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(AddressLine, "address-line.upload.error.length"))
        )
      }

      "return invalid" in {
        val validation = validator.validateAddressLine(
          CsvValue(csvKey, "ASD ^ %"),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(AddressLine, "address-line.upload.error.invalid"))
        )
      }

      // SUCCESS TESTS
      "return address line" in {
        val validation = validator.validateAddressLine(
          CsvValue(csvKey, "Florida Street No 6"),
          name,
          row
        )

        checkSuccess(
          validation,
          "Florida Street No 6"
        )
      }
    }

    "validateTownOrCity" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validateTownOrCity(
          CsvValue(csvKey, ""),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(TownOrCity, "town-or-city.upload.error.required"))
        )
      }

      "return too long as invalid" in {
        val validation = validator.validateTownOrCity(
          CsvValue(csvKey, "More than 35 characters line it is and it will be not valid"),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(TownOrCity, "town-or-city.upload.error.invalid"))
        )
      }

      "return invalid" in {
        val validation = validator.validateTownOrCity(
          CsvValue(csvKey, "ASD ^ %"),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(TownOrCity, "town-or-city.upload.error.invalid"))
        )
      }

      // SUCCESS TESTS
      "return town or city" in {
        val validation = validator.validateTownOrCity(
          CsvValue(csvKey, "London"),
          name,
          row
        )

        checkSuccess(
          validation,
          "London"
        )
      }
    }

    "validateCountry" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validateCountry(
          CsvValue(csvKey, ""),
          row
        )

        checkError(
          validation,
          List(genErr(Country, "country.upload.error.required"))
        )
      }

      "return too long as invalid" in {
        val validation = validator.validateCountry(
          CsvValue(csvKey, "More than 35 characters line it is and it will be not valid"),
          row
        )

        checkError(
          validation,
          List(genErr(Country, "country.upload.error.invalid"))
        )
      }

      "return invalid" in {
        val validation = validator.validateCountry(
          CsvValue(csvKey, "ASD ^ %"),
          row
        )

        checkError(
          validation,
          List(genErr(Country, "country.upload.error.invalid"))
        )
      }

      "return invalid if not in the list" in {
        val validation = validator.validateCountry(
          CsvValue(csvKey, "Abkhazia"),
          row
        )

        checkError(
          validation,
          List(genErr(Country, "country.upload.error.invalid"))
        )
      }

      // SUCCESS TESTS
      "return country" in {
        val validation = validator.validateCountry(
          CsvValue(csvKey, "Algeria"),
          row
        )

        checkSuccess(
          validation,
          "DZ"
        )
      }
    }

    "validateUkPostcode" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validateUkPostcode(
          CsvValue(csvKey, ""),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(UKPostcode, "postcode.upload.error.required"))
        )
      }

      "return too long as invalid" in {
        val validation = validator.validateUkPostcode(
          CsvValue(csvKey, "More than 35 characters line it is and it will be not valid"),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(UKPostcode, "postcode.upload.error.invalid"))
        )
      }

      "return invalid" in {
        val validation = validator.validateUkPostcode(
          CsvValue(csvKey, "ASD ^ %"),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(UKPostcode, "postcode.upload.error.invalid"))
        )
      }

      // SUCCESS TESTS
      "return postcode" in {
        val validation = validator.validateUkPostcode(
          CsvValue(csvKey, "DA15 5DE"),
          name,
          row
        )

        checkSuccess(
          validation,
          "DA15 5DE"
        )
      }
    }

    "validateIsUkAddress" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validateIsUkAddress(
          CsvValue(csvKey, ""),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(YesNoAddress, "isUK.upload.error.required"))
        )
      }

      "return invalid" in {
        val validation = validator.validateIsUkAddress(
          CsvValue(csvKey, "ASD"),
          name,
          row
        )

        checkError(
          validation,
          List(genErr(YesNoAddress, "isUK.upload.error.invalid"))
        )
      }

      "return invalid and too long for too long text" in {
        val validation = validator.validateIsUkAddress(
          CsvValue(csvKey, freeTextWith161Chars),
          name,
          row
        )

        checkError(
          validation,
          List(
            genErr(YesNoAddress, "isUK.upload.error.invalid"),
            genErr(YesNoAddress, "isUK.upload.error.length")
          )
        )
      }

      // SUCCESS TESTS
      "return isUkAddress for Yes" in {
        val validation = validator.validateIsUkAddress(
          CsvValue(csvKey, "Yes"),
          name,
          row
        )

        checkSuccess(
          validation,
          "Yes"
        )
      }

      "return isUkAddress for NO" in {
        val validation = validator.validateIsUkAddress(
          CsvValue(csvKey, "NO"),
          name,
          row
        )

        checkSuccess(
          validation,
          "NO"
        )
      }

      "return isUkAddress for yes" in {
        val validation = validator.validateIsUkAddress(
          CsvValue(csvKey, "yes"),
          name,
          row
        )

        checkSuccess(
          validation,
          "yes"
        )
      }
    }

    "validateDate" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, ""),
          "date",
          row
        )

        checkError(
          validation,
          List(genErr(LocalDateFormat, "date.upload.error.required.date"))
        )
      }

      "return required if can not parse with / or -" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, "ASDASDASD"),
          "date",
          row
        )

        checkError(
          validation,
          List(genErr(LocalDateFormat, "date.upload.error.required.date"))
        )
      }

      "return invalid characters" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, "ASDA/SDA/SD"),
          "date",
          row
        )

        checkError(
          validation,
          List(genErr(LocalDateFormat, "date.upload.error.invalid.characters"))
        )
      }

      "return invalid date if all not correct" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, "40/40/3000"),
          "date",
          row
        )

        checkError(
          validation,
          List(genErr(LocalDateFormat, "date.upload.error.invalid.date"))
        )
      }

      "return invalid date if some part is not correct" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, "40/12/2024"),
          "date",
          row
        )

        checkError(
          validation,
          List(genErr(LocalDateFormat, "date.upload.error.invalid.date"))
        )
      }

      "return required two if two part is missing" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, "//2024"),
          "date",
          row
        )

        checkError(
          validation,
          List(genErr(LocalDateFormat, "date.upload.error.required.two"))
        )
      }

      "return required day if day is missing" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, "/12/2024"),
          "date",
          row
        )

        checkError(
          validation,
          List(genErr(LocalDateFormat, "date.upload.error.required.day"))
        )
      }

      "return required month if month is missing" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, "12//2024"),
          "date",
          row
        )

        checkError(
          validation,
          List(genErr(LocalDateFormat, "date.upload.error.required.month"))
        )
      }

      "return required year if year is missing" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, "12/12/ "),
          "date",
          row
        )

        checkError(
          validation,
          List(genErr(LocalDateFormat, "date.upload.error.required.year"))
        )
      }

      "return required all if all is missing" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, " / / "),
          "date",
          row
        )

        checkError(
          validation,
          List(genErr(LocalDateFormat, "date.upload.error.required.all"))
        )
      }

      // SUCCESS TESTS
      "return date for 12/12/2024" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, "12/12/2024"),
          "date",
          row
        )

        checkSuccess(
          validation,
          LocalDate.of(2024, 12, 12)
        )
      }

      "return date for 12-01-2023" in {
        val validation = validator.validateDate(
          CsvValue(csvKey, "12-01-2023"),
          "date",
          row
        )

        checkSuccess(
          validation,
          LocalDate.of(2023, 1, 12)
        )
      }
    }

    "validateUKOrROWAddress" - {
      // ERROR TESTS
      "return required for isUKAddress" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, ""),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(YesNoAddress, "isUK.upload.error.required"))
        )
      }

      "return invalid for isUKAddress" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "ASD"),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(YesNoAddress, "isUK.upload.error.invalid"))
        )
      }

      "return required for ukAddressLine1" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "Yes"),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, Some("Address")),
          ukAddressLine3 = CsvValue(csvKey, Some("Address")),
          ukTownOrCity = CsvValue(csvKey, Some("London")),
          ukPostcode = CsvValue(csvKey, Some("DA1 2EQ")),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(AddressLine, "address-line.upload.error.required"))
        )
      }

      "return required for ukAddressLine2" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "Yes"),
          ukAddressLine1 = CsvValue(csvKey, Some("Address")),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, Some("Address")),
          ukTownOrCity = CsvValue(csvKey, Some("London")),
          ukPostcode = CsvValue(csvKey, Some("DA1 2EQ")),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(AddressLine, "address-line-2.upload.error.required"))
        )
      }

      "return required for ukPostcode" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "Yes"),
          ukAddressLine1 = CsvValue(csvKey, Some("Address")),
          ukAddressLine2 = CsvValue(csvKey, Some("Address")),
          ukAddressLine3 = CsvValue(csvKey, Some("Address")),
          ukTownOrCity = CsvValue(csvKey, Some("London")),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(UKPostcode, "postcode.upload.error.required"))
        )
      }

      "return required for ukTownOrCity" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "Yes"),
          ukAddressLine1 = CsvValue(csvKey, Some("Address")),
          ukAddressLine2 = CsvValue(csvKey, Some("Address")),
          ukAddressLine3 = CsvValue(csvKey, Some("Address")),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, Some("DA1 2EQ")),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(TownOrCity, "town-or-city.upload.error.required"))
        )
      }

      "return invalid for ukAddressLine1" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "Yes"),
          ukAddressLine1 = CsvValue(csvKey, Some("Address $12 !@")),
          ukAddressLine2 = CsvValue(csvKey, Some("Address")),
          ukAddressLine3 = CsvValue(csvKey, Some("Address")),
          ukTownOrCity = CsvValue(csvKey, Some("Town")),
          ukPostcode = CsvValue(csvKey, Some("DA1 2EQ")),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(AddressLine, "address-line.upload.error.invalid"))
        )
      }

      "return too long for ukAddressLine1" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "Yes"),
          ukAddressLine1 = CsvValue(csvKey, Some(freeTextWith161Chars)),
          ukAddressLine2 = CsvValue(csvKey, Some("Address")),
          ukAddressLine3 = CsvValue(csvKey, Some("Address")),
          ukTownOrCity = CsvValue(csvKey, Some("Town")),
          ukPostcode = CsvValue(csvKey, Some("DA1 2EQ")),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(AddressLine, "address-line.upload.error.length"))
        )
      }

      "return too long for ukAddressLine2" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "Yes"),
          ukAddressLine1 = CsvValue(csvKey, Some("Address")),
          ukAddressLine2 = CsvValue(csvKey, Some(freeTextWith161Chars)),
          ukAddressLine3 = CsvValue(csvKey, Some("Address")),
          ukTownOrCity = CsvValue(csvKey, Some("Town")),
          ukPostcode = CsvValue(csvKey, Some("DA1 2EQ")),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(AddressLine, "address-line.upload.error.length"))
        )
      }

      "return invalid for ukTownOrCity" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "Yes"),
          ukAddressLine1 = CsvValue(csvKey, Some("Address Line 1")),
          ukAddressLine2 = CsvValue(csvKey, Some("Address")),
          ukAddressLine3 = CsvValue(csvKey, Some("Address")),
          ukTownOrCity = CsvValue(csvKey, Some("Town !@3 12 %")),
          ukPostcode = CsvValue(csvKey, Some("DA1 2EQ")),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(TownOrCity, "town-or-city.upload.error.invalid"))
        )
      }

      "return invalid for ukPostcode" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "Yes"),
          ukAddressLine1 = CsvValue(csvKey, Some("Address Line 1")),
          ukAddressLine2 = CsvValue(csvKey, Some("Address")),
          ukAddressLine3 = CsvValue(csvKey, Some("Address")),
          ukTownOrCity = CsvValue(csvKey, Some("Town")),
          ukPostcode = CsvValue(csvKey, Some("DA1 2EQ1")),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(UKPostcode, "postcode.upload.error.invalid"))
        )
      }

      "return required for addressLine1" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "No"),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, Some("Address")),
          addressLine3 = CsvValue(csvKey, Some("Address")),
          addressLine4 = CsvValue(csvKey, Some("Address")),
          country = CsvValue(csvKey, Some("Germany")),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(AddressLine, "address-line-non-uk.upload.error.required"))
        )
      }

      "return required for addressLine2" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "No"),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, Some("Address")),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, Some("Address")),
          addressLine4 = CsvValue(csvKey, Some("Address")),
          country = CsvValue(csvKey, Some("Germany")),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(AddressLine, "address-line-2-non-uk.upload.error.required"))
        )
      }

      "return required for country" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "No"),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, Some("Address Line 1")),
          addressLine2 = CsvValue(csvKey, Some("Address")),
          addressLine3 = CsvValue(csvKey, Some("Address")),
          addressLine4 = CsvValue(csvKey, Some("Address")),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(Country, "country.upload.error.required"))
        )
      }

      "return town or city required for address line 4 as town-or-city" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "No"),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, Some("Address Line 1")),
          addressLine2 = CsvValue(csvKey, Some("Address")),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, Some("Germany")),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(TownOrCity, "town-or-city-non-uk.upload.error.required"))
        )
      }

      "return invalid for address line 1 " in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "No"),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, Some("Address @!32")),
          addressLine2 = CsvValue(csvKey, Some("Address")),
          addressLine3 = CsvValue(csvKey, Some("Address")),
          addressLine4 = CsvValue(csvKey, Some("Town")),
          country = CsvValue(csvKey, Some("Germany")),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(AddressLine, "address-line.upload.error.invalid"))
        )
      }

      "return invalid for country " in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "No"),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, Some("Address Line 1")),
          addressLine2 = CsvValue(csvKey, Some("Address")),
          addressLine3 = CsvValue(csvKey, Some("Address")),
          addressLine4 = CsvValue(csvKey, Some("Town")),
          country = CsvValue(csvKey, Some("Ads")),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(genErr(Country, "country.upload.error.invalid"))
        )
      }

      "return multiple error if all address fields are empty for uk address " in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "Yes"),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(
            genErr(AddressLine, "address-line.upload.error.required"),
            genErr(AddressLine, "address-line-2.upload.error.required"),
            genErr(TownOrCity, "town-or-city.upload.error.required"),
            genErr(UKPostcode, "postcode.upload.error.required")
          )
        )
      }

      "return multiple error if all address fields are empty for non-uk address " in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "No"),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkError(
          validation,
          List(
            genErr(AddressLine, "address-line-non-uk.upload.error.required"),
            genErr(AddressLine, "address-line-2-non-uk.upload.error.required"),
            genErr(TownOrCity, "town-or-city-non-uk.upload.error.required"),
            genErr(Country, "country.upload.error.required")
          )
        )
      }

      // SUCCESS TESTS
      "return non uk address for isUKAddress = No" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "no"),
          ukAddressLine1 = CsvValue(csvKey, None),
          ukAddressLine2 = CsvValue(csvKey, None),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, None),
          ukPostcode = CsvValue(csvKey, None),
          addressLine1 = CsvValue(csvKey, Some("234 Street")),
          addressLine2 = CsvValue(csvKey, Some("Blueberry Apt")),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, Some("Berlin")),
          country = CsvValue(csvKey, Some("Germany")),
          memberFullName = name,
          row = row
        )

        checkSuccess(
          validation,
          ROWAddress(
            line1 = "234 Street",
            line2 = "Blueberry Apt",
            line3 = None,
            line4 = Some("Berlin"),
            country = "DE"
          )
        )
      }

      "return uk address for isUKAddress = Yes" in {
        val validation = validator.validateUKOrROWAddress(
          isUKAddress = CsvValue(csvKey, "Yes"),
          ukAddressLine1 = CsvValue(csvKey, Some("Flat 5")),
          ukAddressLine2 = CsvValue(csvKey, Some("Flower Street")),
          ukAddressLine3 = CsvValue(csvKey, None),
          ukTownOrCity = CsvValue(csvKey, Some("London")),
          ukPostcode = CsvValue(csvKey, Some("SW5 7QL")),
          addressLine1 = CsvValue(csvKey, None),
          addressLine2 = CsvValue(csvKey, None),
          addressLine3 = CsvValue(csvKey, None),
          addressLine4 = CsvValue(csvKey, None),
          country = CsvValue(csvKey, None),
          memberFullName = name,
          row = row
        )

        checkSuccess(
          validation,
          UKAddress(
            line1 = "Flat 5",
            line2 = "Flower Street",
            line3 = None,
            city = Some("London"),
            postcode = "SW5 7QL"
          )
        )
      }
    }

    "validateYesNoQuestion" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validateYesNoQuestion(
          CsvValue(csvKey, ""),
          key = "YesNo",
          name,
          row
        )

        checkError(
          validation,
          List(genErr(YesNoQuestion, "YesNo.upload.error.required"))
        )
      }

      "return too long as invalid" in {
        val validation = validator.validateYesNoQuestion(
          CsvValue(csvKey, freeTextWith161Chars),
          key = "YesNo",
          name,
          row
        )

        checkError(
          validation,
          List(
            genErr(YesNoQuestion, "YesNo.upload.error.invalid"),
            genErr(YesNoQuestion, "YesNo.upload.error.length")
          )
        )
      }

      "return invalid" in {
        val validation = validator.validateYesNoQuestion(
          CsvValue(csvKey, "Bla"),
          key = "YesNo",
          name,
          row
        )

        checkError(
          validation,
          List(
            genErr(YesNoQuestion, "YesNo.upload.error.invalid")
          )
        )
      }

      // SUCCESS TESTS
      "return yes if entered yes" in {
        val validation = validator.validateYesNoQuestion(
          CsvValue(csvKey, "yes"),
          key = "YesNo",
          name,
          row
        )

        checkSuccess(
          validation,
          "yes"
        )
      }

      "return yes if entered as all capital YES" in {
        val validation = validator.validateYesNoQuestion(
          CsvValue(csvKey, "YES"),
          key = "YesNo",
          name,
          row
        )

        checkSuccess(
          validation,
          "YES"
        )
      }

      "return no if entered No" in {
        val validation = validator.validateYesNoQuestion(
          CsvValue(csvKey, "No"),
          key = "YesNo",
          name,
          row
        )

        checkSuccess(
          validation,
          "No"
        )
      }
    }

    "validatePrice" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validatePrice(
          CsvValue(csvKey, ""),
          key = "price",
          name,
          row
        )

        checkError(
          validation,
          List(genErr(Price, "price.upload.error.required"))
        )
      }

      "return invalid" in {
        val validation = validator.validatePrice(
          CsvValue(csvKey, "ASD"),
          key = "price",
          name,
          row
        )

        checkError(
          validation,
          List(genErr(Price, "price.upload.error.numericValueRequired"))
        )
      }

      "return too big" in {
        val validation = validator.validatePrice(
          CsvValue(csvKey, "99999999999.99"),
          key = "price",
          name,
          row
        )

        checkError(
          validation,
          List(
            genErr(Price, "price.upload.error.tooBig")
          )
        )
      }

      // SUCCESS TESTS
      "return price for no decimal" in {
        val validation = validator.validatePrice(
          CsvValue(csvKey, "123"),
          key = "price",
          name,
          row
        )

        checkSuccess(
          validation,
          Money(123)
        )
      }

      "return price for single decimal" in {
        val validation = validator.validatePrice(
          CsvValue(csvKey, "123.20"),
          key = "price",
          name,
          row
        )

        checkSuccess(
          validation,
          Money(123.2)
        )
      }

      "return price for single decimal with 0" in {
        val validation = validator.validatePrice(
          CsvValue(csvKey, "123.2"),
          key = "price",
          name,
          row
        )

        checkSuccess(
          validation,
          Money(123.2)
        )
      }

      "return price for two decimal" in {
        val validation = validator.validatePrice(
          CsvValue(csvKey, "123.22"),
          key = "price",
          name,
          row
        )

        checkSuccess(
          validation,
          Money(123.22)
        )
      }
    }

    "validateCount" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validateCount(
          CsvValue(csvKey, ""),
          key = "count",
          name,
          row
        )

        checkError(
          validation,
          List(genErr(Count, "count.upload.error.required"))
        )
      }

      "return invalid" in {
        val validation = validator.validateCount(
          CsvValue(csvKey, "ASD"),
          key = "count",
          name,
          row
        )

        checkError(
          validation,
          List(genErr(Count, "count.upload.error.invalid"))
        )
      }

      "return too big" in {
        val validation = validator.validateCount(
          CsvValue(csvKey, "99999999999"),
          key = "count",
          name,
          row
        )

        checkError(
          validation,
          List(genErr(Count, "count.upload.error.tooBig"))
        )
      }

      "return too big with max count num" in {
        val validation = validator.validateCount(
          CsvValue(csvKey, "2"),
          key = "count",
          name,
          row,
          maxCount = 1
        )

        checkError(
          validation,
          List(genErr(Count, "count.upload.error.tooBig"))
        )
      }

      "return too small" in {
        val validation = validator.validateCount(
          CsvValue(csvKey, "-2"),
          key = "count",
          name,
          row
        )

        checkError(
          validation,
          List(genErr(Count, "count.upload.error.tooSmall"))
        )
      }

      "return too small with min count num" in {
        val validation = validator.validateCount(
          CsvValue(csvKey, "3"),
          key = "count",
          name,
          row,
          minCount = 4
        )

        checkError(
          validation,
          List(
            genErr(Count, "count.upload.error.tooSmall")
          )
        )
      }

      // SUCCESS TESTS
      "return count" in {
        val validation = validator.validateCount(
          CsvValue(csvKey, "123"),
          key = "count",
          name,
          row
        )

        checkSuccess(
          validation,
          123
        )
      }
    }

    "validatePercentage" - {
      // ERROR TESTS
      "return required" in {
        val validation = validator.validatePercentage(
          CsvValue(csvKey, ""),
          key = "percentage",
          name,
          row
        )

        checkError(
          validation,
          List(genErr(Percentage, "percentage.upload.error.required"))
        )
      }

      "return invalid" in {
        val validation = validator.validatePercentage(
          CsvValue(csvKey, "ASD"),
          key = "percentage",
          name,
          row
        )

        checkError(
          validation,
          List(genErr(Percentage, "percentage.upload.error.invalid"))
        )
      }

      "return too big" in {
        val validation = validator.validatePercentage(
          CsvValue(csvKey, "101"),
          key = "percentage",
          name,
          row
        )

        checkError(
          validation,
          List(
            genErr(Percentage, "percentage.upload.error.tooBig")
          )
        )
      }

      "return invalid if number is less than zero" in {
        val validation = validator.validatePercentage(
          CsvValue(csvKey, "-2"),
          key = "percentage",
          name,
          row
        )

        checkError(
          validation,
          List(
            genErr(Percentage, "percentage.upload.error.invalid")
          )
        )
      }

      // SUCCESS TESTS
      "return percentage for no decimal" in {
        val validation = validator.validatePercentage(
          CsvValue(csvKey, "22"),
          key = "percentage",
          name,
          row
        )

        checkSuccess(
          validation,
          22
        )
      }

      "return percentage for one decimal" in {
        val validation = validator.validatePercentage(
          CsvValue(csvKey, "33.3"),
          key = "percentage",
          name,
          row
        )

        checkSuccess(
          validation,
          33.3
        )
      }

      "return percentage for two decimal" in {
        val validation = validator.validatePercentage(
          CsvValue(csvKey, "33.32"),
          key = "percentage",
          name,
          row
        )

        checkSuccess(
          validation,
          33.32
        )
      }
    }
  }
}
