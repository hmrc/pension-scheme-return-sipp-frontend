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

import cats.implicits._
import utils.DateTimeUtils.localDateShow
import config.Constants.defaultFbVersion
import connectors.PSRConnector
import controllers.actions.IdentifyAndRequireData
import models.SchemeId.Srn
import models.audit.EmailAuditEvent
import models.backend.responses.PsrAssetCountsResponse
import models.requests.DataRequest
import models.{DateRange, Journey, MinimalSchemeDetails, NormalMode, PensionSchemeId}
import navigation.Navigator
import pages.DeclarationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AuditService, ReportDetailsService, SchemeDetailsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{CaptionHeading2, DownloadLinkMessage, Heading2, ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import viewmodels.{Caption, DisplayMessage}
import views.html.ContentPageView

import java.time.LocalDate
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView,
  schemeDetailsService: SchemeDetailsService,
  reportDetailsService: ReportDetailsService,
  psrConnector: PSRConnector,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, fbNumber: Option[String]): Action[AnyContent] =
    identifyAndRequireData.withFormBundleOrVersionAndTaxYear(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying
      val version = request.versionTaxYear.map(v => Some(v.version)).getOrElse(Some("000"))
      val taxYearStartDate = request.versionTaxYear.map(_.taxYear)

      val reportDetails = reportDetailsService.getReportDetails(srn)

      request.versionTaxYear match {
        case Some(versionTaxYear) =>
          psrConnector.getPsrAssetCounts(reportDetails.pstr, fbNumber, taxYearStartDate, version).flatMap {
            assetCounts =>
              getMinimalSchemeDetails(dataRequest.pensionSchemeId, srn) { details =>
                val viewModel =
                  DeclarationController.viewModel(
                    srn,
                    details,
                    assetCounts,
                    fbNumber,
                    taxYearStartDate,
                    version,
                    versionTaxYear.taxYearDateRange
                  )
                Future.successful(Ok(view(viewModel)))
              }
          }
        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }

    }

  def onSubmit(srn: Srn, fbNumber: Option[String]): Action[AnyContent] =
    identifyAndRequireData.withFormBundleOrVersionAndTaxYear(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      val reportDetails = reportDetailsService.getReportDetails(srn)
      val redirect = Redirect(navigator.nextPage(DeclarationPage(srn), NormalMode, dataRequest.userAnswers))
      val version = request.versionTaxYear.map(v => Some(v.version)).getOrElse(Some("000"))
      val taxYearStartDate = request.versionTaxYear.map(_.taxYear)

      request.versionTaxYear match {
        case Some(versionTaxYear) =>
          psrConnector
            .submitPsr(
              reportDetails.pstr,
              fbNumber,
              taxYearStartDate,
              version,
              versionTaxYear.taxYearDateRange,
              reportDetails.schemeName
            )
            .flatMap { response =>
              if (response.emailSent)
                auditService
                  .sendEvent(
                    EmailAuditEvent.buildAuditEvent(
                      taxYear = versionTaxYear.taxYearDateRange,
                      reportVersion = defaultFbVersion
                    ) // defaultFbVersion is 000 as no versions yet - initial submission
                  )
                  .as(redirect)
              else
                Future.successful(redirect)
            }
        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def getMinimalSchemeDetails(id: PensionSchemeId, srn: Srn)(
    f: MinimalSchemeDetails => Future[Result]
  )(implicit hc: HeaderCarrier): Future[Result] =
    schemeDetailsService.getMinimalSchemeDetails(id, srn).flatMap {
      case Some(schemeDetails) => f(schemeDetails)
      case None => Future.successful(Redirect(controllers.routes.UnauthorisedController.onPageLoad))
    }
}

object DeclarationController {

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
    schemeDetails: MinimalSchemeDetails,
    assetCounts: PsrAssetCountsResponse,
    fbNumber: Option[String],
    taxYearStartDate: Option[String],
    version: Option[String],
    taxYear: DateRange
  ): FormPageViewModel[ContentPageViewModel] = {
    val name = schemeDetails.name.replace(" ", "_")
    println("Assets counts: " + assetCounts)

    val links = List(
      Option.when(assetCounts.interestInLandOrPropertyCount > 0)(
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
      Option.when(assetCounts.landArmsLengthCount > 0)(
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
      Option.when(assetCounts.tangibleMoveablePropertyCount > 0)(
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
      Option.when(assetCounts.outstandingLoansCount > 0)(
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
      Option.when(assetCounts.unquotedSharesCount > 0)(
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
      Option.when(assetCounts.assetsFromConnectedPartyCount > 0)(
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
      ContentPageViewModel(),
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
