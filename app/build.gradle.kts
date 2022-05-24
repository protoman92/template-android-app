buildscript {
  val rootAbsolutePath = projectDir.parent
  apply(from = "$rootAbsolutePath/constants.gradle")

  repositories {
    maven { url = java.net.URI(project.extra["ktlintGradleMavenRepository"].toString()) }
  }

  dependencies {
    classpath("org.jlleitschuh.gradle:ktlint-gradle:${project.extra["ktlintGradle"]}")
  }
}

subprojects {
  apply(plugin = "org.jlleitschuh.gradle.ktlint")
}