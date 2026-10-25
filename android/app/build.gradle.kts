import java.util.Properties
import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
val mapSecrets = Properties().apply {
    rootProject.file("secrets.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val mapsApiKey = providers.environmentVariable("ANDROID_MAPS_API_KEY")
    .orElse(mapSecrets.getProperty("MAPS_API_KEY", "")).get()
val releaseSigningFile = rootProject.file(".signing/release.properties")
val releaseSigning = Properties().apply { if (releaseSigningFile.isFile) releaseSigningFile.inputStream().use { load(it) } }
val configuredApiOrigin = providers.gradleProperty("travelApiBaseUrl").orNull
val allowLocalBackend = providers.gradleProperty("allowLocalBackend").orElse("false").map(String::toBoolean).get()
// Optional isolated output for verification when Android Studio also builds this checkout.
providers.gradleProperty("buildOutputDir").orElse(providers.gradleProperty("demoBuildDir")).orNull?.let {
    layout.buildDirectory.set(rootProject.layout.projectDirectory.dir(it.ifBlank { ".verification-build" }).dir("app"))
}
android {
    namespace = "vn.hvp.travelvoice"
    compileSdk = 36
    defaultConfig {
        applicationId = "vn.hvp.travelvoice"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        val serverUrl = providers.gradleProperty("travelApiBaseUrl").orElse("http://10.0.2.2:8080/").get()
        require(serverUrl.matches(Regex("https?://[a-zA-Z0-9.:-]+/?"))) { "travelApiBaseUrl must be an HTTP(S) origin" }
        buildConfigField("String", "API_BASE_URL", "\"${serverUrl.trimEnd('/')}/\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }
    buildFeatures { compose = true; buildConfig = true }
    signingConfigs {
        if (releaseSigningFile.isFile) create("release") {
            storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-dev" }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (releaseSigningFile.isFile) signingConfig = signingConfigs.getByName("release")
        }
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
val verifyReleaseConfiguration by tasks.registering {
    doLast {
        require(releaseSigningFile.isFile) { "Configure .signing/release.properties before building a signed release." }
        require(!mapsApiKey.isBlank()) { "Configure the Android Maps key for the release package and signing certificate." }
        val uri = configuredApiOrigin?.let(::URI)
        val local = uri?.host in listOf("localhost", "127.0.0.1", "10.0.2.2")
        require(uri != null && !uri.host.isNullOrBlank() && !uri.host.endsWith(".invalid") &&
            (uri.scheme == "https" || (allowLocalBackend && local && uri.scheme == "http"))) {
            "Specify an HTTPS backend, or explicitly select the supported local emulator backend with -PallowLocalBackend=true."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(verifyReleaseConfiguration) }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
tasks.withType<Test>().configureEach {
    maxHeapSize = "1g"
    maxParallelForks = 1
    systemProperty("demo.captureDir", layout.buildDirectory.dir("outputs/ui-demo").get().asFile.path)
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("com.google.android.gms:play-services-maps:20.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    // Stable InputManager fix for instrumentation on Android 17 / API 37.
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    testImplementation(composeBom)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.3.0")
}
