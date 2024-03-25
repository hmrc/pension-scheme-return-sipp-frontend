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
import controllers.actions._
import models.SchemeId.Srn
import models.{Journey, Mode, UploadKey, UploadValidated, ValidationError, ValidationErrorType}
import navigation.Navigator
import pages.UploadErrorSummaryPage
import play.api.i18n._
import play.api.mvc._
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage._
import viewmodels.LabelSize
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView
import models.csv.CsvDocumentInvalid

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class FileUploadErrorSummaryController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  uploadService: UploadService,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, journey: Journey): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      uploadService
        .getUploadValidationState(UploadKey.fromRequest(srn, journey.uploadRedirectTag))
        .map {
          case Some(UploadValidated(CsvDocumentInvalid(_, errors))) =>
            errors.toList.collectFirst {
              case validationError @ ValidationError(_, ValidationErrorType.InvalidRowFormat, _) => validationError
            } match {
              case Some(value) => Ok(view(viewModelFormatting(srn, journey, value)))
              case None => Ok(view(viewModelErrors(srn, journey, errors)))
            }
          case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
  }

  def onSubmit(srn: Srn, journey: Journey, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Redirect(navigator.nextPage(UploadErrorSummaryPage(srn, journey), mode, request.userAnswers))
  }
}

object FileUploadErrorSummaryController {

  private def errorSummary(errors: NonEmptyList[ValidationError]): TableMessage = {
    def toMessage(errors: NonEmptyList[ValidationError]) = CompoundMessage(
      ParagraphMessage(errors.head.message),
      ParagraphMessage(Message("fileUploadErrorSummary.table.message", errors.map(_.row).toList.mkString(",")))
    )

    val errorsAcc: List[InlineMessage] =
      errors.groupBy(_.message).foldLeft(List.empty[InlineMessage]) { // TODO Group BY!!!!!!!
        case (acc, (_, errorMessages)) =>
          toMessage(errorMessages) :: acc
      }

    TableMessage(
      content = NonEmptyList.fromListUnsafe(errorsAcc),
      heading = Some(Message("site.error"))
    )
  }

  def viewModelErrors(
    srn: Srn,
    journey: Journey,
    errors: NonEmptyList[ValidationError]
  ): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = s"${journey.messagePrefix}.fileUploadErrorSummary.title",
      heading = s"${journey.messagePrefix}.fileUploadErrorSummary.heading",
      description = Some(
        ParagraphMessage("fileUploadErrorSummary.paragraph") ++
          Heading2("fileUploadErrorSummary.heading2", LabelSize.Medium) ++
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
      onSubmit = routes.FileUploadErrorSummaryController.onSubmit(srn, journey)
    )

  def viewModelFormatting(srn: Srn, journey: Journey, error: ValidationError): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel[ContentPageViewModel](
      title = s"${journey.messagePrefix}.fileUploadErrorSummary.title",
      heading = s"${journey.messagePrefix}.fileUploadErrorSummary.heading",
      description = Some(
        ParagraphMessage("fileUploadErrorSummary.paragraph") ++
          Heading2("fileUploadErrorSummary.heading2", LabelSize.Medium) ++
          TableMessage(
            content = NonEmptyList.one(Message(error.message)),
            heading = Some(Message("site.error"))
          ) ++
          ParagraphMessage(LinkMessage("download.template.file.hintMessage.print", "#print"))
      ),
      page = ContentPageViewModel(isLargeHeading = true),
      refresh = None,
      buttonText = "site.returnToFileUpload",
      onSubmit = routes.FileUploadErrorSummaryController.onSubmit(srn, journey)
    )
}
