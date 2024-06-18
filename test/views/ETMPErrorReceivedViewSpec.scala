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

import org.jsoup.Jsoup
import org.scalatest.matchers.should.Matchers._
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.ETMPErrorReceivedView

class ETMPErrorReceivedViewSpec extends ViewSpec {

  runningApplication { implicit app =>
    val view = injected[ETMPErrorReceivedView]

    implicit val request = FakeRequest()

    "ETMPErrorReceivedView" - {
      "render the title" in {
        title(view()) must startWith(Messages("serviceUnavailable.title"))
      }
      "render the heading" in {
        h1(view()) must startWith(Messages("serviceUnavailable.heading"))
      }
      "render the first paragraph" in {
        Jsoup.parse(view().body).getElementsByClass("govuk-body").first.text shouldBe Messages("serviceUnavailable.paragraph1")
      }
      "render the second paragraph" in {
        Jsoup.parse(view().body).getElementsByClass("govuk-body").last.text shouldBe Messages("serviceUnavailable.paragraph2")
      }
    }

  }
}
