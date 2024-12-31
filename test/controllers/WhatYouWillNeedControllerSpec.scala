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

import controllers.WhatYouWillNeedController.*
import models.requests.common.YesNo.Yes
import models.requests.psr.EtmpPsrStatus.Submitted
import models.{BasicDetails, FormBundleNumber}
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import views.html.ContentPageView

import scala.concurrent.Future

class WhatYouWillNeedControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.WhatYouWillNeedController.onPageLoad(srn)
  private lazy val onSubmit = routes.WhatYouWillNeedController.onSubmit(srn)

  private val taxYearDateRange = dateRangeGen.sample.value
  private val basicDetails = BasicDetails(None, taxYearDateRange, Yes, Submitted)
  private val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[SchemeDateService].toInstance(mockSchemeDateService)
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
        when(mockSchemeDateService.returnBasicDetails(any, any[FormBundleNumber])(any, any))
          .thenReturn(Future.successful(None))
      })

    act.like(redirectNextPage(onSubmit))
  }
}
