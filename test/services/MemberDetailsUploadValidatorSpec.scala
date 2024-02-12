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
import forms.{NameDOBFormProvider, TextFormProvider}
import generators.WrappedMemberDetails
import models.ValidationErrorType.{AddressLine, FirstName, NinoFormat, UKPostcode, YesNoAddress}
import models._
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
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
  private val validations = new ValidationsService(nameDOBFormProvider, textFormProvider)

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
    "First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number"

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
            s"First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number," +
              s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
              s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
              s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
              s"Enter members non-UK address line 4,Enter members non-UK country$lineEndings" +
              //CSV values
              s"Jason,Lawrence,6-10-1989,AB123456A,,YES,1 Avenue,,,,SE111BG,,,,,$lineEndings" +
              s"Pearl,Parsons,12-4-1990,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,$lineEndings" +
              s"Jack-Thomson,Jason,01-10-1989,,reason,NO,,,,,,Flat 1,Burlington Street,,,jamaica$lineEndings"
          }

          val source = Source.single(ByteString(csv))
          val actual = validator.validateCSV(source, None).futureValue

          actual._1 mustBe UploadSuccess(
            List(
              UploadMemberDetails(
                1,
                NameDOB("Jason", "Lawrence", LocalDate.of(1989, 10, 6)),
                Right(Nino("AB123456A")),
                UKAddress("1 Avenue", None, None, None, "SE111BG")
              ),
              UploadMemberDetails(
                2,
                NameDOB("Pearl", "Parsons", LocalDate.of(1990, 4, 12)),
                Left("reason"),
                UKAddress("2 Avenue", Some("1 Drive"), Some("Flat 5"), Some("Brightonston"), "SE101BG")
              ),
              UploadMemberDetails(
                3,
                NameDOB("Jack-Thomson", "Jason", LocalDate.of(1989, 10, 1)),
                Left("reason"),
                ROWAddress("Flat 1", Some("Burlington Street"), None, None, "jamaica")
              )
            )
          )
          actual._2 mustBe 3
        }
    }

    "successfully collect Name errors" in {
      val csv = {
        //Header
        s"First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          //CSV values
          s"Jason-Jason-Law,Lawrence,01-10-1989,AB123456A,,YES,1 Avenue,,,,SE111BG,,,,,\r\n" +
          s"Pearl Carl Jason,Parsons,01-10-1989,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue

      actual._1 mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("A1", FirstName, "memberDetails.firstName.upload.error.invalid"),
          ValidationError("A2", FirstName, "memberDetails.firstName.upload.error.invalid")
        )
      )
      actual._2 mustBe 2
    }

    "successfully collect Nino errors" in {
      val csv = {
        //Header
        s"First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          //CSV values
          s"Jason-Jason,Lawrence,01-10-1989,BAD-NINO,,YES,1 Avenue,,,,SE111BG,,,,,\r\n" +
          s"Pearl Carl,Parsons,01-10-1989,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue

      actual._1 mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("D1", NinoFormat, "memberDetailsNino.upload.error.invalid")
        )
      )
      actual._2 mustBe 2
    }

    "successfully collect DOB errors" in {
      val csv = {
        //Header
        s"First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          //CSV values
          s"Jason,Lawrence,56-10-1989,AB123456A,,YES,1 Avenue,,,,SE111BG,,,,,\r\n" +
          s"Pearl,Parsons,19901012,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue

      actual._1 mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("C1", ValidationErrorType.DateOfBirth, "memberDetails.dateOfBirth.upload.error.invalid.date"),
          ValidationError("C2", ValidationErrorType.DateOfBirth, "memberDetails.dateOfBirth.error.format")
        )
      )
      actual._2 mustBe 2
    }

    "fail when both Nino and No Nino reason are present" in {
      val csv = {
        //Header
        s"First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          //CSV values
          s"Jason,Lawrence,01-10-1988,AB123456A,,YES,1 Avenue,,,,SE111BG,,,,,\r\n" +
          s"Pearl,Parsons,01-10-1988,,,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue

      actual._1 mustBe UploadFormatError
      actual._2 mustBe 2
    }

    "successfully collect Is the members address in the UK? errors" in {
      val csv = {
        //Header
        s"First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          //CSV values
          s"Jason-Jason,Lawrence,01-10-1989,AB123456A,,SomethingElse,1 Avenue,,,,SE111BG,,,,,\r\n" +
          s"Pearl Jason,Parsons,01-10-1989,,reason,,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue

      actual._1 mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("F1", YesNoAddress, "TODO key for invalid value"),
          ValidationError("F1", YesNoAddress, "TODO key for too long value"),
          ValidationError("F2", YesNoAddress, "TODO key for required value")
        )
      )
      actual._2 mustBe 2
    }

    "successfully collect UK Address errors" in {
      val csv = {
        //Header
        s"First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          //CSV values
          s"Jason-Jason,Lawrence,01-10-1989,AB123456A,,YES,1 Avenueaueueueueueueueueueueueeueueueeueueue,2 Avenueaueueueueueueueueueueueeueueueeueueue,,,adsadasdsad,,,,,\r\n" +
          s"Pearl Jason,Parsons,01-10-1989,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue

      actual._1 mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("G1", AddressLine, "TODO key for too long value"),
          ValidationError("H1", AddressLine, "TODO key for too long value"),
          ValidationError("K1", UKPostcode, "TODO key for invalid format value")
        )
      )
      actual._2 mustBe 2
    }

    "successfully collect NON UK Address errors" in {
      val csv = {
        //Header
        s"First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          //CSV values
          s"Pearl Jason,Parsons,01-10-1989,,reason,NO,,,,,,Flaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaat 1,,,,jamaica\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue

      actual._1 mustBe UploadErrors(
        NonEmptyList.of(
          ValidationError("L1", AddressLine, "TODO key for too long value")
        )
      )
      actual._2 mustBe 1
    }

    "Fail when Is the members address in the UK? is YES, but UK fields are missing " in {
      val csv = {
        //Header
        s"First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          //CSV values
          s"Jason,Lawrence,6-10-1989,AB123456A,,YES,,,,,,,,,,\r\n" +
          s"Pearl,Parsons,12-4-1990,,reason,YES,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue

      actual._1 mustBe UploadFormatError
      actual._2 mustBe 1
    }

    "Fail when Is the members address in the UK? is NO, but NON-UK fields are missing " in {
      val csv = {
        //Header
        s"First name,Last name,Date of birth,National Insurance number,Reason for no National Insurance number," +
          s"Is the members address in the UK?,Enter the members UK address line 1,Enter members UK address line 2," +
          s"Enter members UK address line 3,Enter name of members UK town or city,Enter members post code," +
          s"Enter the members non-UK address line 1,Enter members non-UK address line 2,Enter members non-UK address line 3," +
          s"Enter members non-UK address line 4,Enter members non-UK country\r\n" +
          //CSV values
          s"Pearl,Parsons,12-4-1990,,reason,NO,2 Avenue,1 Drive,Flat 5,Brightonston,SE101BG,,,,,\r\n"
      }

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue

      actual._1 mustBe UploadFormatError
      actual._2 mustBe 1
    }

    "fails when no rows provided" in {

      val csv = validHeaders

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue
      actual._1 mustBe UploadFormatError
      actual._2 mustBe 0
    }

    "fails when empty file sent" in {
      val csv = ""

      val source = Source.single(ByteString(csv))

      val actual = validator.validateCSV(source, None).futureValue
      actual._1 mustBe UploadFormatError
      actual._2 mustBe 0
    }

  }
}
