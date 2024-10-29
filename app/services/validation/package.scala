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

package services

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.data.Validated.Invalid
import cats.syntax.validated.*
import models.ValidationError
import models.ValidationErrorType

import util.chaining.*

package object validation {
  def mergeErrors(
    validations: ValidatedNel[ValidationError, Any]*
  ): Option[Validated[NonEmptyList[ValidationError], Nothing]] =
    validations.toList
      .collect { case Invalid(errors) => errors.toList }
      .flatten
      .pipe(l => NonEmptyList.fromList(l).map(_.invalid))

  def checkRequired(row: Int, keyBase: String) = CheckRequiredPartiallyApplied(row, keyBase)

  private[validation] class CheckRequiredPartiallyApplied(row: Int, keyBase: String) {
    def apply[A](
      validation: Option[ValidatedNel[ValidationError, A]],
      field: String,
      errorType: ValidationErrorType
    ): ValidatedNel[ValidationError, A] = validation.getOrElse(
      ValidationError(row, errorType, s"$keyBase.$field.upload.error.required").invalidNel
    )
  }
}
