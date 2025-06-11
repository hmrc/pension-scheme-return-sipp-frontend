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

package forms

import forms.mappings.Constraints
import generators.Generators
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.validation.{Invalid, Valid}

import java.time.LocalDate

class ConstraintsSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators with Constraints {

  "firstError" - {

    "must return Valid when all constraints pass" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("foo")
      result mustEqual Valid
    }

    "must return Invalid when the first constraint fails" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("a" * 11)
      result mustEqual Invalid("error.length", 10)
    }

    "must return Invalid when the second constraint fails" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("")
      result mustEqual Invalid("error.regexp", """^\w+$""")
    }

    "must return Invalid for the first error when both constraints fail" in {
      val result = firstError(maxLength(-1, "error.length"), regexp("""^\w+$""", "error.regexp"))("")
      result mustEqual Invalid("error.length", -1)
    }
  }

  "minimumValue" - {

    "must return Valid for a number greater than the threshold" in {
      val result = minimumValue(1, "error.min").apply(2)
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = minimumValue(1, "error.min").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number below the threshold" in {
      val result = minimumValue(1, "error.min").apply(0)
      result mustEqual Invalid("error.min", 1)
    }
  }

  "maximumValue" - {

    "must return Valid for a number less than the threshold" in {
      val result = maximumValue(1, "error.max").apply(0)
      result mustEqual Valid
    }

    "must return Valid for a number equal to the threshold" in {
      val result = maximumValue(1, "error.max").apply(1)
      result mustEqual Valid
    }

    "must return Invalid for a number above the threshold" in {
      val result = maximumValue(1, "error.max").apply(2)
      result mustEqual Invalid("error.max", 1)
    }
  }

  "regexp" - {

    "must return Valid for an input that matches the expression" in {
      val result = regexp("""^\w+$""", "error.invalid")("foo")
      result mustEqual Valid
    }

    "must return Invalid for an input that does not match the expression" in {
      val result = regexp("""^\d+$""", "error.invalid")("foo")
      result mustEqual Invalid("error.invalid", """^\d+$""")
    }
  }

  "maxLength" - {

    "must return Valid for a string shorter than the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 9)
      result mustEqual Valid
    }

    "must return Valid for an empty string" in {
      val result = maxLength(10, "error.length")("")
      result mustEqual Valid
    }

    "must return Valid for a string equal to the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 10)
      result mustEqual Valid
    }

    "must return Invalid for a string longer than the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 11)
      result mustEqual Invalid("error.length", 10)
    }
  }

  "maxDate" - {

    "must return Valid for a date before or equal to the maximum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        max <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(LocalDate.of(2000, 1, 1), max)
      } yield (max, date)

      forAll(gen) { case (max, date) =>
        val result = maxDate(max, "error.future")(date)
        result mustEqual Valid
      }
    }

    "must return Invalid for a date after the maximum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        max <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(max.plusDays(1), LocalDate.of(3000, 1, 2))
      } yield (max, date)

      forAll(gen) { case (max, date) =>
        val result = maxDate(max, "error.future", "foo")(date)
        result mustEqual Invalid("error.future", "foo")
      }
    }
  }

  "minDate" - {

    "must return Valid for a date after or equal to the minimum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        min <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(min, LocalDate.of(3000, 1, 1))
      } yield (min, date)

      forAll(gen) { case (min, date) =>
        val result = minDate(min, "error.past", "foo")(date)
        result mustEqual Valid
      }
    }

    "must return Invalid for a date before the minimum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        min <- datesBetween(LocalDate.of(2000, 1, 2), LocalDate.of(3000, 1, 1))
        date <- datesBetween(LocalDate.of(2000, 1, 1), min.minusDays(1))
      } yield (min, date)

      forAll(gen) { case (min, date) =>
        val result = minDate(min, "error.past", "foo")(date)
        result mustEqual Invalid("error.past", "foo")
      }
    }
  }


  "success" - {
    "must always return Valid" in {
      val result = success[String].apply("any input")
      result mustEqual Valid
    }
  }

  "whenNotEmpty" - {
    val nonEmptyConstraint = whenNotEmpty(maxLength(5, "error.length"))

    "must return Valid when input is empty" in {
      val result = nonEmptyConstraint("")
      result mustEqual Valid
    }

    "must return Valid when input is non-empty and passes the constraint" in {
      val result = nonEmptyConstraint("12345")
      result mustEqual Valid
    }

    "must return Invalid when input is non-empty and fails the constraint" in {
      val result = nonEmptyConstraint("123456")
      result mustEqual Invalid("error.length", 5)
    }
  }

  "when" - {
    val onlyApplyToEven = when[Int](_ % 2 == 0, failWhen[Int](_ > 5, "error.even.gt5"))

    "must return Valid when predicate is false" in {
      val result = onlyApplyToEven(3) // Odd number, predicate false
      result mustEqual Valid
    }

    "must return Valid when predicate is true and constraint passes" in {
      val result = onlyApplyToEven(4)
      result mustEqual Valid
    }

    "must return Invalid when predicate is true and constraint fails" in {
      val result = onlyApplyToEven(8)
      result mustEqual Invalid("error.even.gt5")
    }
  }

  "failWhen" - {
    val failIfZero = failWhen[Int](_ == 0, "error.zero")

    "must return Invalid when predicate is true" in {
      val result = failIfZero(0)
      result mustEqual Invalid("error.zero")
    }

    "must return Valid when predicate is false" in {
      val result = failIfZero(1)
      result mustEqual Valid
    }
  }

  "inRange" - {
    "must return Valid when value is within range" in {
      val result = inRange(5, 10, "error.range").apply(7)
      result mustEqual Valid
    }

    "must return Valid when value is equal to min or max" in {
      inRange(5, 10, "error.range").apply(5) mustEqual Valid
      inRange(5, 10, "error.range").apply(10) mustEqual Valid
    }

    "must return Invalid when value is out of range" in {
      inRange(5, 10, "error.range").apply(4) mustEqual Invalid("error.range", "5", "10")
      inRange(5, 10, "error.range").apply(11) mustEqual Invalid("error.range", "5", "10")
    }
  }

  "length" - {
    val exactLengthConstraint = length(5, "error.length")

    "must return Valid for a string with exact specified length" in {
      val result = exactLengthConstraint("12345")
      result mustEqual Valid
    }

    "must return Invalid for a string shorter than the specified length" in {
      val result = exactLengthConstraint("1234")
      result mustEqual Invalid("error.length", 5)
    }

    "must return Invalid for a string longer than the specified length" in {
      val result = exactLengthConstraint("123456")
      result mustEqual Invalid("error.length", 5)
    }

    "must return Valid for an empty string if length is 0" in {
      val result = length(0, "error.length")("")
      result mustEqual Valid
    }

    "must return Invalid for a non-empty string if length is 0" in {
      val result = length(0, "error.length")("1")
      result mustEqual Invalid("error.length", 0)
    }
  }

  "isEqual" - {
    "must return Valid when expectedValue is empty" in {
      val result = isEqual(None, "error.equal").apply("any")
      result mustEqual Valid
    }

    "must return Valid when value matches expectedValue" in {
      val result = isEqual(Some("expected"), "error.equal").apply("expected")
      result mustEqual Valid
    }

    "must return Invalid when value does not match expectedValue" in {
      val result = isEqual(Some("expected"), "error.equal").apply("actual")
      result mustEqual Invalid("error.equal")
    }
  }

  "nonEmptySet" - {
    "must return Valid when the set is non-empty" in {
      val result = nonEmptySet("error.emptyset").apply(Set("item"))
      result mustEqual Valid
    }

    "must return Invalid when the set is empty" in {
      val result = nonEmptySet("error.emptyset").apply(Set.empty)
      result mustEqual Invalid("error.emptyset")
    }
  }

}
