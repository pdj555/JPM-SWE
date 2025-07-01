ThisBuild / scalaVersion     := "2.13.14"
ThisBuild / version          := "1.0.0"
ThisBuild / organization     := "com.chase.aurora"

lazy val root = (project in file("."))
  .settings(
    name := "aurora-spark-job",
    libraryDependencies ++= Seq(
      // Spark Core Dependencies
      "org.apache.spark" %% "spark-sql" % "3.5.1" % "provided",
      "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.5.1",
      "org.apache.spark" %% "spark-streaming" % "3.5.1" % "provided",
      
      // Kafka & Serialization
      "org.apache.kafka" % "kafka-clients" % "3.7.0",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.17.1",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.17.1",
      
      // Configuration & Logging
      "com.typesafe" % "config" % "1.4.3",
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      
      // Testing
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
      "org.apache.spark" %% "spark-sql" % "3.5.1" % Test classifier "tests"
    ),
    
    // Assembly settings for creating fat JAR
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case "application.conf"            => MergeStrategy.concat
      case "reference.conf"              => MergeStrategy.concat
      case x => MergeStrategy.first
    },
    
    // Assembly JAR name
    assembly / assemblyJarName := s"${name.value}-${version.value}.jar",
    
    // Scala compiler options
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen"
    )
  )

// Enable SBT Assembly plugin
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.5") 