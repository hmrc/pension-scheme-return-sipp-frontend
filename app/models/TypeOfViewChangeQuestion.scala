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

package models

import utils.WithName
import play.api.mvc.JavascriptLiteral

sealed trait TypeOfViewChangeQuestion {
  val name: String
}

object TypeOfViewChangeQuestion extends Enumerable.Implicits {

  case object ViewReturn extends WithName("01") with TypeOfViewChangeQuestion

  case object  ChangeReturn extends WithName("02") with TypeOfViewChangeQuestion

  val values: List[TypeOfViewChangeQuestion] = List(ViewReturn, ChangeReturn)

  implicit val enumerable: Enumerable[TypeOfViewChangeQuestion] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val jsLiteral: JavascriptLiteral[TypeOfViewChangeQuestion] = (value: TypeOfViewChangeQuestion) => value.name
}
