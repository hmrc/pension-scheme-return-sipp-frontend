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

import cats.effect.IO
import cats.syntax.either._
import connectors.UpscanDownloadStreamConnector
import models.SchemeId.Srn
import models.{Journey, PensionSchemeId, UploadKey, UploadStatus, UploadValidated, ValidationException}
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.Format
import services.PendingFileActionService.{Complete, Pending, PendingState}
import services.UploadService
import services.validation.csv._
import uk.gov.hmrc.http.HeaderCarrier

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
  csvValidatorService: CsvValidatorService
)(implicit ec: ExecutionContext) {

  private val logger: Logger = Logger(classOf[ValidateUploadService])
  private val recoveryState = Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url)

  def validateUpload(
    uploadKey: UploadKey,
    id: PensionSchemeId,
    srn: Srn,
    journey: Journey
  )(implicit headerCarrier: HeaderCarrier, messages: Messages): Future[PendingState] =
    journey match {
      case Journey.InterestInLandOrProperty =>
        streamingValidation(journey, uploadKey, id, srn, interestInLandOrPropertyCsvRowValidator)
      case Journey.ArmsLengthLandOrProperty =>
        streamingValidation(journey, uploadKey, id, srn, armsLengthLandOrPropertyCsvRowValidator)
      case Journey.TangibleMoveableProperty =>
        streamingValidation(journey, uploadKey, id, srn, tangibleMoveableCsvRowValidator)
      case Journey.OutstandingLoans =>
        streamingValidation(journey, uploadKey, id, srn, outstandingLoansCsvRowValidator)
      case Journey.UnquotedShares =>
        streamingValidation(journey, uploadKey, id, srn, unquotedSharesCsvRowValidator)
      case Journey.AssetFromConnectedParty =>
        streamingValidation(journey, uploadKey, id, srn, assetFromConnectedPartyCsvRowValidator)
      case _ =>
        Future.successful(recoveryState)
    }

  private def streamingValidation[T](
    journey: Journey,
    uploadKey: UploadKey,
    id: PensionSchemeId,
    srn: Srn,
    csvRowValidator: CsvRowValidator[T]
  )(implicit headerCarrier: HeaderCarrier, messages: Messages, format: Format[T]): Future[PendingState] = {
    val result = IO.fromFuture(IO(getUploadedFile(uploadKey))).flatMap {
      case Some(file) =>
        val validationResult = for {
          parameters <- IO.fromFuture(IO(csvRowValidationParameterService.csvRowValidationParameters(id, srn)))
          stream = upscanDownloadStreamConnector.stream(file.downloadUrl)
          validation <- csvValidatorService.validateUpload(stream, csvRowValidator, parameters, uploadKey)
          result <- IO.fromFuture(IO(uploadService.setUploadValidationState(uploadKey, UploadValidated(validation))))
        } yield result

        validationResult
          .recoverWith(recoverValidation(journey, uploadKey))
          .unsafeRunAsync(_.leftMap(logger.error(s"Csv validation failed for journey, ${journey.name}", _)))(
            cats.effect.unsafe.implicits.global
          )

        IO.pure(Pending)

      case None => IO.pure(recoveryState)
    }

    Future.successful(result.unsafeRunSync()(cats.effect.unsafe.implicits.global))
  }

  private def getUploadedFile(uploadKey: UploadKey): Future[Option[UploadStatus.Success]] =
    uploadService
      .getUploadStatus(uploadKey)
      .map {
        case Some(upload: UploadStatus.Success) => Some(upload)
        case _ => None
      }

  private def recoverValidation(journey: Journey, uploadKey: UploadKey): PartialFunction[Throwable, IO[Unit]] = {
    case throwable: Throwable =>
      logger.error(
        s"Validation failed with exception for journey, ${journey.name}, persisting ValidationException state.",
        throwable
      )
      IO.fromFuture(IO(uploadService.setUploadValidationState(uploadKey, ValidationException)))
  }
}
