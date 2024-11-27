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

import cats.data.NonEmptyList
import cats.syntax.option.catsSyntaxOptionId
import models.{Pagination, ValidationError, ValidationErrorType}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen.{alphaChar, alphaNumChar, alphaNumStr, alphaStr, choose, chooseNum, listOfN, numChar}
import org.scalatest.EitherValues
import play.api.libs.json.*
import play.api.mvc.Call
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.ListType.Bullet
import viewmodels.DisplayMessage.*
import viewmodels.models.PaginatedViewModel

import java.time.{Instant, LocalDate, ZoneOffset}
import scala.annotation.nowarn

trait BasicGenerators extends EitherValues {

  implicit val basicStringGen: Gen[String] = nonEmptyAlphaString
  implicit val unitGen: Gen[Unit] = Gen.const(())

  def condGen[A](cond: Boolean, gen: Gen[A]): Gen[Option[A]] = if (cond) gen.map(_.some) else Gen.const(None)

  def genIntersperseString(gen: Gen[String], value: String, frequencyV: Int = 1, frequencyN: Int = 10): Gen[String] = {
    val genValue: Gen[Option[String]] = Gen.frequency(frequencyN -> None, frequencyV -> Gen.const(Some(value)))

    for {
      seq1 <- gen
      seq2 <- Gen.listOfN(seq1.length, genValue)
    } yield seq1.toSeq.zip(seq2).foldLeft("") {
      case (acc, (n, Some(v))) => acc + n + v
      case (acc, (n, _)) => acc + n
    }
  }

  def intsInRangeWithCommas(min: Int, max: Int): Gen[String] = {
    val numberGen = choose[Int](min, max).map(_.toString)
    genIntersperseString(numberGen, ",")
  }

  def intsLargerThanMaxValue: Gen[BigInt] =
    arbitrary[BigInt].suchThat(x => x > Int.MaxValue)

  def intsSmallerThanMinValue: Gen[BigInt] =
    arbitrary[BigInt].suchThat(x => x < Int.MinValue)

  def nonNumerics: Gen[String] =
    alphaStr.suchThat(_.nonEmpty)

  def decimals: Gen[String] =
    arbitrary[BigDecimal]
      .suchThat(_.abs < Int.MaxValue)
      .suchThat(!_.isValidInt)
      .map(_.bigDecimal.toPlainString)

  def intsBelowValue(value: Int): Gen[Int] =
    Gen.choose(0, value)

  def intsAboveValue(value: Int): Gen[Int] =
    arbitrary[Int].suchThat(_ > value)

  def intsOutsideRange(min: Int, max: Int): Gen[Int] =
    arbitrary[Int].suchThat(x => x < min || x > max)

  def nonBooleans: Gen[String] =
    arbitrary[String]
      .suchThat(_.nonEmpty)
      .suchThat(_ != "true")
      .suchThat(_ != "false")

  def nonEmptyString: Gen[String] =
    for {
      c <- alphaNumChar
      s <- alphaNumStr
    } yield s"$c$s"

  def nonEmptyAlphaString: Gen[String] =
    for {
      c <- alphaChar
      s <- alphaStr
    } yield s"$c$s"

  @nowarn("msg=exhaustive")
  def nonEmptyListOf[A](gen: Gen[A]): Gen[NonEmptyList[A]] =
    Gen.nonEmptyListOf(gen).map { case head :: tail =>
      NonEmptyList(head, tail)
    }

  def stringLengthBetween(minLength: Int, maxLength: Int, charGen: Gen[Char]): Gen[String] =
    for {
      length <- choose(minLength, maxLength)
      chars <- listOfN(length, charGen)
    } yield chars.mkString

  def numericStringLength(length: Int): Gen[String] =
    stringLengthBetween(length, length, numChar)

  def numericStringLengthBetween(minLength: Int, maxLength: Int): Gen[String] =
    stringLengthBetween(minLength, maxLength, numChar)

  def stringsWithMaxLength(maxLength: Int): Gen[String] =
    stringLengthBetween(1, maxLength, alphaChar)

  def stringsWithMinLength(minLength: Int): Gen[String] =
    stringLengthBetween(minLength, 999999, alphaChar)

  def stringsLongerThan(minLength: Int): Gen[String] =
    for {
      maxLength <- Gen.const((minLength * 2).max(100))
      length <- Gen.chooseNum(minLength + 1, maxLength)
      chars <- listOfN(length, arbitrary[Char])
    } yield chars.mkString

  def stringsExceptSpecificValues(excluded: Seq[String]): Gen[String] =
    nonEmptyString.suchThat(!excluded.contains(_))

  def stringContains(value: String): Gen[String] =
    for {
      s <- nonEmptyString
      i <- chooseNum(0, s.length)
      (l, r) = s.splitAt(i)
    } yield s"$l$value$r"

  def oneOf[T](xs: Seq[Gen[T]]): Gen[T] =
    Gen.oneOf(xs).flatMap(identity)

  def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long =
      date.atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map { millis =>
      Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate
    }
  }

  lazy val earliestDate: LocalDate = LocalDate.of(1970, 1, 1)
  lazy val latestDate: LocalDate = LocalDate.of(3000, 12, 31)

  def date: Gen[LocalDate] =
    datesBetween(earliestDate, latestDate)

  def tooEarlyDateGen: Gen[LocalDate] =
    datesBetween(
      LocalDate.of(1, 1, 1),
      LocalDate.of(1899, 12, 31)
    )

  lazy val nonEmptyMessage: Gen[Message] = nonEmptyString.map(Message(_))
  lazy val nonEmptyLinkMessage: Gen[LinkMessage] =
    for {
      message <- nonEmptyMessage
      url <- relativeUrl
    } yield LinkMessage(message, url)

  lazy val nonEmptyInlineMessage: Gen[InlineMessage] = Gen.oneOf(nonEmptyMessage, nonEmptyLinkMessage)

  lazy val nonEmptyParagraphMessage: Gen[ParagraphMessage] =
    nonEmptyListOf(nonEmptyInlineMessage).map(ParagraphMessage(_))
  lazy val nonEmptyHeading2Message: Gen[Heading2] = nonEmptyInlineMessage.map(Heading2(_))
  lazy val nonEmptyListMessage: Gen[ListMessage] =
    nonEmptyListOf(nonEmptyInlineMessage).map(ListMessage(_, Bullet))

  lazy val nonEmptyBlockMessage: Gen[BlockMessage] =
    Gen.oneOf(nonEmptyParagraphMessage, nonEmptyListMessage, nonEmptyHeading2Message)
  lazy val nonEmptyDisplayMessage: Gen[DisplayMessage] = Gen.oneOf(nonEmptyInlineMessage, nonEmptyBlockMessage)

  def tupleOf[A, B](genA: Gen[A], genB: Gen[B]): Gen[(A, B)] =
    for {
      a <- genA
      b <- genB
    } yield a -> b

  lazy val boolean: Gen[Boolean] = Gen.oneOf(true, false)

  lazy val topLevelDomain: Gen[String] = Gen.oneOf("com", "gov.uk", "co.uk", "net", "org", "io")

  lazy val emailGen: Gen[String] =
    for {
      username <- nonEmptyString
      domain <- nonEmptyString
      topDomain <- topLevelDomain
    } yield s"$username@$domain.$topDomain"

  lazy val ipAddress: Gen[String] =
    for {
      a <- choose(1, 255)
      b <- choose(0, 255)
      c <- choose(0, 255)
      d <- choose(0, 255)
    } yield s"$a.$b.$c.$d"

  lazy val relativeUrl: Gen[String] = nonEmptyListOf(nonEmptyString).map(_.toList.mkString("/", "/", "/"))

  lazy val httpMethod: Gen[String] = Gen.oneOf("GET", "POST")

  lazy val call: Gen[Call] =
    for {
      method <- httpMethod
      url <- relativeUrl
    } yield Call(method, url)

  lazy val postCall: Gen[Call] = relativeUrl.map(url => Call("POST", url))

  lazy val paginationGen: Gen[PaginatedViewModel] =
    for {
      label <- nonEmptyMessage
      totalSize <- Gen.chooseNum(0, 100)
      pageSize <- Gen.chooseNum(1, 10)
      maxPages = Math.ceil(Math.max(1, totalSize).toDouble / pageSize).toInt
      currentPage <- Gen.chooseNum(1, maxPages)
      call <- call
    } yield PaginatedViewModel(label, Pagination(currentPage, pageSize, totalSize, _ => call))

  lazy val jsStringGen: Gen[JsString] = Gen.alphaStr.map(JsString.apply)

  lazy val jsBooleanGen: Gen[JsBoolean] = Gen.oneOf(true, false).map(JsBoolean)

  lazy val jsNumberGen: Gen[JsNumber] =
    Gen.oneOf(
      Gen.chooseNum(Int.MinValue, Int.MaxValue).map(JsNumber(_)),
      Gen.chooseNum(Double.MinValue, Double.MaxValue).map(JsNumber(_)),
      Gen.chooseNum(Long.MinValue, Long.MaxValue).map(JsNumber(_))
    )

  lazy val jsNullGen: Gen[JsNull.type] = Gen.const(JsNull)

  def jsObjectGen(maxDepth: Int): Gen[JsObject] =
    for {
      size <- Gen.chooseNum(1, 10)
      obj <- Gen.listOfN(size, tupleOf(nonEmptyString, jsValueGen(maxDepth - 1)))
    } yield obj.foldLeft(Json.obj())(_ + _)

  def jsArrayGen(maxDepth: Int): Gen[JsArray] =
    for {
      size <- Gen.chooseNum(1, 10)
      array <- Gen.listOfN(size, jsValueGen(maxDepth - 1)).map(JsArray(_))
    } yield array

  def jsValueGen(maxDepth: Int): Gen[JsValue] =
    if (maxDepth <= 0) {
      Gen.oneOf(
        jsStringGen,
        jsBooleanGen,
        jsNumberGen,
        jsNullGen
      )
    } else {
      Gen.oneOf(
        jsStringGen,
        jsBooleanGen,
        jsNumberGen,
        jsNullGen,
        jsObjectGen(maxDepth),
        jsArrayGen(maxDepth)
      )
    }

  def mapOf[A, B](genA: Gen[A], genB: Gen[B], num: Int): Gen[Map[A, B]] =
    for {
      a <- Gen.listOfN(num, genA)
      b <- Gen.listOfN(num, genB)
    } yield a.zip(b).toMap

  def validationErrorGen: Gen[ValidationError] = for {
    row <- Gen.chooseNum(1, 10)
    errorType <- Gen.oneOf(ValidationErrorType.values)
    message <- arbitrary[String]
  } yield ValidationError(row, errorType, message)
}
