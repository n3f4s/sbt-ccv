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
  }

  class ObjectBuilder(name: String, vars: Seq[ObjectBuilder.Var]) {
    override def toString = {
      // val svals = vars map{ case ObjectBuilder.Var(vn, vv, vt) => s"\tval ${vn}: ${vt} = ${vv}" } mkString "\n"
      val svals = vars map{ case ObjectBuilder.Var(vn, vv, vt) => s"\tval ${vn} = ${vv}" } mkString "\n"
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
    }

    import autoImport._

    override lazy val buildSettings = Seq(
      ccvConfName := "Config",
    )

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
