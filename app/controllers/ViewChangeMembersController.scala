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
import forms.TextFormProvider
import models.SchemeId.{Pstr, Srn}
import models.backend.responses.MemberDetails
import models.requests.{DataRequest, UpdateMemberDetailsRequest}
import models.{CSV_DATE_TIME, Mode, Pagination}
import pages.{RemoveMemberPage, RemoveMemberQuestionPage, UpdatePersonalDetailsQuestionPage}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ReportDetailsService, SaveService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, MemberListRow, MemberListViewModel, PaginatedViewModel}
import views.html.MemberListView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewChangeMembersController @Inject()(
  saveService: SaveService,
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  reportDetailsService: ReportDetailsService,
  view: MemberListView,
  textFormProvider: TextFormProvider
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val searchForm = textFormProvider("")

  def onPageLoad(srn: Srn, page: Int, searchParam: Option[String], mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      reportDetailsService.getMemberDetails(request.formBundleNumber, Pstr(dataRequest.schemeDetails.pstr)).flatMap {
        members =>
          val viewModel = dataRequest.userAnswers.get(RemoveMemberQuestionPage(srn)) match {
            case None =>
              Future.successful(
                ViewChangeMembersController.viewModel(srn, page, members, searchParam)
              )
            case Some(value) =>
              for {
                updatedAnswers <- Future.fromTry(dataRequest.userAnswers.remove(RemoveMemberQuestionPage(srn)))
                _ <- saveService.save(updatedAnswers)
              } yield ViewChangeMembersController
                .viewModel(srn, page, members, searchParam, displayDeleteSuccess = value)
          }
          viewModel.map(model => Ok(view(model, searchForm.fill(searchParam.getOrElse("")))))
      }
    }

  def onSearch(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn) { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      searchForm
        .bindFromRequest()
        .fold(
          _ => Redirect(routes.ViewChangeMembersController.onPageLoad(srn, 1, None)),
          searchText => Redirect(routes.ViewChangeMembersController.onPageLoad(srn, 1, Some(searchText)))
        )
    }

  def onSearchClear(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn) { _ =>
      Redirect(routes.ViewChangeMembersController.onPageLoad(srn, 1, None))
    }

  def redirectToUpdateMemberDetails(
    srn: Srn,
    firstName: String,
    lastName: String,
    dateOfBirth: String,
    nino: Option[String],
    mode: Mode
  ): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val memberDetails = MemberDetails(
      firstName = firstName,
      lastName = lastName,
      nino = nino,
      reasonNoNINO = None,
      dateOfBirth = LocalDate.parse(dateOfBirth, CSV_DATE_TIME)
    )
    for {
      updatedAnswers <- Future.fromTry(
        request.userAnswers.set(
          UpdatePersonalDetailsQuestionPage(srn),
          UpdateMemberDetailsRequest(current = memberDetails, updated = memberDetails)
        )
      )
      _ <- saveService.save(updatedAnswers)
    } yield Redirect(routes.ViewChangePersonalDetailsController.onPageLoad(srn))
  }

  def redirectToRemoveMember(
    srn: Srn,
    firstName: String,
    lastName: String,
    dateOfBirth: String,
    nino: Option[String],
    mode: Mode
  ): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    for {
      updatedAnswers <- Future.fromTry(
        request.userAnswers.set(
          RemoveMemberPage(srn),
          MemberDetails(
            firstName = firstName,
            lastName = lastName,
            nino = nino,
            reasonNoNINO = None,
            dateOfBirth = LocalDate.parse(dateOfBirth, CSV_DATE_TIME)
          )
        )
      )
      _ <- saveService.save(updatedAnswers)
    } yield Redirect(routes.RemoveMemberController.onPageLoad(srn))
  }
}

object ViewChangeMembersController {

  private def filterMembers(members: List[MemberDetails], searchText: Option[String]): List[MemberDetails] =
    searchText match {
      case Some(text) =>
        val searchText = text.toLowerCase().trim
        members.filter { member =>
          val memberKey: String =
            member.firstName + " " +
              member.lastName + " " +
              member.dateOfBirth.format(CSV_DATE_TIME) + " " +
              member.nino.getOrElse("")
          memberKey.toLowerCase().contains(searchText)
        }
      case None => members
    }

  def viewModel(
    srn: Srn,
    page: Int,
    data: List[MemberDetails],
    searchText: Option[String],
    displayDeleteSuccess: Boolean = false
  )(implicit messages: Messages): FormPageViewModel[MemberListViewModel] = {
    val filteredMember = filterMembers(data, searchText)
    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.pageSize,
      totalSize = filteredMember.size,
      call = controllers.routes.ViewChangeMembersController.onPageLoad(srn, _, searchText)
    )

    FormPageViewModel(
      title = Message("searchMembers.title", data.size),
      heading = Message("searchMembers.heading", data.size),
      description = Some(
        ParagraphMessage(Message("searchMembers.paragraph2")) ++ ParagraphMessage(Message("searchMembers.paragraph3"))
      ),
      page = MemberListViewModel(
        rows = rows(srn, filteredMember),
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
        ),
        showNotificationBanner = {
          if (displayDeleteSuccess)
            Some(
              (
                "success",
                messages("searchMembers.removeNotification.title"),
                messages("searchMembers.removeNotification.paragraph")
              )
            )
          else
            None
        },
        searchUrl = controllers.routes.ViewChangeMembersController.onSearch(srn),
        clearUrl = controllers.routes.ViewChangeMembersController.onSearchClear(srn)
      ),
      refresh = None,
      buttonText = "searchMembers.continue",
      details = None,
      onSubmit = controllers.routes.ChangeTaskListController.onPageLoad(srn)
    )
  }

  private def rows(
    srn: Srn,
    memberList: List[MemberDetails]
  ): List[MemberListRow] =
    memberList.map { member =>
      val memberMessage = s"${member.firstName} ${member.lastName}"

      MemberListRow(
        memberMessage,
        changeUrl = controllers.routes.ViewChangeMembersController
          .redirectToUpdateMemberDetails(
            srn,
            member.firstName,
            member.lastName,
            member.dateOfBirth.format(CSV_DATE_TIME),
            member.nino
          )
          .url,
        removeUrl = controllers.routes.ViewChangeMembersController
          .redirectToRemoveMember(
            srn,
            member.firstName,
            member.lastName,
            member.dateOfBirth.format(CSV_DATE_TIME),
            member.nino
          )
          .url
      )
    }
}
