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
import models.requests.YesNo
import models.requests.common.DispossalDetail.PurchaserDetail
import models.requests.common.{IndOrOrgType, JointPropertyDetail, LesseeDetail, RegistryDetails, AcquiredFromType => mAcquiredFromType, ConnectedOrUnconnectedType => mConnectedOrUnconnectedType}
import models.{CsvHeaderKey, CsvValue, ValidationError}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi

import java.time.LocalDate

class LandOrPropertyValidationsServiceSpec
    extends AnyFreeSpec
    with ScalaCheckPropertyChecks
    with Generators
    with Matchers {

  private val nameDOBFormProvider = new NameDOBFormProvider {}
  private val textFormProvider = new TextFormProvider {}
  private val datePageFormProvider = new DatePageFormProvider {}
  private val moneyFormProvider = new MoneyFormProvider {}
  private val intFormProvider = new IntFormProvider {}
  private val validator = new LandOrPropertyValidationsService(
    nameDOBFormProvider,
    textFormProvider,
    datePageFormProvider,
    moneyFormProvider,
    intFormProvider
  )

  val row = 1
  val csvKey = CsvHeaderKey(key = "test", cell = "A", index = 1)
  val formKey = "key"
  val name = "fullName"
  val freeTextWith161Chars =
    "LoremipsumdolorsitametconsecteturadipisicingelitseddoeiusmodtemporincididuntutlaboreetdoloremagnaaliquaUtencoadminimveniamquisnostrudexercitationullamcolaborisnisiutaliquipexeacommodoconsequatDuisauteiruredolorinreprehenderitinvoluptatevelitessecillumdoloreeufugiatnullapariaturExcepteursintoccaecatcupidatatnonproidentsuntinculpaquiofficiadeseruntmollitanimidestlaboruma"

  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())

  "LandOrPropertyValidationsServiceSpec" - {

    "validateIsThereARegistryReference" - {
      "return required error if no isThereARegistryReference entered" in {
        val validation =
          validator.validateIsThereARegistryReference(CsvValue(csvKey, ""), CsvValue(csvKey, None), name, row)

        checkError(
          validation,
          List(genErr(YesNoQuestion, "landOrProperty.isThereARegistryReference.upload.error.required"))
        )
      }

      "return invalid error if isThereARegistryReference entered other than YES/NO" in {
        val validation =
          validator.validateIsThereARegistryReference(CsvValue(csvKey, "ASD"), CsvValue(csvKey, None), name, row)

        checkError(
          validation,
          List(genErr(YesNoQuestion, "landOrProperty.isThereARegistryReference.upload.error.invalid"))
        )
      }

      "return required noRegistryReference error if isThereARegistryReference entered as NO but not RegistryReference" in {
        val validation =
          validator.validateIsThereARegistryReference(CsvValue(csvKey, "NO"), CsvValue(csvKey, None), name, row)

        checkError(
          validation,
          List(genErr(FreeText, "landOrProperty.noLandRegistryReference.upload.error.required"))
        )
      }

      "return tooLong noRegistryReference error if isThereARegistryReference entered as NO but and noRegistryReference is more than 160 char" in {
        val validation =
          validator.validateIsThereARegistryReference(
            CsvValue(csvKey, "NO"),
            CsvValue(csvKey, Some(freeTextWith161Chars)),
            name,
            row
          )

        checkError(
          validation,
          List(genErr(FreeText, "landOrProperty.noLandRegistryReference.upload.error.tooLong"))
        )
      }

      "return successfully noRegistryReference if isThereARegistryReference is YES" in {
        val validation =
          validator.validateIsThereARegistryReference(CsvValue(csvKey, "YES"), CsvValue(csvKey, None), name, row)

        checkSuccess(
          validation,
          RegistryDetails(YesNo.Yes, None, None)
        )
      }

      "return successfully RegistryDetails if isThereARegistryReference is YES and enter noRegistryReference, and should ignore noRegistryReference" in {

        val validation = validator.validateIsThereARegistryReference(
          CsvValue(csvKey, "YES"),
          CsvValue(csvKey, Some(freeTextWith161Chars)),
          name,
          row
        )

        checkSuccess(
          validation,
          RegistryDetails(YesNo.Yes, None, None)
        )
      }

      "return successfully RegistryDetails if isThereARegistryReference is NO and valid noRegistryReference" in {

        val validation =
          validator.validateIsThereARegistryReference(CsvValue(csvKey, "NO"), CsvValue(csvKey, Some("Text")), name, row)

        checkSuccess(
          validation,
          RegistryDetails(YesNo.No, None, Some("Text"))
        )
      }
    }

    "validateAcquiredFromType" - {
      "return required error if no acquiredFromType entered" in {
        val validation = validator.validateAcquiredFromType(CsvValue(csvKey, ""), name, row, formKey)

        checkError(
          validation,
          List(genErr(AcquiredFromType, s"$formKey.upload.error.required"))
        )
      }

      "return invalid error if acquiredFromType is not valid" in {
        val validation = validator.validateAcquiredFromType(CsvValue(csvKey, "XXX"), name, row, formKey)

        checkError(
          validation,
          List(genErr(AcquiredFromType, s"$formKey.upload.error.invalid"))
        )
      }

      "return successfully AcquiredFromType" in {
        List("INDIVIDUAL", "Company", "partnership", "oTHer").foreach { q =>
          val validation = validator.validateAcquiredFromType(CsvValue(csvKey, q), name, row, formKey)

          checkSuccess(
            validation,
            q.toUpperCase
          )
        }
      }
    }

    "validateConnectedOrUnconnected" - {
      "return required error if no connectedOrUnconnected entered" in {
        val validation = validator.validateConnectedOrUnconnected(CsvValue(csvKey, ""), formKey, name, row)

        checkError(
          validation,
          List(genErr(ConnectedUnconnectedType, s"$formKey.upload.error.required"))
        )
      }

      "return invalid error if connectedOrUnconnected is not valid" in {
        val validation = validator.validateConnectedOrUnconnected(CsvValue(csvKey, "XXX"), formKey, name, row)

        checkError(
          validation,
          List(genErr(ConnectedUnconnectedType, s"$formKey.upload.error.invalid"))
        )
      }

      "return successfully connected Or Unconnected" in {
        List("Connected", "unconnected").foreach { q =>
          val validation = validator.validateConnectedOrUnconnected(CsvValue(csvKey, q), formKey, name, row)

          checkSuccess(
            validation,
            q.toUpperCase
          )
        }
      }
    }

    "validateAcquiredFrom" - {
      // ERROR TESTS
      "get errors for validateAcquiredFrom" - {
        "return required acquiredFromType error if no acquiredFromType entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, ""),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(AcquiredFromType, s"landOrProperty.acquiredFromType.upload.error.required"))
          )
        }

        "return invalid acquiredFromType error if acquiredFromType is not valid" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "NOT_VALID"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(AcquiredFromType, s"landOrProperty.acquiredFromType.upload.error.invalid"))
          )
        }

        "return required nino error if acquiredFromType is INDIVIDUAL, but no Nino or Other entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "INDIVIDUAL"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"landOrProperty.acquirerNino.upload.error.required"))
          )
        }

        "return invalid nino error if acquiredFromType is INDIVIDUAL and wrong Nino entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "INDIVIDUAL"),
            acquirerNinoForIndividual = CsvValue(csvKey, Some("INVALID_NINO")),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(NinoFormat, s"landOrProperty.acquirerNino.upload.error.invalid"))
          )
        }

        "return too long noIdOrAcquiredFromAnotherSource error if acquiredFromType is INDIVIDUAL but noIdOrAcquiredFromAnotherSource is too long" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "INDIVIDUAL"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, Some(freeTextWith161Chars)),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"landOrProperty.noIdOrAcquiredFromAnother.upload.error.tooLong"))
          )
        }

        "return required Crn error if acquiredFromType is COMPANY, but no Crn or Other entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "COMPANY"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"landOrProperty.acquirerCrn.upload.error.required"))
          )
        }

        "return invalid Crn error if acquiredFromType is COMPANY and wrong Crn entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "COMPANY"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, Some("INVALID_CRN_NUMBER")),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(CrnFormat, s"landOrProperty.acquirerCrn.upload.error.invalid"))
          )
        }

        "return too long noIdOrAcquiredFromAnotherSource error if acquiredFromType is COMPANY but noIdOrAcquiredFromAnotherSource is too long" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "COMPANY"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, Some(freeTextWith161Chars)),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"landOrProperty.noIdOrAcquiredFromAnother.upload.error.tooLong"))
          )
        }

        "return required UTR error if acquiredFromType is PARTNERSHIP, but no UTR or Other entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "PARTNERSHIP"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"landOrProperty.acquirerUtr.upload.error.required"))
          )
        }

        "return invalid UTR error if acquiredFromType is PARTNERSHIP and wrong UTR entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "PARTNERSHIP"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, Some("INVALID_UTR_NUMBER")),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(UtrFormat, s"landOrProperty.acquirerUtr.upload.error.invalid"))
          )
        }

        "return too long noIdOrAcquiredFromAnotherSource error if acquiredFromType is PARTNERSHIP but noIdOrAcquiredFromAnotherSource is too long" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "PARTNERSHIP"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, Some(freeTextWith161Chars)),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"landOrProperty.noIdOrAcquiredFromAnother.upload.error.tooLong"))
          )
        }

        "return required another source error if acquiredFromType is OTHER and no another source entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "OTHER"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"landOrProperty.noIdOrAcquiredFromAnother.upload.error.required"))
          )
        }

        "return too long noIdOrAcquiredFromAnotherSource error if acquiredFromType is OTHER but noIdOrAcquiredFromAnotherSource is too long" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "PARTNERSHIP"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, Some(freeTextWith161Chars)),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"landOrProperty.noIdOrAcquiredFromAnother.upload.error.tooLong"))
          )
        }
      }

      // SUCCESS TESTS
      "get success results for validateAcquiredFrom" - {
        "return successfully nino if acquiredFromType is INDIVIDUAL and correct nino entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "INDIVIDUAL"),
            acquirerNinoForIndividual = CsvValue(csvKey, Some("AB123456C")),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkSuccess(
            validation,
            mAcquiredFromType(
              indivOrOrgType = IndOrOrgType.Individual,
              idNumber = Some("AB123456C"),
              reasonNoIdNumber = None,
              otherDescription = None
            )
          )
        }

        "return successfully nino if acquiredFromType is INDIVIDUAL and correct nino entered even other entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "INDIVIDUAL"),
            acquirerNinoForIndividual = CsvValue(csvKey, Some("AB123456C")),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, Some("A")),
            name,
            row
          )

          checkSuccess(
            validation,
            mAcquiredFromType(
              indivOrOrgType = IndOrOrgType.Individual,
              idNumber = Some("AB123456C"),
              reasonNoIdNumber = None,
              otherDescription = None
            )
          )
        }

        "return successfully no nino reason if acquiredFromType is INDIVIDUAL and no nino reason entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "INDIVIDUAL"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, Some("Reason")),
            name,
            row
          )

          checkSuccess(
            validation,
            mAcquiredFromType(
              indivOrOrgType = IndOrOrgType.Individual,
              idNumber = None,
              reasonNoIdNumber = Some("Reason"),
              otherDescription = None
            )
          )
        }

        "return successfully crn if acquiredFromType is COMPANY and correct Crn number entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "COMPANY"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, Some("01234567")),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkSuccess(
            validation,
            mAcquiredFromType(
              indivOrOrgType = IndOrOrgType.Company,
              idNumber = Some("01234567"),
              reasonNoIdNumber = None,
              otherDescription = None
            )
          )
        }

        "return successfully no crn reason if acquiredFromType is COMPANY and no Crn reason entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "COMPANY"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, Some("Reason")),
            name,
            row
          )

          checkSuccess(
            validation,
            mAcquiredFromType(
              indivOrOrgType = IndOrOrgType.Company,
              idNumber = None,
              reasonNoIdNumber = Some("Reason"),
              otherDescription = None
            )
          )
        }

        "return successfully utr if acquiredFromType is PARTNERSHIP and correct UTR number entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "PARTNERSHIP"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, Some("1234567890")),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, None),
            name,
            row
          )

          checkSuccess(
            validation,
            mAcquiredFromType(
              indivOrOrgType = IndOrOrgType.Partnership,
              idNumber = Some("1234567890"),
              reasonNoIdNumber = None,
              otherDescription = None
            )
          )
        }

        "return successfully no utr reason if acquiredFromType is PARTNERSHIP and no utr reason entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "PARTNERSHIP"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, Some("Reason")),
            name,
            row
          )

          checkSuccess(
            validation,
            mAcquiredFromType(
              indivOrOrgType = IndOrOrgType.Partnership,
              idNumber = None,
              reasonNoIdNumber = Some("Reason"),
              otherDescription = None
            )
          )
        }

        "return successfully other if acquiredFromType is OTHER and correct other details entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "OTHER"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            noIdOrAcquiredFromAnotherSource = CsvValue(csvKey, Some("Other Details")),
            name,
            row
          )

          checkSuccess(
            validation,
            mAcquiredFromType(
              indivOrOrgType = IndOrOrgType.Other,
              idNumber = None,
              reasonNoIdNumber = None,
              otherDescription = Some("Other Details")
            )
          )
        }
      }
    }

    "validateJointlyHeld" - {
      // ERROR TESTS
      "get errors for validateJointlyHeld" - {
        "return required if isRequired flag marked as true" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, None),
            ninoJointlyOwning = CsvValue(csvKey, None),
            noNinoJointlyOwning = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(genErr(FreeText, "landOrProperty.jointlyName.upload.error.required"))
          )
        }

        "return name tooLong error if isRequired flag marked as true and name is too long" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, Some(freeTextWith161Chars)),
            ninoJointlyOwning = CsvValue(csvKey, None),
            noNinoJointlyOwning = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(genErr(FreeText, "landOrProperty.jointlyName.1.upload.error.tooLong"))
          )
        }

        "return nino required error if isRequired flag marked as true and name is entered correctly but not entered any nino or reason" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, Some("VALID NAME")),
            ninoJointlyOwning = CsvValue(csvKey, None),
            noNinoJointlyOwning = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(genErr(NinoFormat, "landOrProperty.jointlyNoNino.1.upload.error.required"))
          )
        }

        "return nino required error if isRequired flag marked as false and name is entered correctly but not entered any nino or reason" in {
          val validation = validator.validateJointlyHeld(
            count = 2,
            nameJointlyOwning = CsvValue(csvKey, Some("VALID NAME")),
            ninoJointlyOwning = CsvValue(csvKey, None),
            noNinoJointlyOwning = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(NinoFormat, "landOrProperty.jointlyNoNino.2.upload.error.required"))
          )
        }

        "return nino invalid error if isRequired flag marked as true and name is entered correctly but nino is entered incorrectly" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, Some("VALID NAME")),
            ninoJointlyOwning = CsvValue(csvKey, Some("INVALID_NINO!")),
            noNinoJointlyOwning = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(genErr(NinoFormat, "landOrProperty.jointlyNino.1.upload.error.invalid"))
          )
        }

        "return nino invalid error if isRequired flag marked as false and name is entered correctly but nino is entered incorrectly" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, Some("VALID NAME")),
            ninoJointlyOwning = CsvValue(csvKey, Some("INVALID_NINO!")),
            noNinoJointlyOwning = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(NinoFormat, "landOrProperty.jointlyNino.1.upload.error.invalid"))
          )
        }

        "return reason no nino tooLong error if isRequired flag marked as true and name is entered correctly and reason for no nino is entered too long" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, Some("VALID NAME")),
            ninoJointlyOwning = CsvValue(csvKey, None),
            noNinoJointlyOwning = CsvValue(csvKey, Some(freeTextWith161Chars)),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(genErr(FreeText, "landOrProperty.jointlyNoNino.1.upload.error.tooLong"))
          )
        }

        "return reason no nino tooLong error if isRequired flag marked as false and name is entered correctly and reason for no nino is entered too long" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, Some("VALID NAME")),
            ninoJointlyOwning = CsvValue(csvKey, None),
            noNinoJointlyOwning = CsvValue(csvKey, Some(freeTextWith161Chars)),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(FreeText, "landOrProperty.jointlyNoNino.1.upload.error.tooLong"))
          )
        }
      }

      // SUCCESS TESTS
      "get success results for validateJointlyHeld" - {
        "return successfully none if isRequired flag marked as true and nothing entered" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, None),
            ninoJointlyOwning = CsvValue(csvKey, None),
            noNinoJointlyOwning = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = false
          )

          checkSuccess(
            validation,
            None
          )
        }

        "return successfully jointlyHeld if isRequired flag marked as true and nino entered correctly" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, Some("VALID NAME")),
            ninoJointlyOwning = CsvValue(csvKey, Some("AB123456C")),
            noNinoJointlyOwning = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkSuccess(
            validation,
            Some(
              JointPropertyDetail(
                personName = "VALID NAME",
                nino = Some("AB123456C"),
                reasonNoNINO = None
              )
            )
          )
        }

        "return successfully jointlyHeld if isRequired flag marked as true and name and reason no nino entered correctly" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, Some("VALID NAME")),
            ninoJointlyOwning = CsvValue(csvKey, None),
            noNinoJointlyOwning = CsvValue(csvKey, Some("REASON")),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            Some(
              JointPropertyDetail(
                personName = "VALID NAME",
                nino = None,
                reasonNoNINO = Some("REASON")
              )
            )
          )
        }

        "return successfully jointlyHeld if isRequired flag marked as false and name and nino entered correctly" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, Some("VALID NAME")),
            ninoJointlyOwning = CsvValue(csvKey, Some("AB123456C")),
            noNinoJointlyOwning = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            Some(
              JointPropertyDetail(
                personName = "VALID NAME",
                nino = Some("AB123456C"),
                reasonNoNINO = None
              )
            )
          )
        }

        "return successfully jointlyHeld if isRequired flag marked as false and name and reason no nino entered correctly" in {
          val validation = validator.validateJointlyHeld(
            count = 1,
            nameJointlyOwning = CsvValue(csvKey, Some("VALID NAME")),
            ninoJointlyOwning = CsvValue(csvKey, None),
            noNinoJointlyOwning = CsvValue(csvKey, Some("REASON")),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            Some(
              JointPropertyDetail(
                personName = "VALID NAME",
                nino = None,
                reasonNoNINO = Some("REASON")
              )
            )
          )
        }
      }
    }

    "validateJointlyHeldAll" - {
      // ERROR TESTS
      "get errors for validateJointlyHeldAll" - {
        "return required isPropertyHeldJointly if isPropertyHeldJointly is empty" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, ""),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, None),
            jointlyPersonList = List.empty,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(YesNoQuestion, "landOrProperty.isPropertyHeldJointly.upload.error.required"))
          )
        }

        "return invalid isPropertyHeldJointly if isPropertyHeldJointly is invalid" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "ASD"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, None),
            jointlyPersonList = List.empty,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(YesNoQuestion, "landOrProperty.isPropertyHeldJointly.upload.error.invalid"))
          )
        }

        "return required personCount required if isPropertyHeldJointly is YES and howManyPersonsJointlyOwnProperty is not entered" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, None),
            jointlyPersonList = List.empty,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(Count, "landOrProperty.personCount.upload.error.required"))
          )
        }

        "return required personCount invalid if isPropertyHeldJointly is YES and howManyPersonsJointlyOwnProperty is entered some random character" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, Some("ASD")),
            jointlyPersonList = List.empty,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(Count, "landOrProperty.personCount.upload.error.invalid"))
          )
        }

        "return required personCount tooLong if isPropertyHeldJointly is YES and howManyPersonsJointlyOwnProperty is entered bigger than 50" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, Some("144")),
            jointlyPersonList = List.empty,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(Count, "landOrProperty.personCount.upload.error.tooBig"))
          )
        }

        "return invalid firstJointlyPerson required error if isPropertyHeldJointly is YES and howManyPersonsJointlyOwnProperty is entered but nothing entered as jointlyPerson" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, Some("1")),
            jointlyPersonList = List.empty,
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(FreeText, "landOrProperty.firstJointlyPerson.upload.error.required"))
          )
        }

        "return required jointlyName if isPropertyHeldJointly is YES and howManyPersonsJointlyOwnProperty is entered but first person does not have valid name" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, Some("1")),
            jointlyPersonList = List(
              (CsvValue(csvKey, None), CsvValue(csvKey, None), CsvValue(csvKey, None))
            ),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(FreeText, "landOrProperty.jointlyName.upload.error.required"))
          )
        }

        "return invalid jointlyNoNino.1 if isPropertyHeldJointly is YES and howManyPersonsJointlyOwnProperty is entered but first person does not have valid nino" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, Some("1")),
            jointlyPersonList = List(
              (CsvValue(csvKey, Some("Valid")), CsvValue(csvKey, None), CsvValue(csvKey, None))
            ),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(NinoFormat, "landOrProperty.jointlyNoNino.1.upload.error.required"))
          )
        }

        "return required jointlyNoNino.1 if isPropertyHeldJointly is YES and howManyPersonsJointlyOwnProperty is entered but first person does not have too long reason" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, Some("1")),
            jointlyPersonList = List(
              (CsvValue(csvKey, Some("Valid")), CsvValue(csvKey, None), CsvValue(csvKey, None))
            ),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(NinoFormat, "landOrProperty.jointlyNoNino.1.upload.error.required"))
          )
        }

        "return errors for other people is YES and howManyPersonsJointlyOwnProperty is entered but other persons has errors" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, Some("5")),
            jointlyPersonList = List(
              (CsvValue(csvKey, Some("Valid")), CsvValue(csvKey, Some("AB123456C")), CsvValue(csvKey, None)),
              (CsvValue(csvKey, Some("Valid")), CsvValue(csvKey, Some("INVALID")), CsvValue(csvKey, None)),
              (
                CsvValue(csvKey, Some(freeTextWith161Chars)),
                CsvValue(csvKey, Some("AB123411C")),
                CsvValue(csvKey, None)
              ),
              (CsvValue(csvKey, Some("Valid")), CsvValue(csvKey, None), CsvValue(csvKey, Some(freeTextWith161Chars)))
            ),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(NinoFormat, "landOrProperty.jointlyNino.2.upload.error.invalid"),
              genErr(FreeText, "landOrProperty.jointlyName.3.upload.error.tooLong"),
              genErr(FreeText, "landOrProperty.jointlyNoNino.4.upload.error.tooLong")
            )
          )
        }
      }

      // SUCCESS TESTS
      "get success results for validateJointlyHeldAll" - {
        "return successfully NO if isPropertyHeldJointly flag marked as false" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "NO"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, None),
            jointlyPersonList = List.empty,
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (YesNo.No, None, None)
          )
        }

        "return successfully YES and isPropertyHeldJointly if isPropertyHeldJointly flag marked as true and entered count and first person with nino correctly" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, Some("1")),
            jointlyPersonList = List(
              (CsvValue(csvKey, Some("NAME")), CsvValue(csvKey, Some("AB123456C")), CsvValue(csvKey, None))
            ),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (
              YesNo.Yes,
              Some(1),
              Some(
                List(
                  JointPropertyDetail("NAME", Some("AB123456C"), None)
                )
              )
            )
          )
        }

        "return successfully YES and isPropertyHeldJointly if isPropertyHeldJointly flag marked as true and entered count and multiple people entered" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, Some("3")),
            jointlyPersonList = List(
              (CsvValue(csvKey, Some("NAME")), CsvValue(csvKey, Some("AB123456C")), CsvValue(csvKey, None)),
              (CsvValue(csvKey, Some("SECOND")), CsvValue(csvKey, Some("AB123451C")), CsvValue(csvKey, None)),
              (CsvValue(csvKey, Some("THIRD")), CsvValue(csvKey, None), CsvValue(csvKey, Some("REASON")))
            ),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (
              YesNo.Yes,
              Some(3),
              Some(
                List(
                  JointPropertyDetail("NAME", Some("AB123456C"), None),
                  JointPropertyDetail("SECOND", Some("AB123451C"), None),
                  JointPropertyDetail("THIRD", None, Some("REASON"))
                )
              )
            )
          )
        }

        "return successfully YES and isPropertyHeldJointly if isPropertyHeldJointly flag marked as true and entered count and first person with reason no nino correctly" in {
          val validation = validator.validateJointlyHeldAll(
            isPropertyHeldJointly = CsvValue(csvKey, "YES"),
            howManyPersonsJointlyOwnProperty = CsvValue(csvKey, Some("1")),
            jointlyPersonList = List(
              (CsvValue(csvKey, Some("NAME")), CsvValue(csvKey, None), CsvValue(csvKey, Some("RESULT")))
            ),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (
              YesNo.Yes,
              Some(1),
              Some(
                List(
                  JointPropertyDetail("NAME", None, Some("RESULT"))
                )
              )
            )
          )
        }
      }
    }

    "validateLeasePerson" - {
      // ERROR TESTS
      "get errors for validateLeasePerson" - {
        "return required lesseeName error if isRequired flag true and lease person name is empty" in {
          val validation = validator.validateLeasePerson(
            count = 1,
            lesseeName = CsvValue(csvKey, None),
            lesseeConnectedOrUnconnected = CsvValue(csvKey, None),
            lesseeGrantedDate = CsvValue(csvKey, None),
            lesseeAnnualAmount = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(genErr(FreeText, "landOrProperty.firstLessee.upload.error.required"))
          )
        }

        "return required lesseeName 1 error if isRequired flag true and lease person name entered as empty" in {
          val validation = validator.validateLeasePerson(
            count = 1,
            lesseeName = CsvValue(csvKey, Some("")),
            lesseeConnectedOrUnconnected = CsvValue(csvKey, None),
            lesseeGrantedDate = CsvValue(csvKey, None),
            lesseeAnnualAmount = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(genErr(FreeText, "landOrProperty.lesseeName.1.upload.error.required"))
          )
        }

        "return required errors if isRequired flag true and lease person name entered but other details not entered" in {

          val validation = validator.validateLeasePerson(
            count = 1,
            lesseeName = CsvValue(csvKey, Some("VALID")),
            lesseeConnectedOrUnconnected = CsvValue(csvKey, None),
            lesseeGrantedDate = CsvValue(csvKey, None),
            lesseeAnnualAmount = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(
              genErr(ConnectedUnconnectedType, "landOrProperty.lesseeType.1.upload.error.required"),
              genErr(Price, "landOrProperty.lesseeAnnualAmount.1.upload.error.required"),
              genErr(LocalDateFormat, "landOrProperty.lesseeGrantedDate.1.upload.error.required")
            )
          )
        }

        "return invalid lesseeConnectedOrUnconnected 1 and required errors if isRequired flag true and lease person name entered but lesseeConnectedOrUnconnected not correct and other details not entered" in {
          val validation = validator.validateLeasePerson(
            count = 1,
            lesseeName = CsvValue(csvKey, Some("VALID")),
            lesseeConnectedOrUnconnected = CsvValue(csvKey, Some("INVALID")),
            lesseeGrantedDate = CsvValue(csvKey, None),
            lesseeAnnualAmount = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(
              genErr(ConnectedUnconnectedType, "landOrProperty.lesseeType.1.upload.error.invalid"),
              genErr(Price, "landOrProperty.lesseeAnnualAmount.1.upload.error.required"),
              genErr(LocalDateFormat, "landOrProperty.lesseeGrantedDate.1.upload.error.required")
            )
          )
        }

        "return required errors if isRequired flag true and lease person name and lesseeConnectedOrUnconnected entered and other details not entered" in {
          val validation = validator.validateLeasePerson(
            count = 1,
            lesseeName = CsvValue(csvKey, Some("VALID")),
            lesseeConnectedOrUnconnected = CsvValue(csvKey, Some("CONNECTED")),
            lesseeGrantedDate = CsvValue(csvKey, None),
            lesseeAnnualAmount = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(
              genErr(Price, "landOrProperty.lesseeAnnualAmount.1.upload.error.required"),
              genErr(LocalDateFormat, "landOrProperty.lesseeGrantedDate.1.upload.error.required")
            )
          )
        }

        "return invalid granted date and amount required errors if isRequired flag true and lease person name and lesseeConnectedOrUnconnected entered and other details not entered" in {
          val validation = validator.validateLeasePerson(
            count = 1,
            lesseeName = CsvValue(csvKey, Some("VALID")),
            lesseeConnectedOrUnconnected = CsvValue(csvKey, Some("CONNECTED")),
            lesseeGrantedDate = CsvValue(csvKey, Some("ASD")),
            lesseeAnnualAmount = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(
              genErr(Price, "landOrProperty.lesseeAnnualAmount.1.upload.error.required"),
              genErr(LocalDateFormat, "landOrProperty.lesseeGrantedDate.1.upload.error.invalid")
            )
          )
        }

        "return invalid granted date, amount and lesseeConnectedOrUnconnected errors if isRequired flag true and lease person name entered but other details are not " in {
          val validation = validator.validateLeasePerson(
            count = 1,
            lesseeName = CsvValue(csvKey, Some("VALID")),
            lesseeConnectedOrUnconnected = CsvValue(csvKey, Some("CONNECTED")),
            lesseeGrantedDate = CsvValue(csvKey, Some("ASD")),
            lesseeAnnualAmount = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(
              genErr(Price, "landOrProperty.lesseeAnnualAmount.1.upload.error.required"),
              genErr(LocalDateFormat, "landOrProperty.lesseeGrantedDate.1.upload.error.invalid")
            )
          )
        }
      }

      // SUCCESS TESTS
      "get success results for validateLeasePerson" - {
        "return successfully None if isRequired No and nothing entered" in {
          val validation = validator.validateLeasePerson(
            count = 1,
            lesseeName = CsvValue(csvKey, None),
            lesseeConnectedOrUnconnected = CsvValue(csvKey, None),
            lesseeGrantedDate = CsvValue(csvKey, None),
            lesseeAnnualAmount = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            None
          )
        }

        "return successfully LesseeDetail if isRequired Yes and all details entered correctly" in {
          val validation = validator.validateLeasePerson(
            count = 1,
            lesseeName = CsvValue(csvKey, Some("VALID NAME")),
            lesseeConnectedOrUnconnected = CsvValue(csvKey, Some("CONNECTED")),
            lesseeGrantedDate = CsvValue(csvKey, Some("11/01/2020")),
            lesseeAnnualAmount = CsvValue(csvKey, Some("123.12")),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkSuccess(
            validation,
            Some(
              LesseeDetail(
                lesseeName = "VALID NAME",
                lesseeConnectedParty = mConnectedOrUnconnectedType.Connected,
                leaseGrantedDate = LocalDate.of(2020, 1, 11),
                annualLeaseAmount = 123.12
              )
            )
          )
        }
      }
    }

    "validateLeasedAll" - {
      // ERROR TESTS
      "get errors for validateLeasedAll" - {
        "return required isLeased error if isLeased not entered" in {
          val validation = validator.validateLeasedAll(
            isLeased = CsvValue(csvKey, ""),
            lesseePeople = List(),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(YesNoQuestion, "landOrProperty.isLeased.upload.error.required"))
          )
        }

        "return invalid isLeased error if isLeased entered other than YES/NO" in {
          val validation = validator.validateLeasedAll(
            isLeased = CsvValue(csvKey, "ASD"),
            lesseePeople = List(),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(YesNoQuestion, "landOrProperty.isLeased.upload.error.invalid"))
          )
        }

        "return first lessee required error if isLeased Yes and no value entered" in {
          val validation = validator.validateLeasedAll(
            isLeased = CsvValue(csvKey, "YES"),
            lesseePeople = List(
              (
                CsvValue(csvKey, None),
                CsvValue(csvKey, None),
                CsvValue(csvKey, None),
                CsvValue(csvKey, None)
              ),
            ),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(genErr(FreeText, "landOrProperty.firstLessee.upload.error.required"))
          )
        }

        "return required errors for first error if isLeased Yes and name entered for first lessee but other fields are empty" in {
          val validation = validator.validateLeasedAll(
            isLeased = CsvValue(csvKey, "YES"),
            lesseePeople = List(
              (
                CsvValue(csvKey, Some("Name")),
                CsvValue(csvKey, None),
                CsvValue(csvKey, None),
                CsvValue(csvKey, None)
              ),
            ),
            memberFullNameDob = name,
            row = row
          )

          checkError(
            validation,
            List(
              genErr(ConnectedUnconnectedType, "landOrProperty.lesseeType.1.upload.error.required"),
              genErr(Price, "landOrProperty.lesseeAnnualAmount.1.upload.error.required"),
              genErr(LocalDateFormat, "landOrProperty.lesseeGrantedDate.1.upload.error.required"),
            )
          )
        }
      }

      // SUCCESS TESTS
      "get success results for validateLeasedAll" - {
        "return successfully Yes, None if isLeased selected as NO" in {
          val validation = validator.validateLeasedAll(
            isLeased = CsvValue(csvKey, "No"),
            lesseePeople = List(),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (YesNo.No, None)
          )
        }

        "return successfully LesseeDetail if isRequired Yes and all details entered correctly" in {
          val validation = validator.validateLeasedAll(
            isLeased = CsvValue(csvKey, "Yes"),
            lesseePeople = List(
              (
                CsvValue(csvKey, Some("Jenifer")),
                CsvValue(csvKey, Some("Connected")),
                CsvValue(csvKey, Some("12/01/2023")),
                CsvValue(csvKey, Some("12331.12"))
              ),
              (
                CsvValue(csvKey, Some("Lee")),
                CsvValue(csvKey, Some("Unconnected")),
                CsvValue(csvKey, Some("28/10/2022")),
                CsvValue(csvKey, Some("9923.12"))
              )
            ),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            (
              YesNo.Yes,
              Some(
                List(
                  LesseeDetail("Jenifer", mConnectedOrUnconnectedType.Connected, LocalDate.of(2023, 1, 12), 12331.12),
                  LesseeDetail("Lee", mConnectedOrUnconnectedType.Unconnected, LocalDate.of(2022, 10, 28), 9923.12),
                )
              )
            )
          )
        }
      }
    }

    "validatePurchaser" - {
      // ERROR TESTS
      "get errors for validatePurchaser" - {
        "return required purchaser error if isRequired flag true and name is empty" in {
          val validation = validator.validatePurchaser(
            count = 1,
            purchaserName = CsvValue(csvKey, None),
            purchaserConnectedOrUnconnected = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkError(
            validation,
            List(genErr(FreeText, "landOrProperty.firstPurchaser.upload.error.required"))
          )
        }
      }

      // SUCCESS TESTS
      "get success results for validatePurchaser" - {
        "return successfully None if isRequired No and nothing entered" in {
          val validation = validator.validatePurchaser(
            count = 1,
            purchaserName = CsvValue(csvKey, None),
            purchaserConnectedOrUnconnected = CsvValue(csvKey, None),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            None
          )
        }

        "return successfully Purchaser if isRequired Yes and all details entered correctly" in {
          val validation = validator.validatePurchaser(
            count = 1,
            purchaserName = CsvValue(csvKey, Some("VALID NAME")),
            purchaserConnectedOrUnconnected = CsvValue(csvKey, Some("unconnected")),
            memberFullNameDob = name,
            row = row,
            isRequired = true
          )

          checkSuccess(
            validation,
            Some(
              PurchaserDetail(
                purchaserConnectedParty = mConnectedOrUnconnectedType.Unconnected,
                purchaserName = "VALID NAME",
              )
            )
          )
        }

        "return successfully Purchaser if isRequired No but all details entered correctly" in {
          val validation = validator.validatePurchaser(
            count = 1,
            purchaserName = CsvValue(csvKey, Some("VALID NAME")),
            purchaserConnectedOrUnconnected = CsvValue(csvKey, Some("unconnected")),
            memberFullNameDob = name,
            row = row
          )

          checkSuccess(
            validation,
            Some(
              PurchaserDetail(
                purchaserConnectedParty = mConnectedOrUnconnectedType.Unconnected,
                purchaserName = "VALID NAME",
              )
            )
          )
        }
      }
    }

    "validateDuplicatedNinoNumbers" - {
      // ERROR TESTS
      "return duplicate error" in {
        val validation = validator.validateDuplicatedNinoNumbers(
          List(
            CsvValue(csvKey, Some("AB123456C")),
            CsvValue(csvKey, Some("AB123456C"))
          ),
          row
        )

        checkError(
          validation,
          List(genErr(NinoFormat, "landOrProperty.ninoNumbers.upload.error.duplicated"))
        )
      }

      // SUCCESS TESTS
      "return None of there is no duplicate" in {
        val validation = validator.validateDuplicatedNinoNumbers(
          List(
            CsvValue(csvKey, Some("AB123456C")),
            CsvValue(csvKey, Some("AB123256D")),
            CsvValue(csvKey, Some("AB123996E"))
          ),
          row
        )

        checkSuccess(
          validation,
          None
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
