/*
 * Copyright 2025 HM Revenue & Customs
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

import config.RefinedTypes._
import utils.BaseSpec

class RefinedTypesSpec  extends BaseSpec {
  "RefinedTypes" - {
    "OneToThree" - {
      "valid refined" in {
        refineUnsafe[Int, OneToThree](3) mustBe Max3.THREE
      }
      "invalid refined" in {
        assertThrows[Exception] {
          refineUnsafe[Int, OneToThree](4)
        }
      }
    }
    "OneTo5000" - {
      List(1,5000).foreach { refineValue =>
        s"valid refined for value $refineValue" in {
          refineUnsafe[Int, OneTo5000](refineValue).value mustBe refineValue
        }
      }
      "invalid refined" in {
        assertThrows[Exception] {
          refineUnsafe[Int, OneTo5000](-1)
        }
      }
    }
  }
}
