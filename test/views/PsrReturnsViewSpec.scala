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

package views

import models.ReportStatus.SubmittedAndSuccessfullyProcessed
import models.{PsrVersionsResponse, ReportSubmitterDetails}
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.PsrReturnsView
import org.scalatest.matchers.should.Matchers._

import java.time.{LocalDate, ZoneId, ZonedDateTime}

class PsrReturnsViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[PsrReturnsView]

    implicit val request = FakeRequest()

    val dateFrom: LocalDate = LocalDate.of(2022, 4, 6)
    val dateTo: LocalDate = LocalDate.of(2023, 4, 6)

    val psrVersionResponse1 = PsrVersionsResponse(
      reportFormBundleNumber = "123456",
      reportVersion = 1,
      reportStatus = SubmittedAndSuccessfullyProcessed,
      compilationOrSubmissionDate = ZonedDateTime.of(2024, 6, 27, 0, 0, 0, 0, ZoneId.of("UTC")),
      reportSubmitterDetails = Some(ReportSubmitterDetails("John", None, None)),
      psaDetails = None
    )
    val psrVersionResponse2 = PsrVersionsResponse(
      reportFormBundleNumber = "654321",
      reportVersion = 2,
      reportStatus = SubmittedAndSuccessfullyProcessed,
      compilationOrSubmissionDate = ZonedDateTime.of(2025, 7, 23, 0, 0, 0, 0, ZoneId.of("UTC")),
      reportSubmitterDetails = Some(ReportSubmitterDetails("Tom", None, None)),
      psaDetails = None
    )
    val psrVersionsResponses = Seq(psrVersionResponse1, psrVersionResponse2)

    "PsrReturnsView" - {

      val versionsView = view(dateFrom.toString, dateTo.toString, "Tom Smith", psrVersionsResponses)

      "render the title" in {
        title(versionsView) must
          startWith(Messages("previousReturns.title"))
      }

      "render the heading" in {
        h1(versionsView) must startWith(Messages("previousReturns.heading", dateFrom, dateTo))
      }

      "render the subheading" in {
        span(versionsView).head must startWith(Messages("previousReturns.subheading", "Tom Smith"))
      }

      "render the name of table's first column" in {
        getElementBySpecificId(versionsView, "firstColumnName").text should startWith("Submission")
      }

      "render the name of table's second column" in {
        getElementBySpecificId(versionsView, "secondColumnName").text should startWith("Submitted on")
      }

      "render the name of table's third column" in {
        getElementBySpecificId(versionsView, "thirdColumnName").text should startWith("Submitted by")
      }

      "render the name of table's final column" in {
        getElementBySpecificId(versionsView, "fourthColumnName").text must startWith("")
      }

      "render the contents of the top row as a List" in {
        tr(versionsView)(1) mustBe List("2", "23 July 2025", "Tom", "View")
      }

      "render the contents of the bottom row as a List" in {
        tr(versionsView)(2) mustBe List("1", "27 June 2024", "John", "View")
      }

    }
  }

}
