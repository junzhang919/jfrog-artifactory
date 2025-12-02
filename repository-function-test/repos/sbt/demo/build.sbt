name :="demo"
version :="0.0.1"
scalaVersion :="2.10.3"
organization := "ep.demo"
publishTo := Some("Artifactory Realm" at "https://arti.private-domain/artifactory/sbt/")
credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
