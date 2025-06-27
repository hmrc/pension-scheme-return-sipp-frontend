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

package generators

import config.RefinedTypes.OneTo5000
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import models.PensionSchemeId.{PsaId, PspId}
import models.SchemeId.{Pstr, Srn}
import models.SchemeStatus.*
import models.backend.responses.MemberDetails
import models.cache.PensionSchemeUser
import models.cache.PensionSchemeUser.{Administrator, Practitioner}
import models.requests.IdentifierRequest.{AdministratorRequest, PractitionerRequest}
import models.requests.common.YesNo
import models.requests.common.YesNo.{No, Yes}
import models.requests.{AllowedAccessRequest, IdentifierRequest}
import models.*
import org.scalacheck.Gen
import org.scalacheck.Gen.numChar
import org.scalacheck.Arbitrary.arbitrary
import pages.TaskListStatusPage
import play.api.mvc.Request
import uk.gov.hmrc.domain.Nino
import viewmodels.models.SectionCompleted

import java.time.{LocalDate, LocalDateTime, ZoneOffset}

trait ModelGenerators extends BasicGenerators {

  implicit lazy val sectionCompletedGen: Gen[SectionCompleted.type] = Gen.const(SectionCompleted)

  lazy val minimalDetailsGen: Gen[MinimalDetails] =
    for {
      email <- emailGen
      isSuspended <- boolean
      orgName <- Gen.option(nonEmptyString)
      individual <- Gen.option(individualDetailsGen)
      rlsFlag <- boolean
      deceasedFlag <- boolean
    } yield MinimalDetails(email, isSuspended, orgName, individual, rlsFlag, deceasedFlag)

  lazy val individualDetailsGen: Gen[IndividualDetails] =
    for {
      firstName <- nonEmptyString
      middleName <- Gen.option(nonEmptyString)
      lastName <- nonEmptyString
    } yield IndividualDetails(firstName, middleName, lastName)

  val validSchemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(
      Open,
      WoundUp,
      Deregistered
    )

  lazy val invalidSchemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(
      Pending,
      PendingInfoRequired,
      PendingInfoReceived,
      Rejected,
      RejectedUnderAppeal
    )

  lazy val schemeStatusGen: Gen[SchemeStatus] =
    Gen.oneOf(validSchemeStatusGen, invalidSchemeStatusGen)

  lazy val establisherGen: Gen[Establisher] =
    for {
      name <- Gen.listOfN(3, nonEmptyString).map(_.mkString(" "))
      kind <- Gen.oneOf(EstablisherKind.Company, EstablisherKind.Individual, EstablisherKind.Partnership)
    } yield Establisher(name, kind)

  lazy val schemeDetailsGen: Gen[SchemeDetails] =
    for {
      name <- nonEmptyString
      pstr <- nonEmptyString
      status <- schemeStatusGen
      schemeType <- nonEmptyString
      authorisingPsa <- Gen.option(nonEmptyString)
      establishers <- Gen.listOf(establisherGen)
    } yield SchemeDetails(name, pstr, status, schemeType, authorisingPsa, establishers)

  lazy val minimalSchemeDetailsGen: Gen[MinimalSchemeDetails] =
    for {
      name <- nonEmptyString
      srn <- srnGen.map(_.value)
      schemeStatus <- schemeStatusGen
      openDate <- Gen.option(date)
      windUpDate <- Gen.option(date)
    } yield MinimalSchemeDetails(name, srn, schemeStatus, openDate, windUpDate)

  lazy val listMinimalSchemeDetailsGen: Gen[ListMinimalSchemeDetails] =
    Gen.listOf(minimalSchemeDetailsGen).map(xs => ListMinimalSchemeDetails(xs))

  lazy val pensionSchemeUserGen: Gen[PensionSchemeUser] =
    Gen.oneOf(Administrator, Practitioner)

  lazy val psaIdGen: Gen[PsaId] = nonEmptyString.map(PsaId.apply)
  lazy val pspIdGen: Gen[PspId] = nonEmptyString.map(PspId.apply)
  lazy val pensionSchemeIdGen: Gen[PensionSchemeId] = Gen.oneOf(psaIdGen, pspIdGen)

  lazy val srnGen: Gen[Srn] =
    Gen
      .listOfN(10, numChar)
      .flatMap { xs =>
        Srn(s"S${xs.mkString}")
          .fold[Gen[Srn]](Gen.fail)(x => Gen.const(x))
      }

  lazy val pstrGen: Gen[Pstr] = nonEmptyString.map(Pstr.apply)

  lazy val schemeIdGen: Gen[SchemeId] = Gen.oneOf(srnGen, pstrGen)

  def practitionerRequestGen[A](request: Request[A]): Gen[PractitionerRequest[A]] =
    for {
      userId <- nonEmptyString
      externalId <- nonEmptyString
      pspId <- pspIdGen
    } yield PractitionerRequest(userId, externalId, request, pspId)

  def administratorRequestGen[A](request: Request[A]): Gen[AdministratorRequest[A]] =
    for {
      userId <- nonEmptyString
      externalId <- nonEmptyString
      psaId <- psaIdGen
    } yield AdministratorRequest(userId, externalId, request, psaId)

  def identifierRequestGen[A](request: Request[A]): Gen[IdentifierRequest[A]] =
    Gen.oneOf(administratorRequestGen[A](request), practitionerRequestGen[A](request))

  def allowedAccessRequestGen[A](request: Request[A]): Gen[AllowedAccessRequest[A]] =
    for {
      request <- identifierRequestGen[A](request)
      schemeDetails <- schemeDetailsGen
      minimalDetails <- minimalDetailsGen
      srn <- srnGen
    } yield AllowedAccessRequest(request, schemeDetails, minimalDetails, srn)

  def modeGen: Gen[Mode] = Gen.oneOf(NormalMode, CheckMode)

  implicit lazy val dateRangeGen: Gen[DateRange] =
    for {
      startDate <- date
      endDate <- date
    } yield DateRange(startDate, endDate)

  def dateRangeWithinRangeGen(range: DateRange): Gen[DateRange] =
    for {
      date1 <- datesBetween(range.from, range.to)
      date2 <- datesBetween(range.from, range.to)
    } yield
      if (date1.isBefore(date2)) DateRange(date1, date2)
      else DateRange(date2, date1)

  lazy val bankAccountGen: Gen[BankAccount] =
    for {
      bankName <- nonEmptyAlphaString.map(_.take(28))
      accountNumber <- Gen.listOfN(8, numChar).map(_.mkString)
      separator <- Gen.oneOf("", " ", "-")
      pairs = Gen.listOfN(2, numChar).map(_.mkString)
      sortCode <- Gen.listOfN(3, pairs).map(_.mkString(separator))
    } yield BankAccount(bankName, accountNumber, sortCode)

  lazy val localDateTimeGen: Gen[LocalDateTime] =
    for {
      seconds <- Gen.chooseNum(
        LocalDateTime.MIN.toEpochSecond(ZoneOffset.UTC),
        LocalDateTime.MAX.toEpochSecond(ZoneOffset.UTC)
      )
      nanos <- Gen.chooseNum(LocalDateTime.MIN.getNano, LocalDateTime.MAX.getNano)
    } yield LocalDateTime.ofEpochSecond(seconds, nanos, ZoneOffset.UTC)

  implicit lazy val moneyGen: Gen[Money] = for {
    whole <- Gen.choose(0, 100000)
    decimals <- Gen.option(Gen.choose(0, 99))
  } yield {
    val decimalString = decimals.map(d => s".$d").getOrElse("")
    val result = s"$whole$decimalString".toDouble
    Money(result)
  }

  implicit lazy val moneyInPeriodGen: Gen[MoneyInPeriod] = for {
    moneyAtStart <- moneyGen
    moneyAtEnd <- moneyGen
  } yield MoneyInPeriod(moneyAtStart, moneyAtEnd)

  lazy val ninoPrefix: Gen[String] =
    (for {
      fst <- Gen.oneOf('A' to 'Z')
      snd <- Gen.oneOf('A' to 'Z')
    } yield s"$fst$snd").retryUntil(s => Nino.isValid(s"${s}000000A"))

  implicit lazy val ninoGen: Gen[Nino] = for {
    prefix <- ninoPrefix
    numbers <- Gen.listOfN(6, Gen.numChar).map(_.mkString)
    suffix <- Gen.oneOf("A", "B", "C", "D")
  } yield Nino(s"$prefix$numbers$suffix")

  implicit lazy val ninoTypeGen: Gen[NinoType] = for {
    nino <- Gen.option(ninoGen).map(_.map(_.nino))
    reasonNoNino <- condGen(nino.isEmpty, arbitrary[String])
  } yield NinoType(nino, reasonNoNino)

  implicit lazy val utrGen: Gen[Utr] = for {
    numbers <- Gen.listOfN(10, Gen.numChar).map(_.mkString)
  } yield Utr(s"$numbers")

  lazy val crnPrefix: Gen[String] =
    (for {
      fst <- Gen.oneOf('A' to 'Z')
      snd <- Gen.oneOf('A' to 'Z')
    } yield s"$fst$snd").retryUntil(s => Crn.isValid(s"${s}000000"))

  implicit lazy val crnGen: Gen[Crn] = for {
    prefix <- crnPrefix
    numbers <- Gen.listOfN(6, Gen.numChar).map(_.mkString)
  } yield Crn(s"$prefix$numbers")

  implicit lazy val nameDobGen: Gen[NameDOB] = for {
    firstName <- nonEmptyAlphaString.map(_.take(10))
    lastName <- nonEmptyAlphaString.map(_.take(10))
    dob <- datesBetween(earliestDate, LocalDate.now())
  } yield NameDOB(firstName, lastName, dob)

  lazy val wrappedMemberDetailsGen: Gen[WrappedMemberDetails] =
    for {
      nameDob <- nameDobGen
      nino <- Gen.either(nonEmptyString, ninoGen)
    } yield WrappedMemberDetails(nameDob, nino)
  
  lazy val yesNoGen: Gen[YesNo] = Gen.oneOf(Yes, No)

  lazy val acquiredFromTypeGen: Gen[String] = Gen.oneOf(List("INDIVIDUAL", "COMPANY", "PARTNERSHIP", "OTHER"))

  lazy val connectedOrUnconnectedTypeGen: Gen[String] = Gen.oneOf(List("CONNECTED", "UNCONNECTED"))

  implicit lazy val memberDetailsGen: Gen[MemberDetails] = for {
    firstName <- nonEmptyString
    lastName <- nonEmptyString
    nino <- Gen.option(ninoGen.map(_.value))
    dob <- localDateTimeGen.map(_.toLocalDate)
  } yield MemberDetails(
    firstName,
    lastName,
    nino,
    reasonNoNINO = None,
    dateOfBirth = dob
  )

  implicit lazy val taskListStatusPageStatusGen: Gen[TaskListStatusPage.Status] =
    boolean.map(TaskListStatusPage.Status.apply)

  implicit lazy val personalDetailsUpdateDataGen: Gen[PersonalDetailsUpdateData] = for {
    current <- memberDetailsGen
    updated <- memberDetailsGen
    isSubmitted <- boolean
  } yield PersonalDetailsUpdateData(
    current,
    updated,
    isSubmitted
  )

  implicit lazy val typeOfViewChangeQuestion: Gen[TypeOfViewChangeQuestion] = Gen.oneOf(TypeOfViewChangeQuestion.values)
}

case class WrappedMemberDetails(nameDob: NameDOB, nino: Either[String, Nino])
