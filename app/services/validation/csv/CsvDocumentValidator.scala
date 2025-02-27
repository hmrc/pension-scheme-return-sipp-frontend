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

import cats.data.NonEmptyList
import models.CsvHeaderKey
import models.csv.{CsvDocumentEmpty, CsvDocumentState, CsvRowState}
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.connectors.csv.scaladsl.CsvParsing
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import org.apache.pekko.util.ByteString
import play.api.i18n.Messages
import services.validation.Validator.indexToCsvKey
import services.validation.csv.CsvDocumentValidator.*

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CsvDocumentValidator @Inject() () {
  def validate[T](
    stream: Source[ByteString, ?],
    csvRowValidator: CsvRowValidator[T],
    csvRowValidationParameters: CsvRowValidationParameters
  )(implicit
    messages: Messages,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Source[(CsvRowState[T], CsvDocumentState), ?] = {
    val rowNumber: AtomicInteger = AtomicInteger(2)

    val firstRowSink: Sink[List[ByteString], Future[List[String]]] =
      Sink.head[List[ByteString]].mapMaterializedValue(_.map(_.map(_.utf8String)))

    val csvFrame: Flow[ByteString, List[ByteString], NotUsed] =
      CsvParsing.lineScanner()

    val csvFrames = stream.via(csvFrame)

    Source
      .future(csvFrames.runWith(firstRowSink))
      .flatMapConcat { csvHeader =>
        val headers = csvHeader.zipWithIndex
          .map { case (key, index) => CsvHeaderKey(key, indexToCsvKey(index), index) }

        csvFrames
          .drop(2) // drop csv headers
          .map(_.map(_.utf8String))
          .map(values => Row(values, headers, rowNumber.incrementAndGet()))
          .mapAsync(FieldValidationParallelism)(validate[T](_, csvRowValidator, csvRowValidationParameters))
          .statefulMap(emptyDocumentState)((document, row) => mapState(document, row), _ => None)
      }
  }
  
  private def emptyDocumentState(): CsvDocumentState = CsvDocumentEmpty
  
  private def mapState[A](csvDocumentState: CsvDocumentState, csvRowState: CsvRowState[A]): (CsvDocumentState, (CsvRowState[A], CsvDocumentState)) = {
    val state = CsvDocumentState.combine(csvDocumentState, csvRowState)
    state -> (csvRowState, state)
  }

  private def validate[T](
    csvRow: Row,
    csvRowValidator: CsvRowValidator[T],
    csvRowValidationParameters: CsvRowValidationParameters
  )(implicit
    messages: Messages,
    executionContext: ExecutionContext
  ): Future[CsvRowState[T]] = Future {
    csvRowValidator.validate(csvRow.line, csvRow.values, csvRow.headers, csvRowValidationParameters)(messages)
  }
}

object CsvDocumentValidator {
  private val FieldValidationParallelism = 8

  private case class Row(
    values: NonEmptyList[String],
    headers: List[CsvHeaderKey],
    line: Int
  )

  private object Row {
    def apply(values: List[String], headers: List[CsvHeaderKey], line: Int) = new Row(
      NonEmptyList.fromList(values).getOrElse(NonEmptyList.one("")),
      headers,
      line
    )
  }
}
