package repositories

import akka.util.ByteString
import config.{FakeCrypto, FrontendAppConfig}
import models._
import uk.gov.hmrc.mongo.play.json.Codecs.JsonOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UploadRepositorySpec extends GridFSRepositorySpec {

  override protected def collectionName: String = "upload"
  override protected def checkTtlIndex: Boolean = false

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.uploadTtl).thenReturn(1)

  private val connection = new MongoGridFsConnection(mongoComponent)
  private val encryptedRegex = "^[A-Za-z0-9+/=]+$"

  protected val repository = new UploadRepository(
    connection,
    crypto = FakeCrypto
  )

  private val validationResult: UploadFormatError = UploadFormatError(
    ValidationError(
      0,
      ValidationErrorType.Formatting,
      "Invalid file format please format file as per provided template"
    )
  )

  ".setUploadResult" - {
    "successfully insert Upload" in {
      val insertResult: Unit = repository.setUploadResult(uploadKey, validationResult).futureValue
      val findResult = repository.getUploadResult(uploadKey).futureValue

      insertResult mustBe ()
      findResult mustBe Some(validationResult)
    }

    "successfully encrypt result" in {
      val uploadKey: UploadKey = UploadKey("test-userid-test", srn, "test-redirect-tag")
      val _: Unit = repository.setUploadResult(uploadKey, validationResult).futureValue

      val rawData = findRaw(uploadKey).futureValue

      rawData.replace("\"", "") must fullyMatch.regex(encryptedRegex)
    }

    "successfully over-write existing Upload with the same key" in {
      val overwrittenResult: UploadFormatError = UploadFormatError(
        ValidationError(
          -1,
          ValidationErrorType.Country,
          "Overwrite"
        )
      )
      val _: Unit = repository.setUploadResult(uploadKey, validationResult).futureValue
      val findResult = repository.getUploadResult(uploadKey).futureValue
      val _: Unit = repository.setUploadResult(uploadKey, overwrittenResult).futureValue
      val finalResult = repository.getUploadResult(uploadKey).futureValue

      findResult mustBe Some(validationResult)
      finalResult mustBe Some(overwrittenResult)
    }
  }

  ".getUploadResult" - {
    "successfully fetch a previously inserted Upload" in {
      val uploadKey: UploadKey = UploadKey("test-userid-test-2", srn, "test-redirect-tag")
      val _: Unit = repository.setUploadResult(uploadKey, validationResult).futureValue

      val findResult = repository.getUploadResult(uploadKey).futureValue

      findResult mustBe Some(validationResult)
    }

    "return none when record doesn't exist" in {
      val uploadKey: UploadKey = UploadKey("doesn't-exist", srn, "doesn't-exist")

      val findResult = repository.getUploadResult(uploadKey).futureValue

      findResult mustBe None
    }
  }

  ".delete" - {
    "do not fail when file does not exist" in {
      val uploadKey: UploadKey = UploadKey("123456", srn, "test-redirect-tag")
      val delete: Unit = repository.delete(uploadKey).futureValue

      delete mustBe ()
    }

    "delete existing file" in {
      val uploadKey: UploadKey = UploadKey("123456", srn, "test-redirect-tag")
      val _: Unit = repository.setUploadResult(uploadKey, validationResult).futureValue
      val findResult = repository.getUploadResult(uploadKey).futureValue
      val _: Unit = repository.delete(uploadKey).futureValue
      val findResultDelete = repository.getUploadResult(uploadKey).futureValue

      findResult mustBe Some(validationResult)
      findResultDelete mustBe None
    }
  }

  private def findRaw(key: UploadKey): Future[String] =
    connection.gridFSBucket
      .downloadToObservable(key.value.toBson())
      .foldLeft(ByteString.empty)(_ ++ ByteString(_))
      .toFuture()
      .map(_.utf8String)

}
