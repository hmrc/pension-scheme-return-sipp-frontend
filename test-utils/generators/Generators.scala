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

import org.scalacheck.{Arbitrary, Gen, Shrink}

trait Generators
    extends UserAnswersGenerator
    with ModelGenerators
    with ViewModelGenerators
    with BasicGenerators
    with TransactionDetailsGenerators {

  implicit lazy val dontShrinkString: Shrink[String] = Shrink.shrinkAny
  implicit def dontShrinkList[T]: Shrink[List[T]] = Shrink.shrinkAny

  implicit def arbitraryGen[A](implicit g: Gen[A]): Arbitrary[A] = Arbitrary(g)
}
