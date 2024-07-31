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

import play.api.mvc.Call
import viewmodels.DisplayMessage.{LinkMessage, Message}

case class ViewChangeNewFileQuestionPageViewModel(
  question: Message,
  hint: Message,
  messageOrLinkMessage: Either[Message, LinkMessage]
)

object ViewChangeNewFileQuestionPageViewModel {

  def apply(
    title: Message,
    heading: Message,
    question: Message,
    hint: Message,
    messageOrLinkMessage: Either[Message, LinkMessage],
    onSubmit: Call
  ): FormPageViewModel[ViewChangeNewFileQuestionPageViewModel] =
    FormPageViewModel(
      title,
      heading,
      ViewChangeNewFileQuestionPageViewModel(
        question,
        hint,
        messageOrLinkMessage
      ),
      onSubmit
    ).withButtonText(
      Message("site.saveAndContinue")
    )

  def apply(
    title: Message,
    heading: Message,
    question: Message,
    hint: Message,
    noAssetMessage: Message,
    onSubmit: Call
  ): FormPageViewModel[ViewChangeNewFileQuestionPageViewModel] =
    FormPageViewModel(
      title,
      heading,
      ViewChangeNewFileQuestionPageViewModel(
        question,
        hint,
        Left(noAssetMessage)
      ),
      onSubmit
    ).withButtonText(
      Message("site.saveAndContinue")
    )
}
