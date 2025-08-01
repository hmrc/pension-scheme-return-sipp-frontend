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

package viewmodels

import cats.data.NonEmptyList
import play.api.i18n.Messages

sealed trait DisplayMessage

object DisplayMessage {

  sealed trait InlineMessage extends DisplayMessage

  sealed trait BlockMessage extends DisplayMessage

  case object Empty extends InlineMessage

  case class Message(key: String, args: List[Message]) extends InlineMessage {

    def toMessage(implicit messages: Messages): String =
      messages(key, args.map(_.toMessage)*)
  }

  object Message {

    def apply(key: String, args: Message*): Message =
      Message(key, args.toList)
  }

  case class CompoundMessage(message: DisplayMessage, next: DisplayMessage) extends InlineMessage

  implicit class DisplayMessageOps(message: DisplayMessage) {
    def ++(other: DisplayMessage): CompoundMessage = CompoundMessage(message, other)
  }

  case class LinkMessage(content: Message, url: String, attrs: Map[String, String], hiddenText: Option[Message] = None)
      extends InlineMessage {

    def withAttr(key: String, value: String): LinkMessage =
      copy(attrs = attrs + (key -> value))
  }

  object LinkMessage {

    def apply(content: Message, url: String): LinkMessage =
      LinkMessage(content, url, Map())

    def apply(content: Message, url: String, hiddenText: Message): LinkMessage =
      LinkMessage(content, url, Map(), Some(hiddenText))
  }

  case class DownloadLinkMessage(content: Message, url: String) extends InlineMessage

  case class CaptionSpan(content: InlineMessage, caption: Caption) extends BlockMessage

  case class Heading2(content: InlineMessage, headingSize: LabelSize = LabelSize.Small) extends BlockMessage

  case class Heading3(content: InlineMessage) extends BlockMessage

  object Heading2 {
    def medium(content: InlineMessage): Heading2 = Heading2(content, LabelSize.Medium)
  }

  case class ParagraphMessage(content: NonEmptyList[InlineMessage]) extends BlockMessage

  object ParagraphMessage {
    def apply(headContent: InlineMessage, tailContents: InlineMessage*): ParagraphMessage =
      ParagraphMessage(NonEmptyList(headContent, tailContents.toList))
  }

  case class HintMessage(content: InlineMessage) extends BlockMessage

  case class InsetTextMessage(content: NonEmptyList[InlineMessage]) extends BlockMessage

  object InsetTextMessage {

    def apply(headContent: InlineMessage, tailContents: InlineMessage*): InsetTextMessage =
      InsetTextMessage(NonEmptyList(headContent, tailContents.toList))
  }

  case class TableMessage(
    content: NonEmptyList[InlineMessage],
    heading: Option[InlineMessage] = None
  ) extends BlockMessage

  case class TableMessageWithKeyValue(
    content: NonEmptyList[(InlineMessage, DisplayMessage)],
    heading: Option[(InlineMessage, InlineMessage)] = None,
    caption: Option[InlineMessage] = None
  ) extends BlockMessage

  case class ListMessage(content: NonEmptyList[InlineMessage], listType: ListType) extends BlockMessage

  object ListMessage {

    def apply(listType: ListType, headContent: InlineMessage, tailContents: InlineMessage*): ListMessage =
      ListMessage(NonEmptyList(headContent, tailContents.toList), listType)

    object Bullet {
      def apply(headContent: InlineMessage, tailContents: InlineMessage*): ListMessage =
        ListMessage(NonEmptyList(headContent, tailContents.toList), ListType.Bullet)
    }

    object NewLine {
      def apply(headContent: InlineMessage, tailContents: InlineMessage*): ListMessage =
        ListMessage(NonEmptyList(headContent, tailContents.toList), ListType.NewLine)
    }
  }

  sealed trait ListType

  object ListType {
    case object Bullet extends ListType
    case object NewLine extends ListType
  }
}
