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

import models.{Journey, JourneyType}
import models.SchemeId.Srn
import org.scalacheck.Gen
import org.scalatest.EitherValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.QueryStringBindable
import utils.BaseSpec

class BindersSpec extends BaseSpec with ScalaCheckPropertyChecks with EitherValues {

  "SRN binder" - {
    "return a valid srn" - {
      "srn is valid" in {
        forAll(srnGen) { validSrn =>
          Binders.srnBinder.bind("srn", validSrn.value) mustBe Right(validSrn)
        }
      }
    }

    "return an error message" - {
      "srn is invalid" in {
        forAll(Gen.alphaNumStr) { invalidSrn =>
          whenever(!invalidSrn.matches(Srn.srnRegex)) {
            Binders.srnBinder.bind("srn", invalidSrn) mustBe Left("Invalid scheme reference number")
          }
        }
      }
    }
  }

  "Journey path binder" - {
    "return a valid Journey" in {
      Journey.values.foreach { journey =>
        Binders.journeyPathBinder.bind("journey", journey.entryName) mustBe Right(journey)
      }
    }

    "return an error for unknown journey" in {
      Binders.journeyPathBinder.bind("journey", "unknown") mustBe
        Left("Unknown journey: unknown")
    }
  }

  "JourneyType query string binder" - {
    implicit val stringBinder: QueryStringBindable[String] = QueryStringBindable.bindableString
    val binder = Binders.journeyTypeQueryStringBinder

    "bind valid JourneyType from query string" in {
      JourneyType.values.foreach { jt =>
        val result = binder.bind("jt", Map("jt" -> Seq(jt.entryName)))
        result mustBe Some(Right(jt))
      }
    }

    "unbind JourneyType to query string" in {
      JourneyType.values.foreach { jt =>
        binder.unbind("jt", jt) mustBe s"jt=${jt.entryName}"
      }
    }
  }
}