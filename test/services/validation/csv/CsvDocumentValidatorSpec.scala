/*
 * Copyright 2025 HM Revenue & Customs
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
import models.UserAnswers
import models.csv.CsvRowState.CsvRowValid
import models.csv.{CsvDocumentState, CsvDocumentValid, CsvRowState}
import models.requests.*
import org.apache.pekko.stream.OverflowStrategy
import org.apache.pekko.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import org.apache.pekko.util.ByteString
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import services.validation.*
import services.validation.csv.CsvValidatorService.ProcessingParallelism
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import java.nio.ByteOrder
import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class CsvDocumentValidatorSpec extends BaseSpec {

  private val validationParams = CsvRowValidationParameters(Some(LocalDate.now().plusDays(1)))
  private val byteString = ByteString(
    """
      |header
      |helper
      |row
      |""".stripMargin)
  private val sourceByteString = Source.single[ByteString](byteString)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())
  implicit val dataRequest: DataRequest[?] =
    DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, UserAnswers("id"))

  "CsvValidatorService" - {

    "validate interest in land or property" in new Fixture {
      private val queue = getQueue[LandOrConnectedPropertyApi.TransactionDetail]
      private val rowValidator = mock[InterestInLandOrPropertyCsvRowValidator]
      when(rowValidator.validate(any, any, any, any)(any)).thenReturn(CsvRowValid(1, Json.obj(), NonEmptyList.one("test")))

      private val result = validator
        .validate(sourceByteString, rowValidator, validationParams)
        .mapAsync(ProcessingParallelism)(elem => queue.offer(elem).map(_ => elem))
        .map(_._2)
      result.runWith(Sink.seq).map(_.toList).futureValue mustBe List(CsvDocumentValid, CsvDocumentValid)
    }
    "validate arms length" in new Fixture {
      private val queue = getQueue[LandOrConnectedPropertyApi.TransactionDetail]
      private val rowValidator = mock[ArmsLengthLandOrPropertyCsvRowValidator]
      when(rowValidator.validate(any, any, any, any)(any)).thenReturn(CsvRowValid(1, Json.obj(), NonEmptyList.one("test")))
      private val result = validator
        .validate(sourceByteString, rowValidator, validationParams)
        .mapAsync(ProcessingParallelism)(elem => queue.offer(elem).map(_ => elem))
        .map(_._2)
      result.runWith(Sink.seq).map(_.toList).futureValue mustBe List(CsvDocumentValid, CsvDocumentValid)
    }

    "validate tangible moveable" in new Fixture {
      private val queue = getQueue[TangibleMoveablePropertyApi.TransactionDetail]
      private val rowValidator = mock[TangibleMoveableCsvRowValidator]
      when(rowValidator.validate(any, any, any, any)(any)).thenReturn(CsvRowValid(1, Json.obj(), NonEmptyList.one("test")))
      private val result = validator
        .validate(sourceByteString, rowValidator, validationParams)
        .mapAsync(ProcessingParallelism)(elem => queue.offer(elem).map(_ => elem))
        .map(_._2)
      result.runWith(Sink.seq).map(_.toList).futureValue mustBe List(CsvDocumentValid, CsvDocumentValid)
    }

    "validate outstanding loans" in new Fixture {
      private val queue = getQueue[OutstandingLoanApi.TransactionDetail]
      private val rowValidator = mock[OutstandingLoansCsvRowValidator]
      when(rowValidator.validate(any, any, any, any)(any)).thenReturn(CsvRowValid(1, Json.obj(), NonEmptyList.one("test")))
      private val result = validator
        .validate(sourceByteString, rowValidator, validationParams)
        .mapAsync(ProcessingParallelism)(elem => queue.offer(elem).map(_ => elem))
        .map(_._2)
      result.runWith(Sink.seq).map(_.toList).futureValue mustBe List(CsvDocumentValid, CsvDocumentValid)
    }

    "validate unquoted shares" in new Fixture {
      private val queue = getQueue[UnquotedShareApi.TransactionDetail]
      private val rowValidator = mock[UnquotedSharesCsvRowValidator]
      when(rowValidator.validate(any, any, any, any)(any)).thenReturn(CsvRowValid(1, Json.obj(), NonEmptyList.one("test")))
      private val result = validator
        .validate(sourceByteString, rowValidator, validationParams)
        .mapAsync(ProcessingParallelism)(elem => queue.offer(elem).map(_ => elem))
        .map(_._2)
      result.runWith(Sink.seq).map(_.toList).futureValue mustBe List(CsvDocumentValid, CsvDocumentValid)
    }

    "validate asset from connected" in new Fixture {
      private val queue = getQueue[AssetsFromConnectedPartyApi.TransactionDetail]
      private val rowValidator = mock[AssetFromConnectedPartyCsvRowValidator]
      when(rowValidator.validate(any, any, any, any)(any)).thenReturn(CsvRowValid(1, Json.obj(), NonEmptyList.one("test")))
      private val result = validator
        .validate(sourceByteString, rowValidator, validationParams)
        .mapAsync(ProcessingParallelism)(elem => queue.offer(elem).map(_ => elem))
        .map(_._2)
      result.runWith(Sink.seq).map(_.toList).futureValue mustBe List(CsvDocumentValid, CsvDocumentValid)
    }
  }

  private def getQueue[T]: SourceQueueWithComplete[(CsvRowState[T], CsvDocumentState)] = {
    Source.queue[(CsvRowState[T], CsvDocumentState)](
        128,
        OverflowStrategy.backpressure,
        ProcessingParallelism
      )
      .preMaterialize()
      ._1
  }

  def framedByteString(str: String): ByteString = {
    val bs = ByteString(str)
    ByteString.newBuilder.putInt(bs.length)(ByteOrder.BIG_ENDIAN).result ++ bs
  }

  trait Fixture {
    val validator = CsvDocumentValidator()
  }
}
