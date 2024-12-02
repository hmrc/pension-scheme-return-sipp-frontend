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

import views.html.{ETMPErrorReceivedView, ETMPRequestDataSizeExceedErrorView}

class ETMPErrorReceivedControllerSpec extends ControllerBaseSpec {

  "ETMPErrorReceivedController" - {

    "ETMPErrorReceivedView" - {
      "Page load with srn" - {
        lazy val onPageLoad = routes.ETMPErrorReceivedController.onEtmpErrorPageLoadWithSrn(srn)

        act.like(renderViewWithInternalServerError(onPageLoad) { implicit app => implicit request =>
          val view = injected[ETMPErrorReceivedView]
          view("http://localhost:9250/contact/report-technical-problem?service=pension-scheme-return-sipp-frontend")
        })
      }

      "Page load without srn" - {
        lazy val onPageLoad = routes.ETMPErrorReceivedController.onEtmpErrorPageLoad

        act.like(renderViewWithInternalServerError(onPageLoad) { implicit app => implicit request =>
          val view = injected[ETMPErrorReceivedView]
          view("http://localhost:9250/contact/report-technical-problem?service=pension-scheme-return-sipp-frontend")
        })
      }
    }

    "ETMPRequestDataSizeExceedErrorView" - {
      "Page load with srn" - {
        lazy val onPageLoad = routes.ETMPErrorReceivedController.onEtmpRequestDataSizeExceedErrorPageLoadWithSrn(srn)

        act.like(renderViewWithRequestEntityTooLarge(onPageLoad) { implicit app => implicit request =>
          val view = injected[ETMPRequestDataSizeExceedErrorView]
          view()
        })
      }

      "Page load without srn" - {
        lazy val onPageLoad = routes.ETMPErrorReceivedController.onEtmpRequestDataSizeExceedErrorPageLoad

        act.like(renderViewWithRequestEntityTooLarge(onPageLoad) { implicit app => implicit request =>
          val view = injected[ETMPRequestDataSizeExceedErrorView]
          view()
        })
      }
    }

  }
}
