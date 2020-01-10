/* TODO
 4- use the plugin
 4- Make the path use scalaVersion
 5- better handling of case class
 */

import sbt.Keys._
import sbt._
import complete.DefaultParsers._
import plugins._

package CCV {
  object ObjectBuilder {
    import scala.reflect.ClassTag

    case class Var(name: String, vval: Any, vtype: String)

    implicit class Quoted(val s: String) {
      def q = "\"" + s + "\""
    }

    def make_var[T](vname: String, vval: T)(implicit ev: ClassTag[T]): Var =
      Var(vname, vval, ev.toString)

    def apply(name: String, vars: Seq[ObjectBuilder.Var]) =
      new ObjectBuilder(name, vars).toString

    def apply(path: String, name: String, vars: Seq[ObjectBuilder.Var], logger: Logger) = {
      val filename = s"${path}/${name.toLowerCase}.scala"
      logger.info(s"Generating values in class ${name}")
      logger.info(s"Writting sources to ${filename}")
      val str_code = new ObjectBuilder(name, vars).toString
      logger.debug(s"Writting:\n${str_code}")
      import java.io._
      val file = new File(filename)
      val bw = new BufferedWriter(new FileWriter(file, false))
      bw.write(str_code)
      bw.flush
      bw.close
      logger.info("Everything went well")
    }
  }

  class ObjectBuilder(name: String, vars: Seq[ObjectBuilder.Var]) {
    override def toString = {
      val svals = vars map{ case ObjectBuilder.Var(vn, vv, vt) => s"\tval ${vn}: ${vt} = ${vv}" } mkString "\n"
      s"""object ${name} {
${svals}
}
"""
    }
  }

  object CCVPlugin extends AutoPlugin {
    override def requires = JvmPlugin

    object autoImport {
      lazy val ccvObjs = settingKey[Seq[CCV.ObjectBuilder.Var]]("Objects containing the variable to access from code")
      lazy val ccvConfName = settingKey[String]("Name of the object containing the variables")
      lazy val ccvPath = settingKey[String]("Path where to write the file")
      lazy val ccv = taskKey[Unit]("Generate the object containing the value to set")
    }
    // TODO: set defaults
    import autoImport._

    override lazy val buildSettings = Seq(
      ccvConfName := "Config",
      ccvPath := s"target/scala-${scalaVersion.value}/src_managed/main/sbt-ccv",
      //ccvPath := (sourceManaged in Compile).value + "/sbt-ccv",
      // ccv := ccvTask.value
    )

    // lazy val ccvTask = Def.task {
    //   val path = (sourceManaged in Compile).value / "sbt-ccv" / s"${ccvConfName.value.toLowerCase}.scala"
    //   CCV.ObjectBuilder(path, ccvConfName.value, ccvObjs.value, streams.value.log)
    // }

    lazy val ccvFileTask = Def.task {
      val logger = streams.value.log
      val str_code = CCV.ObjectBuilder(ccvConfName.value, ccvObjs.value)
      val file = (sourceManaged in Compile).value / "sbt-ccv" / s"${ccvConfName.value.toLowerCase}.scala"

      logger.info(s"Generating values in class ${ccvConfName.value}")
      logger.info(s"Writting sources to ${file.getName}")
      logger.debug(s"Writting:\n${str_code}")

      IO.write(file, str_code)
      Seq(file)
    }

    override lazy val projectSettings = Seq(
      sourceGenerators in Compile += ccvFileTask.taskValue
    )
  }
}
