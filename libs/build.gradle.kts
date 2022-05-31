import com.android.build.gradle.LibraryExtension
import com.android.builder.model.ApiVersion

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

val rootAbsolutePath: String = projectDir.parent

data class ApiVersionImpl(private val apiLevel: Int) : ApiVersion {
  override fun getApiLevel(): Int {
    return this.apiLevel
  }

  override fun getCodename(): String? {
    return null
  }

  override fun getApiString(): String {
    return this.apiLevel.toString()
  }
}

subprojects {
  apply(plugin = "com.android.library")
  apply(plugin = "kotlin-android")
  apply(plugin = "org.jlleitschuh.gradle.ktlint")

  project.apply(from = "$rootAbsolutePath/constants.gradle")
  project.apply(from = "$rootAbsolutePath/libs/constants.gradle")

  configure<LibraryExtension> {
    compileSdkVersion = "android-${project.extra["compileSdk"]}"

    defaultConfig {
      minSdk = project.extra["minSdk"] as Int
      targetSdk = project.extra["targetSdk"] as Int
    }
  }

  dependencies {
    val implementation by configurations
    implementation("io.reactivex.rxjava2:rxjava:${project.extra["rxJava"]}")
    implementation("io.reactivex.rxjava2:rxandroid:${project.extra["rxAndroid"]}")
  }
}

configure(arrayListOf(
  project(":libs:webview")
)) {
  dependencies {
    val implementation by configurations
    implementation("androidx.constraintlayout:constraintlayout:${project.extra["constraintLayout"]}")
    implementation("androidx.webkit:webkit:1.4.0")
    implementation("com.facebook.shimmer:shimmer:${project.extra["shimmer"]}")
    implementation("com.google.code.gson:gson:${project.extra["gson"]}")

    implementation(project(":libs:commonview"))
  }
}

configure(arrayListOf(
  project(":libs:webview:javascriptinterface:notification")
)) {
  dependencies {
    val implementation by configurations
    implementation("com.google.android.material:material:${project.extra["material"]}")

    implementation(project(":libs:webview"))
  }
}

configure(arrayListOf(
  project(":libs:webview:javascriptinterface:sharedpreferences")
)) {
  dependencies {
    val implementation by configurations
    implementation("com.google.code.gson:gson:${project.extra["gson"]}")

    implementation(project(":libs:webview"))
  }
}
