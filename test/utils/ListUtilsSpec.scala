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

package utils

class ListUtilsSpec extends BaseSpec {

  "intersperse" - {
    "insert a separator between each element" in {
      val list = List("a", "b", "c")
      val expected = List("a", "-", "b", "-", "c")

      ListUtils.ListOps(list).intersperse("-") mustEqual expected
    }

    "insert a separator between every second element" in {
      val list = List("a", "b", "c", "d", "e", "f")
      val expected = List("a", "b", "-", "c", "d", "-", "e", "f")

      ListUtils.ListOps(list).intersperse("-", 2) mustEqual expected
    }

    "maybeAppend" - {
      "appends an element when Some is provided" in {
        val list = List("a", "b")
        val expected = List("a", "b", "c")

        ListUtils.ListOps(list).maybeAppend(Some("c")) mustEqual expected
      }

      "returns the same list when None is provided" in {
        val list = List("x", "y")
        val expected = list

        ListUtils.ListOps(list).maybeAppend(None) mustEqual expected
      }

      "works correctly on an empty list with Some" in {
        val list = List.empty[String]
        val expected = List("z")

        ListUtils.ListOps(list).maybeAppend(Some("z")) mustEqual expected
      }

      "returns empty list when input is empty and None is provided" in {
        val list = List.empty[String]

        ListUtils.ListOps(list).maybeAppend(None) mustEqual list
      }
    }
  }
  
}
