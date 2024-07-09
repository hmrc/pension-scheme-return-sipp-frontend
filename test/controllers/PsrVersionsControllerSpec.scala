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
import models.ReportStatus.SubmittedAndSuccessfullyProcessed
import models.{DateRange, PsrVersionsResponse, ReportSubmitterDetails}
import org.mockito.ArgumentMatchers.any
import org.mockito.stubbing.ScalaOngoingStubbing
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.{FakeTaxYearService, PsrVersionsService, SchemeDateService, TaxYearService}
import views.html.PsrReturnsView

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}
import scala.concurrent.Future

class PsrVersionsControllerSpec extends ControllerBaseSpec {

  private val mockPsrVersions = mock[PsrVersionsService]
  private val mockSchemeDateService = mock[SchemeDateService]
  private val mockTaxYearService = mock[TaxYearService]

  val dateFrom: LocalDate = LocalDate.of(2022, 4, 6)
  val dateTo: LocalDate = LocalDate.of(2023, 4, 5)
  val dateRanges: NonEmptyList[DateRange] = NonEmptyList.one(DateRange(dateFrom, dateTo))

  override val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrVersionsService].toInstance(mockPsrVersions),
    inject.bind[SchemeDateService].toInstance(mockSchemeDateService),
    inject.bind[TaxYearService].toInstance(new FakeTaxYearService(dateFrom))
  )

  override def beforeEach(): Unit =
    reset(mockPsrVersions, mockSchemeDateService, mockTaxYearService)

  lazy val onPageLoad = routes.PsrVersionsController.onPageLoad(srn)

  val psrVersionResponse1 = PsrVersionsResponse(
    reportFormBundleNumber = "123456",
    reportVersion = 1,
    reportStatus = SubmittedAndSuccessfullyProcessed,
    compilationOrSubmissionDate = ZonedDateTime.now,
    reportSubmitterDetails = Some(ReportSubmitterDetails("John", None, None)),
    psaDetails = None
  )
  val psrVersionResponse2 = PsrVersionsResponse(
    reportFormBundleNumber = "654321",
    reportVersion = 2,
    reportStatus = SubmittedAndSuccessfullyProcessed,
    compilationOrSubmissionDate = ZonedDateTime.now,
    reportSubmitterDetails = Some(ReportSubmitterDetails("Tom", None, None)),
    psaDetails = None
  )
  val psrVersionsResponses = Seq(psrVersionResponse1, psrVersionResponse2)
  val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  "PsrVersionsController" - {
    lazy val onPageLoad = routes.PsrVersionsController.onPageLoad(srn)

    act.like(
      renderView(onPageLoad, addToSession = Seq(("fbNumber", fbNumber))) { implicit app => implicit request =>
        val view = injected[PsrReturnsView]
        view(
          dateFrom.format(dateFormatter),
          dateTo.format(dateFormatter),
          "testFirstName testLastName",
          psrVersionsResponses
        )
      }.before { getPsrVersions(psrVersionsResponses); returnAccountingPeriodsFromEtmp(dateRanges) }
    )

    def getPsrVersions(
      psrVersions: Seq[PsrVersionsResponse]
    ): ScalaOngoingStubbing[Future[Seq[PsrVersionsResponse]]] =
      when(mockPsrVersions.getPsrVersions(any(), any())(any()))
        .thenReturn(Future.successful(psrVersions))

    def returnAccountingPeriodsFromEtmp(
      datRanges: NonEmptyList[DateRange]
    ) =
      when(mockSchemeDateService.returnAccountingPeriodsFromEtmp(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(datRanges)))

  }
}
