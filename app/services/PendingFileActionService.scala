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

package services

import config.Constants
import controllers.routes
import models.SchemeId.Srn
import models.UploadStatus.Failed
import models.*
import models.csv.{CsvDocumentEmpty, CsvDocumentInvalid, CsvDocumentState, CsvDocumentValid, CsvDocumentValidAndSaved}
import models.UploadState.*
import models.requests.DataRequest
import navigation.Navigator
import pages.UploadErrorPage
import play.api.Logger
import play.api.i18n.Messages
import services.PendingFileActionService.{Complete, Pending, PendingState}
import services.validation.ValidateUploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import java.time.{Clock, Instant}
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class PendingFileActionService @Inject() (
  @Named("sipp") navigator: Navigator,
  uploadService: UploadService,
  validateUploadService: ValidateUploadService,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends FrontendHeaderCarrierProvider {

  private val logger = Logger(classOf[PendingFileActionService])

  def getUploadState(srn: Srn, journey: Journey, journeyType: JourneyType)(implicit
    request: DataRequest[?]
  ): Future[PendingState] = {
    val uploadKey = UploadKey.fromRequest(srn, journey.uploadRedirectTag)
    val failureUrl = routes.UploadFileController.onPageLoad(srn, journey, journeyType).url

    uploadService.getUploadStatus(uploadKey).map {
      case Some(success: UploadStatus.Success) =>
        val successUrl = routes.CheckFileNameController.onPageLoad(srn, journey, journeyType, NormalMode).url
        val redirectUrl = checkFileFormat(success, successUrl, failureUrl)
        Complete(redirectUrl)

      case Some(failed: UploadStatus.Failed) =>
        Complete(failureUrl + s"?${failed.asQueryParams}")

      case None => Complete(failureUrl)

      case _ => Pending
    }
  }

  /**
   * Only required for local testing as the stubbed upscan does not provide a callback for this case
   */
  private def checkFileFormat(success: UploadStatus.Success, successUrl: String, failureUrl: String) =
    if (success.name.endsWith(".csv")) {
      successUrl
    } else {
      failureUrl + s"?${Failed.incorrectFileFormatQueryParam}"
    }

  def getValidationState(
    srn: Srn,
    journey: Journey,
    journeyType: JourneyType
  )(implicit request: DataRequest[?], messages: Messages): Future[PendingState] = {
    val key = UploadKey.fromRequest(srn, journey.uploadRedirectTag)

    uploadService.getUploadValidationState(key).flatMap {
      case Some(UploadValidated(csvDocumentState: CsvDocumentState)) =>
        csvDocumentState match {
          case CsvDocumentEmpty =>
            logger.info("csv document was empty")
            Future.successful(
              Complete(
                navigator
                  .nextPage(
                    UploadErrorPage(
                      srn,
                      journey,
                      journeyType,
                      UploadFormatError(ValidationError(0, ValidationErrorType.InvalidRowFormat, "empty csv"))
                    ),
                    NormalMode,
                    request.userAnswers
                  )
                  .url
              )
            )

          case CsvDocumentValid =>
            logger.info("csv document valid")
            Future.successful(Pending)

          case CsvDocumentValidAndSaved(formBundleNumber) =>
            logger.info("csv document valid and saved")
            Future.successful(
              Complete(
                routes.FileUploadSuccessController.onPageLoad(srn, journey, journeyType, NormalMode).url,
                Map(Constants.formBundleNumber -> formBundleNumber)
              )
            )

          case CsvDocumentInvalid(_, errors) =>
            logger.info("csv document invalid")
            Future.successful(
              Complete(
                navigator
                  .nextPage(
                    UploadErrorPage(srn, journey, journeyType, UploadErrors(errors)),
                    NormalMode,
                    request.userAnswers
                  )
                  .url
              )
            )
        }

      case Some(ValidationException) =>
        logger.error("csv validation exception")
        Future.successful(Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url))

      case Some(SavingToEtmpException(url)) =>
        logger.error("ETMP failed to save")
        Future.successful(Complete(url))

      case Some(UploadValidating(_)) =>
        Future.successful(Pending)

      case Some(Uploaded) =>
        uploadService
          .setUploadValidationState(key, UploadValidating(Instant.now(clock)))
          .flatMap(_ => validateUploadService.validateUpload(key, request.pensionSchemeId, srn, journey, journeyType))

      case None =>
        logger.error("no csv document upload could be found")
        Future.successful(Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url))
    }
  }

}

object PendingFileActionService {
  sealed trait PendingState
  case object Pending extends PendingState
  case class Complete(url: String, params: Map[String, String] = Map.empty) extends PendingState
}
