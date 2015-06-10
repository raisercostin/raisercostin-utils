organization := "org.raisercostin"

name := "jedi-io"

version := "0.9-SNAPSHOT"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
	"net.sf.jopt-simple" % "jopt-simple" % "2.4.1" intransitive() //exclude("org.apache.ant" % "ant")
	,"dom4j" % "dom4j" % "1.6.1"
	,"jaxen" % "jaxen" % "1.1.6"
	,"org.scalatest" %% "scalatest" % "2.2.4" //% "test"
	,"junit" % "junit" % "4.10" //% "test"
	,"org.slf4j" % "slf4j-api" % "1.7.5"
	,"org.slf4j" % "slf4j-simple" % "1.7.5"
	,"commons-io" % "commons-io" % "2.4"
)

sbtPlugin := true

// This is an example.  bintray-sbt requires licenses to be specified 
// (using a canonical name).
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))


resolvers += "raisercostin" at "https://raisercostin.googlecode.com/svn/maven2"

EclipseKeys.eclipseOutput := Some("target2/eclipse")





//publishing to bintray
bintrayOrganization := Some(organization.value)

bintrayReleaseOnPublish in ThisBuild := false

bintrayPackageLabels := Seq("scala", "io", "nio", "file", "path", "stream", "writer")

//publishMavenStyle := true
//bintrayPublishSettings
//repository in bintray := "generic"
