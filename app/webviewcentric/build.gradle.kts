import java.io.ByteArrayOutputStream

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.parcelize")
  id("org.jetbrains.kotlin.kapt")
}

val rootAbsolutePath: String = projectDir.parent

android {
  namespace = "com.swiften.templateapp.webviewcentric"
  compileSdk = 32

  defaultConfig {
    applicationId = "com.swiften.templateapp.webviewcentric"
    minSdk = 22
    targetSdk = 32
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures {
    viewBinding = true
  }

  buildTypes {
    debug {
      buildConfigField(
        type = "String",
        name = "WEB_APP_URL",
        value = "\"file:///android_asset/index.html\""
      )
    }

    release {
      buildConfigField(
        type = "String",
        name = "WEB_APP_URL",
        value = "\"file:///android_asset/index.html\""
      )

      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility(JavaVersion.VERSION_11)
    targetCompatibility(JavaVersion.VERSION_11)
  }

  kotlinOptions {
    jvmTarget = "1.8"
  }
}

dependencies {
  implementation(fileTree("libs").include("*.aar", "*.jar"))
  implementation("androidx.core:core-ktx:1.7.0")
  implementation("androidx.appcompat:appcompat:1.4.1")
  implementation("androidx.preference:preference-ktx:1.2.0")
  implementation("com.google.android.material:material:1.6.0")
  implementation("com.google.code.gson:gson:2.9.0")
  implementation("com.github.protoman92.KotlinRedux:android-all:master-SNAPSHOT")
  implementation("com.github.protoman92.KotlinRedux:common-all:master-SNAPSHOT")
  implementation("io.reactivex.rxjava2:rxjava:2.2.21")
  testImplementation("junit:junit:4.13.2")

  androidTestImplementation("androidx.test.ext:junit:1.1.3")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

  implementation(project(":libs:commonview"))
  implementation(project(":libs:webview"))
  implementation(project(":libs:webview:javascriptinterface:filepicker"))
  implementation(project(":libs:webview:javascriptinterface:genericlifecycle"))
  implementation(project(":libs:webview:javascriptinterface:notification"))
  implementation(project(":libs:webview:javascriptinterface:sharedpreferences"))
}