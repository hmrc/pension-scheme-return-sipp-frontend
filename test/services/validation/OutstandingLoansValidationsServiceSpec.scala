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
import models.requests.common.{
  IndOrOrgType,
  JointPropertyDetail,
  LesseeDetail,
  RegistryDetails,
  AcquiredFromType => mAcquiredFromType,
  ConnectedOrUnconnectedType => mConnectedOrUnconnectedType
}
import models.{CsvHeaderKey, CsvValue, ValidationError}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi

import java.time.LocalDate

class OutstandingLoansValidationsServiceSpec
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
  private val validator = new OutstandingLoansValidationsService(
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

  "OutstandingLoansValidationsServiceSpec" - {

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

    "validateAcquiredFrom" - {
      // ERROR TESTS
      "get errors for validateAcquiredFrom" - {
        "return required acquiredFromType error if no acquiredFromType entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, ""),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(AcquiredFromType, s"outstandingLoans.acquiredFromType.upload.error.required"))
          )
        }

        "return invalid acquiredFromType error if acquiredFromType is not valid" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "NOT_VALID"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(AcquiredFromType, s"outstandingLoans.acquiredFromType.upload.error.invalid"))
          )
        }

        "return required nino error if acquiredFromType is INDIVIDUAL, but no Nino or Other entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "INDIVIDUAL"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"outstandingLoans.acquirerNino.upload.error.required"))
          )
        }

        "return invalid nino error if acquiredFromType is INDIVIDUAL and wrong Nino entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "INDIVIDUAL"),
            acquirerNinoForIndividual = CsvValue(csvKey, Some("INVALID_NINO")),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(NinoFormat, s"outstandingLoans.acquirerNino.upload.error.invalid"))
          )
        }

        "return too long noWhoAcquiredFromTypeReasonSource error if acquiredFromType is INDIVIDUAL but noWhoAcquiredFromTypeReasonSource is too long" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "INDIVIDUAL"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, Some(freeTextWith161Chars)),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"outstandingLoans.noWhoAcquiredFromTypeReason.upload.error.tooLong"))
          )
        }

        "return required Crn error if acquiredFromType is COMPANY, but no Crn or Other entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "COMPANY"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"outstandingLoans.acquirerCrn.upload.error.required"))
          )
        }

        "return invalid Crn error if acquiredFromType is COMPANY and wrong Crn entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "COMPANY"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, Some("INVALID_CRN_NUMBER")),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(CrnFormat, s"outstandingLoans.acquirerCrn.upload.error.invalid"))
          )
        }

        "return too long noWhoAcquiredFromTypeReasonSource error if acquiredFromType is COMPANY but noWhoAcquiredFromTypeReasonSource is too long" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "COMPANY"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, Some(freeTextWith161Chars)),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"outstandingLoans.noWhoAcquiredFromTypeReason.upload.error.tooLong"))
          )
        }

        "return required UTR error if acquiredFromType is PARTNERSHIP, but no UTR or Other entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "PARTNERSHIP"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"outstandingLoans.acquirerUtr.upload.error.required"))
          )
        }

        "return invalid UTR error if acquiredFromType is PARTNERSHIP and wrong UTR entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "PARTNERSHIP"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, Some("INVALID_UTR_NUMBER")),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(UtrFormat, s"outstandingLoans.acquirerUtr.upload.error.invalid"))
          )
        }

        "return too long noWhoAcquiredFromTypeReasonSource error if acquiredFromType is PARTNERSHIP but noWhoAcquiredFromTypeReasonSource is too long" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "PARTNERSHIP"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, Some(freeTextWith161Chars)),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"outstandingLoans.noWhoAcquiredFromTypeReason.upload.error.tooLong"))
          )
        }

        "return required another source error if acquiredFromType is OTHER and no another source entered" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "OTHER"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"outstandingLoans.noWhoAcquiredFromTypeReason.upload.error.required"))
          )
        }

        "return too long noWhoAcquiredFromTypeReasonSource error if acquiredFromType is OTHER but noWhoAcquiredFromTypeReasonSource is too long" in {
          val validation = validator.validateAcquiredFrom(
            acquiredFromType = CsvValue(csvKey, "PARTNERSHIP"),
            acquirerNinoForIndividual = CsvValue(csvKey, None),
            acquirerCrnForCompany = CsvValue(csvKey, None),
            acquirerUtrForPartnership = CsvValue(csvKey, None),
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, Some(freeTextWith161Chars)),
            name,
            row
          )

          checkError(
            validation,
            List(genErr(FreeText, s"outstandingLoans.noWhoAcquiredFromTypeReason.upload.error.tooLong"))
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
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
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
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, Some("A")),
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
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, Some("Reason")),
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
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
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
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, Some("Reason")),
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
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, None),
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
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, Some("Reason")),
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
            whoAcquiredFromTypeReasonAsset = CsvValue(csvKey, Some("Other Details")),
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
