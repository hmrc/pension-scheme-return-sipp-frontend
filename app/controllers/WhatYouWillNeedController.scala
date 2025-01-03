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

import controllers.actions.*
import play.api.i18n.*
import play.api.mvc.*
import navigation.Navigator
import models.{DateRange, NormalMode}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import viewmodels.implicits.*
import viewmodels.DisplayMessage.*
import views.html.ContentPageView
import WhatYouWillNeedController.*
import cats.implicits.toFunctorOps
import config.FrontendAppConfig
import pages.WhatYouWillNeedPage
import models.SchemeId.{Pstr, Srn}
import models.audit.PSRStartAuditEvent
import models.requests.DataRequest
import play.api.Logging
import services.{AuditService, ReportDetailsService, SchemeDateService}
import cats.implicits.toTraverseOps
import models.requests.common.YesNo.No

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class WhatYouWillNeedController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("root") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  formBundleOrVersion: FormBundleOrVersionTaxYearRequiredAction,
  schemeDateService: SchemeDateService,
  getData: DataRetrievalAction,
  createData: DataCreationAction,
  auditService: AuditService,
  reportDetailsService: ReportDetailsService,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView,
  config: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(createData).andThen(formBundleOrVersion).async { implicit request =>
      val managementUrls = config.urls.managePensionsSchemes

      for {
        pstr <- Future.successful(Pstr(request.underlying.schemeDetails.pstr))
        mDetailsFBundle <- request.formBundleNumber.flatTraverse { fbNum =>
          schemeDateService.returnBasicDetails(pstr, fbNum)
        }
        mDetailsVersion <- request.versionTaxYear.flatTraverse { vTxYear =>
          schemeDateService.returnBasicDetails(pstr, vTxYear)
        }
      } yield {
        val mDetails = mDetailsFBundle.orElse(mDetailsVersion)

        mDetails match {
          case Some(details) if details.memberDetails == No =>
            logger.info(
              s"ETMP details retrieved with no member details, redirecting Assets Held page"
            )
            Redirect(routes.AssetsHeldController.onPageLoad(srn))
          case _ =>
            logger.info(
              s"ETMP details retrieved with member details, rendering What You Will Need page, pst: $pstr"
            )
            Ok(
              view(
                viewModel(srn, request.underlying.schemeDetails.schemeName, managementUrls.dashboard, overviewUrl(srn))
              )
            )
        }
      }
    }

  private def overviewUrl(srn: Srn): String =
    config.urls.pensionSchemeFrontend.overview.format(srn.value)

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(createData).async { implicit request =>
      auditService
        .sendEvent(buildAuditEvent(reportDetailsService.getTaxYear()))
        .as(Redirect(navigator.nextPage(WhatYouWillNeedPage(srn), NormalMode, request.userAnswers)))
    }

  private def buildAuditEvent(taxYear: DateRange)(implicit
    req: DataRequest[?]
  ) = PSRStartAuditEvent(
    pensionSchemeId = req.pensionSchemeId,
    minimalDetails = req.minimalDetails,
    schemeDetails = req.schemeDetails,
    taxYear = taxYear
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
      .withBreadcrumbs(
        List(
          schemeName -> managingUrl,
          "whatYouWillNeed.breadcrumbOverview" -> overviewUrl,
          "whatYouWillNeed.title" -> "#"
        )
      )
}
