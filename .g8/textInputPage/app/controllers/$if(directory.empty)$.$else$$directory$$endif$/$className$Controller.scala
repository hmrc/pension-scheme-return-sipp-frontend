$if(directory.empty)$
package controllers
$else$
package controllers.$directory$
$endif$

import controllers.actions.*
import forms.TextFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import controllers.PSRController
import viewmodels.models.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TextInputView
import services.SaveService
import $className;format="cap"$Controller.*
import viewmodels.implicits.*

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
$if(directory.empty)$
import pages.$className$Page
$else$
import pages.$directory$.$className$Page
$endif$
$if(!index.empty)$
import config.RefinedTypes.*
$endif$

class $className;format="cap"$Controller @Inject()(
   override val messagesApi: MessagesApi,
   saveService: SaveService,
   navigator: Navigator,
   identifyAndRequireData: IdentifyAndRequireData,
   formProvider: TextFormProvider,
   val controllerComponents: MessagesControllerComponents,
   view: TextInputView
)(implicit ec: ExecutionContext) extends PSRController {

  private val form = $className;format="cap"$Controller.form(formProvider)

  $if(index.empty)$
  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm($className$Page(srn), form)
      Ok(view(preparedForm, viewModel(srn, mode)))
  }
  $else$
  def onPageLoad(srn: Srn, index: $index$, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm($className$Page(srn, index), form)
      Ok(view(preparedForm, viewModel(srn, index, mode)))
  }
  $endif$

  $if(index.empty)$
  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn), value))
            _              <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage($className$Page(srn), mode, updatedAnswers))
      )
  }
  $else$
  def onSubmit(srn: Srn, index: $index$, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode)))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set($className$Page(srn, index), value))
            _              <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage($className$Page(srn, index), mode, updatedAnswers))
      )
  }
  $endif$
}

object $className;format="cap"$Controller {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "$className;format="decap"$.error.required",
    "$className;format="decap"$.error.tooLong",
    "$className;format="decap"$.error.invalid"
  )

  $if(index.empty)$
  def viewModel(srn: Srn, mode: Mode): FormPageViewModel[TextInputViewModel] = FormPageViewModel(
  $else$
  def viewModel(srn: Srn, index: $index$, mode: Mode): FormPageViewModel[TextInputViewModel] = FormPageViewModel(
  $endif$
    "$className;format="decap"$.title",
    "$className;format="decap"$.heading",
    TextInputViewModel(),
    $if(index.empty)$
    routes.$className;format="cap"$Controller.onSubmit(srn, mode)
    $else$
    routes.$className;format="cap"$Controller.onSubmit(srn, index, mode)
    $endif$
  )
}