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

package services.validation.csv

import cats.effect.IO
import fs2.data.csv.{lowlevel, CsvRow}
import models.CsvHeaderKey
import models.csv.{CsvDocumentEmpty, CsvDocumentState, CsvRowState}
import play.api.i18n.Messages
import services.validation.Validator.indexToCsvKey
import services.validation.csv.CsvDocumentValidator.*

import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

class CsvDocumentValidator @Inject() () {
  def validate[T](
    stream: fs2.Stream[IO, String],
    csvRowValidator: CsvRowValidator[T],
    csvRowValidationParameters: CsvRowValidationParameters
  )(implicit messages: Messages): fs2.Stream[IO, (CsvRowState[T], CsvDocumentState)] = {
    val rowNumber: AtomicLong = AtomicLong(2)

    stream
      .through(lowlevel.rows[IO, String]())
      .through(lowlevel.headers[IO, String])
      .tail
      .map(withRowNumber(_, rowNumber))
      .mapAsync(FieldValidationParallelism)(validate[T](_, csvRowValidator, csvRowValidationParameters))
      .zipWithScan1[CsvDocumentState](CsvDocumentEmpty)(CsvDocumentState.combine)
  }

  private def withRowNumber(csvRow: CsvRow[String], rowNumber: AtomicLong): CsvRow[String] =
    csvRow.withLine(Some(rowNumber.incrementAndGet()))

  private def validate[T](
    csvRow: CsvRow[String],
    csvRowValidator: CsvRowValidator[T],
    csvRowValidationParameters: CsvRowValidationParameters
  )(implicit
    messages: Messages
  ): IO[CsvRowState[T]] = IO {
    val headerKeys: List[CsvHeaderKey] = csvRow.headers
      .map(_.toList)
      .toList
      .flatten
      .zipWithIndex
      .map { case (key, index) => CsvHeaderKey(key.trim, indexToCsvKey(index), index) }
    val line = csvRow.line.get.intValue

    csvRowValidator.validate(line, csvRow.values, headerKeys, csvRowValidationParameters)(messages)
  }
}

object CsvDocumentValidator {
  private val FieldValidationParallelism = 8
}
