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

import controllers.FileUploadTooManyErrorsController.viewModel
import controllers.actions.*
import models.SchemeId.Srn
import models.UploadState.*
import models.audit.FileUploadAuditEvent
import models.csv.{CsvDocumentEmpty, CsvDocumentInvalid}
import models.requests.DataRequest
import models.{Journey, JourneyType, Mode, UploadKey, UploadStatus}
import navigation.Navigator
import pages.FileUploadTooManyErrorsPage
import play.api.Logging
import play.api.i18n.*
import play.api.mvc.*
import services.{AuditService, ReportDetailsService, TaxYearService, UploadService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.*
import viewmodels.implicits.*
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class FileUploadTooManyErrorsController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  uploadService: UploadService,
  auditService: AuditService,
  taxYearService: TaxYearService,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(srn: Srn, journey: Journey, journeyType: JourneyType): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      uploadService.getUploadValidationState(UploadKey.fromRequest(srn, journey.uploadRedirectTag)).map {
        case Some(_ @UploadValidated(uploadState))
            if uploadState.isInstanceOf[CsvDocumentEmpty.type] || uploadState.isInstanceOf[CsvDocumentInvalid] =>
          sendAuditEvent(srn, journey)
          Ok(view(viewModel(srn, journey, journeyType)))
        case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

  def onSubmit(srn: Srn, journey: Journey, journeyType: JourneyType, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Redirect(navigator.nextPage(FileUploadTooManyErrorsPage(srn, journey, journeyType), mode, request.userAnswers))
    }

  private def sendAuditEvent(srn: Srn, journey: Journey)(implicit request: DataRequest[?]) =
    uploadService.getUploadStatus(UploadKey.fromRequest(srn, journey.uploadRedirectTag)).flatMap {
      case Some(upload: UploadStatus.Success) =>
        auditService
          .sendEvent(
            FileUploadAuditEvent.buildAuditEvent(
              fileUploadType = journey.entryName,
              fileUploadStatus = FileUploadAuditEvent.ERROR,
              typeOfError = FileUploadAuditEvent.ERROR_OVER,
              fileName = upload.name,
              fileReference = upload.downloadUrl,
              fileSize = upload.size.getOrElse(0),
              validationCompleted = LocalDate.now(),
              taxYear = taxYearService.fromRequest()
            )
          )
      case _ => Future.successful(logger.error("Sending Audit event failed"))
    }
}

object FileUploadTooManyErrorsController {

  def viewModel(
    srn: Srn,
    journey: Journey,
    journeyType: JourneyType
  ): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = s"${journey.messagePrefix}.fileUploadTooManyErrors.title",
      heading = s"${journey.messagePrefix}.fileUploadTooManyErrors.heading",
      description = Some(
        ParagraphMessage("fileUploadTooManyErrors.paragraph") ++
          ParagraphMessage(Message("fileUploadTooManyErrors.paragraph2")) ++
          ListMessage.Bullet(Message("fileUploadTooManyErrors.bullet1")) ++
          ListMessage.Bullet(Message("fileUploadTooManyErrors.bullet2")) ++
          ListMessage.Bullet(Message("fileUploadTooManyErrors.bullet3")) ++
          ListMessage.Bullet(Message("fileUploadTooManyErrors.bullet4")) ++
          ParagraphMessage(
            "fileUploadTooManyErrors.paragraph3.part1",
            DownloadLinkMessage(
              "fileUploadTooManyErrors.paragraph3.part2",
              controllers.routes.DownloadCsvController.downloadFile(srn, journey).url
            ),
            "fileUploadTooManyErrors.paragraph3.part3"
          ) ++
          ParagraphMessage("fileUploadTooManyErrors.paragraph4")
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadTooManyErrorsController.onSubmit(srn, journey, journeyType)
    )
}
