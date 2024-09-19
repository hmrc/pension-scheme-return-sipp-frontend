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
import org.mockito.ArgumentMatchers.any
import pages.AssetsHeldPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.{FakeTaxYearService, TaxYearService}
import uk.gov.hmrc.time.TaxYear
import views.html.YesNoPageView

import scala.concurrent.Future

class AssetsHeldControllerSpec extends ControllerBaseSpec {
  private val taxYear = TaxYear(date.sample.value.getYear)
  private val mockPsrConnector = mock[PSRConnector]

  when(mockPsrConnector.createEmptyPsr(any())(any()))
    .thenReturn(Future.unit)

  override val additionalBindings: List[GuiceableModule] =
    List(
      bind[TaxYearService].toInstance(new FakeTaxYearService(taxYear.starts)),
        bind[PSRConnector].toInstance((mockPsrConnector)),
    )

  "AssetsHeldController" - {
    lazy val onPageLoad =
      controllers.routes.AssetsHeldController.onPageLoad(srn)
    lazy val onSubmit =
      controllers.routes.AssetsHeldController.onSubmit(srn)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, schemeName, taxYear))
    })

    act.like(renderPrePopView(onPageLoad, AssetsHeldPage(srn), true) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]).fill(true),
          viewModel(srn, schemeName, taxYear)
        )
    })

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

  }
}
