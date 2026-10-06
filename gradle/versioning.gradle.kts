// Версия приложения из git. Подключение: apply(from = rootProject.file("gradle/versioning.gradle.kts")),
// чтение: val appVersionName: String by extra / val appVersionCode: Int by extra.
// Переопределение: -PappVersionName=..., -PappVersionCode=...

fun gitOutput(vararg args: String): String? = try {
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().ifEmpty { null }
} catch (e: Exception) {
    throw e
}

extra["appVersionName"] = providers.gradleProperty("appVersionName").orNull
    ?: gitOutput("describe", "--tags", "--match", "[0-9]*.[0-9]*.[0-9]*")
    ?: "0.0.0-dev"
extra["appVersionCode"] = providers.gradleProperty("appVersionCode").orNull?.toInt()
    ?: gitOutput("rev-list", "--count", "HEAD")?.toIntOrNull()
    ?: 1
