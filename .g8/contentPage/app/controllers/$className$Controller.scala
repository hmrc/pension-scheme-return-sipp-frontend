package controllers

import controllers.actions.*
import play.api.i18n.*
import play.api.mvc.*
import navigation.Navigator
import models.Mode
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.{ContentPageViewModel, FormPageViewModel}
import viewmodels.implicits.*
import viewmodels.DisplayMessage.*
import views.html.ContentPageView
import $className$Controller.*
import pages.$className;format="cap"$Page
import models.SchemeId.Srn

import javax.inject.{Inject, Named}

class $className;format="cap"$Controller @Inject()(
   override val messagesApi: MessagesApi,
   navigator: Navigator,
   identifyAndRequireData: IdentifyAndRequireData,
   val controllerComponents: MessagesControllerComponents,
   view: ContentPageView
) extends FrontendBaseController with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
      Ok(view(viewModel(srn, mode)))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
      Redirect(navigator.nextPage($className;format="cap"$Page(srn), mode, request.userAnswers))
  }
}

object $className;format="cap"$Controller {
  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[ContentPageViewModel] = FormPageViewModel[ContentPageViewModel](
    title = "$className;format="decap"$.title",
    heading = "$className;format="decap"$.heading",
    description = Some(ParagraphMessage("$className;format="decap"$.paragraph")),
    page = ContentPageViewModel(isLargeHeading = true),
    refresh = None,
    buttonText = "site.continue",
    onSubmit = routes.$className$Controller.onSubmit(srn, mode)
  )
}
