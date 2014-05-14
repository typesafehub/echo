import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtAspectj
import com.typesafe.sbt.SbtAspectj.{ Aspectj, AspectjKeys }
import com.typesafe.sbt.SbtCotest
import com.typesafe.sbt.SbtCotest.CotestKeys.cotestProjectName
import com.typesafe.sbt.SbtGit
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

object EchoBuild extends Build {

  lazy val echo = Project(
    id = "echo",
    base = file("."),
    settings = parentSettings ++ Seq(
      name := "echo",
      parallelExecution in GlobalScope := false
    ),
    aggregate = Seq(
      trace, collect, cotests, sigarLibs
    )
  )

  lazy val trace = Project(
    id = "trace",
    base = file("trace"),
    settings = parentSettings ++ Seq(
      name := "echo-all-trace"
    ),
    aggregate = Seq(
      event29, event210, event211,
      trace29, trace210, trace211,
      traceAkka20,
      traceScala210,
      traceAkka21, traceAkka22, traceAkka23Scala210,
      tracePlayCommon, tracePlay21, tracePlay22, tracePlay23Scala210,
      traceScala211, traceAkka23Scala211, tracePlay23Scala211
    )
  )

  // temporary project during transition time for tracing
  lazy val event29 = Project(
    id = "event29",
    base = file("trace/event"),
    settings = defaultSettings ++ Seq(
      name := "trace-event",
      scalaVersion := Dependency.V.Scala29,
      crossPaths := true,
      target <<= target / "29",
      libraryDependencies ++= Dependencies.event
    )
  )

  // temporary project during transition time for tracing
  lazy val event210 = Project(
    id = "event210",
    base = file("trace/event"),
    settings = defaultSettings ++ Seq(
      name := "trace-event",
      scalaVersion := Dependency.V.Scala210,
      crossPaths := true,
      target <<= target / "210",
      libraryDependencies ++= Dependencies.event
    )
  )

  lazy val event211 = Project(
    id = "event211",
    base = file("trace/event"),
    settings = defaultSettings ++ Seq(
      name := "trace-event",
      scalaVersion := Dependency.V.Scala211,
      crossPaths := true,
      target <<= target / "211",
      libraryDependencies ++= Dependencies.event
    )
  )

  lazy val trace29 = Project(
    id = "trace29",
    base = file("trace/core"),
    dependencies = Seq(event29),
    settings = defaultSettings ++ Seq(
      name := "trace-core",
      scalaVersion := Dependency.V.Scala29,
      crossPaths := true,
      target <<= target / "29",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.9" / "scala",
      libraryDependencies ++= Dependencies.trace29
    )
  )

  lazy val trace210 = Project(
    id = "trace210",
    base = file("trace/core"),
    dependencies = Seq(event210),
    settings = defaultSettings ++ Seq(
      name := "trace-core",
      scalaVersion := Dependency.V.Scala210,
      crossPaths := true,
      target <<= target / "210",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.10" / "scala",
      libraryDependencies ++= Dependencies.trace
    )
  )

  lazy val trace211 = Project(
    id = "trace211",
    base = file("trace/core"),
    dependencies = Seq(event211),
    settings = defaultSettings ++ Seq(
      name := "trace-core",
      scalaVersion := Dependency.V.Scala211,
      crossPaths := true,
      target <<= target / "211",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.11" / "scala",
      libraryDependencies ++= Dependencies.trace211
    )
  )

  lazy val traceAkka20 = Project(
    id = "trace-akka20",
    base = file("trace/akka/2.0.x"),
    dependencies = Seq(trace29),
    settings = defaultSettings ++ aspectjSettings ++ Seq(
      name := "trace-akka-" + Dependency.V.Akka20,
      normalizedName <<= name,
      scalaVersion := Dependency.V.Scala29,
      libraryDependencies ++= Dependencies.traceAkka(Dependency.V.Akka20, CrossVersion.Disabled),
      ivyXML := Dependencies.traceAkkaExcludes,
      // ignore deprecation warnings in akka
      scalacOptions ~= { _ diff Seq("-deprecation") }
    )
  )

  lazy val traceScala210 = Project(
    id = "trace-scala210",
    base = file("trace/scala/2.10.x"),
    dependencies = Seq(trace210),
    settings = defaultSettings ++ aspectjSettings ++ Seq(
      name := "trace-scala-" + Dependency.V.Scala210,
      normalizedName <<= name,
      scalaVersion := Dependency.V.Scala210
    )
  )

  lazy val traceAkka21 = Project(
    id = "trace-akka21",
    base = file("trace/akka/2.1.x"),
    dependencies = Seq(traceScala210),
    settings = defaultSettings ++ aspectjSettings ++ Seq(
      name := "trace-akka-" + Dependency.V.Akka21,
      normalizedName <<= name,
      scalaVersion := Dependency.V.Scala210,
      libraryDependencies ++= Dependencies.traceAkka(Dependency.V.Akka21, CrossVersion.binary),
      ivyXML := Dependencies.traceAkkaExcludes
    )
  )

  lazy val traceAkka22 = Project(
    id = "trace-akka22",
    base = file("trace/akka/2.2.x"),
    dependencies = Seq(traceScala210),
    settings = defaultSettings ++ aspectjSettings ++ Seq(
      name := "trace-akka-" + Dependency.V.Akka22,
      normalizedName <<= name,
      scalaVersion := Dependency.V.Scala210,
      crossPaths := true,
      libraryDependencies ++= Dependencies.traceAkka(Dependency.V.Akka22, CrossVersion.binary),
      ivyXML := Dependencies.traceAkkaExcludes
    )
  )

  lazy val traceAkka23Scala210 = Project(
    id = "trace-akka23-scala210",
    base = file("trace/akka/2.3.x"),
    dependencies = Seq(traceScala210),
    settings = defaultSettings ++ aspectjSettings ++ Seq(
      name := "trace-akka-" + Dependency.V.Akka23,
      normalizedName <<= name,
      publishToPublicRepos,
      scalaVersion := Dependency.V.Scala210,
      crossPaths := true,
      target <<= target / "210",
      libraryDependencies ++= Dependencies.traceAkka(Dependency.V.Akka23, CrossVersion.binary),
      ivyXML := Dependencies.traceAkkaExcludes
    )
  )

  lazy val traceAkka23Scala211 = Project(
    id = "trace-akka23-scala211",
    base = file("trace/akka/2.3.x"),
    dependencies = Seq(traceScala211),
    settings = defaultSettings ++ aspectjSettings ++ Seq(
      name := "trace-akka-" + Dependency.V.Akka23,
      normalizedName <<= name,
      publishToPublicRepos,
      scalaVersion := Dependency.V.Scala211,
      crossPaths := true,
      target <<= target / "211",
      libraryDependencies ++= Dependencies.traceAkka(Dependency.V.Akka23, CrossVersion.binary),
      ivyXML := Dependencies.traceAkkaExcludes
    )
  )

  lazy val tracePlayCommon = Project(
    id = "trace-play-common",
    base = file("trace/play/common"),
    settings = defaultSettings ++ Seq(
      name := "trace-play-common",
      scalaVersion := Dependency.V.Scala210
    )
  )

  lazy val tracePlay21 = Project(
    id = "trace-play21",
    base = file("trace/play/2.1.x"),
    dependencies = Seq(traceAkka21, tracePlayCommon),
    settings = defaultSettings ++ aspectjSettings ++ Seq(
      name := "trace-play-" + Dependency.V.Play21,
      normalizedName <<= name,
      scalaVersion := Dependency.V.Scala210,
      libraryDependencies ++= Dependencies.play21Trace,
      ivyXML := Dependencies.traceAkkaExcludes
    )
  )

  lazy val tracePlay22 = Project(
    id = "trace-play22",
    base = file("trace/play/2.2.x"),
    dependencies = Seq(traceAkka22, tracePlayCommon),
    settings = defaultSettings ++ aspectjSettings ++ Seq(
      name := "trace-play-" + Dependency.V.Play22,
      normalizedName <<= name,
      scalaVersion := Dependency.V.Scala210,
      libraryDependencies ++= Dependencies.play22Trace,
      ivyXML := Dependencies.traceAkkaExcludes
    )
  )

  lazy val tracePlay23Scala210 = Project(
    id = "trace-play23-scala210",
    base = file("trace/play/2.3.x"),
    dependencies = Seq(traceAkka23Scala210, tracePlayCommon),
    settings = defaultSettings ++ aspectjSettings ++ Seq(
      name := "trace-play-" + Dependency.V.Play23,
      normalizedName <<= name,
      publishToPublicRepos,
      scalaVersion := Dependency.V.Scala210,
      crossPaths := true,
      target <<= target / "210",
      libraryDependencies ++= Dependencies.play23Trace,
      ivyXML := Dependencies.traceAkkaExcludes
    )
  )

  lazy val tracePlay23Scala211 = Project(
    id = "trace-play23-scala211",
    base = file("trace/play/2.3.x"),
    dependencies = Seq(traceAkka23Scala211, tracePlayCommon),
    settings = defaultSettings ++ aspectjSettings ++ Seq(
      name := "trace-play-" + Dependency.V.Play23,
      normalizedName <<= name,
      publishToPublicRepos,
      crossPaths := true,
      target <<= target / "211",
      scalaVersion := Dependency.V.Scala211,
      libraryDependencies ++= Dependencies.play23Trace,
      ivyXML := Dependencies.traceAkkaExcludes
    )
  )

  lazy val traceScala211 = Project(
    id = "trace-scala211",
    base = file("trace/scala/2.11.x"),
    dependencies = Seq(trace211),
    settings = defaultSettings ++ aspectjSettings ++ Seq(
      name := "trace-scala-" + Dependency.V.Scala211,
      normalizedName <<= name,
      scalaVersion := Dependency.V.Scala211
    )
  )

  lazy val collect = Project(
    id = "collect",
    base = file("collect"),
    dependencies = Seq(event210),
    settings = defaultSettings ++ Seq(
      name := "trace-collect",
      libraryDependencies ++= Dependencies.collect
    )
  )

  lazy val cotests = Project(
    id = "cotests",
    base = file("cotests"),
    settings = defaultSettings ++ noPublish ++
      SbtCotest.cotestSettings(
        cotestsTraceAkka20, cotestsTraceAkka21, cotestsTraceAkka22, cotestsTraceAkka23Scala210, cotestsTraceAkka23Scala211,
        cotestsTracePlay21, cotestsTracePlay22, cotestsTracePlay23Scala210, cotestsTracePlay23Scala211,
        cotestsTrace2Akka20, cotestsTrace2Akka21, cotestsTrace2Akka22, cotestsTrace2Akka23Scala210, cotestsTrace2Akka23Scala211,
        cotestsCollect)
  )

  lazy val cotestsCommon29 = Project(
    id = "cotests-common29",
    base = file("cotests/common"),
    settings = defaultSettings ++ Seq(
      name := "cotests-common",
      scalaVersion := Dependency.V.Scala29,
      crossPaths := true,
      target <<= target / "29",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.9" / "scala",
      libraryDependencies ++= Dependencies.cotests29
    )
  )

  lazy val cotestsCommon210 = Project(
    id = "cotests-common210",
    base = file("cotests/common"),
    settings = defaultSettings ++ Seq(
      name := "cotests-common",
      scalaVersion := Dependency.V.Scala210,
      crossPaths := true,
      target <<= target / "210",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.10" / "scala",
      libraryDependencies ++= Dependencies.cotests
    )
  )

  lazy val cotestsCommon211 = Project(
    id = "cotests-common211",
    base = file("cotests/common"),
    settings = defaultSettings ++ Seq(
      name := "cotests-common",
      scalaVersion := Dependency.V.Scala211,
      crossPaths := true,
      target <<= target / "211",
      scalaSource in Test := baseDirectory.value / "src" / "test" / "2.11" / "scala",
      libraryDependencies ++= Dependencies.cotests211
    )
  )

  lazy val cotestsTraceAkka20 = Project(
    id = "cotests-trace-akka20",
    base = file("cotests/trace/akka/2.0"),
    dependencies = Seq(cotestsCommon29 % "test->test", traceAkka20),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-akka-2.0",
      scalaVersion := Dependency.V.Scala29,
      cotestProjectName := "trace"
    )
  )

  lazy val cotestsTraceAkka21 = Project(
    id = "cotests-trace-akka21",
    base = file("cotests/trace/akka/2.1"),
    dependencies = Seq(cotestsCommon210 % "test->test", traceAkka21),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-akka-2.1",
      scalaVersion := Dependency.V.Scala210,
      cotestProjectName := "trace",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
    )
  )

  lazy val cotestsTraceAkka22 = Project(
    id = "cotests-trace-akka22",
    base = file("cotests/trace/akka/2.2/2.10"),
    dependencies = Seq(cotestsCommon210 % "test->test", traceAkka22),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-akka-2.2-scala-2.10",
      scalaVersion := Dependency.V.Scala210,
      cotestProjectName := "trace",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
    )
  )

  lazy val cotestsTraceAkka23Scala210 = Project(
    id = "cotests-trace-akka23-scala210",
    base = file("cotests/trace/akka/2.3/2.10"),
    dependencies = Seq(cotestsCommon210 % "test->test", traceAkka23Scala210),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-akka-2.3-scala-2.10",
      scalaVersion := Dependency.V.Scala210,
      cotestProjectName := "trace",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
    )
  )

  lazy val cotestsTraceAkka23Scala211 = Project(
    id = "cotests-trace-akka23-scala211",
    base = file("cotests/trace/akka/2.3/2.11"),
    dependencies = Seq(cotestsCommon211 % "test->test", traceAkka23Scala211),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-akka-2.3-scala-2.11",
      scalaVersion := Dependency.V.Scala211,
      cotestProjectName := "trace",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
    )
  )

  lazy val cotestsTracePlayCommon21 = Project(
    id = "cotests-trace-play-common21",
    base = file("cotests/trace/play/common"),
    dependencies = Seq(cotestsCommon210 % "test->test", tracePlay21 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-play-common21",
      scalaVersion := Dependency.V.Scala210,
      scalaSource in Test := baseDirectory.value / "src" / "test" / "play-2.1" / "scala",
      crossPaths := true,
      target <<= target / "play-21"
    )
  )

  lazy val cotestsTracePlayCommon22 = Project(
    id = "cotests-trace-play-common22",
    base = file("cotests/trace/play/common"),
    dependencies = Seq(cotestsCommon210 % "test->test", tracePlay22 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-play-common22",
      scalaVersion := Dependency.V.Scala210,
      scalaSource in Test := baseDirectory.value / "src" / "test" / "play-2.2" / "scala",
      crossPaths := true,
      target <<= target / "play-22"
    )
  )

  lazy val cotestsTracePlayCommon23Scala210 = Project(
    id = "cotests-trace-play-common23-scala210",
    base = file("cotests/trace/play/common"),
    dependencies = Seq(cotestsCommon210 % "test->test", tracePlay23Scala210 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-play-common23-scala210",
      scalaVersion := Dependency.V.Scala210,
      libraryDependencies += Dependency.play23ws,
      scalaSource in Test := baseDirectory.value / "src" / "test" / "play-2.3" / "scala",
      crossPaths := true,
      target <<= target / "play-23/scala-2.10"
    )
  )

  lazy val cotestsTracePlayCommon23Scala211 = Project(
    id = "cotests-trace-play-common23-scala211",
    base = file("cotests/trace/play/common"),
    dependencies = Seq(cotestsCommon211 % "test->test", tracePlay23Scala211 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-play-common23-scala211",
      scalaVersion := Dependency.V.Scala211,
      libraryDependencies += Dependency.play23ws,
      scalaSource in Test := baseDirectory.value / "src" / "test" / "play-2.3" / "scala",
      crossPaths := true,
      target <<= target / "play-23/scala-2.11"
    )
  )

  lazy val cotestsTracePlay21 = Project(
    id = "cotests-trace-play21",
    base = file("cotests/trace/play/2.1.x"),
    dependencies = Seq(cotestsTraceAkka21 % "test->test", tracePlay21 % "test->test", cotestsTracePlayCommon21 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-play-2.1.x",
      scalaVersion := Dependency.V.Scala210,
      cotestProjectName := "trace",
      javaOptions in Test ++= Seq(
        "-Dactivator.trace.enabled=true",
        "-Dactivator.trace.futures=off",
        "-Dactivator.trace.iteratees=on",
        "-Dactivator.trace.events.futures=off",
        "-Dactivator.trace.events.iteratees=on",
        "-Dactivator.trace.play.traceable./get/filtered/*=off",
        "-Dactivator.trace.play.sampling./getSampled=3",
        "-Dactivator.trace.use-dispatcher-monitor=off",
        "-Dactivator.trace.use-system-metrics-monitor=off"
      )
    )
  )

  lazy val cotestsTracePlay22 = Project(
    id = "cotests-trace-play22",
    base = file("cotests/trace/play/2.2.x"),
    dependencies = Seq(cotestsTraceAkka22 % "test->test", tracePlay22 % "test->test", cotestsTracePlayCommon22 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-play-2.2.x",
      scalaVersion := Dependency.V.Scala210,
      cotestProjectName := "trace",
      javaOptions in Test ++= Seq(
        "-Dactivator.trace.enabled=true",
        "-Dactivator.trace.futures=off",
        "-Dactivator.trace.iteratees=on",
        "-Dactivator.trace.events.futures=off",
        "-Dactivator.trace.events.iteratees=on",
        "-Dactivator.trace.use-dispatcher-monitor=off",
        "-Dactivator.trace.play.traceable./get/filtered/*=off",
        "-Dactivator.trace.play.sampling./getSampled=3",
        "-Dactivator.trace.use-system-metrics-monitor=off"
      ),
      // ignore deprecation warnings (intended usage of deprecated api)
      scalacOptions ~= { _ diff Seq("-deprecation") }
    )
  )

  lazy val cotestsTracePlay23Scala210 = Project(
    id = "cotests-trace-play23-scala210",
    base = file("cotests/trace/play/2.3.x/2.10"),
    dependencies = Seq(cotestsTraceAkka23Scala210 % "test->test", tracePlay23Scala210 % "test->test", cotestsTracePlayCommon23Scala210 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-play-2.3.x-scala-2.10",
      scalaVersion := Dependency.V.Scala210,
      cotestProjectName := "trace",
      javaOptions in Test ++= Seq(
        "-Datmos.trace.enabled=true",
        "-Datmos.trace.futures=off",
        "-Datmos.trace.iteratees=on",
        "-Datmos.trace.events.futures=off",
        "-Datmos.trace.events.iteratees=on",
        "-Datmos.trace.use-dispatcher-monitor=off",
        "-Datmos.trace.play.traceable./get/filtered/*=off",
        "-Datmos.trace.play.sampling./getSampled=3",
        "-Datmos.trace.use-system-metrics-monitor=off"
      ),
      javaOptions in Test += ("-Datmos.integrationtest=" + System.getProperty("atmos.integrationtest", "off")),
      // ignore deprecation warnings (intended usage of deprecated api)
      scalacOptions ~= { _ diff Seq("-deprecation") }
    )
  )

  lazy val cotestsTracePlay23Scala211 = Project(
    id = "cotests-trace-play23-scala211",
    base = file("cotests/trace/play/2.3.x/2.11"),
    dependencies = Seq(cotestsTraceAkka23Scala211 % "test->test", tracePlay23Scala211 % "test->test", cotestsTracePlayCommon23Scala211 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace-play-2.3.x-scala-2.11",
      scalaVersion := Dependency.V.Scala211,
      cotestProjectName := "trace",
      javaOptions in Test ++= Seq(
        "-Datmos.trace.enabled=true",
        "-Datmos.trace.futures=off",
        "-Datmos.trace.iteratees=on",
        "-Datmos.trace.events.futures=off",
        "-Datmos.trace.events.iteratees=on",
        "-Datmos.trace.use-dispatcher-monitor=off",
        "-Datmos.trace.play.traceable./get/filtered/*=off",
        "-Datmos.trace.play.sampling./getSampled=3",
        "-Datmos.trace.use-system-metrics-monitor=off"
      ),
      javaOptions in Test += ("-Datmos.integrationtest=" + System.getProperty("atmos.integrationtest", "off")),
      // ignore deprecation warnings (intended usage of deprecated api)
      scalacOptions ~= { _ diff Seq("-deprecation") }
    )
  )

 lazy val cotestsTrace2Akka20 = Project(
    id = "cotests-trace2-akka20",
    base = file("cotests/trace2/akka/2.0"),
    dependencies = Seq(cotestsTraceAkka20 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace2-akka-2.0",
      scalaVersion := Dependency.V.Scala29,
      cotestProjectName := "trace2"
    )
  )

  lazy val cotestsTrace2Akka21 = Project(
    id = "cotests-trace2-akka21",
    base = file("cotests/trace2/akka/2.1"),
    dependencies = Seq(cotestsTraceAkka21 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace2-akka-2.1",
      scalaVersion := Dependency.V.Scala210,
      cotestProjectName := "trace2",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
    )
  )

  lazy val cotestsTrace2Akka22 = Project(
    id = "cotests-trace2-akka22",
    base = file("cotests/trace2/akka/2.2/2.10"),
    dependencies = Seq(cotestsTraceAkka22 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace2-akka-2.2",
      scalaVersion := Dependency.V.Scala210,
      cotestProjectName := "trace2",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
    )
  )

  lazy val cotestsTrace2Akka23Scala210 = Project(
    id = "cotests-trace2-akka23-scala211",
    base = file("cotests/trace2/akka/2.3/2.10"),
    dependencies = Seq(cotestsTraceAkka23Scala210 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace2-akka-2.3-scala-2.10",
      scalaVersion := Dependency.V.Scala210,
      cotestProjectName := "trace2",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
    )
  )

  lazy val cotestsTrace2Akka23Scala211 = Project(
    id = "cotests-trace2-akka23-scala211",
    base = file("cotests/trace2/akka/2.3/2.11"),
    dependencies = Seq(cotestsTraceAkka23Scala211 % "test->test"),
    settings = defaultSettings ++ tracedTestSettings ++ Seq(
      name := "cotests-trace2-akka-2.3-scala-2.11",
      scalaVersion := Dependency.V.Scala211,
      cotestProjectName := "trace2",
      javaOptions in Test += "-Dactivator.trace.enabled=true"
    )
  )

  lazy val cotestsCollect = Project(
    id = "cotests-collect",
    base = file("cotests/collect"),
    dependencies = Seq(cotestsCommon210 % "test->test", collect % "compile;test->test"),
    settings = defaultSettings ++ Seq(
      cotestProjectName := "collect"
    )
  )

  lazy val sigarLibs = Project(
    id = "sigar-libs",
    base = file("sigar"),
    settings = defaultSettings ++ Seq(
      name := "trace-sigar-libs",
      resourceDirectory in Compile <<= baseDirectory / "lib",
      autoScalaLibrary := false,
      pomIncludeRepository := { _ => false },
      publishArtifact in (Compile, packageDoc) := false,
      publishArtifact in (Compile, packageSrc) := false
    )
  )

  // Settings
  lazy val versionSettings = SbtGit.versionWithGit ++ Seq(
    SbtGit.git.baseVersion := "0.1.1"
  )

  lazy val buildSettings = versionSettings ++ Seq(
    organization := "com.typesafe.trace",
    scalaVersion := Dependency.V.Scala210,
    crossPaths := false,
    publishArtifact in packageSrc := false,
    publishArtifact in packageDoc := false,
    organizationName := "Typesafe Inc.",
    organizationHomepage := Some(url("http://www.typesafe.com")),
    pomIncludeRepository := { _ => false },
    publishToPublicRepos,
    // TODO: reenable once dependencies are aligned again
    // disable scala library conflict warnings
    conflictWarning := ConflictWarning.disable,
    // TODO: can be reenabled when all scala versions are final again
    // disable scala binary version warnings
    ivyScala ~= { _.map(_.copy(checkExplicit = false, overrideScalaVersion = false)) },
    // reset these per project rather than globally
    scalaBinaryVersion <<= scalaVersion { v => if (v contains "-") v else CrossVersion.binaryScalaVersion(v) },
    crossScalaVersions <<= Seq(scalaVersion).join
  )

  def publishToPublicRepos = publishToRepos("maven-releases", "maven-snapshots")

  def publishToRepos(releases: String, snapshots: String) = {
    publishTo <<= (version) { v => if (v endsWith "SNAPSHOT") typesafeRepo(snapshots) else typesafeRepo(releases) }
  }

  def typesafeRepo(name: String) = Some(name at "https://private-repo.typesafe.com/typesafe/" + name)

  def noPublish = Seq(
    publish := {},
    publishLocal := {}
  )

  lazy val parentSettings = Defaults.defaultSettings ++ buildSettings ++ noPublish

  lazy val projectSettings = buildSettings ++ formatSettings ++ Seq(
    resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",

    // compile options
    scalacOptions <++= scalaVersion map { sv =>
      val opts = Seq("-encoding", "UTF-8", "-deprecation", "-unchecked")
      val opts210 = Seq("-feature", "-Xlint")
      if (sv.startsWith("2.10")) opts ++ opts210 else opts
    },
    javacOptions  ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
  )

  lazy val defaultSettings = Defaults.defaultSettings ++ projectSettings

  def sigarDir = resourceDirectory in (sigarLibs, Compile)

  def weaveAgent = AspectjKeys.weaverOptions in Aspectj in traceScala210

  def fileProperty(property: String, file: File) = "-D%s=%s" format (property, file.absolutePath)

  // Format settings

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
  }

  // Aspectj settings

  lazy val aspectjSettings = SbtAspectj.aspectjSettings ++ inConfig(Aspectj)(Seq(
    AspectjKeys.compileOnly := true,
    AspectjKeys.lintProperties += "typeNotExposedToWeaver = ignore",
    products in Compile <++= products in Aspectj,
    AspectjKeys.ajc <<= AspectjKeys.ajc triggeredBy (compile in Compile)
  ))

  // Traced test settings

  lazy val tracedTestSettings = Seq(
    Keys.fork in Test := true,
    javaOptions in Test <++= (weaveAgent, sigarDir) map {
      (weaveOpts, sigar) => weaveOpts ++ Seq(fileProperty("java.library.path", sigar))
    }
  )
}

// Dependencies

object Dependencies {
  import Dependency._

  val event = Seq(config, protobuf)

  val trace29 = Seq(Test.scalatest29, Test.junit)

  val trace = Seq(Test.scalatest, Test.junit)

  val trace211 = Seq(Test.scalatest, Test.junit)

  def play21Trace = Seq(
    play21, Test.playTest21
  )

  def play22Trace = Seq(
    play22, Test.playTest22
  )

  def play23Trace = Seq(
    play23, Test.playTest23
  )

  def traceAkka(version: String, crossVersion: CrossVersion) = Seq(
    "com.typesafe.akka" % "akka-actor"  % version cross crossVersion,
    "com.typesafe.akka" % "akka-remote" % version cross crossVersion,
    "com.typesafe.akka" % "akka-slf4j"  % version cross crossVersion,
    sigar
  )

  val traceAkkaExcludes = {
    <dependencies>
      <exclude module="slf4j-simple"/>
    </dependencies>
  }

  val collect29 = Seq(
    akkaSlf4j, slf4j, logback,
    Test.akkaTestKit, Test.scalatest29, Test.junit
  )

  val collect = Seq(
    akkaSlf4j, slf4j, logback,
    Test.akkaTestKit, Test.scalatest, Test.junit
  )

  val cotests29 = Seq(Test.scalatest29, Test.junit, Test.logback)

  val cotests = Seq(Test.scalatest, Test.junit, Test.logback)

  val cotests211 = Seq(Test.scalatest, Test.junit, Test.logback)
}

object Dependency {

  // Versions

  object V {
    val Akka20         = "2.0.5"
    val Akka21         = "2.1.4"
    val Akka22         = "2.2.4"
    val Akka23         = "2.3.2"
    val Config         = "1.0.2"
    val Logback        = "1.0.13"
    val Play21         = "2.1.5"
    val Play22         = "2.2.2"
    val Play23         = "2.3.0-RC1"
    val Protobuf       = "2.4.1"
    val Scala29        = "2.9.2"
    val Scala210       = "2.10.3"
    val Scala211       = "2.11.0"
    val Scalatest      = "2.1.5"
    val Scalatest29    = "1.9.1"
    val Slf4j          = "1.7.5"
    val Akka22Scala211 = Akka22
  }

  val akkaSlf4j         = "com.typesafe.akka"         %% "akka-slf4j"         % V.Akka22
  val play21            = "play"                      %% "play"               % V.Play21
  val play22            = "com.typesafe.play"         %% "play"               % V.Play22
  val play23            = "com.typesafe.play"         %% "play"               % V.Play23
  val play23ws          = "com.typesafe.play"         %% "play-ws"            % V.Play23
  val config            = "com.typesafe"              % "config"              % V.Config
  val logback           = "ch.qos.logback"            % "logback-classic"     % V.Logback
  val protobuf          = "com.google.protobuf"       % "protobuf-java"       % V.Protobuf
  val sigar             = "org.fusesource"            % "sigar"               % "1.6.4"
  val slf4j             = "org.slf4j"                 % "slf4j-api"           % V.Slf4j

  object Test {
    val akkaTestKit   = "com.typesafe.akka"   %% "akka-testkit"            % V.Akka22      % "test"
    val junit         = "junit"               % "junit"                    % "4.5"         % "test"
    val logback       = "ch.qos.logback"      % "logback-classic"          % V.Logback     % "test"
    val playTest21    = "play"                %% "play-test"               % V.Play21      % "test"
    val playTest22    = "com.typesafe.play"   %% "play-test"               % V.Play22      % "test"
    val playTest23    = "com.typesafe.play"   %% "play-test"               % V.Play23      % "test"
    val scalatest29   = "org.scalatest"       %% "scalatest"               % V.Scalatest29 % "test"
    val scalatest     = "org.scalatest"       %% "scalatest"               % V.Scalatest   % "test"
  }
}
