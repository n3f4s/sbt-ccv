/* TODO
 3- test the plugin
 4- use the plugin
 5- better handling of case class
 */

import sbt.Keys._
import sbt._
import complete.DefaultParsers._

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
    def apply(filename: String, name: String, vars: Seq[ObjectBuilder.Var]) = {
      val str_code = new ObjectBuilder(name, vars).toString
      import java.io._
      val file = new File(filename)
      val bw = new BufferedWriter(new FileWriter(file, false))
      bw.write(str_code)
      bw.flush
      bw.close
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
      ccvPath := "target/scala-2.12/src_managed/main/sbt-ccv",
      ccv := ccvTask.value
    )
    lazy val ccvTask = Def.task {
      CCV.ObjectBuilder(ccvConfName.value, ccvConfName.value, ccvObjs.value)
    }
  }
}

// object Test extends App {
//   import CCV._
//   import ObjectBuilder.Quoted
//   case class TCC(foo: Int, bar: String) {
//     override def toString = s"TCC($foo, $bar)"
//   }
//   println(new ObjectBuilder("Test", Seq(
//                               ObjectBuilder.make_var("foo", 1),
//                               ObjectBuilder.make_var("bar", "foo".q),
//                               ObjectBuilder.make_var("baz", TCC(10, "test".q)),
//                             )).toString)
// }
