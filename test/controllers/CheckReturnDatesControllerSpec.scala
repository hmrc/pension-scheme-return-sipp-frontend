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

import cats.data.NonEmptyList
import forms.YesNoPageFormProvider
import models.{MinimalSchemeDetails, NormalMode}
import org.mockito.stubbing.OngoingStubbing
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.{CheckReturnDatesPage, WhichTaxYearPage}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.{AnyContent, Call}
import services.{SchemeDateService, SchemeDetailsService}
import utils.DateTimeUtils
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits.*
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView
import models.requests.FormBundleOrTaxYearRequest
import play.api.Application
import play.api.test.Helpers._
import play.api.test.FakeRequest

import scala.concurrent.Future

class CheckReturnDatesControllerSpec extends ControllerBaseSpec with ScalaCheckPropertyChecks { self =>

  private val mockSchemeDetailsService = mock[SchemeDetailsService]
  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private val session: Seq[(String, String)] = Seq(("version", "001"), ("taxYear", "2020-04-06"))

  override val additionalBindings: List[GuiceableModule] =
    List(
      bind[SchemeDetailsService].toInstance(mockSchemeDetailsService),
      bind[SchemeDateService].toInstance(mockSchemeDateService)
    )

  private val userAnswers = defaultUserAnswers.unsafeSet(WhichTaxYearPage(srn), dateRange)

  private def runWithApp(testCode: Application => Unit): Unit = {
    val app = applicationBuilder().build()
    running(app) {
      testCode(app)
    }
  }

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val checkReturnDatesRoute: String = routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
  lazy val onPageLoad: Call = routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)
  lazy val onSubmit: Call = routes.CheckReturnDatesController.onSubmit(srn, NormalMode)

  "CheckReturnDates.viewModel" - {

    val minimalSchemeDetails = minimalSchemeDetailsGen.sample.value
    val dateRanges = Gen.listOfN(3, dateRangeGen).sample.value

    when(mockSchemeDateService.returnAccountingPeriods(any[FormBundleOrTaxYearRequest[AnyContent]])(any, any, any))
      .thenReturn(Future.successful(NonEmptyList.fromList(dateRanges)))

    "contain correct title key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.title mustBe Message("checkReturnDates.title")
      }
    }

    "contain correct heading key" in {
      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.heading mustBe Message("checkReturnDates.heading")
      }
    }

    "contain from date when it is after open date" in {

      val updatedDetails = minimalSchemeDetails.copy(openDate = Some(earliestDate), windUpDate = None)

      forAll(date, date) { (fromDate, toDate) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, toDate, updatedDetails)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe Some(
          ParagraphMessage(Message("checkReturnDates.description", formattedFromDate, formattedToDate))
        )
      }
    }

    "contain open date when it is after from date" in {

      val detailsGen = minimalSchemeDetailsGen.map(_.copy(windUpDate = None))

      forAll(detailsGen, date) { (details, toDate) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, earliestDate, toDate, details)
        val formattedFromDate = DateTimeUtils.formatHtml(details.openDate.getOrElse(earliestDate))
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe Some(
          ParagraphMessage(Message("checkReturnDates.description", formattedFromDate, formattedToDate))
        )
      }
    }

    "contain to date when it is before wind up date" in {

      val updatedDetails = minimalSchemeDetails.copy(openDate = None, windUpDate = Some(latestDate))

      forAll(date, date) { (fromDate, toDate) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, toDate, updatedDetails)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe Some(
          ParagraphMessage(Message("checkReturnDates.description", formattedFromDate, formattedToDate))
        )
      }
    }

    "contain wind up date when it is before to date" in {

      val detailsGen = minimalSchemeDetailsGen.map(_.copy(openDate = None))

      forAll(detailsGen, date) { (details, fromDate) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, latestDate, details)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(details.windUpDate.getOrElse(latestDate))

        viewModel.description mustBe Some(
          ParagraphMessage(Message("checkReturnDates.description", formattedFromDate, formattedToDate))
        )
      }
    }

    "contain correct legend key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.page.legend.value mustBe Message("checkReturnDates.legend")
      }
    }

    "populate the onSubmit with srn and mode" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.onSubmit mustBe routes.CheckReturnDatesController.onSubmit(srn, mode)
      }
    }
  }

  "CheckReturnDates Controller" - {

    val minimalSchemeDetails = minimalSchemeDetailsGen.sample.value
    lazy val viewModel: FormPageViewModel[YesNoPageViewModel] =
      CheckReturnDatesController.viewModel(
        srn,
        NormalMode,
        dateRange.from,
        dateRange.to,
        minimalSchemeDetails
      )

    val form = CheckReturnDatesController.form(YesNoPageFormProvider())

    act.like(renderView(onPageLoad, userAnswers, session) { implicit app => implicit request =>
      val view = injected[YesNoPageView]
      view(form, viewModel)
    }.before(setSchemeDetails(Some(minimalSchemeDetails))))

    act.like(renderPrePopView(onPageLoad, CheckReturnDatesPage(srn), true, session, userAnswers) {
      implicit app => implicit request =>
        val view = injected[YesNoPageView]
        view(form.fill(true), viewModel)
    }.before(setSchemeDetails(Some(minimalSchemeDetails))))

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(setSchemeDetails(Some(minimalSchemeDetails)))
        .updateName("onPageLoad" + _)
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(setSchemeDetails(None))
        .updateName(_ => "onPageLoad redirect to journey recovery page when scheme date not found")
    )

    act.like(
      saveAndContinue(onSubmit, userAnswers, session, formData(form, true)*)
        .before(setSchemeDetails(Some(minimalSchemeDetails)))
    )

    act.like(invalidForm(onSubmit, userAnswers, session).before(setSchemeDetails(Some(minimalSchemeDetails))))

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(setSchemeDetails(Some(minimalSchemeDetails)))
        .updateName("onSubmit" + _)
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(setSchemeDetails(None))
        .updateName(_ => "onSubmit redirect to journey recovery page when scheme date not found")
    )
  }

  "redirect to journey recovery page when refinement fails in setCachedDateRanges" in {
    val minimalSchemeDetails = minimalSchemeDetailsGen.sample.value
    val badDateRange = dateRangeGen.sample.value

    when(mockSchemeDateService.returnAccountingPeriods(any[FormBundleOrTaxYearRequest[AnyContent]])(any, any, any))
      .thenReturn(Future.successful(Some(List.fill(4)(badDateRange))))

    setSchemeDetails(Some(minimalSchemeDetails))

    runWithApp { implicit app =>
      val request = FakeRequest(POST, onSubmit.url)
        .withFormUrlEncodedBody("value" -> "false")
        .withSession(session*)

      val result = route(app, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }

  "redirect to journey recovery page when more than 3 date ranges cause refineV failure" in {
    val minimalSchemeDetails = minimalSchemeDetailsGen.sample.value
    val tooManyDateRanges = List.fill(4)(dateRangeGen.sample.value)

    when(mockSchemeDateService.returnAccountingPeriods(any[FormBundleOrTaxYearRequest[AnyContent]])(any, any, any))
      .thenReturn(Future.successful(Some(tooManyDateRanges)))

    setSchemeDetails(Some(minimalSchemeDetails))

    runWithApp { implicit app =>
      val request = FakeRequest(POST, onSubmit.url)
        .withFormUrlEncodedBody("value" -> "false")
        .withSession(session*)

      val result = route(app, request).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }

  def setSchemeDetails(
    schemeDetails: Option[MinimalSchemeDetails]
  ): OngoingStubbing[Future[Option[MinimalSchemeDetails]]] =
    when(mockSchemeDetailsService.getMinimalSchemeDetails(any, any)(any, any))
      .thenReturn(Future.successful(schemeDetails))
}
