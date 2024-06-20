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
import controllers.ReturnSubmittedController.viewModel
import models.DateRange
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.TaxYearService
import viewmodels.models.SubmissionViewModel

import java.time.LocalDateTime

class ReturnSubmittedControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ReturnSubmittedController.onPageLoad(srn)

  private val returnPeriod1 = dateRangeGen.sample.value
  private val returnPeriod2 = dateRangeGen.sample.value
  private val returnPeriod3 = dateRangeGen.sample.value
 val submissionDateTime = localDateTimeGen.sample.value

  private val pensionSchemeEnquiriesUrl =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/pension-scheme-enquiries"
  private val mpsDashboardUrl = "http://localhost:8204/manage-pension-schemes/overview"


  private val mockTaxYearService = mock[TaxYearService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[TaxYearService].toInstance(mockTaxYearService)
  )

  override def beforeEach(): Unit = {
    reset(mockTaxYearService)
    when(mockTaxYearService.current).thenReturn(defaultTaxYear)
  }


  "ReturnSubmittedController" - {

    List(
      ("tax year return period", NonEmptyList.one(returnPeriod1)),
      ("single accounting period", NonEmptyList.one(returnPeriod1)),
      ("multiple accounting periods", NonEmptyList.of(returnPeriod1, returnPeriod2, returnPeriod3))
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))
  }

 def buildViewModel(
                              returnPeriods: NonEmptyList[DateRange],
                              submissionDate: LocalDateTime
                            ): SubmissionViewModel =
    viewModel(
      schemeName,
      email,
      returnPeriods,
      submissionDate,
      pensionSchemeEnquiriesUrl,
      mpsDashboardUrl
    )
}