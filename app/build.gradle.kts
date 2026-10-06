import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

apply(from = rootProject.file("gradle/versioning.gradle.kts"))

val appVersionName: String by extra
val appVersionCode: Int by extra

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(name: String): String? = providers.environmentVariable("RATEX_$name").orNull?.takeIf { it.isNotBlank() }
    ?: keystoreProperties.getProperty(name)?.takeIf { it.isNotBlank() }

fun missingSigningValues(prefix: String): List<String> = listOf("STORE_FILE", "STORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
    .map { "${prefix}_$it" }
    .filter { signingValue(it) == null }

android {
    namespace = "ru.fasdev.ratex"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "ru.fasdev.ratex"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
        }
        create("prod") {
            dimension = "env"
        }
    }

    signingConfigs {
        create("release") {
            if (missingSigningValues("RELEASE").isEmpty()) {
                storeFile = rootProject.file(signingValue("RELEASE_STORE_FILE")!!)
                storePassword = signingValue("RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("RELEASE_KEY_ALIAS")
                keyPassword = signingValue("RELEASE_KEY_PASSWORD")
            }
        }
        getByName("debug") {
            if (missingSigningValues("DEBUG").isEmpty()) {
                storeFile = rootProject.file(signingValue("DEBUG_STORE_FILE")!!)
                storePassword = signingValue("DEBUG_STORE_PASSWORD")
                keyAlias = signingValue("DEBUG_KEY_ALIAS")
                keyPassword = signingValue("DEBUG_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release").takeIf { missingSigningValues("RELEASE").isEmpty() }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

val checkReleaseSigning = tasks.register("checkReleaseSigning") {
    group = "verification"
    description = "Fails if release signing keys are not configured"
    doLast {
        val missing = missingSigningValues("RELEASE")
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Release signing is not configured. Set env RATEX_<NAME> or keystore.properties <NAME> for: " +
                    missing.joinToString()
            )
        }
    }
}

val releaseTaskPattern = Regex("(assemble|package|bundle)(Dev|Prod)?Release")
tasks.configureEach {
    if (name.matches(releaseTaskPattern)) dependsOn(checkReleaseSigning)
    // Без ключей release падает сразу, до компиляции, R8 и lint
    if (name != checkReleaseSigning.name && name.contains("Release")) mustRunAfter(checkReleaseSigning)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Test impl
    androidTestImplementation(libs.androidx.test.espresso.core)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.truth)

    androidTestImplementation(libs.androidx.test.uiautomator)

    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.androidx.test.core)

    implementation(libs.kotlin.stdlib)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Dagger
    api(libs.dagger)
    ksp(libs.dagger.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Ktor Client (движок OkHttp), Kotlin Serialization
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.kotlinx.coroutines.test)
}
