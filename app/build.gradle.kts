import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.FileInputStream
import java.util.Properties

val versionMajor = 1
val versionMinor = 0
val versionPatch = 3

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.wallwar"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.wallwar.game"
    minSdk = 24
    targetSdk = 36
    versionCode = versionMajor * 100 + versionMinor + versionPatch
    versionName = "$versionMajor.$versionMinor.$versionPatch"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  flavorDimensions += "store"
  productFlavors {
    create("play") {
      dimension = "store"
      buildConfigField("String", "TARGET_STORE", "\"PLAY\"")
      manifestPlaceholders["marketApplicationId"] = "com.android.vending"
      manifestPlaceholders["marketBindAddress"] = "com.android.vending.billing.InAppBillingService.BIND"
      manifestPlaceholders["marketPermission"] = "com.android.vending.BILLING"
    }
    create("bazaar") {
      dimension = "store"
      buildConfigField("String", "TARGET_STORE", "\"BAZAAR\"")
      manifestPlaceholders["marketApplicationId"] = "com.farsitel.bazaar"
      manifestPlaceholders["marketBindAddress"] = "ir.cafebazaar.pardakht.InAppBillingService.BIND"
      manifestPlaceholders["marketPermission"] = "com.farsitel.bazaar.permission.PAY_THROUGH_BAZAAR"
    }
    create("myket") {
      dimension = "store"
      buildConfigField("String", "TARGET_STORE", "\"MYKET\"")
      buildConfigField("String", "MYKET_PUBLIC_KEY", "\"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC68X9fQz5L2CXb8+/wkDqa4qAfcXFA5xwDpOaFZuY2Q4T6N0pzMr3gwsRBWcQdtkPE3TmN0w6mho7cODDxDOPZJSiY+bwRUu92PtVV/gzoy0iIyDskh/utFDesUUYGZEX9ljr26D/h1VsHiYu2p2qayfPlgC/aTtseiL9coKd/3wIDAQAB\"")
      manifestPlaceholders["marketApplicationId"] = "ir.mservices.market"
      manifestPlaceholders["marketBindAddress"] = "ir.mservices.market.InAppBillingService.BIND"
      manifestPlaceholders["marketPermission"] = "ir.mservices.market.BILLING"
    }
  }

  val keystorePropertiesFile = rootProject.file("keystore.properties")
  val keystoreProperties = Properties()
  val hasKeystore = if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    val storeFile = keystoreProperties["KEYSTORE_FILE"]?.toString()?.let { file(it) }
    storeFile != null && storeFile.exists()
  } else {
    false
  }

  signingConfigs {
    if (hasKeystore) {
      create("release") {
        storeFile = file(keystoreProperties["KEYSTORE_FILE"].toString())
        storePassword = keystoreProperties["KEYSTORE_PASSWORD"] as String
        keyAlias = keystoreProperties["KEY_ALIAS"] as String
        keyPassword = keystoreProperties["KEY_PASSWORD"] as String
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      if (hasKeystore) {
        signingConfig = signingConfigs.getByName("release")
      } else {
        signingConfig = signingConfigs.getByName("debug")
      }
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug { }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  packaging {
    resources {
      excludes += "com/google/api/**"
      excludes += "google/protobuf/**"
      excludes += "META-INF/versions/**"
      pickFirsts += "google/type/**"
      pickFirsts += "google/rpc/**"
      pickFirsts += "META-INF/INDEX.LIST"
      pickFirsts += "META-INF/io.netty.versions.properties"
      pickFirsts += "META-INF/DEPENDENCIES"
      pickFirsts += "META-INF/LICENSE*"
      pickFirsts += "META-INF/NOTICE*"
      pickFirsts += "META-INF/*.md"
    }
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.hilt.android)
  implementation(libs.hilt.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.messaging)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Firebase Auth with Google Sign-In requires all of the following to be uncommented together.
  // If you are using Firebase Auth with other providers (e.g. Email/Password), you may only need
  // firebase-auth.
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.guava)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.nakama.java)
  implementation(libs.adivery)
  implementation(libs.play.services.ads)
  "playImplementation"(libs.play.billing)
  "bazaarImplementation"(libs.poolakey)
  "myketImplementation"(libs.myket.billing)
  implementation("androidx.browser:browser:1.8.0")
  implementation("com.google.android.gms:play-services-ads-identifier:18.1.0")
  implementation("com.google.code.gson:gson:2.11.0")
  implementation("com.google.protobuf:protobuf-java:3.25.1")
  // implementation(libs.play.services.location)
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
  "ksp"(libs.hilt.compiler)
}


configurations.all {
  exclude(group = "com.google.api.grpc", module = "proto-google-common-protos")
  exclude(group = "com.google.firebase", module = "protolite-well-known-types")
}

tasks.configureEach {
  if (name.contains("DuplicateClasses", ignoreCase = true)) {
    enabled = false
  }
}
