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

package config

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.boolean.And
import eu.timepit.refined.numeric.{Greater, LessEqual}
import eu.timepit.refined.refineV
import models.Enumerable
import play.api.libs.json.*

object RefinedTypes {

  type OneToThree = Greater[0] And LessEqual[3]
  type Max3 = Int Refined OneToThree

  object Max3 {
    val ONE = refineUnsafe[Int, OneToThree](1)
    val TWO = refineUnsafe[Int, OneToThree](2)
    val THREE = refineUnsafe[Int, OneToThree](3)
  }

  type OneTo300 = Greater[0] And LessEqual[300]
  type Max300 = Int Refined OneTo300

  type OneTo5000 = Greater[0] And LessEqual[5000]
  type Max5000 = Int Refined OneTo5000

  type OneTo50 = Greater[0] And LessEqual[50]
  type Max50 = Int Refined OneTo50

  type OneTo5 = Greater[0] And LessEqual[5]
  type Max5 = Int Refined OneTo5
  implicit def indexReads[A](implicit ev: Validate[Int, A]): Reads[Refined[Int, A]] = {
    case JsNumber(value) =>
      refineV[A](value.toInt) match {
        case Left(err) => JsError(err)
        case Right(refined) => JsSuccess(refined)
      }
    case _ => JsError("index was not a number")
  }

  implicit def indexWrites[A]: Writes[Refined[Int, A]] = (o: Refined[Int, A]) => JsNumber(o.value)

  // used by generators
  object Max300 {
    type Refined = Greater[0] And LessEqual[300]

    implicit val enumerable: Enumerable[Max300] = Enumerable(
      (1 to 300).toList
        .map(refineUnsafe[Int, Refined])
        .map(index => index.value.toString -> index)*
    )
  }
  object Max5000 {
    type Refined = Greater[0] And LessEqual[5000]

    implicit val enumerable: Enumerable[Max5000] = Enumerable(
      (1 to 5000).toList
        .map(refineUnsafe[Int, Refined])
        .map(index => index.value.toString -> index)*
    )
  }

  object Max50 {
    type Refined = Greater[0] And LessEqual[50]

    implicit val enumerable: Enumerable[Max50] = Enumerable(
      (1 to 50).toList
        .map(refineUnsafe[Int, Refined])
        .map(index => index.value.toString -> index)*
    )
  }

  object Max5 {
    type Refined = Greater[0] And LessEqual[5]

    implicit val enumerable: Enumerable[Max5] = Enumerable(
      (1 to 5).toList
        .map(refineUnsafe[Int, Refined])
        .map(index => index.value.toString -> index)*
    )
  }

  // this method exists because compile-time refinement in refined is not yet implemented
  def refineUnsafe[A, B](a: A)(implicit validate: Validate[A, B]): A Refined B =
    refineV[B](a).fold(err => throw new Exception(err), identity)
}
