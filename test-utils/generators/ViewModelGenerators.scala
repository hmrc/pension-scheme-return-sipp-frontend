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

package generators

import cats.data.NonEmptyList
import org.scalacheck.Gen
import play.api.data.{Form, FormError}
import viewmodels.DisplayMessage.Message
import viewmodels.InputWidth
import viewmodels.models.*
import viewmodels.models.MultipleQuestionsViewModel.{DoubleQuestion, SingleQuestion, TripleQuestion}
import viewmodels.models.TaskListSectionViewModel.{MessageTaskListItem, TaskListItem, TaskListItemViewModel}
import viewmodels.models.TaskListStatus.TaskListStatus

trait ViewModelGenerators extends BasicGenerators {

  def formPageViewModelGen[A](implicit gen: Gen[A]): Gen[FormPageViewModel[A]] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyInlineMessage
      aHeading <- Gen.option(nonEmptyBlockMessage)
      description <- Gen.option(nonEmptyBlockMessage)
      page <- gen
      refresh <- Gen.option(Gen.const(1))
      buttonText <- nonEmptyMessage
      details <- Gen.option(furtherDetailsViewModel)
      onSubmit <- call
    } yield FormPageViewModel(title, aHeading, heading, description, page, refresh, buttonText, details, onSubmit)

  def pageViewModelGen[A](implicit gen: Gen[A]): Gen[PageViewModel[A]] =
    for {
      title <- nonEmptyMessage
      heading <- nonEmptyInlineMessage
      caption <- Gen.option(nonEmptyMessage)
      description <- Gen.option(nonEmptyBlockMessage)
      page <- gen
    } yield PageViewModel(title, caption, heading, description, page)

  implicit lazy val contentPageViewModelGen: Gen[ContentPageViewModel] =
    for {
      isStartButton <- boolean
      isLargeHeading <- boolean
    } yield ContentPageViewModel(isStartButton, isLargeHeading)

  implicit lazy val contentTablePageViewModelGen: Gen[ContentTablePageViewModel] =
    for {
      inset <- nonEmptyDisplayMessage
      rows <- Gen.listOf(tupleOf(nonEmptyMessage, nonEmptyMessage))
    } yield ContentTablePageViewModel(inset, rows)

  lazy val actionItemGen: Gen[SummaryAction] =
    for {
      content <- nonEmptyString.map(Message(_))
      href <- relativeUrl
      hidden <- nonEmptyString.map(Message(_))
    } yield SummaryAction(content, href, hidden)

  lazy val summaryListRowGen: Gen[CheckYourAnswersRowViewModel] =
    for {
      key <- nonEmptyString
      value <- nonEmptyString
      items <- Gen.listOf(actionItemGen)
    } yield CheckYourAnswersRowViewModel(Message(key), Message(value), items)

  lazy val summaryListSectionGen: Gen[CheckYourAnswersSection] =
    for {
      heading <- Gen.option(nonEmptyMessage)
      rows <- Gen.listOf(summaryListRowGen)
    } yield CheckYourAnswersSection(heading, rows)

  implicit lazy val checkYourAnswersViewModelGen: Gen[CheckYourAnswersViewModel] =
    for {
      sections <- Gen.listOfN(2, summaryListSectionGen)
    } yield CheckYourAnswersViewModel(sections)

  implicit lazy val bankAccountViewModelGen: Gen[BankAccountViewModel] =
    for {
      bankNameHeading <- nonEmptyMessage
      accountNumberHeading <- nonEmptyMessage
      accountNumberHint <- nonEmptyMessage
      sortCodeHeading <- nonEmptyMessage
      sortCodeHint <- nonEmptyMessage
    } yield BankAccountViewModel(
      bankNameHeading,
      accountNumberHeading,
      accountNumberHint,
      sortCodeHeading,
      sortCodeHint
    )

  implicit lazy val submissionViewModelGen: Gen[SubmissionViewModel] =
    for {
      title <- nonEmptyMessage
      panelHeading <- nonEmptyMessage
      panelContent <- nonEmptyMessage
      email <- Gen.option(nonEmptyMessage)
      scheme <- nonEmptyMessage
      periodOfReturn <- nonEmptyMessage
      dateSubmitted <- nonEmptyMessage
      whatHappensNextContent <- nonEmptyMessage
    } yield SubmissionViewModel(
      title,
      panelHeading,
      panelContent,
      email,
      scheme,
      periodOfReturn,
      dateSubmitted,
      whatHappensNextContent
    )

  implicit lazy val nameDOBViewModelGen: Gen[NameDOBViewModel] =
    for {
      firstName <- nonEmptyMessage
      lastName <- nonEmptyMessage
      dateOfBirth <- nonEmptyMessage
      dateOfBirthHint <- nonEmptyMessage
    } yield NameDOBViewModel(
      firstName,
      lastName,
      dateOfBirth,
      dateOfBirthHint
    )

  val listRowActionGen: Gen[RowAction] =
    for {
      label <- nonEmptyMessage
      url <- relativeUrl
      hiddenText <- nonEmptyMessage
    } yield RowAction(label, url, hiddenText)

  lazy val summaryRowGen: Gen[ListRow] =
    for {
      text <- nonEmptyMessage
      actionCount <- Gen.chooseNum(0, 2)
      actions <- Gen.listOfN(actionCount, listRowActionGen)
    } yield ListRow(text, actions)

  lazy val listRadiosRowGen: Gen[ListRadiosRow] =
    for {
      index <- Gen.chooseNum(1, 100)
      text <- nonEmptyMessage
    } yield ListRadiosRow(index, text)

  def summaryViewModelGen(rows: Int): Gen[ListViewModel] =
    summaryViewModelGen(rows = Some(rows))

  def summaryViewModelGen(
    showRadios: Boolean = true,
    rows: Option[Int] = None,
    paginate: Boolean = false
  ): Gen[ListViewModel] =
    for {
      inset <- nonEmptyDisplayMessage
      rows <- rows.fold(Gen.choose(1, 10))(Gen.const).flatMap(Gen.listOfN(_, summaryRowGen))
      radioText <- nonEmptyMessage
      pagination <- if (paginate) Gen.option(paginationGen) else Gen.const(None)
      yesHint <- nonEmptyMessage
    } yield ListViewModel(
      inset,
      rows,
      radioText,
      showRadios,
      pagination,
      Some(yesHint)
    )

  def listRadiosViewModelGen(
    rows: Option[Int] = None,
    paginate: Boolean = false
  ): Gen[ListRadiosViewModel] =
    for {
      legend <- nonEmptyDisplayMessage
      rows <- rows.fold(Gen.choose(1, 10))(Gen.const).flatMap(Gen.listOfN(_, listRadiosRowGen))
      pagination <- if (paginate) Gen.option(paginationGen) else Gen.const(None)
    } yield ListRadiosViewModel(
      Some(legend),
      rows,
      pagination
    )

  implicit lazy val yesNoPageViewModelGen: Gen[YesNoPageViewModel] =
    for {
      legend <- Gen.option(nonEmptyMessage)
      hint <- Gen.option(nonEmptyMessage)
      yes <- Gen.option(nonEmptyMessage)
      no <- Gen.option(nonEmptyMessage)
      details <- Gen.option(furtherDetailsViewModel)
    } yield YesNoPageViewModel(legend, hint, yes, no, details)

  def furtherDetailsViewModel: Gen[FurtherDetailsViewModel] =
    for {
      title <- nonEmptyMessage
      contents <- nonEmptyDisplayMessage
    } yield FurtherDetailsViewModel(title, contents)

  def radioListRowViewModelGen: Gen[RadioListRowViewModel] =
    for {
      content <- nonEmptyMessage
      value <- nonEmptyString
    } yield RadioListRowViewModel(content, value)

  implicit lazy val radioListViewModelGen: Gen[RadioListViewModel] =
    for {
      legend <- Gen.option(nonEmptyMessage)
      items <- Gen.listOfN(5, radioListRowViewModelGen)
      divider <- Gen.oneOf(Nil, List(RadioListRowDivider("divider")))
    } yield RadioListViewModel(legend, items ++ divider)

  implicit lazy val dateRangeViewModelGen: Gen[DateRangeViewModel] =
    for {
      startDateLabel <- nonEmptyMessage
      endDateLabel <- nonEmptyMessage
    } yield DateRangeViewModel(
      startDateLabel,
      endDateLabel,
      currentDateRanges = Nil
    )

  def fieldGen: Gen[QuestionField] =
    for {
      label <- nonEmptyInlineMessage
      hint <- Gen.option(nonEmptyInlineMessage)
      fieldType <- Gen.oneOf(FieldType.Input, FieldType.Date, FieldType.Currency)
    } yield QuestionField(label, hint, Some(InputWidth.Full), Nil, fieldType)

  def singleQuestionGen[A](form: Form[A]): Gen[SingleQuestion[A]] =
    fieldGen.map(SingleQuestion(form, _))

  def doubleQuestionGen[A](form: Form[(A, A)]): Gen[DoubleQuestion[A]] =
    for {
      field1 <- fieldGen
      field2 <- fieldGen
    } yield DoubleQuestion(form, field1, field2)

  def tripleQuestionGen[A](form: Form[(A, A, A)]): Gen[TripleQuestion[A, A, A]] =
    for {
      field1 <- fieldGen
      field2 <- fieldGen
      field3 <- fieldGen
    } yield TripleQuestion(form, field1, field2, field3)

  implicit lazy val textInputViewModelGen: Gen[TextInputViewModel] =
    for {
      label <- Gen.option(nonEmptyMessage)
      isFixedLength <- boolean
    } yield TextInputViewModel(label, isFixedLength)

  implicit lazy val uploadViewModelGen: Gen[UploadViewModel] =
    for {
      formFields <- mapOf(Gen.alphaStr, Gen.alphaStr, 10)
      error <- Gen.option(Gen.alphaStr)
    } yield UploadViewModel(formFields, error.map(FormError("file-upload", _)))

  implicit lazy val textAreaViewModelGen: Gen[TextAreaViewModel] = Gen.chooseNum(1, 100).map(TextAreaViewModel(_))

  lazy val taskListStatusGen: Gen[TaskListStatus] =
    Gen.oneOf(
      TaskListStatus.UnableToStart,
      TaskListStatus.NotStarted,
      TaskListStatus.InProgress,
      TaskListStatus.Completed
    )

  lazy val messageTaskListItemGen: Gen[MessageTaskListItem] =
    for {
      message <- nonEmptyInlineMessage
      hint <- Gen.option(nonEmptyMessage)
    } yield MessageTaskListItem(message, hint)

  lazy val taskListItemViewModelGen: Gen[TaskListItemViewModel] =
    for {
      link <- nonEmptyLinkMessage
      hint <- Gen.option(nonEmptyMessage)
      status <- taskListStatusGen
    } yield TaskListItemViewModel(link, hint, Some(status))

  lazy val taskListItemGen: Gen[TaskListItem] = Gen.oneOf(taskListItemViewModelGen, messageTaskListItemGen)

  lazy val taskListSectionViewModelGen: Gen[TaskListSectionViewModel] = {
    val itemsGen: Gen[NonEmptyList[TaskListItem]] =
      Gen.nonEmptyListOf(taskListItemGen).map(NonEmptyList.fromList(_).get)

    for {
      sectionTitle <- nonEmptyMessage
      items <- itemsGen
      postActionLink <- Gen.option(nonEmptyLinkMessage)
    } yield new TaskListSectionViewModel(sectionTitle, items, postActionLink)
  }

  implicit lazy val taskListViewModel: Gen[TaskListViewModel] =
    Gen
      .nonEmptyListOf(taskListSectionViewModelGen)
      .map(NonEmptyList.fromList(_).get)
      .map(TaskListViewModel(_))
}
