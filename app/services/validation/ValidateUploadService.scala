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
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxApplicativeId
import config.Crypto
import connectors.{PSRConnector, UpscanDownloadStreamConnector}
import models.SchemeId.Srn
import models.csv.{CsvDocumentValid, CsvDocumentValidAndSaved, CsvRowState}
import models.error.{EtmpRequestDataSizeExceedError, EtmpServerError}
import models.requests._
import models.requests.psr.ReportDetails
import models.{Journey, JourneyType, NormalMode, PensionSchemeId, SavingToEtmpException, UploadKey, UploadState, UploadStatus, UploadValidated, ValidationException}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Framing, Sink, Source}
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.i18n.Messages
import play.api.libs.json.Format
import repositories.CsvRowStateSerialization.IntLength
import repositories.{CsvRowStateSerialization, UploadRepository}
import services.PendingFileActionService.{Complete, Pending, PendingState}
import services.validation.csv._
import services.{ReportDetailsService, UploadService}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}

import java.nio.ByteOrder
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidateUploadService @Inject()(
  uploadService: UploadService,
  csvRowValidationParameterService: CsvRowValidationParameterService,
  interestInLandOrPropertyCsvRowValidator: InterestInLandOrPropertyCsvRowValidator,
  armsLengthLandOrPropertyCsvRowValidator: ArmsLengthLandOrPropertyCsvRowValidator,
  tangibleMoveableCsvRowValidator: TangibleMoveableCsvRowValidator,
  outstandingLoansCsvRowValidator: OutstandingLoansCsvRowValidator,
  unquotedSharesCsvRowValidator: UnquotedSharesCsvRowValidator,
  assetFromConnectedPartyCsvRowValidator: AssetFromConnectedPartyCsvRowValidator,
  upscanDownloadStreamConnector: UpscanDownloadStreamConnector,
  csvValidatorService: CsvValidatorService,
  psrConnector: PSRConnector,
  uploadRepository: UploadRepository,
  crypto: Crypto,
  reportDetailsService: ReportDetailsService
)(implicit ec: ExecutionContext, materializer: Materializer)
    extends Logging {

  private val recoveryState = Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url)
  private implicit val cryptoEncDec: Encrypter with Decrypter = crypto.getCrypto

  def validateUpload(
    uploadKey: UploadKey,
    id: PensionSchemeId,
    srn: Srn,
    journey: Journey,
    journeyType: JourneyType
  )(implicit headerCarrier: HeaderCarrier, messages: Messages, req: DataRequest[_]): Future[PendingState] =
    IO.fromFuture(IO(getUploadedFile(uploadKey)))
      .flatMap {
        case Some(file) =>
          validate(file, journey, uploadKey, id, srn)
            .flatMap(submit(srn, journey, uploadKey, _).attempt.flatMap {
              case Left(e: NotFoundException) =>
                IO(controllers.routes.ETMPErrorReceivedController.onEtmpErrorPageLoadWithSrn(srn).url)
              case Left(e: EtmpServerError) =>
                IO(controllers.routes.ETMPErrorReceivedController.onEtmpErrorPageLoadWithSrn(srn).url)
              case Left(e: EtmpRequestDataSizeExceedError) =>
                IO(
                  controllers.routes.ETMPErrorReceivedController
                    .onEtmpRequestDataSizeExceedErrorPageLoadWithSrn(srn)
                    .url
                )
              case Left(e: InternalServerException) =>
                IO(controllers.routes.ETMPErrorReceivedController.onEtmpErrorPageLoadWithSrn(srn).url)
              case Left(e: IllegalStateException) =>
                IO("ValidationException")
              case Left(e) =>
                IO(controllers.routes.ETMPErrorReceivedController.onEtmpErrorPageLoadWithSrn(srn).url)
              case Right(_) =>
                IO.pure(controllers.routes.FileUploadSuccessController.onPageLoad(srn, journey, journeyType, NormalMode).url)
            })
            .flatMap { url =>
              if (url == controllers.routes.FileUploadSuccessController.onPageLoad(srn, journey, journeyType, NormalMode).url) {
                IO.fromFuture(
                    IO(uploadService.setUploadValidationState(uploadKey, UploadValidated(CsvDocumentValidAndSaved)))
                  )
              } else if (url != "ValidationException") {
                IO.fromFuture(
                    IO(uploadService.setUploadValidationState(uploadKey, SavingToEtmpException(url)))
                  )
              } else {
                IO.unit
              }
            }
            .onError(t => IO(logger.error(s"Csv validation/submission failed for journey, ${journey.entryName}", t)))
            .start
            .as(Pending: PendingState)

        case None => recoveryState.pure[IO]
      }
      .unsafeToFuture()

  private def validate(
    file: UploadStatus.Success,
    journey: Journey,
    uploadKey: UploadKey,
    id: PensionSchemeId,
    srn: Srn
  )(implicit headerCarrier: HeaderCarrier, messages: Messages): IO[UploadState] =
    journey match {
      case Journey.InterestInLandOrProperty =>
        streamingValidation(file, journey, uploadKey, id, srn, interestInLandOrPropertyCsvRowValidator)
      case Journey.ArmsLengthLandOrProperty =>
        streamingValidation(file, journey, uploadKey, id, srn, armsLengthLandOrPropertyCsvRowValidator)
      case Journey.TangibleMoveableProperty =>
        streamingValidation(file, journey, uploadKey, id, srn, tangibleMoveableCsvRowValidator)
      case Journey.OutstandingLoans =>
        streamingValidation(file, journey, uploadKey, id, srn, outstandingLoansCsvRowValidator)
      case Journey.UnquotedShares =>
        streamingValidation(file, journey, uploadKey, id, srn, unquotedSharesCsvRowValidator)
      case Journey.AssetFromConnectedParty =>
        streamingValidation(file, journey, uploadKey, id, srn, assetFromConnectedPartyCsvRowValidator)
    }

  private def submit(srn: Srn, journey: Journey, key: UploadKey, uploadState: UploadState)(
    implicit hc: HeaderCarrier,
    req: DataRequest[_]
  ): IO[Unit] = {
    def readAndSubmit[T: Format, Req](
      makeRequest: (ReportDetails, Option[NonEmptyList[T]]) => Req,
      submit: PSRConnector => Req => Future[Unit]
    ): IO[Unit] =
      readTransactionDetails[T](key)
        .map(makeRequest(reportDetailsService.getReportDetails(srn), _))
        .flatMap(request => IO.fromFuture(IO(submit(psrConnector)(request))))

    if (uploadState == UploadValidated(CsvDocumentValid)) {
      journey match {
        case Journey.InterestInLandOrProperty =>
          readAndSubmit(LandOrConnectedPropertyRequest.apply, _.submitLandOrConnectedProperty)
        case Journey.ArmsLengthLandOrProperty =>
          readAndSubmit(LandOrConnectedPropertyRequest.apply, _.submitLandArmsLength)
        case Journey.TangibleMoveableProperty =>
          readAndSubmit(TangibleMoveablePropertyRequest.apply, _.submitTangibleMoveableProperty)
        case Journey.OutstandingLoans =>
          readAndSubmit(OutstandingLoanRequest.apply, _.submitOutstandingLoans)
        case Journey.UnquotedShares =>
          readAndSubmit(UnquotedShareRequest.apply, _.submitUnquotedShares)
        case Journey.AssetFromConnectedParty =>
          readAndSubmit(AssetsFromConnectedPartyRequest.apply, _.submitAssetsFromConnectedParty)
      }
    } else {
      throw new IllegalStateException("Expected UploadValidated(CsvDocumentValid)")
    }
  }

  private def streamingValidation[T](
    file: UploadStatus.Success,
    journey: Journey,
    uploadKey: UploadKey,
    id: PensionSchemeId,
    srn: Srn,
    csvRowValidator: CsvRowValidator[T]
  )(implicit headerCarrier: HeaderCarrier, messages: Messages, format: Format[T]): IO[UploadState] = {

    val validationResult = for {
      parameters <- IO.fromFuture(IO(csvRowValidationParameterService.csvRowValidationParameters(id, srn)))
      stream = upscanDownloadStreamConnector.stream(file.downloadUrl)
      validation <- csvValidatorService.validateUpload(stream, csvRowValidator, parameters, uploadKey)
    } yield UploadValidated(validation)

    validationResult
      .recoverWith(recoverValidation(journey, _))
      .flatTap(state => IO.fromFuture(IO(uploadService.setUploadValidationState(uploadKey, state))))
  }

  private def getUploadedFile(uploadKey: UploadKey): Future[Option[UploadStatus.Success]] =
    uploadService
      .getUploadStatus(uploadKey)
      .map {
        case Some(upload: UploadStatus.Success) => Some(upload)
        case _ => None
      }

  private def readTransactionDetails[T: Format](key: UploadKey): IO[Option[NonEmptyList[T]]] = {
    val lengthFieldFrame =
      Framing.lengthField(fieldLength = IntLength, maximumFrameLength = 256 * 1000, byteOrder = ByteOrder.BIG_ENDIAN)
    lazy val records = Source
      .fromPublisher(uploadRepository.streamUploadResult(key))
      .map(ByteString.apply)
      .via(lengthFieldFrame)
      .map(_.toByteBuffer)
      .map(CsvRowStateSerialization.read[T])
      .flatMapConcat {
        case CsvRowState.CsvRowValid(_, validated, _) => Source.single(validated)
        case CsvRowState.CsvRowInvalid(_, errors, _) =>
          Source.failed[T](
            new Throwable(errors.map(_.toString).toList.mkString("\n"))
          )
      }
      .runWith(Sink.seq[T])
      .map(seq => NonEmptyList.fromList(seq.toList))
    IO.fromFuture(IO(records))
  }

  private def recoverValidation(journey: Journey, throwable: Throwable): IO[UploadState] =
    IO(
      logger.error(
        s"Validation failed with exception for journey, ${journey.entryName}, persisting ValidationException state.",
        throwable
      )
    ).as(ValidationException)
}
