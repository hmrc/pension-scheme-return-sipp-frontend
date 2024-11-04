/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.ReturnSubmittedController.viewModel
import models.DateRange
import models.requests.psr.{EtmpPsrStatus, ReportDetails}
import org.scalatestplus.mockito.MockitoSugar
import pages.ReturnSubmittedPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.{ReportDetailsService, SaveService, SchemeDateService}
import views.html.SubmissionView
import generators.GeneratorsObject.convertTryToSuccessOrFailure
import java.time.{LocalDate, LocalDateTime}

class ReturnSubmittedControllerSpec extends ControllerBaseSpec with MockitoSugar {

  private lazy val onPageLoad = routes.ReturnSubmittedController.onPageLoad(srn)

  private val mockReportDetailsService = mock[ReportDetailsService]
  private val mockSaveService = mock[SaveService]
  private val mockDateService = mock[SchemeDateService]

  private val submissionDateTime = LocalDateTime.of(2024, 10, 14, 12, 0)

  private val pensionSchemeEnquiriesUrl =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/pension-scheme-enquiries"
  private val mpsDashboardUrl = s"http://localhost:8204/manage-pension-schemes/pension-scheme-summary/${srn.value}"

  private val taxYearDateRange = DateRange(LocalDate.of(2023, 4, 6), LocalDate.of(2024, 4, 5))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockReportDetailsService, mockSaveService, mockDateService)
    when(mockDateService.now()).thenReturn(submissionDateTime)
  }

  override val additionalBindings: List[GuiceableModule] = List(
    bind[ReportDetailsService].toInstance(mockReportDetailsService),
    bind[SaveService].toInstance(mockSaveService),
    bind[SchemeDateService].toInstance(mockDateService)
  )

  "ReturnSubmittedController" - {

    "onPageLoad" - {

      val userAnswersWithSubmissionDate =
        defaultUserAnswers.set(ReturnSubmittedPage(srn), submissionDateTime).success.value

      act.like(
        renderView(onPageLoad, userAnswersWithSubmissionDate) { implicit app => implicit request =>
          val view = app.injector.instanceOf[SubmissionView]
          view(
            viewModel(
              schemeName = schemeName,
              email = email,
              returnPeriods = NonEmptyList.one(taxYearDateRange),
              submissionDate = submissionDateTime,
              pensionSchemeEnquiriesUrl = pensionSchemeEnquiriesUrl,
              managePensionSchemeDashboardUrl = mpsDashboardUrl
            )
          )
        }.before(
          when(mockReportDetailsService.getReportDetails()(any)).thenReturn(
            ReportDetails(
              pstr = pstr,
              status = EtmpPsrStatus.Compiled,
              periodStart = taxYearDateRange.from,
              periodEnd = taxYearDateRange.to,
              schemeName = Some(schemeName),
              version = None
            )
          )
        )
      )

      act.like(
        journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _)
      )

    }
  }
}
