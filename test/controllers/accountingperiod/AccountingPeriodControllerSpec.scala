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

package controllers.accountingperiod

import cats.data.NonEmptyList
import controllers.ControllerBaseSpec
import config.RefinedTypes.Max3
import forms.DateRangeFormProvider
import models.{DateRange, NormalMode}
import pages.WhichTaxYearPage
import pages.accountingperiod.AccountingPeriodPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsPath
import services.{SchemeDateService, TaxYearService}
import uk.gov.hmrc.time.TaxYear
import views.html.DateRangeView

import scala.concurrent.Future

class AccountingPeriodControllerSpec extends ControllerBaseSpec {

  val taxYear: TaxYear = TaxYear(2021)
  private val session: Seq[(String, String)] = Seq(("version", "001"), ("taxYear", "2021-04-06"))
  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  private val mockTaxYearService = mock[TaxYearService]
  private val userAnswers = defaultUserAnswers.unsafeSet(WhichTaxYearPage(srn), dateRange)

  override def beforeEach(): Unit = reset(mockTaxYearService)

  "AccountingPeriodController" - {

    val form = AccountingPeriodController.form(DateRangeFormProvider(), taxYear, List())

    val rangeGen = dateRangeWithinRangeGen(dateRange)
    val dateRangeData = rangeGen.sample.value
    val otherDateRangeData = rangeGen.sample.value

    lazy val viewModel = AccountingPeriodController.viewModel(srn, Nil, Max3.ONE, NormalMode)

    lazy val onPageLoad = routes.AccountingPeriodController.onPageLoad(srn, 1, NormalMode)
    lazy val onSubmit = routes.AccountingPeriodController.onSubmit(srn, 1, NormalMode)

    when(mockSchemeDateService.returnAccountingPeriods(any)(any, any)).thenReturn(
      Future.successful(Some(NonEmptyList.one(dateRangeData)))
    )

    act.like(renderView(onPageLoad, userAnswers, addToSession = session) { implicit app => implicit request =>
      val view = injected[DateRangeView]
      view(form, viewModel)
    }.before(when(mockSchemeDateService.returnAccountingPeriods(any)(any, any)).thenReturn(
      Future.successful(None))
    ))

    act.like(
      renderPrePopView(onPageLoad, AccountingPeriodPage(srn, Max3.ONE, NormalMode), dateRangeData, session, userAnswers) {
        implicit app => implicit request =>
          val view = injected[DateRangeView]
          val model = AccountingPeriodController.viewModel(srn, List(dateRangeData), Max3.ONE, NormalMode)
          view(form.fill(dateRangeData), model)
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, userAnswers, session, formData(form, dateRangeData)*))

    act.like(invalidForm(call = onSubmit, userAnswers = userAnswers, addToSession = session))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

    "allow accounting period to be updated" - {
      act.like(
        saveAndContinue(
          onSubmit,
          userAnswers,
          Some(JsPath \ "accountingPeriods"),
          Seq.empty,
          formData(form, dateRangeData)*
        )
      )
    }

    "return a 400 if range intersects" - {
      val userAnswers =
        emptyUserAnswers
          .set(WhichTaxYearPage(srn), dateRange)
          .get
          .set(AccountingPeriodPage(srn, Max3.ONE, NormalMode), otherDateRangeData)
          .get
          .set(AccountingPeriodPage(srn, Max3.TWO, NormalMode), dateRangeData)
          .get

      act.like(invalidForm(onSubmit, userAnswers, formData(form, dateRangeData)*))
    }
  }
}
