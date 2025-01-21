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

import connectors.PSRConnector
import controllers.AssetsHeldController.{form, viewModel}
import forms.YesNoPageFormProvider
import models.DateRange
import pages.AssetsHeldPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import views.html.YesNoPageView

import scala.concurrent.Future
import java.time.LocalDate

class AssetsHeldControllerSpec extends ControllerBaseSpec {
  private val taxYearDates: DateRange = DateRange(LocalDate.of(2020, 4, 6), LocalDate.of(2021, 4, 5))
  private val session: Seq[(String, String)] = Seq(("version", "001"), ("taxYear", "2020-04-06"))

  private val mockPsrConnector = mock[PSRConnector]

  when(mockPsrConnector.updateMemberTransactions(any)(any, any)).thenReturn(Future.unit)

  override val additionalBindings: List[GuiceableModule] = List(bind[PSRConnector].toInstance(mockPsrConnector))

  "AssetsHeldController" - {
    lazy val onPageLoad = controllers.routes.AssetsHeldController.onPageLoad(srn)
    lazy val onSubmit = controllers.routes.AssetsHeldController.onSubmit(srn)

    act.like(renderView(onPageLoad, addToSession = session) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, schemeName, taxYearDates))
    })

    act.like(renderPrePopView(onPageLoad, AssetsHeldPage(srn), addToSession = session, true) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, schemeName, taxYearDates))
    })

    act.like(redirectNextPage(onSubmit, session, "value" -> "true"))

    act.like(redirectNextPage(onSubmit, session, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, session, "value" -> "true"))

    act.like(invalidForm(onSubmit, session))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

  }
}
