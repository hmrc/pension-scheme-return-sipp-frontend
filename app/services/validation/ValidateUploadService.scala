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

import models.SchemeId.Srn
import models.{Journey, NormalMode, PensionSchemeId, UploadKey, UploadStatus, UploadValidated}
import play.api.Logger
import play.api.i18n.Messages
import services.PendingFileActionService.{Complete, Pending, PendingState}
import services.{SchemeDetailsService, UploadService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class ValidateUploadService @Inject()(
  uploadService: UploadService,
  schemeDetailsService: SchemeDetailsService,
  uploadValidatorForMemberDetails: MemberDetailsUploadValidator,
  uploadValidatorForLandOrProperty: InterestLandOrPropertyUploadValidator
) {

  private val logger: Logger = Logger(classOf[ValidateUploadService])
  def validateUpload(
    uploadKey: UploadKey,
    id: PensionSchemeId,
    srn: Srn,
    journey: Journey
  )(implicit headerCarrier: HeaderCarrier, messages: Messages): Future[PendingState] = journey match {
    case Journey.MemberDetails => validate(uploadKey, id, srn, uploadValidatorForMemberDetails)
    case Journey.InterestInLandOrProperty => validate(uploadKey, id, srn, uploadValidatorForLandOrProperty)
    case Journey.ArmsLengthLandOrProperty =>
      Future.successful(
        Complete(
          controllers.routes.FileUploadSuccessController.onPageLoad(srn, journey, NormalMode).url
        )
      )
  }

  private def validate(
    uploadKey: UploadKey,
    id: PensionSchemeId,
    srn: Srn,
    validator: UploadValidator
  )(implicit headerCarrier: HeaderCarrier, messages: Messages): Future[PendingState] =
    getUploadedFile(uploadKey).flatMap {
      case None => Future.successful(Complete(controllers.routes.JourneyRecoveryController.onPageLoad().url))
      case Some(file) =>
        val _ = (for {
          source <- uploadService.downloadFromUpscan(file.downloadUrl)
          scheme <- schemeDetailsService.getMinimalSchemeDetails(id, srn)
          validated <- validator.validateUpload(source._2, scheme.flatMap(_.windUpDate))
          _ <- uploadService.saveValidatedUpload(uploadKey, validated._1)
        } yield ()).recover {
          case NonFatal(e) =>
            logger.error("Validations failed with error: ", e)
          // uploadService.saveValidatedUpload(uploadKey, validated._1) TODO add a failure state for this case and persist
        }

        Future.successful(Pending)
    }

  private def getUploadedFile(uploadKey: UploadKey): Future[Option[UploadStatus.Success]] =
    uploadService
      .getUploadStatus(uploadKey)
      .map {
        case Some(upload: UploadStatus.Success) => Some(upload)
        case _ => None
      }
}
