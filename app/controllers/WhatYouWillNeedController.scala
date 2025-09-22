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

import cats.syntax.option.*
import cats.implicits.{catsSyntaxApplicativeByName, toFlatMapOps}
import config.{Constants, FrontendAppConfig}
import connectors.PSRConnector
import controllers.WhatYouWillNeedController.*
import controllers.actions.*
import models.SchemeId.Srn
import models.audit.PSRStartAuditEvent
import models.requests.DataRequest
import models.requests.common.YesNo.{No, Yes}
import models.{BasicDetails, DateRange, NormalMode}
import navigation.Navigator
import pages.WhatYouWillNeedPage
import play.api.Logging
import play.api.i18n.*
import play.api.mvc.*
import services.{AuditService, ReportDetailsService, SchemeDateService, TaxYearService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.*
import viewmodels.implicits.*
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import views.html.ContentPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class WhatYouWillNeedController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("root") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  formBundleOrVersion: FormBundleOrVersionTaxYearRequiredAction,
  schemeDateService: SchemeDateService,
  psrConnector: PSRConnector,
  getData: DataRetrievalAction,
  createData: DataCreationAction,
  auditService: AuditService,
  taxYearService: TaxYearService,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView,
  config: FrontendAppConfig,
  reportDetailsService: ReportDetailsService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(createData).andThen(formBundleOrVersion).async {
      implicit request =>
        val managementUrls = config.urls.managePensionsSchemes
        implicit val dataRequest: DataRequest[AnyContent] = request.underlying

        schemeDateService
          .returnBasicDetails(request)
          .flatTap(d =>
            auditService.sendEvent(buildAuditEvent(taxYearService.fromRequest())(request.underlying)).whenA(d.isEmpty)
          )
          .map {
            case Some(details) if details.memberDetails == No =>
              logger.info(
                s"ETMP details retrieved with no member details, redirecting Assets Held page"
              )
              Redirect(routes.AssetsHeldController.onPageLoad(srn))

            case Some(details) if details.oneOrMoreTransactionFilesUploaded == Yes =>
              logger.info(
                s"ETMP details retrieved with at least one transaction file, redirecting to Task List page"
              )
              Redirect(routes.TaskListController.onPageLoad(srn))

            case _ =>
              Ok(
                view(
                  viewModel(
                    srn,
                    request.underlying.schemeDetails.schemeName,
                    managementUrls.dashboard,
                    overviewUrl(srn)
                  )
                )
              )
          }
    }

  private def overviewUrl(srn: Srn): String =
    config.urls.pensionSchemeFrontend.overview.format(srn.value)

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(createData).andThen(formBundleOrVersion).async {
      request =>
        implicit val underlying = request.underlying

        schemeDateService
          .returnBasicDetails(request)
          .flatMap {
            case None =>
              psrConnector.createEmptyPsr(reportDetailsService.getReportDetails()).map(_.formBundleNumber.some)
            case _ => Future.successful(None)
          }
          .map { maybeFbNumber =>
            val redirect = Redirect(navigator.nextPage(WhatYouWillNeedPage(srn), NormalMode, underlying.userAnswers))
            maybeFbNumber.fold(redirect)(fbNumber => redirect.addingToSession(Constants.formBundleNumber -> fbNumber))
          }
    }

  private def buildAuditEvent(taxYear: DateRange)(implicit
    req: DataRequest[?]
  ) =
    PSRStartAuditEvent(
      pensionSchemeId = req.pensionSchemeId,
      minimalDetails = req.minimalDetails,
      schemeDetails = req.schemeDetails,
      taxYear = taxYear,
      req = req,
      srn = req.srn
    )
}

object WhatYouWillNeedController {
  def viewModel(
    srn: Srn,
    schemeName: String,
    managingUrl: String,
    overviewUrl: String
  ): FormPageViewModel[ContentPageViewModel] =
    FormPageViewModel(
      Message("whatYouWillNeed.title"),
      Message("whatYouWillNeed.heading"),
      ContentPageViewModel(isStartButton = true, isLargeHeading = false),
      routes.WhatYouWillNeedController.onSubmit(srn)
    ).withButtonText(Message("site.continue"))
      .withDescription(
        ParagraphMessage("whatYouWillNeed.paragraph1") ++
          ListMessage(
            ListType.Bullet,
            "whatYouWillNeed.listItem1",
            "whatYouWillNeed.listItem2"
          ) ++
          ParagraphMessage("whatYouWillNeed.paragraph2") ++
          Heading2("whatYouWillNeed.heading2") ++
          ParagraphMessage("whatYouWillNeed.paragraph3")
      )
}
