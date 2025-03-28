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

package models
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.domain.{SimpleName, SimpleObjectReads, SimpleObjectWrites, TaxIdentifier}

case class Utr(utr: String) extends TaxIdentifier with SimpleName {

  require(Utr.isValid(utr), s"$utr is not a valid utr.")

  override def toString = utr

  def value = utr

  val name = "utr"
}

object Utr extends (String => Utr) {
  implicit val utrWrite: Writes[Utr] = SimpleObjectWrites[Utr](_.value)
  implicit val utrRead: Reads[Utr] = SimpleObjectReads[Utr]("utr", Utr.apply)

  private val validUtrFormat = """^\d{5}\s*?\d{5}"""

  def isValid(utr: String): Boolean = utr != null && utr.matches(validUtrFormat)

}
