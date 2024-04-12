package repositories

import akka.util.ByteString
import cats.effect.IO
import config.{FakeCrypto, FrontendAppConfig}
import fs2._
import fs2.interop.reactivestreams._
import models._
import org.mongodb.scala._
import org.mongodb.scala.gridfs.GridFSUploadObservable
import org.reactivestreams.Publisher
import play.api.libs.json.Json
import repositories.UploadRepository._

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.Implicits.global

class UploadRepositorySpec extends GridFSRepositorySpec {

  override protected def collectionName: String = "upload"
  override protected def checkTtlIndex: Boolean = false

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.uploadTtl).thenReturn(1)

  private val connection = new MongoGridFsConnection(mongoComponent)

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

  ".publish" - {
    "store the stream of data" in {
      val uploadKey: UploadKey = UploadKey("123456", srn, "test-redirect-tag")
      val _: Unit = testStore(validationResult, uploadKey)(repository.publish).futureValue
      val findResult = testFind(uploadKey)(repository.streamUploadResult).futureValue

      findResult mustBe Some(validationResult)
    }
  }

  ".streamUploadResult" - {
    "retrieve the stream of data" in {
      val uploadKey: UploadKey = UploadKey("654321", srn, "test-another-tag")
      val _: Unit = testStore(validationResult, uploadKey)(repository.publish).futureValue
      val findResult = testFind(uploadKey)(repository.streamUploadResult).futureValue

      findResult mustBe Some(validationResult)
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
      val _: Unit = testStore(validationResult, uploadKey)(repository.publish).futureValue
      val findResult = testFind(uploadKey)(repository.streamUploadResult).futureValue
      val _: Unit = repository.delete(uploadKey).futureValue
      val findResultDelete = testFind(uploadKey)(repository.streamUploadResult).futureValue

      findResult mustBe Some(validationResult)
      findResultDelete mustBe None
    }
  }

  private def testFind(key: UploadKey)(find: (UploadKey) => Publisher[ByteBuffer]) =
    find(key)
      .toObservable()
      .headOption()
      .map(_.flatMap(buff => Json.parse(ByteString(buff).utf8String).asOpt[Upload]))

  private def testStore(upload: Upload, key: UploadKey)(
    publish: (UploadKey, Publisher[ByteBuffer]) => GridFSUploadObservable[Unit]
  ) = {
    val uploadAsBytes = Json.toJson(upload).toString().getBytes(StandardCharsets.UTF_8)

    Stream
      .resource(Stream.emit[IO, ByteBuffer](ByteBuffer.wrap(uploadAsBytes)).toUnicastPublisher)
      .flatMap(pub => publish(key, pub).toStreamBuffered[IO](1))
      .compile
      .lastOrError
      .unsafeToFuture()(cats.effect.unsafe.implicits.global)
  }

}
