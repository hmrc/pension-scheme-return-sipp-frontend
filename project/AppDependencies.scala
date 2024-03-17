import play.core.PlayVersion
import sbt.*

object AppDependencies {

  private val bootstrapVersion = "7.23.0"
  private val hmrcMongoVersion = "1.4.0"
  val http4sVersion = "0.23.26"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"        %% "play-frontend-hmrc"             % "7.28.0-play-28",
    "uk.gov.hmrc"        %% "play-conditional-form-mapping"  % "1.13.0-play-28",
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-28"     % bootstrapVersion,
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-28"             % hmrcMongoVersion,
    "uk.gov.hmrc"        %% "tax-year"                       % "3.0.0",
    "org.typelevel"      %% "cats-core"                      % "2.10.0",
    "eu.timepit"         %% "refined"                        % "0.11.0",
    "uk.gov.hmrc"        %% "domain"                         % "8.1.0-play-28",
    "uk.gov.hmrc"        %% "crypto-json-play-28"            % "7.3.0",
    "com.lightbend.akka" %% "akka-stream-alpakka-csv"        % "4.0.0",
    "org.http4s"         %% "http4s-ember-client"            % http4sVersion,
    "org.http4s"         %% "http4s-dsl"                     % http4sVersion,
    "org.http4s"         %% "http4s-circe"                   % http4sVersion,
    "io.circe"           %% "circe-generic"                  % "0.14.6",
    "org.gnieh"          %% "fs2-data-csv"                   % "1.10.0",
    "org.gnieh"          %% "fs2-data-csv-generic"           % "1.10.0",
    "co.fs2"             %% "fs2-reactive-streams"           % "3.9.4"
  )

  val test = Seq(
    "com.typesafe.play"       %% "play-test"               % PlayVersion.current,
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28" % hmrcMongoVersion,
    "org.scalatest"           %% "scalatest"               % "3.2.10",
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.15.0",
    "org.scalatestplus"       %% "mockito-4-6"             % "3.2.15.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "5.1.0",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.15.4",
    "org.mockito"             %% "mockito-scala"           % "1.17.12",
    "org.scalacheck"          %% "scalacheck"              % "1.17.0",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.64.6"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
