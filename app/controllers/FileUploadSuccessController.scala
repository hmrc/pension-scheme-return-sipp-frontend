/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import controllers.FileUploadSuccessController.viewModel
import controllers.actions.*
import models.SchemeId.Srn
import models.audit.FileUploadAuditEvent
import models.{Journey, JourneyType, Mode, UploadKey, UploadState, UploadStatus}
import navigation.Navigator
import pages.{TaskListStatusPage, UploadSuccessPage}
import play.api.Logging
import play.api.i18n.*
import play.api.mvc.*
import services.{AuditService, ReportDetailsService, SaveService, UploadService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.*
import viewmodels.implicits.*
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class FileUploadSuccessController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  uploadService: UploadService,
  saveService: SaveService,
  auditService: AuditService,
  reportDetailsService: ReportDetailsService,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(srn: Srn, journey: Journey, journeyType: JourneyType, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val key = UploadKey.fromRequest(srn, journey.uploadRedirectTag)
      uploadService.getUploadStatus(key).flatMap {
        case Some(upload: UploadStatus.Success) =>
          for {
            updatedAnswers <- Future.fromTry(
              request.userAnswers
                .set(TaskListStatusPage(srn, journey), TaskListStatusPage.Status(completedWithNo = false))
            )
            _ <- saveService.save(updatedAnswers)
            _ <- auditService.sendEvent(
              FileUploadAuditEvent.buildAuditEvent(
                fileUploadType = journey.entryName,
                fileUploadStatus = FileUploadAuditEvent.SUCCESS,
                fileName = upload.name,
                fileReference = upload.downloadUrl,
                fileSize = upload.size.getOrElse(0),
                validationCompleted = LocalDate.now(),
                taxYear = reportDetailsService.getTaxYear()
              )
            )
          } yield Ok(view(viewModel(srn, upload.name, journey, journeyType, mode)))
        case _ => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit(srn: Srn, journey: Journey, journeyType: JourneyType, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      uploadService.getUploadValidationState(UploadKey.fromRequest(srn, journey.uploadRedirectTag)).flatMap {
        case Some(UploadState.UploadValidated(_)) =>
          Future.successful(
            Redirect(navigator.nextPage(UploadSuccessPage(srn, journey, journeyType), mode, request.userAnswers))
          )
        case other =>
          logger.warn(s"Upload has different state than expected ${other}")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

}

object FileUploadSuccessController {
  def viewModel(
    srn: Srn,
    fileName: String,
    journey: Journey,
    journeyType: JourneyType,
    mode: Mode
  ): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      title = s"${journey.messagePrefix}.fileUploadSuccess.title",
      heading = s"${journey.messagePrefix}.fileUploadSuccess.heading",
      ContentPageViewModel(isLargeHeading = true),
      onSubmit = routes.FileUploadSuccessController.onSubmit(srn, journey, journeyType, mode)
    ).withButtonText("generic.fileUploadSuccess.continue")
      .withDescription(
        ParagraphMessage(
          Message(s"${journey.messagePrefix}.fileUploadSuccess.paragraph", fileName)
        )
      )
      .withoutBackButton()
}
