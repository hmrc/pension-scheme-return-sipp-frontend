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

package utils

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import models.ValidationError
import models.ValidationErrorType.ValidationErrorType
import org.scalatest.Assertion
import org.scalatest.Assertions.fail
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

object ValidationSpecUtils {
  def genErr(errType: ValidationErrorType, errKey: String, row: Int = 1): ValidationError =
    ValidationError(row, errType, errKey)

  def checkError[T](
    validation: Option[ValidatedNel[ValidationError, T]],
    expectedErrors: List[ValidationError]
  ): Assertion = {
    validation.get.isInvalid mustBe true
    validation.get match {
      case Validated.Invalid(errors) =>
        val errorList: NonEmptyList[ValidationError] = errors
        errorList.toList mustBe expectedErrors
      case _ =>
        fail("Expected to get invalid")
    }
  }

  def checkSuccess[T](validation: Option[ValidatedNel[ValidationError, T]], expectedObject: T): Assertion = {
    validation.get.isValid mustBe true
    validation.get match {
      case Validated.Valid(success) =>
        success mustBe expectedObject
      case _ =>
        fail("Expected to get valid object")
    }
  }
}
