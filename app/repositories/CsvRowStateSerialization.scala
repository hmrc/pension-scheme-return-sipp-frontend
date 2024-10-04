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

package repositories

import org.apache.pekko.util.ByteString
import models.csv.CsvRowState
import play.api.libs.json.{Format, Json}
import repositories.UploadRepository.MongoUpload._
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object CsvRowStateSerialization {
  val IntLength = 4

  def write[T](
    csvRowState: CsvRowState[T]
  )(implicit
    crypto: Encrypter with Decrypter,
    format: Format[T]
  ): ByteBuffer = {
    val json =
      Json
        .toJson(SensitiveCsvRow(csvRowState))(
          UploadRepository.MongoUpload.sensitiveCsvRowFormat(
            crypto,
            format
          )
        )

    val bytes = json.toString().getBytes(StandardCharsets.UTF_8)
    val length = bytes.length

    ByteBuffer
      .allocate(IntLength + length)
      .putInt(length)
      .put(bytes)
      .position(0)
  }

  def read[T](byteBuffer: ByteBuffer)(implicit
    crypto: Encrypter with Decrypter,
    format: Format[T]
  ): CsvRowState[T] =
    Json
      .parse(ByteString(byteBuffer.array().drop(IntLength)).utf8String)
      .as[SensitiveCsvRow[T]]
      .decryptedValue
}
