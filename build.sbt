
lazy val `akka-service-discovery-core` = (project)
  .settings(
    libraryDependencies ++= Seq(
      Boilerplate.Modules.enumeratum
    ) ++ Seq(
      Boilerplate.Modules.scalatest
    ).map(_ % Test)
  )

lazy val `akka-service-discovery-cluster` = (project)
  .dependsOn(`akka-service-discovery-core`, `akka-service-discovery`)
  .configs(MultiJvm)
  .settings(
    libraryDependencies ++= Seq(
      Boilerplate.Modules.akka("actor"),
      Boilerplate.Modules.akkaDataReplication,
      Boilerplate.Modules.ficus,
      Boilerplate.Modules.argonaut
    ) ++ Seq(
      Boilerplate.Modules.scalatest,
      Boilerplate.Modules.akka("multi-node-testkit"),
      Boilerplate.Modules.akka("slf4j"),
      Boilerplate.Modules.slf4j_api,
      Boilerplate.Modules.logback
    ).map(_ % Test),

    compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
    parallelExecution in Test := false,
    executeTests in Test <<= (executeTests in Test, executeTests in MultiJvm) map {
      case (testResults, multiNodeResults)  =>
        val overall =
          if (testResults.overall.id < multiNodeResults.overall.id)
            multiNodeResults.overall
          else
            testResults.overall
        Tests.Output(overall,
          testResults.events ++ multiNodeResults.events,
          testResults.summaries ++ multiNodeResults.summaries)
    }
  )

lazy val `akka-service-discovery-eureka` = (project)
  .dependsOn(`akka-service-discovery-core`, `akka-service-discovery`)

lazy val `akka-service-discovery` = (project)
  .dependsOn(`akka-service-discovery-core`)
  .settings(
    libraryDependencies ++= Seq(
      Boilerplate.Modules.akka("actor"),
      Boilerplate.Modules.ficus
    )
  )

lazy val root = ((project) in file("."))
  .aggregate(`akka-service-discovery`, `akka-service-discovery-cluster`, `akka-service-discovery-eureka`, `akka-service-discovery-core`)
  .settings(inConfig(GraphvizPlugin.Config) {
    dot := {
      val pngs = dot.in(`akka-service-discovery-core`).value
      val copy = pngs map { png ⇒
        png → (file("doc") / png.name)
      }
      IO.copy(copy, overwrite = true)
      pngs
    }
  })
