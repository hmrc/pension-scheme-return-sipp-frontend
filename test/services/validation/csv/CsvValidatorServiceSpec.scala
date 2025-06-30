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
import config.Crypto
import models.csv.CsvDocumentValid
import models.csv.CsvRowState.CsvRowValid
import models.requests.DataRequest
import models.{SchemeId, UploadKey, UserAnswers}
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import repositories.{CsvRowStateSerialization, UploadRepository}
import services.validation.*
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.{Md5Hash, ObjectSummaryWithMd5, Path}
import utils.BaseSpec

import java.nio.ByteOrder
import java.time.{Instant, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CsvValidatorServiceSpec extends BaseSpec {

  private val landOrPropertyValidationsService = mock[LandOrPropertyValidationsService]
  private val tangibleMoveablePropertyValidationsService = mock[TangibleMoveablePropertyValidationsService]
  private val validationsService = mock[ValidationsService]
  private val unquotedSharesValidationsService = mock[UnquotedSharesValidationsService]
  private val assetsFromConnectedPartyValidationsService = mock[AssetsFromConnectedPartyValidationsService]
  private val csvDocumentValidator = mock[CsvDocumentValidator]

  private val srn: SchemeId.Srn = srnGen.sample.value
  private val uploadKey: UploadKey = UploadKey("usr", srn, "1")
  private val validationParams = CsvRowValidationParameters(Some(LocalDate.now().plusDays(1)))
  private val raw = "dummy"
  private val csvRowValid = CsvRowValid(1, Json.obj(), NonEmptyList.one(raw))
  private val source = Source.empty[ByteString]
  private val objectSummaryWithMd: ObjectSummaryWithMd5 = ObjectSummaryWithMd5(
    location = Path.File("/some/file.txt"),
    contentLength = 100,
    contentMd5 = Md5Hash("md5hash"),
    lastModified = Instant.now()
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())
  implicit val dataRequest: DataRequest[?] =
    DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, UserAnswers("id"))

  "CsvValidatorService" - {

    "validate and upload interest in land or property" in new Fixture {
      val validator = InterestInLandOrPropertyCsvRowValidator(landOrPropertyValidationsService)
      when(csvDocumentValidator.validate(any, any, any)(using any, any))
        .thenReturn(Source(List((csvRowValid, CsvDocumentValid))))
      whenReady(service.validateUpload(source, validator, validationParams, uploadKey)) { result =>
        result mustBe CsvDocumentValid
      }
    }

    "validate and upload  arms length" in new Fixture {
      val validator = ArmsLengthLandOrPropertyCsvRowValidator(landOrPropertyValidationsService)
      when(csvDocumentValidator.validate(any, any, any)(using any, any))
        .thenReturn(Source(List((csvRowValid, CsvDocumentValid))))
      whenReady(service.validateUpload(source, validator, validationParams, uploadKey)) { result =>
        result mustBe CsvDocumentValid
      }
    }

    "validate and upload tangible moveable" in new Fixture {
      val validator = TangibleMoveableCsvRowValidator(tangibleMoveablePropertyValidationsService)
      when(csvDocumentValidator.validate(any, any, any)(using any, any))
        .thenReturn(Source(List((csvRowValid, CsvDocumentValid))))
      whenReady(service.validateUpload(source, validator, validationParams, uploadKey)) { result =>
        result mustBe CsvDocumentValid
      }
    }

    "validate and upload outstanding loans" in new Fixture {
      val validator = OutstandingLoansCsvRowValidator(validationsService)
      when(csvDocumentValidator.validate(any, any, any)(using any, any))
        .thenReturn(Source(List((csvRowValid, CsvDocumentValid))))
      whenReady(service.validateUpload(source, validator, validationParams, uploadKey)) { result =>
        result mustBe CsvDocumentValid
      }
    }

    "validate and upload unquoted shares" in new Fixture {
      val validator = UnquotedSharesCsvRowValidator(unquotedSharesValidationsService)
      when(csvDocumentValidator.validate(any, any, any)(using any, any))
        .thenReturn(Source(List((csvRowValid, CsvDocumentValid))))
      whenReady(service.validateUpload(source, validator, validationParams, uploadKey)) { result =>
        result mustBe CsvDocumentValid
      }
    }

    "validate and upload asset from connected" in new Fixture {
      val validator = AssetFromConnectedPartyCsvRowValidator(assetsFromConnectedPartyValidationsService)
      when(csvDocumentValidator.validate(any, any, any)(using any, any))
        .thenReturn(Source(List((csvRowValid, CsvDocumentValid))))
      whenReady(service.validateUpload(source, validator, validationParams, uploadKey)) { result =>
        result mustBe CsvDocumentValid
      }
    }
  }

  def framedByteString(str: String): ByteString = {
    val bs = ByteString(str)
    ByteString.newBuilder.putInt(bs.length)(ByteOrder.BIG_ENDIAN).result ++ bs
  }

  trait Fixture {
    private val uploadRepository = mock[UploadRepository]
    private val encryptDecrypter = mock[Encrypter & Decrypter]
    private val crypto: Crypto = new Crypto {
      override def getCrypto: Encrypter & Decrypter = encryptDecrypter
    }
    private val csvRowStateSerialization = mock[CsvRowStateSerialization]

    when(uploadRepository.save(any, any)(any))
      .thenReturn(
        Future.successful(
          objectSummaryWithMd
        )
      )

    val service = CsvValidatorService(
      uploadRepository,
      csvDocumentValidator,
      crypto,
      csvRowStateSerialization
    )
  }
}
