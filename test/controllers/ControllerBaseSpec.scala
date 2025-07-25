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

package controllers

import cats.data.NonEmptyList
import cats.syntax.option.*
import controllers.actions.*
import generators.Generators
import models.*
import models.PensionSchemeId.{PsaId, PspId}
import models.UploadState.UploadValidated
import models.UserAnswers.SensitiveJsObject
import models.backend.responses.MemberDetails
import models.csv.CsvDocumentInvalid
import org.scalatest.OptionValues
import play.api.Application
import play.api.data.Form
import play.api.http.*
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Call
import play.api.test.*
import queries.Settable
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseSpec, DisplayMessageUtils}

import java.time.LocalDate

trait ControllerBaseSpec
    extends BaseSpec
    with ControllerBehaviours
    with DefaultAwaitTimeout
    with HttpVerbs
    with Writeables
    with HeaderNames
    with Status
    with PlayRunners
    with RouteInvokers
    with ResultExtractors
    with TestValues
    with DisplayMessageUtils {

  val baseUrl = "/pension-scheme-return"

  val testOnwardRoute: Call = Call("GET", "/foo")

  val defaultTaxYear: TaxYear = TaxYear(2022)

  protected def applicationBuilder(
    userAnswers: Option[UserAnswers] = None,
    schemeDetails: SchemeDetails = defaultSchemeDetails,
    minimalDetails: MinimalDetails = defaultMinimalDetails,
    isPsa: Boolean = true
  ): GuiceApplicationBuilder = {
    val identifierActionBind = if (isPsa) {
      bind[IdentifierAction].to[FakePsaIdentifierAction]
    } else {
      bind[IdentifierAction].to[FakePspIdentifierAction]
    }

    GuiceApplicationBuilder()
      .overrides(
        List[GuiceableModule](
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          identifierActionBind,
          bind[AllowAccessActionProvider].toInstance(FakeAllowAccessActionProvider(schemeDetails, minimalDetails)),
          bind[DataRetrievalAction].toInstance(FakeDataRetrievalAction(userAnswers)),
          bind[DataCreationAction].toInstance(FakeDataCreationAction(userAnswers.getOrElse(emptyUserAnswers)))
        ) ++ additionalBindings*
      )
      .configure("play.filters.csp.nonce.enabled" -> false)
  }

  protected val additionalBindings: List[GuiceableModule] = List()

  def runningApplication[T](block: Application => T): T =
    running(_ => applicationBuilder())(block)

  def formData[A](form: Form[A], data: A): List[(String, String)] = form.fill(data).data.toList

  extension (ua: UserAnswers)
    def unsafeSet[A: Writes](page: Settable[A], value: A): UserAnswers = ua.set(page, value).get

}

trait TestValues { self: OptionValues & Generators =>
  val accountNumber = "12345678"
  val sortCode = "123456"
  val srn: SchemeId.Srn = srnGen.sample.value
  val fbNumber: String = "test-fb-number"
  val schemeName = "testSchemeName"
  val email = "testEmail"
  val uploadKey: UploadKey = UploadKey("test-userid", srn, "test-redirect-tag")
  val reference: Reference = Reference("test-ref")
  val uploadFileName = "test-file-name"
  val psaId: PsaId = PsaId("A1234567")
  val pspId: PspId = PspId("A7654321")
  val individualName = "testIndividualName"
  val nino: Nino = ninoGen.sample.get
  val noninoReason: String = "reason"
  val utr: Utr = utrGen.sample.get
  val noUtrReason: String = "no utr reason"
  val leaseName = "testLeaseName"
  val money: Money = Money(123456)
  val moneyNegative: Money = Money(1123456)
  val double: Double = 7.7
  val percentage: Percentage = Percentage(7.7)
  val loanPeriod = 5
  val companyName = "testCompanyName"
  val partnershipName = "testPartnershipName"
  val otherName = "testOtherName"
  val crn: Crn = crnGen.sample.get
  val noCrnReason: String = "no crn reason"
  val recipientName = "testRecipientName"
  val employerName = "testEmployerName"
  val individualRecipientName: String = "individual " + recipientName
  val companyRecipientName: String = "company " + recipientName
  val partnershipRecipientName: String = "partnership " + recipientName
  val otherRecipientName: String = "other " + recipientName
  val otherRecipientDescription = "other description"
  val pstr = "testPstr"
  val version = "001"
  val titleNumber = "AB123456"
  val buyersName = "testBuyersName"
  val lenderName = "testLenderName"
  val amountBorrowed: (Money, Percentage) = (money, percentage)
  val reasonBorrowed = "test reason borrowed"
  val transferringSchemeName = "transferring scheme"

  val individualDetails: IndividualDetails = IndividualDetails("testFirstName", Some("testMiddleName"), "testLastName")
  val organisationName = "testOrganisation"

  val userAnswersId: String = "id"

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  val defaultUserAnswers: UserAnswers =
    UserAnswers(userAnswersId, SensitiveJsObject(Json.obj("non" -> "empty")))

  val dateRange: DateRange = DateRange(
    from = LocalDate.of(2020, 4, 6),
    to = LocalDate.of(2021, 4, 5)
  )

  val tooEarlyDate: LocalDate = LocalDate.of(1899, 12, 31)

  val defaultSchemeDetails: SchemeDetails = SchemeDetails(
    schemeName,
    "testPSTR",
    SchemeStatus.Open,
    "testSchemeType",
    Some("A1234567"),
    List(Establisher("testFirstName testLastName", EstablisherKind.Individual))
  )

  val minimalSchemeDetails: MinimalSchemeDetails = MinimalSchemeDetails(
    schemeName,
    srn.value,
    SchemeStatus.Open,
    LocalDate.now().some,
    LocalDate.now().plusDays(30).some
  )

  val defaultMinimalDetails: MinimalDetails = MinimalDetails(
    email,
    isPsaSuspended = false,
    Some(organisationName),
    Some(individualDetails),
    rlsFlag = false,
    deceasedFlag = false
  )

  val organizationMinimalDetails: MinimalDetails = MinimalDetails(
    email,
    isPsaSuspended = false,
    Some(organisationName),
    None,
    rlsFlag = false,
    deceasedFlag = false
  )

  val memberDetails: MemberDetails = MemberDetails(
    "testFirstName",
    "testLastName",
    Some(nino.value),
    None,
    LocalDate.of(1990, 12, 12)
  )

  val uploadSuccessful: UploadStatus.Success = UploadStatus.Success(uploadFileName, "text/csv", "test-url", None)
  val uploadFailure: UploadStatus.Failed.type = UploadStatus.Failed

  val listOfValidationErrors: NonEmptyList[ValidationError] = NonEmptyList.of(
    ValidationError(1, ValidationErrorType.FirstName, "error A1"),
    ValidationError(2, ValidationErrorType.LastName, "error C3"),
    ValidationError(3, ValidationErrorType.DateOfBirth, "error F2")
  )

  val uploadResultErrors: UploadValidated = UploadValidated(CsvDocumentInvalid(3, listOfValidationErrors))
}
