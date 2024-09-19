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

import com.google.inject.name.Names
import controllers.actions._
import navigation.{Navigator, RootNavigator, SippNavigator}
import play.api.inject.Binding
import play.api.{Configuration, Environment}
import services.validation.csv.CsvDocumentValidatorConfig

import java.time.{Clock, ZoneOffset}

class Module extends play.api.inject.Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(
      bind[DataRetrievalAction].to(classOf[DataRetrievalActionImpl]).eagerly(),
      bind[DataRequiredAction].to(classOf[DataRequiredActionImpl]).eagerly(),
      bind[DataCreationAction].to(classOf[DataCreationActionImpl]).eagerly(),
      bind[FormBundleRequiredAction].to(classOf[FormBundleRequiredActionImpl]).eagerly(),
      bind[VersionTaxYearRequiredAction].to(classOf[VersionTaxYearRequiredActionImpl]).eagerly(),
      bind[FormBundleOrVersionTaxYearRequiredAction]
        .to(classOf[FormBundleOrVersionTaxYearRequiredActionImpl])
        .eagerly(),
      bind[Clock].toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC)),
      bind[Navigator].qualifiedWith(Names.named("root")).to(classOf[RootNavigator]).eagerly(),
      bind[Navigator].qualifiedWith(Names.named("sipp")).to(classOf[SippNavigator]).eagerly(),
      bind[CsvDocumentValidatorConfig].toInstance(csvDocumentValidatorConfig(configuration)).eagerly(),
      if (configuration.get[Boolean]("mongodb.encryption.enabled")) {
        bind[Crypto].to(classOf[CryptoImpl]).eagerly()
      } else {
        bind[Crypto].toInstance(Crypto.noop).eagerly()
      }
    )

  private def csvDocumentValidatorConfig(configuration: Configuration): CsvDocumentValidatorConfig = {
    val csvErrorLimit = configuration.get[Int]("validation.csv.error-limit")
    CsvDocumentValidatorConfig(csvErrorLimit)
  }
}
