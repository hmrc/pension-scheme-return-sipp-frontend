package repositories

import generators.Generators
import models.{ErrorDetails, Reference, SchemeId, UploadKey, UploadStatus}
import org.mockito.MockitoSugar
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.mongo.test.{DefaultPlayMongoRepositorySupport, MongoSupport, TtlIndexedMongoSupport}

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}

trait GridFSRepositorySpec extends AnyFreeSpec
  with Matchers
  with MongoSupport
  with TtlIndexedMongoSupport
  with ScalaFutures
  with IntegrationPatience
  with OptionValues
  with MockitoSugar
  with Generators {

  def srn: SchemeId.Srn = srnGen.sample.value
  val uploadKey: UploadKey = UploadKey("test-userid", srn, "test-redirect-tag")
  val reference: Reference = Reference("test-ref")
  val instant: Instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
  val failure: UploadStatus.Failed = UploadStatus.Failed(ErrorDetails("reason", "message"))
}
