import sbt._
import com.twitter.sbt._

class ScalaJsonProject(info: ProjectInfo) extends StandardProject(info) with SubversionPublisher {
  val specs = buildScalaVersion match {
    case "2.8.1" => "org.scala-tools.testing" % "specs_2.8.1" % "1.6.6" % "test"
    case "2.9.1" => "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9" % "test"
  }

  override def subversionRepository = Some("https://svn.twitter.biz/maven-public")

  override def disableCrossPaths = false

  override def compileOptions = super.compileOptions ++ Seq(Unchecked) ++
    compileOptions("-encoding", "utf8")

  override def pomExtra =
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
      <license>
        <name>Scala License</name>
        <url>http://www.scala-lang.org/node/146</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
}
