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
import controllers.WhatYouWillNeedController.*
import models.requests.{AllowedAccessRequest, DataRequest, FormBundleOrVersionTaxYearRequest}
import models.requests.common.YesNo
import models.requests.common.YesNo.No
import models.requests.psr.EtmpPsrStatus.Submitted
import models.{BasicDetails, FormBundleNumber, UserAnswers, VersionTaxYear}
import play.api.inject
import play.api.inject.guice.GuiceableModule
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import services.SchemeDateService
import uk.gov.hmrc.time.TaxYear
import views.html.ContentPageView

import scala.concurrent.Future

class WhatYouWillNeedControllerSpec extends ControllerBaseSpec {
  private val mockPsrConnector = mock[PSRConnector]
  when(mockPsrConnector.createEmptyPsr(any)(any, any)).thenReturn(Future.unit)

  private lazy val onPageLoad = routes.WhatYouWillNeedController.onPageLoad(srn)
  private lazy val onSubmit = routes.WhatYouWillNeedController.onSubmit(srn)

  private val taxYear = TaxYear(date.sample.value.getYear)
  private val taxYearDateRange = dateRangeGen.sample.value
  private val basicDetails = BasicDetails(None, taxYearDateRange, No, Submitted, No)
  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  private val defaultUserAnswers: UserAnswers = UserAnswers("id")
  val allowedAccessRequest: AllowedAccessRequest[AnyContent] =
    allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val dataRequest: DataRequest[AnyContent] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[SchemeDateService].toInstance(mockSchemeDateService),
    inject.bind[PSRConnector].toInstance(mockPsrConnector)
  )

  "WhatYouWillNeedController" - {

    act
      .like(renderView(onPageLoad, addToSession = Seq(("fbNumber", fbNumber))) { implicit app => implicit request =>
        injected[ContentPageView].apply(
          viewModel(
            srn,
            schemeName = "testSchemeName",
            "http://localhost:8204/manage-pension-schemes/overview",
            s"http://localhost:10701/pension-scheme-return/${srn.value}/overview"
          )
        )
      }.before {
        when(
          mockSchemeDateService.returnBasicDetails(any[FormBundleOrVersionTaxYearRequest[AnyContent]])(any, any, any)
        )
          .thenReturn(Future.successful(None))
      })

    act
      .like(
        renderView(
          onPageLoad,
          addToSession = Seq(
            ("taxYear", taxYear.starts.toString),
            ("version", "001")
          )
        ) { implicit app => implicit request =>
          injected[ContentPageView].apply(
            viewModel(
              srn,
              schemeName = "testSchemeName",
              "http://localhost:8204/manage-pension-schemes/overview",
              s"http://localhost:10701/pension-scheme-return/${srn.value}/overview"
            )
          )
        }.before {
          when(
            mockSchemeDateService.returnBasicDetails(any[FormBundleOrVersionTaxYearRequest[AnyContent]])(any, any, any)
          )
            .thenReturn(Future.successful(None))
        }.withName("return OK and the correct view with version and tax year")
      )

    act.like(
      redirectToPage(
        onPageLoad,
        controllers.routes.AssetsHeldController.onPageLoad(srn, models.NormalMode),
        addToSession = Seq(("fbNumber", fbNumber))
      ).before {
        when(
          mockSchemeDateService.returnBasicDetails(any[FormBundleOrVersionTaxYearRequest[AnyContent]])(any, any, any)
        )
          .thenReturn(Future.successful(Some(basicDetails)))
      }.withName("redirect to AssetsHeldController when basic details are returned with form bundle number")
    )

    act.like(
      redirectToPage(
        onPageLoad,
        controllers.routes.AssetsHeldController.onPageLoad(srn, models.NormalMode),
        addToSession = Seq(
          ("taxYear", taxYear.starts.toString),
          ("version", "001")
        )
      ).before {
        when(
          mockSchemeDateService.returnBasicDetails(any[FormBundleOrVersionTaxYearRequest[AnyContent]])(any, any, any)
        )
          .thenReturn(Future.successful(Some(basicDetails)))
      }.withName("redirect to AssetsHeldController when basic details are returned with version and tax year")
    )

    act.like(
      redirectToPage(
        onPageLoad,
        controllers.routes.AssetsHeldController.onPageLoad(srn, models.NormalMode),
        addToSession = Seq(("fbNumber", fbNumber))
      ).before {
        when(mockSchemeDateService.returnBasicDetails(any, any[FormBundleNumber])(any, any, any))
          .thenReturn(Future.successful(Some(basicDetails.copy(oneOrMoreTransactionFilesUploaded = YesNo.Yes))))
      }.withName("redirect to TaskListController when basic details are returned with form bundle number")
    )

    act.like(
      redirectToPage(
        onPageLoad,
        controllers.routes.AssetsHeldController.onPageLoad(srn, models.NormalMode),
        addToSession = Seq(
          ("taxYear", taxYear.starts.toString),
          ("version", "001")
        )
      ).before {
        when(mockSchemeDateService.returnBasicDetails(any, any[VersionTaxYear])(any, any, any))
          .thenReturn(Future.successful(Some(basicDetails.copy(oneOrMoreTransactionFilesUploaded = YesNo.Yes))))
      }.withName("redirect to TaskListController when basic details are returned with version and tax year")
    )

    act.like(redirectNextPage(onSubmit, addToSession = Seq(("fbNumber", fbNumber))))
  }
}
