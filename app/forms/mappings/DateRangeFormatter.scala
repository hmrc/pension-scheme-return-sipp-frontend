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

package forms.mappings

import cats.data.Validated.*
import cats.syntax.all.*
import forms.mappings.errors.DateFormErrors
import models.DateRange
import play.api.data.FormError
import play.api.data.format.Formatter
import utils.DateTimeUtils.localDateShow

import java.time.LocalDate

private[mappings] class DateRangeFormatter(
  startDateErrors: DateFormErrors,
  endDateErrors: DateFormErrors,
  invalidRangeError: String,
  allowedRange: Option[DateRange],
  startDateAllowedDateRangeError: Option[String],
  endDateAllowedDateRangeError: Option[String],
  startDateDuplicateRangeError: Option[String],
  endDateDuplicateRangeError: Option[String],
  gapNotAllowedError: Option[String],
  duplicateRanges: List[DateRange]
) extends Formatter[DateRange]
    with Formatters {

  val startDateFormatter = LocalDateFormatter(startDateErrors)
  val endDateFormatter = LocalDateFormatter(endDateErrors)

  private def verifyValidRange(key: String, range: DateRange): Either[Seq[FormError], DateRange] =
    if (range.from.isBefore(range.to)) Right(range)
    else Left(List(FormError(s"$key.endDate", invalidRangeError, List(range.from.show, range.to.show))))

  private def verifyRangeBounds(
    key: String,
    date: LocalDate,
    error: Option[String]
  ): Either[Seq[FormError], LocalDate] =
    allowedRange
      .zip(error)
      .map { case (range, error) =>
        if (range.contains(date)) Right(date)
        else Left(List(FormError(key, error, List(range.from.show, range.to.show))))
      }
      .getOrElse(Right(date))

  private def verifyUniqueRange(key: String, range: DateRange): Either[Seq[FormError], DateRange] =
    (startDateDuplicateRangeError zip endDateDuplicateRangeError)
      .flatMap { case (startDateError, endDateError) =>
        duplicateRanges
          .find(d => d.intersects(range))
          .map { i =>
            if (i.contains(range.from))
              Seq(FormError(s"$key.startDate", startDateError, List(i.from.show, i.to.show)))
            else
              Seq(FormError(s"$key.endDate", endDateError, List(i.from.show, i.to.show)))
          }
      }
      .toLeft(range)

  private def verifyNoGap(key: String, dateRange: DateRange) =
    gapNotAllowedError
      .map { gapNotAllowedError =>
        (dateRange :: duplicateRanges)
          .sortBy(_.from)
          .sliding(2)
          .collectFirst {
            case List(d1 @ DateRange(_, end1), d2 @ DateRange(start2, _)) if end1.plusDays(1) != start2 =>
              val field = if(d1 == dateRange) "endDate" else "startDate"
              Seq(FormError(s"$key.$field", gapNotAllowedError))
          }
          .toLeft(())
      }
      .getOrElse(().asRight)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], DateRange] =
    for {
      dateRange <- (
        startDateFormatter.bind(s"$key.startDate", data).toValidated,
        endDateFormatter.bind(s"$key.endDate", data).toValidated
      ).mapN(DateRange(_, _)).toEither
      _ <- verifyValidRange(key, dateRange)
      _ <- (
        verifyRangeBounds(s"$key.startDate", dateRange.from, startDateAllowedDateRangeError).toValidated,
        verifyRangeBounds(s"$key.endDate", dateRange.to, endDateAllowedDateRangeError).toValidated
      ).mapN(DateRange(_, _)).toEither
      _ <- verifyUniqueRange(key, dateRange)
      _ <- verifyNoGap(key, dateRange)
    } yield dateRange

  override def unbind(key: String, value: DateRange): Map[String, String] =
    startDateFormatter.unbind(s"$key.startDate", value.from) ++
      endDateFormatter.unbind(s"$key.endDate", value.to)
}
