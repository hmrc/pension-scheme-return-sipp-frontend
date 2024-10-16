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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.PSRConnector
import controllers.actions.*
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.view.TaskListViewModelService
import services.view.TaskListViewModelService.{SchemeSectionsStatus, ViewMode}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear
import views.html.TaskListView

import scala.concurrent.ExecutionContext

class ViewTaskListController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView,
  appConfig: FrontendAppConfig,
  psrConnector: PSRConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, fbNumber: Option[String]): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn).async { request =>
      implicit val dataRequest = request.underlying
      val overviewURL = s"${appConfig.pensionSchemeReturnFrontend.baseUrl}/pension-scheme-return/${srn.value}/overview"
      val formBundleNumber = fbNumber.getOrElse(request.formBundleNumber.value)

      psrConnector
        .getPSRSubmission(
          request.underlying.schemeDetails.pstr,
          optFbNumber = Some(formBundleNumber),
          optPsrVersion = None,
          optPeriodStartDate = None
        )
        .map { submission =>
          val dates = TaxYear(submission.details.periodStart.getYear)
          val viewModel = ViewTaskListController.taskListViewModelService.viewModel(
            srn,
            request.underlying.schemeDetails.schemeName,
            dates.starts,
            dates.finishes,
            overviewURL,
            SchemeSectionsStatus.fromPSRSubmission(submission),
            formBundleNumber
          )

          Ok(view(viewModel))
        }

    }
}

object ViewTaskListController {
  val taskListViewModelService = new TaskListViewModelService(ViewMode.View)
}
