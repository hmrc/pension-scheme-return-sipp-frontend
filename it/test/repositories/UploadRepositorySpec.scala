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

import cats.implicits.toTraverseOps
import config.FakeCrypto
import models.*
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.util.ByteString
import org.reactivestreams.Publisher
import play.api.libs.json.Json
import repositories.UploadRepository.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.play.test.stub
import uk.gov.hmrc.objectstore.client.{ObjectSummaryWithMd5, RetentionPeriod}

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.UUID.randomUUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UploadRepositorySpec extends ObjectStoreRepositorySpec {
  private val validationResult: UploadFormatError = UploadFormatError(
    ValidationError(
      0,
      ValidationErrorType.Formatting,
      "Invalid file format please format file as per provided template"
    )
  )

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val baseUrl = s"baseUrl-${randomUUID().toString}"
  private val owner = s"owner-${randomUUID().toString}"
  private val token = s"token-${randomUUID().toString}"
  private val config = ObjectStoreClientConfig(baseUrl, owner, token, RetentionPeriod.OneWeek)
  private lazy val objectStoreStub = new stub.StubPlayObjectStoreClient(config)
  private val repository = UploadRepository(crypto = FakeCrypto, objectStoreStub)

  ".save" - {
    "store the stream of data" in {
      val uploadKey: UploadKey = UploadKey("123456", srn, "test-redirect-tag")
      val _: Unit = testStore(validationResult, uploadKey)(repository.save).futureValue
      val findResult = testFind(uploadKey)(repository.retrieve).futureValue

      findResult mustBe Some(validationResult)
    }

    "replace data with the same key" in {
      val uploadKey: UploadKey = UploadKey("123456", srn, "test-redirect-tag")
      val _: Unit = testStore(validationResult, uploadKey)(repository.save).futureValue

      val replacedValidationResult: UploadFormatError =
        validationResult.copy(validationResult.detail.copy(message = "replaced"))
      val _: Unit = testStore(replacedValidationResult, uploadKey)(repository.save).futureValue

      val findResult = testFind(uploadKey)(repository.retrieve).futureValue

      findResult mustBe Some(replacedValidationResult)
    }
  }

  ".retrieve" - {
    "retrieve the stream of data" in {
      val uploadKey: UploadKey = UploadKey("654321", srn, "test-another-tag")
      val _: Unit = testStore(validationResult, uploadKey)(repository.save).futureValue
      val findResult = testFind(uploadKey)(repository.retrieve).futureValue

      findResult mustBe Some(validationResult)
    }

    "return None when no data/file is found for the provided key" in {
      val uploadKey: UploadKey = UploadKey("654321", srn, "test-another-tag")
      val _: Unit = testStore(validationResult, uploadKey)(repository.save).futureValue

      val nonExistantKey: UploadKey = UploadKey("654321", srn, "test-another-tag-non-existant")
      val findResult = testFind(nonExistantKey)(repository.retrieve).futureValue

      findResult mustBe None
    }
  }

  private def testFind(
    key: UploadKey
  )(find: (UploadKey) => Future[Option[Source[ByteString, NotUsed]]]): Future[Option[Upload]] =
    find(key)
      .flatMap(
        _.flatTraverse(source =>
          source.runFold(ByteString.empty)(_ ++ _).map(bString => Json.parse(bString.utf8String).asOpt[Upload])
        )
      )

  private def testStore(upload: Upload, key: UploadKey)(
    publish: (UploadKey, Publisher[ByteBuffer]) => Future[ObjectSummaryWithMd5]
  ): Future[ObjectSummaryWithMd5] = {
    val uploadAsBytes = Json.toJson(upload).toString.getBytes(StandardCharsets.UTF_8)
    val publisher = Source.single(ByteBuffer.wrap(uploadAsBytes)).runWith(Sink.asPublisher(fanout = false))
    publish(key, publisher)
  }

}
