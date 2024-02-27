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

package controllers.memberdetails

import akka.stream.Materializer
import controllers.actions._
import controllers.memberdetails.CheckMemberDetailsFileController.viewModel
import forms.YesNoPageFormProvider
import models.Journey.MemberDetails
import models.SchemeId.Srn
import models.UploadStatus.UploadStatus
import models.audit.{PSRFileValidationAuditEvent, PSRUpscanFileUploadAuditEvent}
import models.requests.DataRequest
import models.{DateRange, Mode, Upload, UploadErrors, UploadFormatError, UploadKey, UploadStatus, UploadSuccess}
import navigation.Navigator
import pages.memberdetails.CheckMemberDetailsFilePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services._
import services.validation.MemberDetailsUploadValidator
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.ParagraphMessage
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class CheckMemberDetailsFileController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  uploadService: UploadService,
  uploadValidator: MemberDetailsUploadValidator,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext, mat: Materializer)
    extends FrontendBaseController
    with I18nSupport {

  private val form = CheckMemberDetailsFileController.form(formProvider)
  val redirectTag = MemberDetails.uploadRedirectTag

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    //val startTime = System.currentTimeMillis  TODO commented out code to be re-enabled as part of upload validation

    val preparedForm = request.userAnswers.fillForm(CheckMemberDetailsFilePage(srn), form)
    val uploadKey = UploadKey.fromRequest(srn, redirectTag)

    uploadService.getUploadStatus(uploadKey).map {
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(upload: UploadStatus.Success) => {
        //auditUpload(srn, upload, startTime)
        Ok(view(preparedForm, viewModel(srn, Some(upload.name), mode)))
      }
      case Some(failure: UploadStatus.Failed) =>
        //auditUpload(srn, failure, startTime)
        Ok(view(preparedForm, viewModel(srn, Some(""), mode)))
      case Some(_) => Ok(view(preparedForm, viewModel(srn, None, mode)))
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val uploadKey = UploadKey.fromRequest(srn, redirectTag)

    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          getUploadedFile(uploadKey)
            .map(file => BadRequest(view(formWithErrors, viewModel(srn, file.map(_.name), mode)))),
        value =>
          getUploadedFile(uploadKey).flatMap {
            case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            case Some(_) =>
              for {
                _ <- uploadService.setUploadedStatus(uploadKey)
                updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckMemberDetailsFilePage(srn), value))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(CheckMemberDetailsFilePage(srn), mode, updatedAnswers))
          }
      )
  }
  // todo: handle all Upscan upload states
  //       None is an error case as the initial state set on the previous page should be InProgress
  private def getUploadedFile(uploadKey: UploadKey): Future[Option[UploadStatus.Success]] =
    uploadService
      .getUploadStatus(uploadKey)
      .map {
        case Some(upload: UploadStatus.Success) => Some(upload)
        case _ => None
      }

  private def buildUploadAuditEvent(taxYear: DateRange, uploadStatus: UploadStatus, duration: Long)(
    implicit req: DataRequest[_]
  ) = PSRUpscanFileUploadAuditEvent(
    pensionSchemeId = req.pensionSchemeId,
    minimalDetails = req.minimalDetails,
    schemeDetails = req.schemeDetails,
    taxYear = taxYear,
    uploadStatus,
    duration
  )

//  private def auditUpload(srn: Srn, uploadStatus: UploadStatus, startTime: Long)(
//    implicit request: DataRequest[_]
//  ): Unit = {
//    val endTime = System.currentTimeMillis
//    val duration = endTime - startTime
//    schemeDateService
//      .taxYearOrAccountingPeriods(srn)
//      .merge
//      .getOrRecoverJourney
//      .map(taxYear => {
//        auditService.sendEvent(buildUploadAuditEvent(taxYear, uploadStatus, duration))
//      })
//  }

//  private def buildDownloadAuditEvent(taxYear: DateRange, responseStatus: Int, duration: Long)(
//    implicit req: DataRequest[_]
//  ) = PSRUpscanFileDownloadAuditEvent(
//    schemeName = req.schemeDetails.schemeName,
//    schemeAdministratorName = req.schemeDetails.establishers.head.name,
//    psaOrPspId = req.pensionSchemeId.value,
//    schemeTaxReference = req.schemeDetails.pstr,
//    affinityGroup = if (req.minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual",
//    credentialRole = if (req.pensionSchemeId.isPSP) "PSP" else "PSA",
//    taxYear = taxYear,
//    downloadStatus = responseStatus match {
//      case 200 => "Success"
//      case _ => "Failed"
//    },
//    duration
//  )

//  private def auditDownload(srn: Srn, responseStatus: Int, duration: Long)(implicit request: DataRequest[_]): Unit =
//    schemeDateService
//      .taxYearOrAccountingPeriods(srn)
//      .merge
//      .getOrRecoverJourney
//      .map(
//        taxYear => auditService.sendEvent(buildDownloadAuditEvent(taxYear, responseStatus, duration))
//      )

//  private def auditValidation(srn: Srn, outcome: (Upload, Int, Long))(
//    implicit request: DataRequest[_]
//  ): Unit =
//    schemeDateService
//      .taxYearOrAccountingPeriods(srn)
//      .merge
//      .getOrRecoverJourney
//      .map(
//        taxYear => auditService.sendEvent(buildValidationAuditEvent(taxYear, outcome))
//      )

  private def buildValidationAuditEvent(taxYear: DateRange, outcome: (Upload, Int, Long))(
    implicit req: DataRequest[_]
  ) = PSRFileValidationAuditEvent(
    pensionSchemeId = req.pensionSchemeId,
    minimalDetails = req.minimalDetails,
    schemeDetails = req.schemeDetails,
    taxYear = taxYear,
    validationCheckStatus = outcome._1 match {
      case _: UploadSuccess => "Success"
      case _ => "Failed"
    },
    fileValidationTimeInMilliSeconds = outcome._3,
    numberOfEntries = outcome._2,
    numberOfFailures = outcome._1 match {
      case _: UploadSuccess => 0
      case errors: UploadErrors => errors.errors.size
      case _: UploadFormatError.type => 1
      case _ => 0
    }
  )
}

object CheckMemberDetailsFileController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "checkMemberDetailsFile.error.required"
  )

  def viewModel(srn: Srn, fileName: Option[String], mode: Mode): FormPageViewModel[YesNoPageViewModel] = {
    val refresh = if (fileName.isEmpty) Some(1) else None
    FormPageViewModel(
      "checkMemberDetailsFile.title",
      "checkMemberDetailsFile.heading",
      YesNoPageViewModel(
        legend = Some("checkMemberDetailsFile.legend"),
        yes = Some("checkMemberDetailsFile.yes"),
        no = Some("checkMemberDetailsFile.no")
      ),
      onSubmit = routes.CheckMemberDetailsFileController.onSubmit(srn, mode)
    ).refreshPage(refresh)
      .withDescription(
        fileName.map(name => ParagraphMessage(name))
      )
  }
}
