package me.enkode.sbt.plugins

import sbt._, Keys._

object GraphvizPlugin extends AutoPlugin {
  override def trigger = allRequirements

  val Config = config("graphviz")

  object autoImport {
    val dotCmd = settingKey[String]("how to execute the 'dot' command")
    val dotOut = settingKey[File]("directory for rendered .dot files")

    val dotVerbose = settingKey[Boolean]("run dot in verbose mode")
    val dotOutputFormat = settingKey[String]("dot output")

    val dot = taskKey[Seq[File]]("render dot files")

    lazy val baseGraphvizSettings: Seq[Def.Setting[_]] = Seq(
      dotCmd := "dot",
      dotVerbose := false,
      dotOutputFormat := "png",

      sources := {
        val src = sourceDirectory.value
        ((src / "doc") ** "*.dot").get
      },

      dotOut := {
        target.value / "dot"
      },

      dot := {
        val log = streams.value.log
        def oFile(iFile: File): File = {
          dotOut.value  / s"${iFile.base}.${dotOutputFormat.value}"
        }

        if (!dotOut.value.exists()) {
          dotOut.value.mkdirs()
        }

        for {
          dotSrc ← sources.value
        } yield {
          val dotOut = oFile(dotSrc)

          val dotArgs = (if (dotVerbose.value) List("-v") else Nil) :::
            s"-T${dotOutputFormat.value}" ::
            dotSrc.getAbsolutePath ::
            "-o" :: dotOut.getAbsolutePath ::
            Nil

          log.info(s"running: ${dotCmd.value} ${dotArgs.mkString(" ")}")

          Process(dotCmd.value, dotArgs).!
          dotOut
        }
      }
    )
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Config)(baseGraphvizSettings) ++ Seq(
      watchSources <++= sources in Config
    )
}