/*
 * Copyright 2024 HM Revenue & Customs
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

import config.Constants
import controllers.actions._
import models.SchemeId.{Pstr, Srn}
import models.backend.responses.MemberDetails
import models.requests.DataRequest
import models.{Mode, SippPagination}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ReportDetailsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, MemberListRow, MemberListViewModel, PaginatedViewModel}
import views.html.MemberListView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ViewChangeMembersController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  reportDetailsService: ReportDetailsService,
  view: MemberListView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, page: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      reportDetailsService.getMemberDetails(request.formBundleNumber, Pstr(dataRequest.schemeDetails.pstr)).map {
        members =>
          val viewModel = ViewChangeMembersController.viewModel(srn, page, members)
          Ok(view(viewModel))
      }

    }
}

object ViewChangeMembersController {

  def viewModel(
    srn: Srn,
    page: Int,
    data: List[MemberDetails]
  ): FormPageViewModel[MemberListViewModel] = {
    val pagination = SippPagination(
      currentPage = page,
      pageSize = Constants.pageSize,
      totalSize = data.size,
      call = controllers.routes.ViewChangeMembersController.onPageLoad(srn, _)
    )

    FormPageViewModel(
      title = Message("searchMembers.title", data.size),
      heading = Message("searchMembers.heading", data.size),
      description = Some(
        ParagraphMessage(Message("searchMembers.paragraph2")) ++ ParagraphMessage(Message("searchMembers.paragraph3"))
      ),
      page = MemberListViewModel(
        rows = rows(data),
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "searchMembers.paragraph1",
              pagination.pageStart,
              pagination.pageEnd,
              pagination.totalSize
            ),
            pagination
          )
        )
      ),
      refresh = None,
      buttonText = "searchMembers.continue",
      details = None,
      onSubmit = controllers.routes.ViewTaskListController.onPageLoad(srn)
    )
  }

  private def rows(
    memberList: List[MemberDetails]
  ): List[MemberListRow] =
    memberList.map { member =>
      val memberMessage = s"${member.firstName} ${member.lastName}"

      MemberListRow(
        memberMessage,
        changeUrl = controllers.routes.JourneyRecoveryController.onPageLoad().url,
        removeUrl = controllers.routes.JourneyRecoveryController.onPageLoad().url
      )
    }
}
