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

package views

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import play.twirl.api.Html

import scala.jdk.CollectionConverters.IteratorHasAsScala

trait HtmlHelper extends HtmlModels {

  def mainContent(html: Html): Element =
    Jsoup.parse(html.body).getElementsByAttributeValue("data-testid", "main-content").first()

  def title(html: Html): String = Jsoup.parse(html.body).title()

  def h1(html: Html): String = mainContent(html).getElementsByTag("h1").first().text()
  def h2(html: Html): List[String] =
    mainContent(html).getElementsByTag("h2").iterator().asScala.map(_.text()).toList

  def elementText(html: Html): List[String] =
    p(html) ++ li(html) ++ labels(html) ++ legend(html) ++ h2(html)

  def p(html: Html): List[String] =
    mainContent(html).getElementsByTag("p").iterator().asScala.map(_.text()).toList

  def li(html: Html): List[String] =
    mainContent(html).getElementsByTag("li").iterator().asScala.map(_.text()).toList

  def legend(html: Html): List[String] =
    mainContent(html).getElementsByTag("legend").iterator().asScala.map(_.text()).toList

  def span(html: Html): List[String] =
    mainContent(html).getElementsByTag("span").iterator().asScala.map(_.text()).toList

  def div(html: Html): List[String] =
    mainContent(html).getElementsByTag("div").iterator().asScala.map(_.text()).toList

  def panel(html: Html): Panel = {
    val parent = mainContent(html).getElementsByClass("govuk-panel").first()
    val title = parent.getElementsByClass("govuk-panel__title").first()
    val maybeBody = parent.getElementsByClass("govuk-panel__body").iterator().asScala.toList.headOption
    Panel(title, maybeBody)
  }

  def byClass(html: Html, className: String): List[String] =
    mainContent(html).getElementsByClass(className).iterator().asScala.map(_.text()).toList

  def radios(html: Html): List[RadioItem] =
    mainContent(html).select("input[type=radio]").iterator().asScala.toList.map(RadioItem(_))

  def dividers(html: Html): List[Element] =
    mainContent(html).getElementsByClass("govuk-radios__divider").iterator().asScala.toList

  def labels(html: Html): List[String] =
    mainContent(html).getElementsByTag("label").iterator().asScala.map(_.text()).toList

  def inputLabel(html: Html)(name: String): Element =
    mainContent(html).selectFirst(s"label[for=$name]")

  def input(html: Html)(name: String): Option[Input] =
    mainContent(html).select(s"input[name=$name]").iterator().asScala.map(Input(_)).toList.headOption

  def inputByClass(html: Html)(cssClass: String): Element =
    mainContent(html).select(s"input.$cssClass").first()

  def upload(html: Html): Element = inputByClass(html)("govuk-file-upload")

  def inputHint(html: Html)(name: String): Element =
    mainContent(html).getElementById(s"$name-hint")

  def tr(html: Html): List[List[String]] =
    mainContent(html)
      .getElementsByTag("tr")
      .iterator()
      .asScala
      .toList
      .map(
        _.getElementsByTag("td").iterator().asScala.toList.map(_.text())
      )

  def inset(html: Html): Element =
    mainContent(html).getElementsByClass("govuk-inset-text").first()

  def details(html: Html): Element =
    mainContent(html).selectFirst("details.govuk-details")

  def buttons(html: Html): Elements =
    mainContent(html).getElementsByTag("button")

  def button(html: Html): Element = buttons(html).first()

  def anchorButton(html: Html): AnchorTag =
    AnchorTag(mainContent(html).select("a[role=button]").first())

  def anchors(html: Html): List[AnchorTag] =
    mainContent(html).getElementsByTag("a").iterator().asScala.map(e => AnchorTag(e)).toList

  def form(html: Html): Form =
    Form(mainContent(html).getElementsByTag("form").first())

  def errorSummary(html: Html): Elements =
    mainContent(html).getElementsByClass("govuk-error-summary")

  def errorMessage(html: Html): Elements =
    mainContent(html).getElementsByClass("govuk-error-message")

  def summaryListKeys(html: Html): List[String] =
    mainContent(html).getElementsByClass("govuk-summary-list__key").iterator().asScala.toList.map(_.text())

  def summaryListValues(html: Html): List[String] =
    mainContent(html).getElementsByClass("govuk-summary-list__value").iterator().asScala.toList.map(_.text())

  def summaryListActions(html: Html): List[AnchorTag] =
    mainContent(html).select(".govuk-summary-list__actions a").iterator().asScala.toList.map(AnchorTag(_))

  def summaryListRows(html: Html): List[Element] =
    mainContent(html).getElementsByClass("govuk-summary-list__row").iterator().asScala.toList

  def radioListRows(html: Html): List[Element] =
    mainContent(html).getElementsByClass("govuk-radios__item").iterator().asScala.toList

  def paginationLabel(html: Html): Option[String] =
    Option(mainContent(html).getElementById("pagination-label")).map(_.text())

  def paginationElements(html: Html): List[PaginationElement] =
    mainContent(html)
      .getElementsByClass("govuk-pagination__link")
      .iterator()
      .asScala
      .toList
      .map(elem => PaginationElement(elem.text(), elem.parent.hasClass("govuk-pagination__item--current")))

  def date(html: Html)(id: String): DateElements = {
    val elements = mainContent(html).selectFirst(s"div.govuk-date-input")
    DateElements(
      elements.selectFirst(s"label[for=$id.day]"),
      elements.selectFirst(s"label[for=$id.month]"),
      elements.selectFirst(s"label[for=$id.year]")
    )
  }

  def textAreas(html: Html)(name: String): Elements =
    mainContent(html).select(s"textarea[name=$name]")

  def getElementBySpecificId(html: Html, id: String): Element =
    mainContent(html).getElementById(id)

  case class DateElements(day: Element, month: Element, year: Element)
  case class Panel(title: Element, body: Option[Element])
}
