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

import models.{MinimalDetails, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.{JsPath, Writes}
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import queries.Settable
import services.SaveService

import scala.concurrent.Future

trait ControllerBehaviours { self: ControllerBaseSpec =>

  import Behaviours.*

  private def navigatorBindings(onwardRoute: Call): List[GuiceableModule] =
    List(
      bind[Navigator].qualifiedWith("root").toInstance(FakeNavigator(onwardRoute)),
      bind[Navigator].qualifiedWith("sipp").toInstance(FakeNavigator(onwardRoute))
    )

  def renderView(
    call: => Call,
    userAnswers: UserAnswers = defaultUserAnswers,
    addToSession: Seq[(String, String)] = Seq(),
    minimalDetails: MinimalDetails = defaultMinimalDetails,
    isPsa: Boolean = true
  )(
    view: Application => Request[?] => Html
  ): BehaviourTest =
    "return OK and the correct view".hasBehaviour {
      val appBuilder = applicationBuilder(Some(userAnswers), minimalDetails = minimalDetails, isPsa = isPsa)
      render(appBuilder, call, addToSession)(view)
    }

  def renderViewWithInternalServerError(call: => Call, userAnswers: UserAnswers = defaultUserAnswers)(
    view: Application => Request[?] => Html
  ): BehaviourTest =
    "return INTERNAL_SERVER_ERROR and the correct view".hasBehaviour {
      val appBuilder = applicationBuilder(Some(userAnswers))
      internalServerError(appBuilder, call)(view)
    }

  def renderViewWithRequestEntityTooLarge(call: => Call, userAnswers: UserAnswers = defaultUserAnswers)(
    view: Application => Request[?] => Html
  ): BehaviourTest =
    "return REQUEST_ENTITY_TOO_LARGE and the correct view".hasBehaviour {
      val appBuilder = applicationBuilder(Some(userAnswers))
      requestEntityTooLarge(appBuilder, call)(view)
    }

  def renderPrePopView[A: Writes](call: => Call, page: Settable[A], value: A)(
    view: Application => Request[?] => Html
  ): BehaviourTest =
    renderPrePopView(call, page, value, Seq.empty, defaultUserAnswers)(view)

  def renderPrePopView[A: Writes](call: => Call, page: Settable[A], addToSession: Seq[(String, String)], value: A)(
    view: Application => Request[?] => Html
  ): BehaviourTest =
    renderPrePopView(call, page, value, addToSession, defaultUserAnswers)(view)

  def renderPrePopView[A: Writes](call: => Call, page: Settable[A], value: A, userAnswers: UserAnswers)(
    view: Application => Request[?] => Html
  ): BehaviourTest =
    renderPrePopView(call, page, value, Seq.empty, userAnswers)(view)

  def renderPrePopView[A: Writes](
    call: => Call,
    page: Settable[A],
    value: A,
    addToSession: Seq[(String, String)],
    userAnswers: UserAnswers
  )(
    view: Application => Request[?] => Html
  ): BehaviourTest =
    "return OK and the correct pre-populated view for a GET".hasBehaviour {
      val appBuilder = applicationBuilder(Some(userAnswers.set(page, value).success.value))
      render(appBuilder, call, addToSession)(view)
    }

  def redirectWhenCacheEmpty(call: => Call, nextPage: => Call): BehaviourTest =
    s"redirect to $nextPage when cache empty".hasBehaviour {
      running(_ => applicationBuilder(userAnswers = Some(emptyUserAnswers))) { app =>
        val request = FakeRequest(call)
        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe nextPage.url
      }
    }

  def journeyRecoveryPage(
    call: => Call,
    userAnswers: Option[UserAnswers],
    minimalDetails: MinimalDetails = defaultMinimalDetails,
    addToSession: Seq[(String, String)] = Seq()
  ): BehaviourTest =
    s"must redirect to Journey Recovery if no existing data is found".hasBehaviour {
      val application = applicationBuilder(userAnswers = userAnswers, minimalDetails = minimalDetails).build()

      running(application) {

        val result = route(application, FakeRequest(call).withSession(addToSession*)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  def notFound(call: => Call, userAnswers: UserAnswers): BehaviourTest =
    "must return 404 NOT_FOUND if no existing data is found".hasBehaviour {
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      running(application) {
        val result = route(application, FakeRequest(call)).value
        status(result) mustEqual NOT_FOUND
      }
    }

  def journeyRecoveryPage(call: => Call): BehaviourTest =
    journeyRecoveryPage(call, None)

  private def render(appBuilder: GuiceApplicationBuilder, call: => Call, addToSession: Seq[(String, String)])(
    view: Application => Request[?] => Html
  ): Unit =
    running(_ => appBuilder) { app =>
      val request = FakeRequest(call).withSession(addToSession*)
      val result = route(app, request).value
      val expectedView = view(app)(request)

      status(result) mustEqual OK
      contentAsString(result) mustEqual expectedView.body
    }

  private def internalServerError(appBuilder: GuiceApplicationBuilder, call: => Call)(
    view: Application => Request[?] => Html
  ): Unit =
    running(_ => appBuilder) { app =>
      val request = FakeRequest(call)
      val result = route(app, request).value
      val expectedView = view(app)(request)

      status(result) mustEqual INTERNAL_SERVER_ERROR
      contentAsString(result) mustEqual expectedView.body
    }

  private def requestEntityTooLarge(appBuilder: GuiceApplicationBuilder, call: => Call)(
    view: Application => Request[?] => Html
  ): Unit =
    running(_ => appBuilder) { app =>
      val request = FakeRequest(call)
      val result = route(app, request).value
      val expectedView = view(app)(request)

      status(result) mustEqual REQUEST_ENTITY_TOO_LARGE
      contentAsString(result) mustEqual expectedView.body
    }

  def invalidForm(
    call: => Call,
    userAnswers: UserAnswers,
    addToSession: Seq[(String, String)],
    isPsa: Boolean,
    form: (String, String)*
  ): BehaviourTest =
    s"return BAD_REQUEST for a POST with invalid form data $form".hasBehaviour {
      val appBuilder = applicationBuilder(Some(userAnswers), isPsa = isPsa)

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withSession(addToSession*).withFormUrlEncodedBody(form*)
        val result = route(app, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

  def invalidForm(
    call1: => Call,
    userAnswers1: UserAnswers,
    addToSession1: Seq[(String, String)],
    form1: (String, String)*
  ): BehaviourTest =
    invalidForm(call = call1, userAnswers = userAnswers1, addToSession = addToSession1, isPsa = true, form = form1*)

  def invalidForm(call: => Call, userAnswers: UserAnswers, form: (String, String)*): BehaviourTest =
    invalidForm(call, userAnswers, Seq.empty, form*)

  def invalidForm(call: => Call, form: (String, String)*): BehaviourTest =
    invalidForm(call, defaultUserAnswers, Seq.empty, form*)

  def invalidForm(call: => Call, addToSession: Seq[(String, String)], form: (String, String)*): BehaviourTest =
    invalidForm(call, defaultUserAnswers, addToSession, form*)

  def redirectNextPage(
    call: => Call,
    userAnswers: UserAnswers,
    addToSession: Seq[(String, String)],
    form: (String, String)*
  ): BehaviourTest =
    s"redirect to the next page with form ${form.toList}".hasBehaviour {
      val appBuilder = applicationBuilder(Some(userAnswers)).overrides(
        navigatorBindings(testOnwardRoute)*
      )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withSession(addToSession*).withFormUrlEncodedBody(form*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url
      }
    }

  def redirectNextPage(call: => Call, userAnswers: UserAnswers, form: (String, String)*): BehaviourTest =
    redirectNextPage(call, userAnswers, Seq.empty, form*)

  def redirectNextPage(call: => Call, form: (String, String)*): BehaviourTest =
    redirectNextPage(call, defaultUserAnswers, Seq.empty, form*)

  def redirectNextPage(call: => Call, addToSession: Seq[(String, String)], form: (String, String)*): BehaviourTest =
    redirectNextPage(call, defaultUserAnswers, addToSession, form*)

  def redirectToPage(
    call: => Call,
    page: => Call,
    userAnswers: UserAnswers,
    addToSession: Seq[(String, String)],
    form: (String, String)*
  ): BehaviourTest =
    s"redirect to page with form $form".hasBehaviour {
      val appBuilder = applicationBuilder(Some(userAnswers))

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form*).withSession(addToSession*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        val location = redirectLocation(result)
        location.value mustEqual page.url
      }
    }

  def redirectToPage(call: => Call, page: => Call, form: (String, String)*): BehaviourTest =
    redirectToPage(call, page, defaultUserAnswers, Nil, form*)

  def redirectToPage(
    call: => Call,
    page: => Call,
    addToSession: Seq[(String, String)],
    form: (String, String)*
  ): BehaviourTest =
    redirectToPage(call, page, defaultUserAnswers, addToSession, form*)

  def runForSaveAndContinue(
    call: => Call,
    saveService: SaveService,
    userAnswers: UserAnswers,
    expectedDataPath: Option[JsPath],
    userDetailsCaptor: ArgumentCaptor[UserAnswers],
    verifySaveService: => Unit,
    form: (String, String)*
  ): Unit = {
    val appBuilder = applicationBuilder(Some(userAnswers))
      .overrides(bind[SaveService].toInstance(saveService))
      .overrides(navigatorBindings(testOnwardRoute)*)

    running(_ => appBuilder) { app =>
      val request = FakeRequest(call).withFormUrlEncodedBody(form*)
      val result = route(app, request).value
      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual testOnwardRoute.url
      verifySaveService
      if (expectedDataPath.nonEmpty) {
        val data = userDetailsCaptor.getValue.data.decryptedValue
        assert(expectedDataPath.get(data).nonEmpty)
      }
    }
  }

  def updateAndSaveAndContinue(
    call: => Call,
    userAnswers: UserAnswers,
    expectedDataPath: Option[JsPath],
    form: (String, String)*
  ): BehaviourTest =
    s"update and save data and continue to next page with ${form.toList}".hasBehaviour {
      val saveService = mock[SaveService]
      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(saveService.updateAndSave(captor.capture(), any)(any, any)(using any, any, any, any))
        .thenReturn(Future.successful(userAnswers))

      runForSaveAndContinue(
        call,
        saveService,
        userAnswers,
        expectedDataPath,
        captor,
        verify(saveService, times(1)).updateAndSave(captor.capture(), any)(any, any)(using any, any, any, any),
        form*
      )
    }

  def updateAndSaveAndContinue(
    call: => Call,
    userAnswers: UserAnswers,
    form: (String, String)*
  ): BehaviourTest = updateAndSaveAndContinue(call, userAnswers, None, form*)

  def setAndSaveAndContinue(
    call: => Call,
    userAnswers: UserAnswers,
    expectedDataPath: Option[JsPath],
    form: (String, String)*
  ): BehaviourTest =
    s"set and save data and continue to next page with ${form.toList}".hasBehaviour {
      val saveService = mock[SaveService]
      val userDetailsCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(saveService.setAndSave(userDetailsCaptor.capture(), any, any)(using any, any, any))
        .thenReturn(Future.successful(userAnswers))

      runForSaveAndContinue(
        call,
        saveService,
        userAnswers,
        expectedDataPath,
        userDetailsCaptor,
        verify(saveService, times(1)).setAndSave(any, any, any)(using any, any, any): Unit,
        form*
      )
    }

  def setAndSaveAndContinue(call: => Call, userAnswers: UserAnswers, form: (String, String)*): BehaviourTest =
    setAndSaveAndContinue(call, userAnswers, None, form*)

  def saveAndContinue(
    call: => Call,
    userAnswers: UserAnswers,
    expectedDataPath: Option[JsPath],
    addToSession: Seq[(String, String)],
    form: (String, String)*
  ): BehaviourTest =
    s"save data and continue to next page with ${form.toList}".hasBehaviour {

      val saveService = mock[SaveService]
      val userDetailsCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(saveService.save(userDetailsCaptor.capture())(any, any)).thenReturn(Future.successful(()))

      val appBuilder = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[SaveService].toInstance(saveService)
        )
        .overrides(
          navigatorBindings(testOnwardRoute)*
        )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withSession(addToSession*).withFormUrlEncodedBody(form*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url

        verify(saveService, times(1)).save(any)(any, any)
        if (expectedDataPath.nonEmpty) {
          val data = userDetailsCaptor.getValue.data.decryptedValue
          assert(expectedDataPath.get(data).nonEmpty)
        }
      }
    }

  def saveAndContinue(call: => Call, form: (String, String)*): BehaviourTest =
    saveAndContinue(call, defaultUserAnswers, None, Seq.empty, form*)

  def saveAndContinue(call: => Call, addToSession: Seq[(String, String)], form: (String, String)*): BehaviourTest =
    saveAndContinue(call, defaultUserAnswers, None, addToSession, form*)

  def saveAndContinue(call: => Call, userAnswers: UserAnswers, form: (String, String)*): BehaviourTest =
    saveAndContinue(call, userAnswers, None, Seq.empty, form*)

  def saveAndContinue(
    call: => Call,
    userAnswers: UserAnswers,
    addToSession: Seq[(String, String)],
    form: (String, String)*
  ): BehaviourTest =
    saveAndContinue(call, userAnswers, None, addToSession, form*)

  def saveAndContinue(call: => Call, jsPathOption: Option[JsPath], form: (String, String)*): BehaviourTest =
    saveAndContinue(call, defaultUserAnswers, jsPathOption, Seq.empty, form*)

  def continueNoSave(call: => Call, userAnswers: UserAnswers, form: (String, String)*): BehaviourTest =
    "continue to the next page without saving".hasBehaviour {

      val saveService = mock[SaveService]
      when(saveService.save(any)(any, any)).thenReturn(Future.failed(Exception("Unreachable code")))

      val appBuilder = applicationBuilder(Some(userAnswers))
        .overrides(
          bind[SaveService].toInstance(saveService)
        )
        .overrides(
          navigatorBindings(testOnwardRoute)*
        )

      running(_ => appBuilder) { app =>
        val request = FakeRequest(call).withFormUrlEncodedBody(form*)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url

        verify(saveService, never).save(any)(any, any)
      }
    }

  def continueNoSave(call: => Call, form: (String, String)*): BehaviourTest =
    continueNoSave(call, defaultUserAnswers, form*)

  def agreeAndContinue(
    call: => Call,
    userAnswers: UserAnswers,
    addToSession: Seq[(String, String)],
    isPsa: Boolean,
    form: (String, String)*
  ): BehaviourTest =
    "agree and continue to next page".hasBehaviour {

      val appBuilder = applicationBuilder(Some(userAnswers), isPsa = isPsa)
        .overrides(
          navigatorBindings(testOnwardRoute)*
        )

      running(_ => appBuilder) { app =>
        val result = route(
          app,
          FakeRequest(call)
            .withFormUrlEncodedBody(form*)
            .withSession(addToSession*)
        ).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url

      }
    }

  def continue(call: => Call, userAnswers: UserAnswers): BehaviourTest =
    "continue to next page".hasBehaviour {

      val appBuilder = applicationBuilder(Some(userAnswers))
        .overrides(
          navigatorBindings(testOnwardRoute)*
        )

      running(_ => appBuilder) { app =>
        val result = route(app, FakeRequest(call)).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testOnwardRoute.url

      }
    }

  def continue(call: => Call): BehaviourTest =
    continue(call, defaultUserAnswers)

  def streamContent(call: => Call, userAnswers: UserAnswers = defaultUserAnswers): BehaviourTest =
    "Download streamed content".hasBehaviour {
      val appBuilder = applicationBuilder(Some(userAnswers))
      running(_ => appBuilder) { app =>
        val result = route(app, FakeRequest(call)).value
        status(result) mustEqual OK
        val headersMap = headers(result)
        headersMap("Content-Disposition") must startWith("attachment;")
      }
    }
}
