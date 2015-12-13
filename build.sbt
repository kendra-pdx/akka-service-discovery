
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
  .settings(
    libraryDependencies ++= Seq(
      Boilerplate.Modules.akka("actor"),
      Boilerplate.Modules.akkaDataReplication
    )
  )

lazy val `akka-service-discovery-eureka` = (project)
  .dependsOn(`akka-service-discovery-core`, `akka-service-discovery`)

lazy val `akka-service-discovery` = (project)
  .dependsOn(`akka-service-discovery-core`)
  .settings(
    libraryDependencies ++= Seq(
      Boilerplate.Modules.akka("actor")
    )
  )

lazy val root = ((project) in file("."))
  .aggregate(`akka-service-discovery`, `akka-service-discovery-cluster`, `akka-service-discovery-eureka`)
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
