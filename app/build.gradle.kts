plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "nl.natje.tv"
    compileSdk = 36
    defaultConfig {
        applicationId = "nl.natje.tv"
        minSdk = 26
        targetSdk = 34
        versionCode = 10005
        versionName = "1.0.5"
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isIncludeAndroidResources = true }
    lint { abortOnError = true; disable += setOf("ObsoleteSdkInt", "SetTextI18n", "HardcodedText") }
}
dependencies {
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
}
tasks.register("writeDependencyAudit") {
    doLast {
        val artifacts=configurations.getByName("releaseRuntimeClasspath").resolvedConfiguration.resolvedArtifacts
        val rows=artifacts.sortedBy { it.moduleVersion.id.toString() }.map {
            mapOf("group" to it.moduleVersion.id.group,"name" to it.name,"version" to it.moduleVersion.id.version,"file" to it.file.absolutePath)
        }
        rootProject.file("docs/DEPENDENCIES.json").writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(rows)))
    }
}
