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

import cats.data.NonEmptyList
import controllers.FileUploadErrorSummaryController.{viewModelErrors, viewModelFormatting}
import controllers.actions.*
import models.SchemeId.Srn
import models.UploadState.UploadValidated
import models.audit.FileUploadAuditEvent
import models.csv.CsvDocumentInvalid
import models.requests.DataRequest
import models.{Journey, JourneyType, Mode, UploadKey, UploadStatus, ValidationError, ValidationErrorType}
import navigation.Navigator
import pages.UploadErrorSummaryPage
import play.api.Logging
import play.api.i18n.*
import play.api.mvc.*
import services.{AuditService, TaxYearService, UploadService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{InlineMessage, *}
import viewmodels.implicits.*
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import viewmodels.{DisplayMessage, LabelSize}
import views.html.ContentPageView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class FileUploadErrorSummaryController @Inject() (
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
      uploadService
        .getUploadValidationState(UploadKey.fromRequest(srn, journey.uploadRedirectTag))
        .map {
          case Some(UploadValidated(CsvDocumentInvalid(_, errors))) =>
            sendAuditEvent(srn, journey)
            errors.toList.collectFirst {
              case validationError @ ValidationError(_, ValidationErrorType.InvalidRowFormat, _) => validationError
            } match {
              case Some(value) => Ok(view(viewModelFormatting(srn, journey, journeyType, value)))
              case None => Ok(view(viewModelErrors(srn, journey, journeyType, errors)))
            }
          case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }

  private def sendAuditEvent(srn: Srn, journey: Journey)(implicit request: DataRequest[?]) =
    uploadService.getUploadStatus(UploadKey.fromRequest(srn, journey.uploadRedirectTag)).flatMap {
      case Some(upload: UploadStatus.Success) =>
        auditService
          .sendEvent(
            FileUploadAuditEvent.buildAuditEvent(
              fileUploadType = journey.entryName,
              fileUploadStatus = FileUploadAuditEvent.ERROR,
              typeOfError = FileUploadAuditEvent.ERROR_UNDER,
              fileName = upload.name,
              fileReference = upload.downloadUrl,
              fileSize = upload.size.getOrElse(0),
              validationCompleted = LocalDate.now(),
              taxYear = taxYearService.fromRequest()
            )
          )
      case _ => Future.successful(logger.error("Sending Audit event failed"))
    }

  def onSubmit(srn: Srn, journey: Journey, journeyType: JourneyType, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Redirect(navigator.nextPage(UploadErrorSummaryPage(srn, journey, journeyType), mode, request.userAnswers))
    }
}

object FileUploadErrorSummaryController {

  private def errorSummary(errors: NonEmptyList[ValidationError]): TableMessageWithKeyValue = {
    def toMessage(errors: NonEmptyList[ValidationError]) = (
      Message(errors.head.message),
      ParagraphMessage(errors.map(_.row).toList.mkString(", "))
    )

    val errorsAcc =
      errors.groupBy(_.message).foldLeft(List.empty[(InlineMessage, DisplayMessage)]) {
        case (acc, (_, errorMessages)) => toMessage(errorMessages) :: acc
      }

    TableMessageWithKeyValue(
      content = NonEmptyList.fromListUnsafe(errorsAcc),
      heading = Some((Message("site.errors"), Message("site.rows"))),
      caption = Some(Message("fileUploadErrorSummary.caption"))
    )
  }

  def viewModelErrors(
    srn: Srn,
    journey: Journey,
    journeyType: JourneyType,
    errors: NonEmptyList[ValidationError]
  ): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = s"${journey.messagePrefix}.fileUploadErrorSummary.title",
      heading = s"${journey.messagePrefix}.fileUploadErrorSummary.heading",
      description = Some(
        ParagraphMessage("fileUploadErrorSummary.paragraph") ++
          errorSummary(errors) ++
          ParagraphMessage(
            "fileUploadErrorSummary.linkMessage.paragraph.start",
            DownloadLinkMessage(
              "fileUploadErrorSummary.linkMessage",
              controllers.routes.DownloadCsvController.downloadFile(srn, journey).url
            ),
            "fileUploadErrorSummary.linkMessage.paragraph.end"
          ) ++
          ParagraphMessage(LinkMessage("download.template.file.hintMessage.print", "#print"))
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadErrorSummaryController.onSubmit(srn, journey, journeyType)
    )

  def viewModelFormatting(
    srn: Srn,
    journey: Journey,
    journeyType: JourneyType,
    error: ValidationError
  ): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = s"${journey.messagePrefix}.fileUploadErrorSummary.title",
      heading = s"${journey.messagePrefix}.fileUploadErrorSummary.heading",
      description = Some(
        ParagraphMessage("fileUploadErrorSummary.paragraph") ++
          Heading2("fileUploadErrorSummary.heading2", LabelSize.Medium) ++
          ParagraphMessage(Message(error.message)) ++
          ParagraphMessage(LinkMessage("download.template.file.hintMessage.print", "#print"))
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadErrorSummaryController.onSubmit(srn, journey, journeyType)
    )
}
