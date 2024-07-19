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

import controllers.actions._
import play.api.i18n._
import play.api.mvc._
import navigation.Navigator
import models.{DateRange, NormalMode}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import viewmodels.implicits._
import viewmodels.DisplayMessage._
import views.html.ContentPageView
import WhatYouWillNeedController._
import config.FrontendAppConfig
import pages.WhatYouWillNeedPage
import models.SchemeId.Srn
import models.audit.PSRStartAuditEvent
import models.requests.DataRequest
import services.{AuditService, TaxYearService}

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class WhatYouWillNeedController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("root") navigator: Navigator,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  createData: DataCreationAction,
  auditService: AuditService,
  taxYearService: TaxYearService,
  val controllerComponents: MessagesControllerComponents,
  view: ContentPageView,
  config: FrontendAppConfig,
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identify.andThen(allowAccess(srn)) { implicit request =>
    val managementUrls = config.urls.managePensionsSchemes

    Ok(view(viewModel(srn, request.schemeDetails.schemeName, managementUrls.dashboard, overviewUrl(srn))))
  }

  private def overviewUrl(srn: Srn): String =
    config.urls.pensionSchemeFrontend.overview.format(srn.value)

  def onSubmit(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(createData).async { implicit request =>
      auditService
        .sendEvent(buildAuditEvent(DateRange.from(taxYearService.current)))
        .map(_ => Redirect(navigator.nextPage(WhatYouWillNeedPage(srn), NormalMode, request.userAnswers)))
    }

  private def buildAuditEvent(taxYear: DateRange)(
    implicit req: DataRequest[_]
  ) = PSRStartAuditEvent(
    pensionSchemeId = req.pensionSchemeId,
    minimalDetails = req.minimalDetails,
    schemeDetails = req.schemeDetails,
    taxYear = taxYear
  )
}

object WhatYouWillNeedController {
  def viewModel(srn: Srn, schemeName: String, managingUrl: String, overviewUrl: String): FormPageViewModel[ContentPageViewModel] =
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
            "whatYouWillNeed.listItem2",
            "whatYouWillNeed.listItem3"
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
