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

package services

import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.data.NonEmptyList
import controllers.TestValues
import forms.{DatePageFormProvider, IntFormProvider, MoneyFormProvider, NameDOBFormProvider, TextFormProvider}
import generators.WrappedMemberDetails
import models.ValidationErrorType._
import models._
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import services.validation.{MemberDetailsUploadValidator, ValidationsService}
import uk.gov.hmrc.domain.Nino
import utils.BaseSpec

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global

class MemberDetailsUploadValidatorSpec extends BaseSpec with TestValues {

  private val nameDOBFormProvider = new NameDOBFormProvider {}
  private val textFormProvider = new TextFormProvider {}
  private val datePageFormProvider = new DatePageFormProvider {}
  private val moneyFormProvider = new MoneyFormProvider {}
  private val intFormProvider = new IntFormProvider {}
  private val validations = new ValidationsService(
    nameDOBFormProvider,
    textFormProvider,
    datePageFormProvider,
    moneyFormProvider,
    intFormProvider
  )

  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())

  val validator =
    new MemberDetailsUploadValidator(validations)

  def formatDate(d: LocalDate): String = {
    val df = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    df.format(d)
  }

  def formatNino(e: Either[String, Nino]): String =
    e match {
      case Right(v) => s"$v,"
      case Left(v) => s",$v"
    }

  val validHeaders =
    "First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,\"If no National Insurance number for member, give reason\"," +
      s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
      s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
      s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
      s"Enter members non-UK address line 4,Enter members non-UK country"

  def validRow =
    detailsToRow(wrappedMemberDetailsGen.sample.get)

  def detailsToRow(details: WrappedMemberDetails): String =
    s"${details.nameDob.firstName},${details.nameDob.lastName},${formatDate(details.nameDob.dob)},${formatNino(details.nino)}"

  "validateCSV" - {
    List(
      ("windows", "\r\n"),
      ("*nix", "\n")
    ).foreach {
      case (name, lineEndings) =>
        s"$name line endings: successfully validates and saves the correct user answers" in {

          val csv = {
            //Header
            s"First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,If no National Insurance number for member\\, give reason," +
              s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
              s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
              s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
              s"Enter members non-UK address line 4,Enter members non-UK country$lineEndings" +
              s",,,,,,,,,,,,,,,$lineEndings" + //explainer row
              //CSV values
              s"Jason,Lawrence,6-10-1989,AB123456A,,YES,1 Avenue,,,Brightonston,SE111BG,,,,,$lineEndings" +
              s"Pearl,Parsons,12/4/1990,sh227613B,,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,$lineEndings" +
              s"Jack-Thomson,Jason,01-10-1989,,reason,NO,,,,,,Flat 1,Burlington Street,,Brightonston,jamaica$lineEndings"
          }

          val source = Source.single(ByteString(csv))
          val actual = validator.validateUpload(source, None).futureValue

          actual._1 mustBe UploadSuccessMemberDetails(
            List(
              MemberDetailsUpload(
                3,
                "Jason",
                "Lawrence",
                "6-10-1989",
                Some("AB123456A"),
                None,
                "YES",
                Some("1 Avenue"),
                None,
                None,
                Some("Brightonston"),
                Some("SE111BG"),
                None,
                None,
                None,
                None,
                None
              ),
              MemberDetailsUpload(
                4,
                "Pearl",
                "Parsons",
                "12/4/1990",
                Some("SH227613B"),
                None,
                "YES",
                Some("2 Avenue"),
                Some("1 Drive"),
                Some("Flat 5"),
                Some("Brightonston"),
                Some("SE101BG"),
                None,
                None,
                None,
                None,
                None
              ),
              MemberDetailsUpload(
                5,
                "Jack-Thomson",
                "Jason",
                "01-10-1989",
                None,
                Some("reason"),
                "NO",
                None,
                None,
                None,
                None,
                None,
                Some("Flat 1"),
                Some("Burlington Street"),
                None,
                Some("Brightonston"),
                Some("jamaica")
              )
            )
          )
          actual._2 mustBe 3
        }
    }

    "successfully collect Name errors" in {
      val csv = {
        //Header
        s"First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,If no National Insurance number for member\\, give reason," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          s",,,,,,,,,,,,,,,\r\n" + //explainer row
          //CSV values
          s"Jason-Jason-Law,Lawrence,01-10-1989,AB123456A,,YES,1 Avenue,,,Brightonston,SE111BG,,,,,\r\n" +
          s"Pearl Carl Jason,Parsons,01-10-1989,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, None).futureValue

      assertErrors(
        actual,
        NonEmptyList.of(
          ValidationError(3, FirstName, "memberDetails.firstName.upload.error.invalid"),
          ValidationError(4, FirstName, "memberDetails.firstName.upload.error.invalid")
        )
      )

      actual._2 mustBe 2
    }

    "successfully collect Nino errors" in {
      val csv = {
        //Header
        s"First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,If no National Insurance number for member\\, give reason," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          s",,,,,,,,,,,,,,,\r\n" + //explainer row
          //CSV values
          s"Jason-Jason,Lawrence,01-10-1989,BAD-NINO,,YES,1 Avenue,,,Brightonston,SE111BG,,,,,\r\n" +
          s"Pearl Carl,Parsons,01-10-1989,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n" +
          s"Jason,Lawrence,6-10-1989,,reason,YES,1 Avenue,,,Brightonston,SE111BG,,,,,\r\n" +
          s"Jason,Lawrence,6-10-1989,AB123456A,,YES,1 Avenue,,,Brightonston,SE111BG,,,,,\r\n" +
          s"Jason,Lawrence,6-10-1989,ab123456A,,YES,1 Avenue,,,Brightonston,SE111BG,,,,,\r\n" +
          s"Jason,Lawrence,6-10-1989,ab123456A,,YES,1 Avenue,,,Brightonston,SE111BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, None).futureValue

      assertErrors(
        actual,
        NonEmptyList.of(
          ValidationError(3, NinoFormat, "memberDetailsNino.upload.error.invalid"),
          ValidationError(7, NinoFormat, "memberDetailsNino.upload.error.duplicate"),
          ValidationError(8, NinoFormat, "memberDetailsNino.upload.error.duplicate")
        )
      )

      actual._2 mustBe 6
    }

    "successfully collect Yes/No errors" in {
      val csv = {
        //Header
        s"First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,If no National Insurance number for member\\, give reason," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          s",,,,,,,,,,,,,,,\r\n" + //explainer row
          //CSV values
          s"Jason-Jason,Lawrence,01-10-1989,,reason,Certainly,1 Avenue,,,Brightonston,SE111BG,,,,,\r\n" +
          s"Pearl Carl,Parsons,01-10-1989,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n" +
          s"Pearl Carl,Parsons,01-10-1989,,reason,,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, None).futureValue

      assertErrors(
        actual,
        NonEmptyList.of(
          ValidationError(3, YesNoAddress, "isUK.upload.error.invalid"),
          ValidationError(5, YesNoAddress, "isUK.upload.error.required")
        )
      )

      actual._2 mustBe 3
    }

    "successfully collect DOB errors" in {
      val csv = {
        //Header
        s"First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,If no National Insurance number for member\\, give reason," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          s",,,,,,,,,,,,,,,\r\n" + //explainer row
          //CSV values
          s"Jason,Lawrence,56-10-1989,AB123456A,,YES,1 Avenue,,,Brightonston,SE111BG,,,,,\r\n" +
          s"Pearl,Parsons,19901012,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n" +
          s"Pearl,Parsons,12/12/12,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n" +
          s"Pearl,Parsons,3/1/2023,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, Some(LocalDate.of(2023, 1, 2))).futureValue

      assertErrors(
        actual,
        NonEmptyList.of(
          ValidationError(3, ValidationErrorType.DateOfBirth, "memberDetails.dateOfBirth.upload.error.invalid.date"),
          ValidationError(4, ValidationErrorType.DateOfBirth, "memberDetails.dateOfBirth.error.format"),
          ValidationError(5, ValidationErrorType.DateOfBirth, "memberDetails.dateOfBirth.upload.error.after"),
          ValidationError(6, ValidationErrorType.DateOfBirth, "memberDetails.dateOfBirth.upload.error.future")
        )
      )

      actual._2 mustBe 4
    }

    "successfully collect errors when both Nino and No Nino reason are not present" in {
      val csv = {
        //Header
        s"First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,If no National Insurance number for member\\, give reason," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          s",,,,,,,,,,,,,,,\r\n" + //explainer row
          //CSV values
          s"Jason,Lawrence,01-10-1988,AB123456A,,YES,1 Avenue,,,Brightonston,SE111BG,,,,,\r\n" +
          s"Pearl,Parsons,01-10-1988,,,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, None).futureValue

      assertErrors(
        actual,
        NonEmptyList.of(
          ValidationError(4, NoNinoReason, "noNINO.upload.error.required")
        )
      )
      actual._2 mustBe 2
    }

    "successfully collect Is the members address in the UK? errors" in {
      val csv = {
        //Header
        s"First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,If no National Insurance number for member\\, give reason," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          s",,,,,,,,,,,,,,,\r\n" + //explainer row
          //CSV values
          s"Jason-Jason,Lawrence,01-10-1989,AB123456A,,SomethingElse,1 Avenue,,,Brightonston,SE111BG,,,,,\r\n" +
          s"Pearl Jason,Parsons,01-10-1989,,reason,,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, None).futureValue

      assertErrors(
        actual,
        NonEmptyList.of(
          ValidationError(3, YesNoAddress, "isUK.upload.error.invalid"),
          ValidationError(4, YesNoAddress, "isUK.upload.error.required")
        )
      )

      actual._2 mustBe 2
    }

    "successfully collect UK Address errors" in {
      val csv = {
        //Header
        s"First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,If no National Insurance number for member\\, give reason," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          s",,,,,,,,,,,,,,,\r\n" + //explainer row
          //CSV values
          s"Jason-Jason,Lawrence,01-10-1989,AB123456A,,YES,1 Avenueaueueueueueueueueueueueeueueueeueueue,2 Avenueaueueueueueueueueueueueeueueueeueueue,,sdfdsf,UQ2 3JM£%^&*,,,,,\r\n" +
          s"Pearl Jason,Parsons,01-10-1989,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n" +
          s"Pearl Jason,Parsons,01-10-1989,,reason,YES,2 Avenue,1 Drive,Flat 5,,SE101BG,,,,,\r\n" +
          s"Pearl Jason,Parsons,01-10-1989,,reason,YES,,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, None).futureValue

      assertErrors(
        actual,
        NonEmptyList.of(
          ValidationError(3, AddressLine, "address-line.upload.error.length"),
          ValidationError(3, AddressLine, "address-line.upload.error.length"),
          ValidationError(3, UKPostcode, "postcode.upload.error.invalid"),
          ValidationError(5, TownOrCity, "town-or-city.upload.error.required"),
          ValidationError(6, AddressLine, "address-line.upload.error.required")
        )
      )

      actual._2 mustBe 4
    }

    "successfully collect NON UK Address errors" in {
      val csv = {
        //Header
        s"First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,If no National Insurance number for member\\, give reason," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          s",,,,,,,,,,,,,,,\r\n" + //explainer row
          //CSV values
          s"Pearl Jason,Parsons,01-10-1989,,reason,NO,,,,,,Flaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaat 1,,,aaaa,jamaica\r\n" +
          s"Pearl Jason,Parsons,01-10-1989,,reason,NO,,,,,,Flat 1,,,,jamaica\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, None).futureValue

      assertErrors(
        actual,
        NonEmptyList.of(
          ValidationError(3, AddressLine, "address-line.upload.error.length"),
          ValidationError(4, AddressLine, "town-or-city-non-uk.upload.error.required")
        )
      )

      actual._2 mustBe 2
    }

    "Fail when Is the members address in the UK? is YES, but UK fields are missing " in {
      val csv = {
        //Header
        s"First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,If no National Insurance number for member\\, give reason," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          //CSV values
          s",,,,,,,,,,,,,,,\r\n" + //explainer row
          s"Jason,Lawrence,6-10-1989,AB123456A,,YES,,,,,,,,,,\r\n" +
          s"Pearl,Parsons,12-4-1990,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, None).futureValue

      assertErrors(
        actual,
        NonEmptyList.of(
          ValidationError(3, AddressLine, "address-line.upload.error.required")
        )
      )

      actual._2 mustBe 2
    }

    "Fail when Is the members address in the UK? is NO, but NON-UK fields are missing " in {
      val csv = {
        //Header
        s"First name of scheme member,Last name of scheme member,Member date of birth,Member National Insurance number,If no National Insurance number for member\\, give reason," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK  country\r\n" +
          s",,,,,,,,,,,,,,,\r\n" + //explainer row
          //CSV values
          s"Pearl,Parsons,12-4-1990,,reason,NO,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, None).futureValue

      actual._1 mustBe a[UploadFormatError]
      actual._2 mustBe 1
    }

    "fails when no rows provided" in {

      val csv = validHeaders

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, None).futureValue
      actual._1 mustBe a[UploadFormatError]
      actual._2 mustBe 0
    }

    "fails when empty file sent" in {
      val csv = ""

      val source = Source.single(ByteString(csv))

      val actual = validator.validateUpload(source, None).futureValue
      actual._1 mustBe a[UploadFormatError]
      actual._2 mustBe 0
    }
  }

  private def assertErrors(call: => (Upload, Int, Long), errors: NonEmptyList[ValidationError]) =
    call._1 match {
      case UploadErrorsMemberDetails(_, err) => err mustBe errors
      case _ => fail("No Upload Errors exist")

    }
}
