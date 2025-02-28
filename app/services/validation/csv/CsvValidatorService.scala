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

package services.validation.csv

import config.Crypto
import models.*
import models.csv.{CsvDocumentState, CsvRowState}
import org.apache.pekko
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.util.ByteString
import org.reactivestreams.Publisher
import play.api.Logging
import play.api.i18n.Messages
import play.api.libs.json.Format
import repositories.{CsvRowStateSerialization, UploadRepository}
import services.validation.Validator
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.ObjectSummaryWithMd5

import java.nio.ByteBuffer
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CsvValidatorService @Inject() (
  uploadRepository: UploadRepository,
  csvDocumentValidator: CsvDocumentValidator,
  crypto: Crypto,
  csvRowStateSerialization: CsvRowStateSerialization
) extends Validator
    with Logging {

  private implicit val cryptoEncDec: Encrypter & Decrypter = crypto.getCrypto

  def validateUpload[T](
    stream: Source[ByteString, ?],
    csvRowValidator: CsvRowValidator[T],
    csvRowValidationParameters: CsvRowValidationParameters,
    uploadKey: UploadKey
  )(implicit
    messages: Messages,
    format: Format[T],
    headerCarrier: HeaderCarrier,
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[CsvDocumentState] = {
    val publisher = csvDocumentValidator
      .validate(stream, csvRowValidator, csvRowValidationParameters)
      .runWith(Sink.asPublisher(true))

    for {
      state <- Source.fromPublisher(publisher).map(_._2).runWith(Sink.last)
      _ <- publish(uploadKey, Source.fromPublisher(publisher))
    } yield state
  }

  private def publish[T](
    uploadKey: UploadKey,
    source: Source[(CsvRowState[T], CsvDocumentState), ?]
  )(implicit
    format: Format[T],
    headerCarrier: HeaderCarrier,
    materializer: Materializer
  ): Future[ObjectSummaryWithMd5]  = {
    val serialized: Source[ByteBuffer, ?] = source
      .map(_._1)
      .map(csvRowStateSerialization.write[T])

    val publisher: Publisher[ByteBuffer] = serialized.runWith(Sink.asPublisher(fanout = false))
    uploadRepository.save(uploadKey, publisher)
  }
}
