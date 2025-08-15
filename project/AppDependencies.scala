import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.19.0"
  private val hmrcMongoVersion = "2.7.0"
  private val pekkoVersion = "1.1.0"

  private val pekko = "org.apache.pekko"

  private val dependencyOverrides = Seq(
    pekko %% "pekko-protobuf-v3"           % pekkoVersion,
    pekko %% "pekko-actor"                 % pekkoVersion,
    pekko %% "pekko-actor-typed"           % pekkoVersion,
    pekko %% "pekko-stream"                % pekkoVersion,
    pekko %% "pekko-slf4j"                 % pekkoVersion,
    pekko %% "pekko-serialization-jackson" % pekkoVersion
  )

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                %% "play-frontend-hmrc-play-30"             % "12.8.0",
    "uk.gov.hmrc"                %% "play-conditional-form-mapping-play-30"  % "3.3.0",
    "uk.gov.hmrc"                %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc.mongo"          %% "hmrc-mongo-play-30"                     % hmrcMongoVersion,
    "uk.gov.hmrc"                %% "tax-year"                               % "6.0.0",
    "org.typelevel"              %% "cats-core"                              % "2.12.0",
    "eu.timepit"                 %% "refined"                                % "0.11.2",
    "uk.gov.hmrc"                %% "domain-play-30"                         % "12.1.0",
    "uk.gov.hmrc"                %% "crypto-json-play-30"                    % "8.3.0",
    "io.circe"                   %% "circe-generic"                          % "0.14.9",
    "co.fs2"                     %% "fs2-reactive-streams"                   % "3.10.2",
    "com.beachape"               %% "enumeratum-play-json"                   % "1.8.1",
    "com.softwaremill.quicklens" %% "quicklens"                              % "1.9.7",
    "uk.gov.hmrc.objectstore"    %% "object-store-client-play-30"            % "2.4.0",
    "org.apache.pekko"           %% "pekko-connectors-csv"                   % "1.0.2",
  ) ++ dependencyOverrides

  val test = Seq(
    "uk.gov.hmrc"                %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"          %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatest"              %% "scalatest"               % "3.2.19",
    "org.scalatestplus"          %% "scalacheck-1-18"         % "3.2.19.0",
    "org.scalatestplus.play"     %% "scalatestplus-play"      % "7.0.1",
    "org.jsoup"                  %  "jsoup"                   % "1.18.1"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
