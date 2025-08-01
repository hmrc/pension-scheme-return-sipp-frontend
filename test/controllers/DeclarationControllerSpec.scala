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
import models.DateRange
import models.backend.responses.PsrAssetCountsResponse
import models.requests.PsrSubmissionRequest.PsrSubmittedResponse
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.{FakeTaxYearService, SchemeDetailsService, TaxYearService}
import uk.gov.hmrc.time.TaxYear
import views.html.DeclarationPageView
import forms.TextFormProvider

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerBaseSpec {

  private val mockSchemeDetailsService = mock[SchemeDetailsService]
  private val mockPsrConnector = mock[PSRConnector]
  private val taxYear = TaxYear(date.sample.value.getYear)
  private val assetCounts = PsrAssetCountsResponse(
    interestInLandOrPropertyCount = 1,
    landArmsLengthCount = 1,
    assetsFromConnectedPartyCount = 1,
    tangibleMoveablePropertyCount = 1,
    outstandingLoansCount = 1,
    unquotedSharesCount = 1
  )

  override val additionalBindings: List[GuiceableModule] =
    List(
      bind[SchemeDetailsService].toInstance(mockSchemeDetailsService),
      bind[PSRConnector].toInstance(mockPsrConnector),
      bind[TaxYearService].toInstance(FakeTaxYearService(taxYear.starts))
    )

  "DeclarationController" - {

    val minimalSchemeDetails = minimalSchemeDetailsGen.sample.value
    when(mockSchemeDetailsService.getMinimalSchemeDetails(any, any)(any, any))
      .thenReturn(Future.successful(Some(minimalSchemeDetails)))
    when(mockPsrConnector.submitPsr(any, any, any, any, any, any, any, any)(any, any))
      .thenReturn(Future.successful(PsrSubmittedResponse(emailSent = true)))
    when(mockPsrConnector.getPsrAssetCounts(any, any, any, any)(any, any))
      .thenReturn(Future.successful(Some(assetCounts)))

    lazy val viewModel =
      DeclarationController.viewModel(
        srn,
        psaId,
        minimalSchemeDetails,
        Some(assetCounts),
        None,
        Some(taxYear.starts.toString),
        Some("001"),
        DateRange.from(taxYear)
      )
    lazy val onPageLoad = routes.DeclarationController.onPageLoad(srn, None)
    lazy val onSubmit = routes.DeclarationController.onSubmit(srn, None)

    act.like(
      renderView(
        onPageLoad,
        addToSession = Seq(
          ("fbNumber", fbNumber),
          ("taxYear", taxYear.starts.toString),
          ("version", "001")
        )
      ) { implicit app => implicit request =>
        val view = injected[DeclarationPageView]
        view(DeclarationController.form(TextFormProvider(), defaultSchemeDetails.authorisingPSAID), viewModel)
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      agreeAndContinue(
        onSubmit,
        userAnswers = defaultUserAnswers,
        addToSession = Seq(
          ("fbNumber", fbNumber),
          ("taxYear", "2024-04-04"),
          ("version", "001")
        ),
        isPsa = true
      ).withName("submit as PSA")
    )

    act.like(
      agreeAndContinue(
        call = onSubmit,
        userAnswers = defaultUserAnswers,
        addToSession = Seq(
          ("fbNumber", fbNumber),
          ("taxYear", "2024-04-04"),
          ("version", "001")
        ),
        isPsa = false,
        "value" -> defaultSchemeDetails.authorisingPSAID.value
      ).withName("submit as PSP")
    )

    act.like(
      invalidForm(
        call = onSubmit,
        userAnswers = defaultUserAnswers,
        addToSession = Seq.empty,
        isPsa = false,
        form = "value" -> "invalidPsaId"
      ).withName("submit invalid form as PSP")
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
