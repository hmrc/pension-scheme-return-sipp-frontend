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

package services.validation

import cats.data.NonEmptyList
import cats.effect.IO
import cats.syntax.option.*
import config.Crypto
import connectors.{PSRConnector, UpscanDownloadStreamConnector}
import models.Journey.*
import models.JourneyType.*
import models.UploadState.UploadValidated
import models.UploadStatus.Success
import models.backend.responses.SippPsrJourneySubmissionEtmpResponse
import models.csv.CsvRowState.CsvRowValid
import models.csv.{CsvDocumentValid, CsvDocumentValidAndSaved}
import models.requests.DataRequest
import models.{Journey, JourneyType, PensionSchemeId, SchemeId, UploadKey, UserAnswers}
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.scalacheck.Gen
import org.scalatest.time.SpanSugar.*
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import repositories.{CsvRowStateSerialization, UploadRepository}
import services.PendingFileActionService.{Complete, Pending}
import services.validation.csv.*
import services.{ReportDetailsService, UploadService}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import java.nio.ByteOrder
import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ValidateUploadServiceSpec extends BaseSpec {

  val srn: SchemeId.Srn = srnGen.sample.value
  val uploadKey: UploadKey = UploadKey("usr", srn, "1")
  val id: PensionSchemeId.PspId = PensionSchemeId.PspId("id")
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())
  implicit val dataRequest: DataRequest[?] =
    DataRequest(allowedAccessRequestGen(FakeRequest()).sample.value, UserAnswers("id"))
  val successfulUploadStatus: Success = Success("name", "csv", "download", 100L.some)
  val fbNumber = "fb"
  val etmpSubmitResponse: SippPsrJourneySubmissionEtmpResponse = SippPsrJourneySubmissionEtmpResponse(fbNumber)

  "ValidateUploadService" - {
    "return recovery state when file not found" in new Fixture {
      when(uploadService.getUploadStatus(uploadKey)).thenReturn(Future.successful(None))
      whenReady(service.validateUpload(uploadKey, id, srn, UnquotedShares, Standard)) { result =>
        result mustBe Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

    "validate and submit interest in land or property" in new Fixture {
      when(psrConnector.submitLandOrConnectedProperty(any, eqTo(Standard), any, any)(any, any))
        .thenReturn(Future.successful(etmpSubmitResponse))
      testValidate(InterestInLandOrProperty, Standard)(using landOrPropertyGen)
    }

    "validate and submit arms length" in new Fixture {
      when(psrConnector.submitLandArmsLength(any, eqTo(Standard), any, any)(any, any))
        .thenReturn(Future.successful(etmpSubmitResponse))
      testValidate(ArmsLengthLandOrProperty, Standard)(using landOrPropertyGen)
    }

    "validate and submit tangible moveable" in new Fixture {
      when(psrConnector.submitTangibleMoveableProperty(any, eqTo(Standard), any, any)(any, any))
        .thenReturn(Future.successful(etmpSubmitResponse))
      testValidate(TangibleMoveableProperty, Standard)(using tangibleMoveablePropertyGen)
    }

    "validate and submit outstanding loans" in new Fixture {
      when(psrConnector.submitOutstandingLoans(any, eqTo(Standard), any, any)(any, any))
        .thenReturn(Future.successful(etmpSubmitResponse))
      testValidate(OutstandingLoans, Standard)(using outstandingLoansGen)
    }

    "validate and submit unquoted shares" in new Fixture {
      when(psrConnector.submitUnquotedShares(any, eqTo(Standard), any, any)(any, any))
        .thenReturn(Future.successful(etmpSubmitResponse))
      testValidate(UnquotedShares, Standard)(using unquotedSharesGen)
    }

    "validate and submit asset from connected" in new Fixture {
      when(psrConnector.submitAssetsFromConnectedParty(any, eqTo(Standard), any, any)(any, any))
        .thenReturn(Future.successful(etmpSubmitResponse))
      testValidate(AssetFromConnectedParty, Standard)(using assetsFromConnectedGen)
    }
  }

  def framedByteString(str: String): ByteString = {
    val bs = ByteString(str)
    ByteString.newBuilder.putInt(bs.length)(ByteOrder.BIG_ENDIAN).result ++ bs
  }

  trait Fixture {
    val uploadService = mock[UploadService]
    val csvRowValidationParameterService = mock[CsvRowValidationParameterService]
    val interestInLandOrPropertyCsvRowValidator = mock[InterestInLandOrPropertyCsvRowValidator]
    val armsLengthLandOrPropertyCsvRowValidator = mock[ArmsLengthLandOrPropertyCsvRowValidator]
    val tangibleMoveableCsvRowValidator = mock[TangibleMoveableCsvRowValidator]
    val outstandingLoansCsvRowValidator = mock[OutstandingLoansCsvRowValidator]
    val unquotedSharesCsvRowValidator = mock[UnquotedSharesCsvRowValidator]
    val assetFromConnectedPartyCsvRowValidator = mock[AssetFromConnectedPartyCsvRowValidator]
    val upscanDownloadStreamConnector = mock[UpscanDownloadStreamConnector]
    val csvValidatorService = mock[CsvValidatorService]
    val psrConnector = mock[PSRConnector]
    val uploadRepository = mock[UploadRepository]
    val encryptDecrypter = mock[Encrypter & Decrypter]
    val crypto: Crypto = new Crypto {
      override def getCrypto: Encrypter & Decrypter = encryptDecrypter
    }
    val reportDetailsService = mock[ReportDetailsService]
    val csvRowStateSerialization = mock[CsvRowStateSerialization]

    val service = ValidateUploadService(
      uploadService,
      csvRowValidationParameterService,
      interestInLandOrPropertyCsvRowValidator,
      armsLengthLandOrPropertyCsvRowValidator,
      tangibleMoveableCsvRowValidator,
      outstandingLoansCsvRowValidator,
      unquotedSharesCsvRowValidator,
      assetFromConnectedPartyCsvRowValidator,
      upscanDownloadStreamConnector,
      csvValidatorService,
      psrConnector,
      uploadRepository,
      crypto,
      reportDetailsService,
      csvRowStateSerialization
    )

    when(csvRowValidationParameterService.csvRowValidationParameters(any, any)(any))
      .thenReturn(Future.successful(CsvRowValidationParameters(LocalDate.now().plusMonths(6).some)))

    def testValidate[T: Gen](journey: Journey, journeyType: JourneyType): Future[Unit] = {
      when(uploadService.getUploadStatus(uploadKey)).thenReturn(Future.successful(successfulUploadStatus.some))
      when(csvValidatorService.validateUpload(any, any, any, eqTo(uploadKey))(any, any, any))
        .thenReturn(IO.pure(CsvDocumentValid))
      when(uploadService.setUploadValidationState(uploadKey, UploadValidated(CsvDocumentValid)))
        .thenReturn(Future.successful(()))
      when(uploadRepository.retrieve(eqTo(uploadKey))(any))
        .thenReturn(Future.successful(Some(Source.single(framedByteString("Hello")))))
      when(csvRowStateSerialization.read(any)(using any, any))
        .thenReturn(CsvRowValid(1, implicitly[Gen[T]].sample.value, NonEmptyList.one("dummy")))
      val uploadStatus: UploadValidated = UploadValidated(CsvDocumentValidAndSaved(fbNumber))
      when(uploadService.setUploadValidationState(uploadKey, uploadStatus))
        .thenReturn(Future.successful(()))

      whenReady(service.validateUpload(uploadKey, id, srn, journey, journeyType)) { result =>
        result mustBe Pending
        keepRetrying(2.seconds, 50.millis) {
          verify(uploadService).setUploadValidationState(uploadKey, uploadStatus)
        }
      }
    }
  }
}
