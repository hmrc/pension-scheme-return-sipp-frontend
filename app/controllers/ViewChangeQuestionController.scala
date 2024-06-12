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

import services.SaveService
import controllers.actions._
import navigation.Navigator
import forms.RadioListFormProvider
import models.{Mode, TypeOfViewChangeQuestion}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import controllers.ViewChangeQuestionController._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.RadioListView
import models.SchemeId.Srn
import models.TypeOfViewChangeQuestion.{ChangeReturn, ViewReturn}
import pages.ViewChangeQuestionPage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RadioListRowViewModel, RadioListViewModel}

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Named}

class ViewChangeQuestionController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            @Named("sipp") navigator: Navigator,
                                            identifyAndRequireData: IdentifyAndRequireData,
                                            formProvider: RadioListFormProvider,
                                            saveService: SaveService,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: RadioListView
                                          )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  private val form = ViewChangeQuestionController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(ViewChangeQuestionPage(srn)),
          viewModel(srn, mode)
        )
      )
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, mode)
                  )
                )
              ),
          answer => {
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(ViewChangeQuestionPage(srn), answer)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                ViewChangeQuestionPage(srn),
                mode,
                updatedAnswers
              )
            )
          }
        )
    }
}

object ViewChangeQuestionController {

  def form(formProvider: RadioListFormProvider): Form[TypeOfViewChangeQuestion] = formProvider[TypeOfViewChangeQuestion](
    "viewChangeQuestion.error.required"
  )

  val radioListItems: List[RadioListRowViewModel] =
    List(
      RadioListRowViewModel(Message("viewChangeQuestion.radioList1"), ViewReturn.name),
      RadioListRowViewModel(Message("viewChangeQuestion.radioList2"), ChangeReturn.name),
    )

  def viewModel(
                 srn: Srn,
                 mode: Mode
               ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      Message("viewChangeQuestion.title"),
      Message("viewChangeQuestion.heading"),
      RadioListViewModel(
        None,
        radioListItems,
        hint = Some(Message("viewChangeQuestion.hint"))
      ),
      routes.ViewChangeQuestionController.onSubmit(srn, mode)
    )
}
