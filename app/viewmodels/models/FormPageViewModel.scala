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

import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{BlockMessage, InlineMessage, Message}

case class FormPageViewModel[+A](
  title: Message,
  additionalHeading: Option[BlockMessage] = None,
  heading: InlineMessage,
  description: Option[DisplayMessage],
  page: A,
  refresh: Option[Int],
  buttonText: Message,
  details: Option[FurtherDetailsViewModel] = None,
  onSubmit: Call,
  showBackLink: Boolean = true,
  breadcrumbs: Option[List[(String, String)]] = None,
  summaryList: Option[SummaryList] = None,
  hiddenSubmit: Boolean = false
) {

  def withDescription(message: Option[DisplayMessage]): FormPageViewModel[A] =
    copy(description = message)

  def withDescription(message: DisplayMessage): FormPageViewModel[A] =
    withDescription(Some(message))

  def refreshPage(refresh: Option[Int]): FormPageViewModel[A] =
    copy(refresh = refresh)

  def withButtonText(message: Message): FormPageViewModel[A] =
    copy(buttonText = message)

  def withAdditionalHeadingText(message: BlockMessage): FormPageViewModel[A] =
    copy(additionalHeading = Some(message))

  def withBreadcrumbs(breadcrumbs: List[(String, String)]): FormPageViewModel[A] =
    copy(breadcrumbs = Some(breadcrumbs))

  def withoutBackButton(): FormPageViewModel[A] =
    copy(showBackLink = false)

  def withSummaryList(summaryList: SummaryList): FormPageViewModel[A] =
    copy(summaryList = Some(summaryList))

  def withSubmitVisibility(isHidden: Boolean): FormPageViewModel[A] =
    copy(hiddenSubmit = isHidden)
}

object FormPageViewModel {

  def applyWithContinue[A](
    title: Message,
    heading: InlineMessage,
    page: A,
    onSubmit: Call
  ): FormPageViewModel[A] =
    FormPageViewModel(
      title,
      None,
      heading,
      None,
      page,
      refresh = None,
      Message("site.continue"),
      None,
      onSubmit
    )

  def apply[A](
    title: Message,
    heading: InlineMessage,
    page: A,
    onSubmit: Call
  ): FormPageViewModel[A] =
    FormPageViewModel(
      title,
      None,
      heading,
      None,
      page,
      refresh = None,
      Message("site.saveAndContinue"),
      None,
      onSubmit
    )

  def apply[A](
    title: Message,
    heading: InlineMessage,
    page: A,
    onSubmit: Call,
    buttonText: Message
  ): FormPageViewModel[A] =
    FormPageViewModel(
      title,
      None,
      heading,
      None,
      page,
      refresh = None,
      buttonText,
      None,
      onSubmit
    )

  def apply[A](
    title: Message,
    heading: InlineMessage,
    description: DisplayMessage,
    page: A,
    onSubmit: Call
  ): FormPageViewModel[A] =
    FormPageViewModel(
      title,
      None,
      heading,
      Some(description),
      page,
      refresh = None,
      Message("site.saveAndContinue"),
      None,
      onSubmit
    )

  def apply[A](
    title: Message,
    heading: InlineMessage,
    page: A,
    details: Option[FurtherDetailsViewModel],
    onSubmit: Call
  ): FormPageViewModel[A] =
    FormPageViewModel(
      title,
      None,
      heading,
      None,
      page,
      refresh = None,
      Message("site.saveAndContinue"),
      details,
      onSubmit
    )
}
