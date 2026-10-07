import java.util.Properties

//region Helpers functional

//region Version block
/**
 * Выполняет `git` с переданными аргументами и возвращает его stdout.
 *
 * Код выхода игнорируется: например, `git describe` без тегов завершается с ошибкой и ничего не печатает.
 *
 * @param args аргументы команды `git`, например `"rev-list", "--count", "HEAD"`.
 * @return обрезанный по краям stdout, либо `null`, если вывод пустой.
 */
fun gitOutput(vararg args: String): String? = try {
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { null }
} catch (e: Exception) {
    throw e
}

val appVersionName: String
    get() {
        return providers.gradleProperty("appVersionName").orNull
            ?: gitOutput("describe", "--tags", "--match", "[0-9]*.[0-9]*.[0-9]*")
            ?: "0.0.0-dev"
    }
val appVersionCode: Int
    get() {
        return providers.gradleProperty("appVersionCode").orNull?.toInt()
            ?: gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull()
            ?: 0
    }
//endregion

//region Signature block
val keystoreProperties = Properties().apply {
    val file = rootProject.file("signature/keystore.properties")
    if (file.exists()) {
        file.inputStream().use {
            load(it)
        }
    }
}

/**
 * Возвращает значение параметра подписи.
 *
 * Источники по приоритету: переменная окружения `RATEX_<name>`, затем ключ `<name>` в `keystore.properties`.
 *
 * @param name имя параметра без префикса `RATEX_`, например `RELEASE_STORE_PASSWORD`.
 * @return значение или `null`, если оно нигде не задано или состоит из пробелов.
 */
fun signingValue(name: String): String? = providers.environmentVariable("RATEX_$name").orNull
    ?.takeIf { it.isNotBlank() }
    ?: keystoreProperties.getProperty(name)
        ?.takeIf { it.isNotBlank() }

/**
 * Возвращает имена параметров подписи, которые не заданы для указанного набора ключей.
 *
 * Полный набор: `<prefix>_STORE_FILE`, `<prefix>_STORE_PASSWORD`, `<prefix>_KEY_ALIAS`, `<prefix>_KEY_PASSWORD`.
 *
 * @param prefix `RELEASE` или `DEBUG`.
 * @return незаданные имена; пустой список означает, что ключ настроен полностью.
 */
fun missingSigningValues(prefix: String): List<String> = listOf("STORE_FILE", "STORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
    .map { "${prefix}_$it" }
    .filter { signingValue(it) == null }
//endregion

//region Frankfurter block
val frankfurterDefaultBaseUrl = "https://api.frankfurter.dev"

/**
 * Возвращает базовый URL Frankfurter API (без версии, путь `/v2/rates` добавляет код).
 *
 * Источники по приоритету: переменная окружения `RATEX_FRANKFURTER_BASE_URL`, затем gradle-свойство `frankfurterBaseUrl`,
 * затем [flavorDefault] (значение окружения `dev`/`prod`).
 *
 * @param flavorDefault значение по умолчанию для текущего flavor.
 * @return URL без хвостового `/`.
 */
fun frankfurterBaseUrl(flavorDefault: String): String = (
    providers.environmentVariable("RATEX_FRANKFURTER_BASE_URL").orNull?.takeIf { it.isNotBlank() }
        ?: providers.gradleProperty("frankfurterBaseUrl").orNull?.takeIf { it.isNotBlank() }
        ?: flavorDefault
    ).trimEnd('/')
//endregion

//endregion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

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
            buildConfigField("String", "FRANKFURTER_BASE_URL", "\"${frankfurterBaseUrl(frankfurterDefaultBaseUrl)}\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "FRANKFURTER_BASE_URL", "\"${frankfurterBaseUrl(frankfurterDefaultBaseUrl)}\"")
        }
    }

    signingConfigs {
        create("release") {
            val hasReleaseConfig = missingSigningValues("RELEASE").isEmpty()
            if (hasReleaseConfig) {
                storeFile = rootProject.file(signingValue("RELEASE_STORE_FILE")!!)
                storePassword = signingValue("RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("RELEASE_KEY_ALIAS")
                keyPassword = signingValue("RELEASE_KEY_PASSWORD")
            }
        }
        getByName("debug") {
            val hasDebugConfig = missingSigningValues("DEBUG").isEmpty()
            if (hasDebugConfig) {
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
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug").takeIf { missingSigningValues("DEBUG").isEmpty() }
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
