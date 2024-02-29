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

package controllers.landorproperty

import controllers.actions._
import controllers.landorproperty.LandOrPropertyContributionsController.viewModel
import forms.YesNoPageFormProvider
import models.Journey.InterestInLandOrProperty
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.landorproperty.LandOrPropertyContributionsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class LandOrPropertyContributionsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = LandOrPropertyContributionsController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val preparedForm = request.userAnswers.fillForm(LandOrPropertyContributionsPage(srn), form)
    Ok(view(preparedForm, viewModel(srn, mode, request.schemeDetails.schemeName)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, viewModel(srn, mode, request.schemeDetails.schemeName)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(LandOrPropertyContributionsPage(srn), value))
            _ <- saveService.save(updatedAnswers)
            redirectTo <- Future
              .successful(Redirect(navigator.nextPage(LandOrPropertyContributionsPage(srn), mode, updatedAnswers)))
          } yield redirectTo
      )
  }
}

object LandOrPropertyContributionsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    s"${InterestInLandOrProperty.messagePrefix}.held.error.required"
  )

  def viewModel(srn: Srn, mode: Mode, schemeName: String): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      s"${InterestInLandOrProperty.messagePrefix}.held.title",
      Message(s"${InterestInLandOrProperty.messagePrefix}.held.heading", schemeName),
      controllers.landorproperty.routes.LandOrPropertyContributionsController.onSubmit(srn, mode)
    )
}
