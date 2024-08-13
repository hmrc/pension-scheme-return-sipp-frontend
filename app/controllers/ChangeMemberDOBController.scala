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
import controllers.ChangeMemberDOBController.viewModel
import controllers.actions._
import forms.DatePageFormProvider
import forms.mappings.errors.DateFormErrors
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.{UpdateMembersDOBQuestionPage, UpdatePersonalDetailsQuestionPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{DOBViewModel, FormPageViewModel}
import views.html.DOBView

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, FormatStyle}
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ChangeMemberDOBController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: DatePageFormProvider,
  view: DOBView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val form = ChangeMemberDOBController.form(formProvider, None)
    Ok(view(form.fromUserAnswers(UpdateMembersDOBQuestionPage(srn)), viewModel(srn, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val form = ChangeMemberDOBController.form(formProvider, None)
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, viewModel(srn, mode)))
          ),
        answer => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(UpdateMembersDOBQuestionPage(srn), answer))
            updatedPersonalDetailsQuestion <- Future(
              request.userAnswers.get(UpdatePersonalDetailsQuestionPage(srn)).flatMap { details =>
                updatedAnswers
                  .set(
                    UpdatePersonalDetailsQuestionPage(srn),
                    details.copy(updated = details.updated.copy(dateOfBirth = answer))
                  )
                  .toOption
              }
            )
            _ <- saveService.save(updatedPersonalDetailsQuestion.getOrElse(updatedAnswers))
          } yield Redirect(navigator.nextPage(UpdateMembersDOBQuestionPage(srn), mode, updatedAnswers))
        }
      )
  }

}

object ChangeMemberDOBController {

  def form(
    formProvider: DatePageFormProvider,
    validDateThreshold: Option[LocalDate]
  )(implicit messages: Messages): Form[LocalDate] = {
    val dateThreshold: LocalDate = validDateThreshold.getOrElse(LocalDate.now())
    formProvider(
      DateFormErrors(
        "memberDetails.dateOfBirth.upload.error.required.all",
        "memberDetails.dateOfBirth.upload.error.required.day",
        "memberDetails.dateOfBirth.upload.error.required.month",
        "memberDetails.dateOfBirth.upload.error.required.year",
        "memberDetails.dateOfBirth.upload.error.required.two",
        "memberDetails.dateOfBirth.upload.error.invalid.date",
        "memberDetails.dateOfBirth.upload.error.invalid.characters",
        List(
          DateFormErrors
            .failIfDateAfter(
              dateThreshold,
              messages(
                "memberDetails.dateOfBirth.upload.error.future",
                dateThreshold.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
              )
            ),
          DateFormErrors
            .failIfDateBefore(
              Constants.earliestDate,
              messages(
                "memberDetails.dateOfBirth.upload.error.after",
                Constants.earliestDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
              )
            )
        )
      )
    )
  }

  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[DOBViewModel] = FormPageViewModel(
    Message("memberDetails.dob.title"),
    Message("memberDetails.dob.heading"),
    DOBViewModel(
      Message("memberDetails.dob.dateOfBirth"),
      Message("memberDetails.dob.dateOfBirth.hint")
    ),
    routes.ChangeMemberDOBController.onSubmit(srn)
  )
}
