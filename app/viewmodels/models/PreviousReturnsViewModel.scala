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

package viewmodels.models

import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.Message

class PreviousReturnsViewModel (rows: List[CheckYourAnswersRowViewModel],
                                 marginBottom: Option[Int] = None,
                                inset: Option[DisplayMessage] = None,
                                paginatedViewModel: Option[PaginatedViewModel] = None)

case class ViewLink(content: Message, href: String, visuallyHiddenContent: Message) {

  def withVisuallyHiddenContent(content: Message): ViewLink = copy(visuallyHiddenContent = content)
  def withVisuallyHiddenContent(content: String): ViewLink = withVisuallyHiddenContent(Message(content))

}

object ViewLink {

  def apply(content: String, href: String): ViewLink =
    ViewLink(Message(content), href, Message(""))
}
