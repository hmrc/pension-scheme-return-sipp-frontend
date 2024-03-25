package repositories

import config.{FakeCrypto, FrontendAppConfig}
import models._
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters
import repositories.UploadMetadataRepository.MongoUpload
import repositories.UploadMetadataRepository.MongoUpload.SensitiveUploadStatus
import models.csv.CsvDocumentValid

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class UploadMetadataRepositorySpec extends BaseRepositorySpec[MongoUpload] {

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.uploadTtl) thenReturn 1

  private val oldInstant = Instant.now.truncatedTo(ChronoUnit.MILLIS).minusMillis(1000)
  private val initialUploadDetails = UploadDetails(uploadKey, reference, UploadStatus.InProgress, oldInstant)
  private val initialMongoUpload = MongoUpload(uploadKey, reference, SensitiveUploadStatus(UploadStatus.InProgress), oldInstant, None)
  private val encryptedRegex = "^[A-Za-z0-9+/=]+$"

  protected override val repository = new UploadMetadataRepository(
    mongoComponent,
    stubClock,
    mockAppConfig,
    crypto = FakeCrypto
  )

  private val fileFormatError = UploadFormatError(ValidationError(1, ValidationErrorType.Formatting, "Invalid file format, please format file as per provided template"))

  ".insert" - {
    "successfully insert UploadDetails" in {
      val insertResult: Unit = repository.insert(initialUploadDetails).futureValue
      val findResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      insertResult mustBe ()
      findResult mustBe initialMongoUpload
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

      val updateResult: Unit = repository.updateStatus(reference, failure).futureValue
      val findAfterUpdateResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      updateResult mustBe ()
      findAfterUpdateResult mustBe initialMongoUpload.copy(status = SensitiveUploadStatus(failure), lastUpdated = instant)
    }

    "successfully update the ttl and status to success" in {
      insertInitialUploadDetails()

      val uploadSuccessful = UploadStatus.Success("test-name", "text/csv", "/test-url", None)
      val updateResult: Unit = repository.updateStatus(reference, uploadSuccessful).futureValue
      val findAfterUpdateResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      updateResult mustBe()
      findAfterUpdateResult mustBe initialMongoUpload.copy(status = SensitiveUploadStatus(uploadSuccessful), lastUpdated = instant)
    }

    "successfully encrypt status" in {
      insertInitialUploadDetails()

      val uploadSuccessful = UploadStatus.Success("test-name", "text/csv", "/test-url", None)
      repository.updateStatus(reference, uploadSuccessful).futureValue

      val rawData =
        repository
          .collection
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

      val updateResult: Unit = repository.setValidationState(uploadKey, UploadValidated(CsvDocumentValid)).futureValue
      val findAfterUpdateResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

      updateResult mustBe()
      findAfterUpdateResult.lastUpdated mustBe instant
      findAfterUpdateResult.validationState.value.decryptedValue mustBe UploadValidated
    }
  }

  ".getUploadResult" - {
    "successfully get the upload result" in {
      insertInitialUploadDetails()

      val updateResult: Unit = repository.setValidationState(uploadKey, UploadValidated(CsvDocumentValid)).futureValue
      val getResult = repository.getValidationState(uploadKey).futureValue

      updateResult mustBe()
      getResult mustBe Some(UploadValidated)
    }
  }

  ".remove" - {
    "successfully remove a previously inserted record" in {
      insertInitialUploadDetails()

      val removeResult: Unit = repository.remove(uploadKey).futureValue
      val findAfterRemoveResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption

      removeResult mustBe()
      findAfterRemoveResult mustBe None
    }
  }

  "successfully encrypt result" in {
    insertInitialUploadDetails()

    repository.setValidationState(uploadKey, UploadValidated(CsvDocumentValid)).futureValue
    val rawData =
      repository
        .collection
        .find[BsonDocument](Filters.equal("id", uploadKey.value))
        .toFuture()
        .futureValue
        .headOption

    assert(rawData.nonEmpty)
    rawData.map(_.get("validationState").asString().getValue must fullyMatch.regex(encryptedRegex))
  }

  private def insertInitialUploadDetails(): Unit = {
    val insertResult: Unit = repository.insert(initialUploadDetails).futureValue
    val findResult = find(Filters.equal("id", uploadKey.value)).futureValue.headOption.value

    insertResult mustBe()
    findResult mustBe initialMongoUpload
  }
}
