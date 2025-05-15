ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "1.0.0"
ThisBuild / organization := "click.seichi"

ThisBuild / resolvers ++= Seq(
  "hub.spigotmc.org" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots",

  // Spigot に含まれる Bungeecord の依存のため
  "oss.sonatype.org" at "https://oss.sonatype.org/content/repositories/snapshots",
)

val providedDependencies112 = Seq(
  "org.spigotmc" % "spigot-api" % "1.12.2-R0.1-SNAPSHOT",
).map(_ % "provided")

val providedDependencies118 = Seq(
  "org.spigotmc" % "spigot-api" % "1.18.2-R0.1-SNAPSHOT",
).map(_ % "provided")

val dependenciesToEmbed = Seq(
  // DB
  "org.mariadb.jdbc" % "mariadb-java-client" % "3.5.3",
  "org.scalikejdbc" %% "scalikejdbc" % "4.3.2",

  "org.typelevel" %% "cats-core" % "2.13.0",
  "org.typelevel" %% "cats-effect" % "2.5.5",
)

ThisBuild / assemblyMergeStrategy := {
  case PathList(ps @ _*) if ps.last endsWith "LICENSE" => MergeStrategy.rename
  case PathList(ps @ _*) if ps.last endsWith "module-info.class" => MergeStrategy.last
  case PathList("org", "apache", "commons", "logging", xs @ _*) =>
    MergeStrategy.last
  case otherFile =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(otherFile)
}

lazy val common = (project in file("./src/common"))
  .settings(
    libraryDependencies := dependenciesToEmbed,
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
    Compile / unmanagedSourceDirectories += baseDirectory.value / "scala"
  )

lazy val version112 = (project in file("./src/1-12"))
  .dependsOn(common)
  .settings(
    libraryDependencies := providedDependencies112,
    javaHome := sys.env.get("JDK_8").map(file),
    javacOptions ++= Seq("-release", "8"),
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "scala",
    assembly / assemblyOutputPath := baseDirectory.value / "target" / "build" / "SerializedItemStackFixer-1-12.jar",
  )

lazy val version118 = (project in file("./src/1-18"))
  .dependsOn(common)
  .settings(
    libraryDependencies := providedDependencies118,
    javaHome := sys.env.get("JDK_17").map(file),
    javacOptions ++= Seq("-release", "17"),
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / unmanagedSourceDirectories += baseDirectory.value / "scala",
    assembly / assemblyOutputPath := baseDirectory.value / "target" / "build" / "SerializedItemStackFixer-1-18.jar",
  )

lazy val root = (project in file(""))
  .aggregate(common, version112, version118)
  .settings(
    libraryDependencies := dependenciesToEmbed,
    scalacOptions ++= Seq(
      "-encoding", "UTF-8"
    ),
  )
