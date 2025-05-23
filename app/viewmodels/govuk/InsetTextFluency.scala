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

package viewmodels.govuk

import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, HtmlContent}
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import viewmodels.ErrorMessageAwareness

object insettext extends InsetTextFluency

trait InsetTextFluency {

  object InsetTextViewModel extends ErrorMessageAwareness {
    def apply(content: Content): InsetText =
      InsetText(content = content)

    def apply(html: Html): InsetText =
      apply(HtmlContent(html))

    def apply(elem: scala.xml.Elem): InsetText =
      apply(Html(elem.toString))
  }
}
