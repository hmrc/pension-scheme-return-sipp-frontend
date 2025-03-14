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

package viewmodels.models

import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.Message

case class ListRow(
  text: DisplayMessage,
  actions: List[RowAction]
)

case class RowAction(
  label: Message,
  url: String,
  hiddenText: Message
)

case class ListViewModel(
  inset: DisplayMessage,
  rows: List[ListRow],
  radioText: Message,
  // whether to render the radio buttons to add another entity to the list or continue
  showRadios: Boolean = true,
  paginatedViewModel: Option[PaginatedViewModel] = None,
  yesHintText: Option[Message] = None,
  showInsetWithRadios: Boolean = false
)
