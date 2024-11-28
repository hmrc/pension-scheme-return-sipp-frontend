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

import cats.implicits.toFunctorOps
import cats.syntax.show.toShow
import config.Constants.defaultFbVersion
import connectors.PSRConnector
import controllers.actions.IdentifyAndRequireData
import models.SchemeId.Srn
import models.audit.EmailAuditEvent
import models.backend.responses.PsrAssetCountsResponse
import models.{DateRange, Journey, JourneyType, MinimalSchemeDetails, NormalMode, PensionSchemeId, TypeOfViewChangeQuestion}
import navigation.Navigator
import pages.DeclarationPage
import pages.ViewChangeQuestionPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AuditService, ReportDetailsService, SchemeDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{CaptionHeading2, DownloadLinkMessage, Heading2, ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, TextInputViewModel}
import viewmodels.{Caption, DisplayMessage}
import views.html.DeclarationPageView
import forms.TextFormProvider
import models.requests.DataRequest
import models.requests.psr.ReportDetails
import play.api.data.Form

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  formProvider: TextFormProvider,
  view: DeclarationPageView,
  schemeDetailsService: SchemeDetailsService,
  reportDetailsService: ReportDetailsService,
  psrConnector: PSRConnector,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, fbNumber: Option[String]): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val reportDetails = reportDetailsService.getReportDetails()
      val version = reportDetails.version
      val taxYearStartDate = Some(reportDetails.periodStart.toString)
      val pensionSchemeId = request.pensionSchemeId
      
      psrConnector.getPsrAssetCounts(reportDetails.pstr, fbNumber, taxYearStartDate, version).flatMap {
        assetCounts =>
          getMinimalSchemeDetails(pensionSchemeId, srn) { details =>
            val viewModel =
              DeclarationController.viewModel(
                srn,
                pensionSchemeId,
                details,
                assetCounts,
                fbNumber,
                taxYearStartDate,
                version,
                reportDetails.taxYearDateRange
              )
            Future.successful(Ok(
              view(
                DeclarationController.form(formProvider, request.schemeDetails.authorisingPSAID),
                viewModel)
              ))
          }
      }
  }

  def onSubmit(srn: Srn, fbNumber: Option[String]): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val reportDetails = reportDetailsService.getReportDetails()
      val version = reportDetails.version
      val taxYearStartDate = Some(reportDetails.periodStart.toString)
      val pensionSchemeId = request.pensionSchemeId
      
      val journeyType = request.userAnswers.get(ViewChangeQuestionPage(srn)) match {
        case Some(TypeOfViewChangeQuestion.ChangeReturn) => JourneyType.Amend
        case _ => JourneyType.Standard
      }

      if (pensionSchemeId.isPSP) {

        DeclarationController
          .form(formProvider, request.schemeDetails.authorisingPSAID)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              psrConnector.getPsrAssetCounts(reportDetails.pstr, fbNumber, taxYearStartDate, version).flatMap { assetCounts =>
                getMinimalSchemeDetails(request.pensionSchemeId, srn) { details =>
                  Future.successful(
                    BadRequest(
                      view(
                        formWithErrors,
                        DeclarationController.viewModel(
                          srn,
                          pensionSchemeId,
                          details,
                          assetCounts,
                          fbNumber,
                          taxYearStartDate,
                          version,
                          reportDetails.taxYearDateRange
                        )
                      )
                    )
                  )
                }
              },
            psaId => submit(
              srn,
              reportDetails,
              journeyType,
              fbNumber,
              taxYearStartDate,
              version,
              psaId
            )
          )
      } else {
        submit(
          srn,
          reportDetails,
          journeyType,
          fbNumber,
          taxYearStartDate,
          version,
          pensionSchemeId.value
        )
      }
  }

  private def submit(
              srn: Srn,
              reportDetails: ReportDetails,
              journeyType: JourneyType,
              fbNumber: Option[String],
              taxYearStartDate: Option[String],
              version: Option[String],
              psaId: String
            )(implicit request: DataRequest[?]) = {
    val redirect = Redirect(navigator.nextPage(DeclarationPage(srn), NormalMode, request.userAnswers))
    
    psrConnector
      .submitPsr(
        reportDetails.pstr,
        journeyType,
        fbNumber,
        taxYearStartDate,
        version,
        reportDetails.taxYearDateRange,
        reportDetails.schemeName,
        psaId
      )
      .flatMap { response =>
        if (response.emailSent)
          auditService
            .sendEvent(
              EmailAuditEvent.buildAuditEvent(
                taxYear = reportDetails.taxYearDateRange,
                reportVersion = defaultFbVersion
              ) // defaultFbVersion is 000 as no versions yet - initial submission
            )
            .as(redirect)
        else
          Future.successful(redirect)
      }
  }

  private def getMinimalSchemeDetails(id: PensionSchemeId, srn: Srn)(
    f: MinimalSchemeDetails => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] =
    schemeDetailsService.getMinimalSchemeDetails(id, srn).flatMap {
      case Some(schemeDetails) => f(schemeDetails)
      case None => Future.successful(Redirect(routes.UnauthorisedController.onPageLoad))
    }
}

object DeclarationController {

  def form(formProvider: TextFormProvider, authorisingPsaId: Option[String]): Form[String] = formProvider.psaId(
    "pspDeclaration.psaId.error.required",
    "pspDeclaration.psaId.error.invalid.characters",
    "pspDeclaration.psaId.error.invalid.characters",
    "pspDeclaration.psaId.error.invalid.noMatch",
    authorisingPsaId
  )

  private def max(d1: LocalDate, d2: LocalDate): LocalDate =
    if (d1.isAfter(d2)) d1 else d2

  private def min(d1: LocalDate, d2: LocalDate): LocalDate =
    if (d1.isAfter(d2)) d2 else d1

  private def createLink(
    messageKey: String,
    schemaName: String,
    srn: Srn,
    fbNumber: Option[String],
    taxYearStartDate: Option[String],
    version: Option[String],
    journey: Journey
  ): ParagraphMessage = {
    val url = controllers.routes.DownloadCsvController
      .downloadEtmpFile(srn, journey, fbNumber, taxYearStartDate, version)
      .url

    ParagraphMessage(
      DownloadLinkMessage(Message(messageKey, schemaName), url)
    )
  }

  def viewModel(
    srn: Srn,
    pensionSchemeId: PensionSchemeId,
    schemeDetails: MinimalSchemeDetails,
    assetCounts: Option[PsrAssetCountsResponse],
    fbNumber: Option[String],
    taxYearStartDate: Option[String],
    version: Option[String],
    taxYear: DateRange
  ): FormPageViewModel[TextInputViewModel] = {
    val name = schemeDetails.name.replace(" ", "_")

    val links = List(
      Option.when(assetCounts.exists(_.interestInLandOrPropertyCount > 0))(
        createLink(
          "psaDeclaration.downloadInterestInLand",
          name,
          srn,
          fbNumber,
          taxYearStartDate,
          version,
          Journey.InterestInLandOrProperty
        )
      ),
      Option.when(assetCounts.exists(_.landArmsLengthCount > 0))(
        createLink(
          "psaDeclaration.downloadArmsLength",
          name,
          srn,
          fbNumber,
          taxYearStartDate,
          version,
          Journey.ArmsLengthLandOrProperty
        )
      ),
      Option.when(assetCounts.exists(_.tangibleMoveablePropertyCount > 0))(
        createLink(
          "psaDeclaration.downloadTangibleMoveable",
          name,
          srn,
          fbNumber,
          taxYearStartDate,
          version,
          Journey.TangibleMoveableProperty
        )
      ),
      Option.when(assetCounts.exists(_.outstandingLoansCount > 0))(
        createLink(
          "psaDeclaration.downloadOutstandingLoan",
          name,
          srn,
          fbNumber,
          taxYearStartDate,
          version,
          Journey.OutstandingLoans
        )
      ),
      Option.when(assetCounts.exists(_.unquotedSharesCount > 0))(
        createLink(
          "psaDeclaration.downloadUnquotedShares",
          name,
          srn,
          fbNumber,
          taxYearStartDate,
          version,
          Journey.UnquotedShares
        )
      ),
      Option.when(assetCounts.exists(_.assetsFromConnectedPartyCount > 0))(
        createLink(
          "psaDeclaration.downloadAssetsFromConnected",
          name,
          srn,
          fbNumber,
          taxYearStartDate,
          version,
          Journey.AssetFromConnectedParty
        )
      )
    ).flatten

    val linkMessages = if (links.isEmpty) {
      ParagraphMessage("psaDeclaration.noData")
    } else {
      links.foldLeft[DisplayMessage](ParagraphMessage("")) { (acc, link) =>
        acc ++ link
      }
    }

    FormPageViewModel(
      Message("psaDeclaration.title"),
      Message("psaDeclaration.heading"),
      if(pensionSchemeId.isPSP) {
        TextInputViewModel(Some(Message("pspDeclaration.psaId.label")), isFixedLength = true)
      } else {
        TextInputViewModel(None, isFixedLength = true)
      },
      routes.DeclarationController.onSubmit(srn, fbNumber)
    ).withButtonText(Message("site.agreeAndContinue"))
      .withDescription(
        ParagraphMessage(
          Message(
            "psaDeclaration.taxYear",
            min(schemeDetails.windUpDate.getOrElse(taxYear.from), taxYear.to).show,
            max(schemeDetails.openDate.getOrElse(taxYear.from), taxYear.to).show
          )
        ) ++
          ParagraphMessage("psaDeclaration.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "psaDeclaration.listItem1",
            "psaDeclaration.listItem2",
            "psaDeclaration.listItem3",
            "psaDeclaration.listItem4"
          ) ++
          Heading2("psaDeclaration.dataAddedHeading") ++
          linkMessages
      )
      .withAdditionalHeadingText(CaptionHeading2(Message(schemeDetails.name), Caption.Large))
  }
}
