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
import config.Constants.maxAccountingPeriods
import config.RefinedTypes.OneToThree
import connectors.PSRConnector
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineV
import forms.YesNoPageFormProvider
import models.{DateRange, NormalMode}
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import pages.accountingperiod.AccountingPeriodPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import views.html.ListView

import scala.concurrent.Future

class AccountingPeriodListControllerSpec extends ControllerBaseSpec {

  private val session: Seq[(String, String)] = Seq(("version", "001"), ("taxYear", "2020-04-06"))
  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private val mockPsrConnector = mock[PSRConnector]
  when(mockPsrConnector.updateAccountingPeriodsDetails(any)(any, any)).thenReturn(Future.unit)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PSRConnector].toInstance(mockPsrConnector)
  )

  "AccountingPeriodListController" - {

    val dateRanges = Gen.listOfN(3, dateRangeGen).sample.value

    val userAnswers =
      dateRanges.zipWithIndex
        .foldLeft(defaultUserAnswers) { case (userAnswers, (range, index)) =>
          val refinedIndex = refineV[OneToThree](index + 1).toOption.value
          userAnswers.set(AccountingPeriodPage(srn, refinedIndex, NormalMode), range).get
        }

    val form = AccountingPeriodListController.form(YesNoPageFormProvider())

    lazy val viewModel = AccountingPeriodListController.viewModel(srn, NormalMode, dateRanges)
    lazy val onPageLoad = routes.AccountingPeriodListController.onPageLoad(srn, NormalMode)
    lazy val onSubmit = routes.AccountingPeriodListController.onSubmit(srn, NormalMode)
    lazy val accountingPeriodPage = controllers.routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)
    when(mockSchemeDateService.returnAccountingPeriods(any)(any, any)).thenReturn(Future.successful(NonEmptyList.fromList(dateRanges)))

    act.like(renderView(onPageLoad, userAnswers, addToSession = session) { implicit app => implicit request =>
      val view = injected[ListView]
      view(form, viewModel)
    })

    act.like(redirectToPage(onPageLoad, accountingPeriodPage, addToSession = session).before {
      when(mockSchemeDateService.returnAccountingPeriods(any)(any, any)).thenReturn(Future.successful(None))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(redirectNextPage(onSubmit, defaultUserAnswers, session, "value" -> "true"))

    act.like(invalidForm(onSubmit, addToSession = session))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "AccountingPeriodListController.viewModel" - {

    "contain the correct number of rows" in {
      val rowsGen = Gen.choose(0, maxAccountingPeriods).flatMap(Gen.listOfN(_, dateRangeGen))

      forAll(srnGen, rowsGen, modeGen) { (srn, rows, mode) =>
        val viewModel = AccountingPeriodListController.viewModel(srn, mode, rows)
        viewModel.page.rows.length mustBe rows.length
      }
    }

    "discard any rows over 3" in {
      val rowsGen = Gen.listOf(dateRangeGen)

      forAll(srnGen, rowsGen, modeGen) { (srn, rows, mode) =>
        val viewModel = AccountingPeriodListController.viewModel(srn, mode, rows)
        viewModel.page.rows.length must be <= 3
      }
    }
  }
}
