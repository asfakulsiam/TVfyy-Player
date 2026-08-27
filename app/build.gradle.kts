import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk = 36

  val gitRefName = System.getenv("GITHUB_REF_NAME") ?: ""
  val cleanTag = if (gitRefName.startsWith("v")) gitRefName.removePrefix("v") else gitRefName
  val computedVersionName = if (cleanTag.isNotBlank() && cleanTag.matches(Regex("\\d+\\.\\d+.*"))) cleanTag else "1.1.0"
  val computedVersionCode = try {
    val parts = computedVersionName.split(".").mapNotNull { it.takeWhile { c -> c.isDigit() }.toIntOrNull() }
    val major = parts.getOrElse(0) { 1 }
    val minor = parts.getOrElse(1) { 1 }
    val patch = parts.getOrElse(2) { 0 }
    (major * 10000) + (minor * 100) + patch
  } catch (_: Exception) {
    1
  }

  defaultConfig {
    applicationId = "com.aistudio.tvfyyplayer.app"
    minSdk = 24
    targetSdk = 36
    versionCode = computedVersionCode
    versionName = computedVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val envKeystorePath = System.getenv("KEYSTORE_PATH")
      val envStorePassword = System.getenv("STORE_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD") ?: "android"
      val envKeyAlias = System.getenv("KEY_ALIAS") ?: System.getenv("KEY_ALIAS_NAME") ?: "androiddebugkey"
      val envKeyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEY_ALIAS_PASSWORD") ?: "android"

      val targetStoreFile = when {
        !envKeystorePath.isNullOrBlank() && file(envKeystorePath).exists() -> file(envKeystorePath)
        !envKeystorePath.isNullOrBlank() && rootProject.file(envKeystorePath).exists() -> rootProject.file(envKeystorePath)
        file("tvfyy-player-release.jks").exists() -> file("tvfyy-player-release.jks")
        rootProject.file("tvfyy-player-release.jks").exists() -> rootProject.file("tvfyy-player-release.jks")
        file("release.keystore").exists() -> file("release.keystore")
        file("release.jks").exists() -> file("release.jks")
        rootProject.file("release.keystore").exists() -> rootProject.file("release.keystore")
        rootProject.file("release.jks").exists() -> rootProject.file("release.jks")
        rootProject.file("debug.keystore").exists() -> rootProject.file("debug.keystore")
        file("debug.keystore").exists() -> file("debug.keystore")
        else -> null
      }

      if (targetStoreFile != null) {
        storeFile = targetStoreFile
        storePassword = envStorePassword
        keyAlias = envKeyAlias
        keyPassword = envKeyPassword
      }
    }
    create("debugConfig") {
      val debugStore = if (rootProject.file("debug.keystore").exists()) {
        rootProject.file("debug.keystore")
      } else {
        file("debug.keystore")
      }
      if (debugStore.exists()) {
        storeFile = debugStore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // Media3 dependencies
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.androidx.media3.exoplayer.dash)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.datasource.okhttp)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
