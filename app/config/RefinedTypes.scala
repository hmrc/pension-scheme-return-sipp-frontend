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

object RefinedTypes {

  type OneToThree = Greater[0] And LessEqual[3]
  type Max3 = Int Refined OneToThree

  object Max3 {
    val ONE: Max3 = refineUnsafe[Int, OneToThree](1)
    val TWO: Max3 = refineUnsafe[Int, OneToThree](2)
    val THREE: Max3 = refineUnsafe[Int, OneToThree](3)
  }

  type OneTo5000 = Greater[0] And LessEqual[5000]
  type Max5000 = Int Refined OneTo5000

  object Max5000 {
    implicit val enumerable: Enumerable[Max5000] = Enumerable(
      (1 to 5000).toList
        .map(refineUnsafe[Int, OneTo5000])
        .map(index => index.value.toString -> index)*
    )
  }

  def refineUnsafe[A, B](a: A)(implicit validate: Validate[A, B]): A Refined B =
    refineV[B](a).fold(err => throw Exception(err), identity)
}
