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

import config.{FakeCrypto, FrontendAppConfig}
import models.*
import models.UploadState.UploadValidated
import models.csv.CsvDocumentValid
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import org.mongodb.scala.ObservableFuture
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import repositories.UploadMetadataRepository.MongoUpload
import repositories.UploadMetadataRepository.MongoUpload.SensitiveUploadStatus

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class UploadMetadataRepositorySpec extends BaseRepositorySpec[MongoUpload] with MockitoSugar {

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.uploadTtl).thenReturn(1L)

  private val oldInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS).minusMillis(1000)
  private val initialUploadDetails = UploadDetails(uploadKey, reference, UploadStatus.InProgress, oldInstant)
  private val initialMongoUpload =
    MongoUpload(uploadKey, reference, SensitiveUploadStatus(UploadStatus.InProgress), oldInstant, None)
  private val encryptedRegex = "^[A-Za-z0-9+/=]+$"

  override protected val repository: UploadMetadataRepository =
    UploadMetadataRepository(mongoComponent, stubClock, mockAppConfig, FakeCrypto)

  ".insert" - {
    "successfully insert UploadDetails" in {
      repository.upsert(initialUploadDetails).futureValue
      val findResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      findResult mustBe initialMongoUpload
    }

    "successfully upsert UploadDetails if a previous entry exists" in {
      val anotherUploadDetails =
        UploadDetails(
          uploadKey,
          Reference("new-reference"),
          UploadStatus.Failed(ErrorDetails("test", "test-failure")),
          oldInstant
        )

      repository.upsert(initialUploadDetails).futureValue
      repository.upsert(anotherUploadDetails).futureValue
      val findResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      findResult.reference mustBe anotherUploadDetails.reference
    }
  }

  ".find" - {
    "successfully fetch a previously inserted UploadDetails" in {
      insertInitialUploadDetails()
    }
  }

  ".updateStatus" - {
    "successfully update the ttl and status to failed" in {
      insertInitialUploadDetails()

      repository.updateStatus(reference, failure).futureValue
      val findAfterUpdateResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      findAfterUpdateResult mustBe initialMongoUpload.copy(
        status = SensitiveUploadStatus(failure),
        lastUpdated = instant
      )
    }

    "successfully update the ttl and status to success" in {
      insertInitialUploadDetails()

      val uploadSuccessful = UploadStatus.Success("test-name", "text/csv", "/test-url", None)
      repository.updateStatus(reference, uploadSuccessful).futureValue
      val findAfterUpdateResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      findAfterUpdateResult mustBe initialMongoUpload.copy(
        status = SensitiveUploadStatus(uploadSuccessful),
        lastUpdated = instant
      )
    }

    "successfully encrypt status" in {
      insertInitialUploadDetails()

      val uploadSuccessful = UploadStatus.Success("test-name", "text/csv", "/test-url", None)
      repository.updateStatus(reference, uploadSuccessful).futureValue

      val rawData =
        repository.collection
          .find[BsonDocument](Filters.equal("id", uploadKey.value))
          .toFuture()
          .futureValue
          .headOption

      assert(rawData.nonEmpty)
      rawData.map(_.get("status").asString().getValue must fullyMatch.regex(encryptedRegex))
    }

  }

  ".setUploadResult" - {
    "successfully update the ttl and upload result to UploadValidated" in {
      insertInitialUploadDetails()

      repository.setValidationState(uploadKey, UploadValidated(CsvDocumentValid)).futureValue
      val findAfterUpdateResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      findAfterUpdateResult.lastUpdated mustBe instant
      findAfterUpdateResult.validationState.value.decryptedValue mustBe UploadValidated(CsvDocumentValid)
    }
  }

  ".getUploadResult" - {
    "successfully get the upload result" in {
      insertInitialUploadDetails()

      repository.setValidationState(uploadKey, UploadValidated(CsvDocumentValid)).futureValue
      val getResult = repository.getValidationState(uploadKey).futureValue

      getResult mustBe Some(UploadValidated(CsvDocumentValid))
    }
  }

  ".remove" - {
    "successfully remove a previously inserted record" in {
      insertInitialUploadDetails()

      repository.remove(uploadKey).futureValue
      val findAfterRemoveResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption

      findAfterRemoveResult mustBe None
    }
  }

  "successfully encrypt result" in {
    insertInitialUploadDetails()

    repository.setValidationState(uploadKey, UploadValidated(CsvDocumentValid)).futureValue
    val rawData =
      repository.collection
        .find[BsonDocument](Filters.equal("id", uploadKey.value))
        .toFuture()
        .futureValue
        .headOption

    assert(rawData.nonEmpty)
    rawData.map(_.get("validationState").asString().getValue must fullyMatch.regex(encryptedRegex))
  }

  private def insertInitialUploadDetails(): Unit = {
    repository.upsert(initialUploadDetails).futureValue
    val findResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

    findResult mustBe initialMongoUpload
  }
}
