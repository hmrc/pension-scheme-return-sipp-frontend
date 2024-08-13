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

import cats.implicits.{catsSyntaxOptionId, toFunctorOps}
import connectors.PSRConnector
import controllers.ViewChangePersonalDetailsController.viewModel
import controllers.actions.IdentifyAndRequireData
import models.PersonalDetailsUpdateData
import models.SchemeId.Srn
import models.backend.responses.MemberDetails
import models.requests.{DataRequest, UpdateMemberDetailsRequest}
import pages.UpdatePersonalDetailsQuestionPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.ViewChangePersonalDetailsViewModel
import viewmodels.models.ViewChangePersonalDetailsViewModel.ViewChangePersonalDetailsRowViewModel
import views.html.ViewChangePersonalDetailsView

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewChangePersonalDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  identifyAndRequireData: IdentifyAndRequireData,
  personalDetailsView: ViewChangePersonalDetailsView,
  psrConnector: PSRConnector,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn) { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      dataRequest.userAnswers.get(UpdatePersonalDetailsQuestionPage(srn)) match {
        case None =>
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(PersonalDetailsUpdateData(_, updated, _)) =>
          Ok(personalDetailsView(viewModel(srn, dataRequest.schemeDetails.schemeName, updated)))
      }
    }

  def onSubmit(srn: Srn): Action[AnyContent] =
    identifyAndRequireData.withFormBundle(srn).async { request =>
      implicit val dataRequest: DataRequest[AnyContent] = request.underlying

      dataRequest.userAnswers.get(UpdatePersonalDetailsQuestionPage(srn)) match {
        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(data) =>
          val pstr = dataRequest.underlying.schemeDetails.pstr
          val fbNumber = request.formBundleNumber.value

          val submission = if (data.updated != data.current) {
            val request = UpdateMemberDetailsRequest(data.current, data.updated)
            val updatedData = data.copy(isSubmitted = true)
            for {
              _ <- psrConnector.updateMemberDetails(pstr, fbNumber.some, None, None, request)
              _ <- saveService.setAndSave(dataRequest.userAnswers, UpdatePersonalDetailsQuestionPage(srn), updatedData)
            } yield ()
          } else Future.unit
          submission.as(Redirect(routes.ViewChangeMembersController.onPageLoad(srn, 1, None)))
      }
    }
}

object ViewChangePersonalDetailsController {
  def viewModel(srn: Srn, schemeName: String, member: MemberDetails) =
    ViewChangePersonalDetailsViewModel(
      srn = srn,
      title = "Change individual member details",
      heading = schemeName,
      memberName = s"${member.firstName} ${member.lastName}",
      ViewChangePersonalDetailsRowViewModel(
        "viewChange.personalDetails.firstName",
        member.firstName,
        controllers.routes.JourneyRecoveryController.onPageLoad().url
      ),
      ViewChangePersonalDetailsRowViewModel(
        "viewChange.personalDetails.lastName",
        member.lastName,
        controllers.routes.JourneyRecoveryController.onPageLoad().url
      ),
      ViewChangePersonalDetailsRowViewModel(
        member.nino
          .as(Message("viewChange.personalDetails.nino"))
          .getOrElse(Message("viewChange.personalDetails.reasonNoNINO", s"${member.firstName} ${member.lastName}")),
        member.nino.orElse(member.reasonNoNINO).mkString,
        controllers.routes.ViewChangeDoesMemberHasNinoController.onPageLoad(srn).url
      ),
      ViewChangePersonalDetailsRowViewModel(
        "viewChange.personalDetails.dob",
        member.dateOfBirth.format(DateTimeFormatter.ofPattern("dd MM yyyy")),
        controllers.routes.JourneyRecoveryController.onPageLoad().url
      )
    )
}
