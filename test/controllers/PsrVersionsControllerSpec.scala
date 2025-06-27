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
import models.requests.common.YesNo
import models.requests.common.YesNo.Yes
import models.requests.psr.EtmpPsrStatus.Compiled
import models.{BasicDetails, DateRange, FormBundleNumber, PsrVersionsResponse, ReportSubmitterDetails}
import models.MinimalDetails
import org.mockito.stubbing.OngoingStubbing
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
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
    inject.bind[TaxYearService].toInstance(FakeTaxYearService(dateFrom))
  )

  override def beforeEach(): Unit =
    reset(mockPsrVersions, mockSchemeDateService, mockTaxYearService)

  lazy val onPageLoad: Call = routes.PsrVersionsController.onPageLoad(srn)

  val psrVersionResponse1: PsrVersionsResponse = PsrVersionsResponse(
    reportFormBundleNumber = "123456",
    reportVersion = 1,
    reportStatus = SubmittedAndSuccessfullyProcessed,
    compilationOrSubmissionDate = ZonedDateTime.now,
    reportSubmitterDetails = Some(ReportSubmitterDetails("John", None, None)),
    psaDetails = None
  )
  val psrVersionResponse2: PsrVersionsResponse = PsrVersionsResponse(
    reportFormBundleNumber = "654321",
    reportVersion = 2,
    reportStatus = SubmittedAndSuccessfullyProcessed,
    compilationOrSubmissionDate = ZonedDateTime.now,
    reportSubmitterDetails = Some(ReportSubmitterDetails("Tom", None, None)),
    psaDetails = None
  )
  val psrVersionsResponses: Seq[PsrVersionsResponse] = Seq(psrVersionResponse1, psrVersionResponse2)
  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  "PsrVersionsController" - {
    lazy val onPageLoad = routes.PsrVersionsController.onPageLoad(srn)

    List(
      (defaultMinimalDetails, individualDetails.firstName + " " + individualDetails.lastName),
      (organizationMinimalDetails, organisationName),
      (defaultMinimalDetails.copy(organisationName = None, individualDetails = None), "")
    ).foreach { (minimalDetails1, expectedName) =>
      act.like(
        renderView(
          call = onPageLoad,
          userAnswers = defaultUserAnswers,
          addToSession = Seq(("fbNumber", fbNumber)),
          minimalDetails = minimalDetails1
        ) { implicit app => implicit request =>
          val view = injected[PsrReturnsView]
          view(
            srn,
            dateFrom.format(dateFormatter),
            dateTo.format(dateFormatter),
            expectedName,
            psrVersionsResponses
          )
        }.before {
          getPsrVersions(psrVersionsResponses); returnAccountingPeriodsFromEtmp(dateRanges)
        }.withName(s"should render view with ${expectedName}")
      )
    }

    def getPsrVersions(
      psrVersions: Seq[PsrVersionsResponse]
    ): OngoingStubbing[Future[Seq[PsrVersionsResponse]]] =
      when(mockPsrVersions.getPsrVersions(any, any)(any, any))
        .thenReturn(Future.successful(psrVersions))

    def returnAccountingPeriodsFromEtmp(
      datRanges: NonEmptyList[DateRange]
    ) =
      when(mockSchemeDateService.returnBasicDetails(any, any[FormBundleNumber])(any, any, any))
        .thenReturn(Future.successful(BasicDetails(Some(datRanges), datRanges.head, YesNo.Yes, Compiled, Yes)))

  }
}
