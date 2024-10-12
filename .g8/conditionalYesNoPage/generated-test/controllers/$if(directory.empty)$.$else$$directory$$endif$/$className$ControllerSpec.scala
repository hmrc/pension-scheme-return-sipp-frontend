$if(directory.empty)$
package controllers
$else$
package controllers.$directory$
$endif$

import models.*
import navigation.{FakeNavigator, Navigator}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import forms.*
import views.html.*
import controllers.ControllerBaseSpec
import $className;format="cap"$Controller.*
$if(directory.empty)$
import pages.$className$Page
$else$
import pages.$directory$.$className$Page
$endif$
$if(!index.empty)$
import config.RefinedTypes.*
$endif$

import scala.concurrent.Future

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec {

  $if(index.empty)$
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)
  $else$
  private val index = refineUnsafe[Int, $index$.Refined](1)
  private lazy val onPageLoad = routes.$className; format = "cap" $Controller.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.$className; format = "cap" $Controller.onSubmit(srn, index, NormalMode)
  $endif$

  "$className;format="cap"$Controller" - {

    $if(index.empty)$
    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, $className;format="cap"$Page(srn), ConditionalYesNo.yes[String, String]("test")) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView].apply(form(injected[YesNoPageFormProvider]).fill(Right("test")), viewModel(srn, NormalMode))
    })
    $else$
    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, $className;format="cap"$Page(srn, index), ConditionalYesNo.yes[String, String]("test")) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView].apply(form(injected[YesNoPageFormProvider]).fill(Right("test")), viewModel(srn, index, NormalMode))
    })
    $endif$

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> "test"))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> "test"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
