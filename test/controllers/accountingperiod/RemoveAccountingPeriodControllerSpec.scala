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
import controllers.accountingperiod.RemoveAccountingPeriodController.*
import config.RefinedTypes.Max3
import connectors.PSRConnector
import forms.YesNoPageFormProvider
import models.NormalMode
import play.api.inject.guice.GuiceableModule
import play.api.inject.bind
import services.SchemeDateService
import views.html.YesNoPageView

import scala.concurrent.Future

class RemoveAccountingPeriodControllerSpec extends ControllerBaseSpec {

  private val session: Seq[(String, String)] = Seq(("version", "001"), ("taxYear", "2020-04-06"))
  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  private val mockPsrConnector = mock[PSRConnector]
  when(mockPsrConnector.updateAccountingPeriodsDetails(any)(any, any)).thenReturn(Future.unit)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService),
    bind[PSRConnector].toInstance(mockPsrConnector)
  )

  private lazy val onPageLoad = routes.RemoveAccountingPeriodController.onPageLoad(srn, 1, NormalMode)
  private lazy val onSubmit = routes.RemoveAccountingPeriodController.onSubmit(srn, 1, NormalMode)

  private val period = dateRangeGen.sample.value

  "RemoveSchemeBankAccountController" - {

    when(mockSchemeDateService.returnAccountingPeriods(any)(any, any)).thenReturn(
      Future.successful(Some(NonEmptyList.one(period)))
    )

    act.like(renderView(onPageLoad, addToSession = session) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, Max3.ONE, period, NormalMode)
      )
    })

    act.like(redirectToPage(onPageLoad, controllers.routes.JourneyRecoveryController.onPageLoad()))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
