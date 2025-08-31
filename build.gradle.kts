plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
    kotlin("plugin.serialization") version "1.9.0"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

val junitVersion = "5.8.2"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("com.example.whisprer")
    mainClass.set("com.example.whisprer.WhisprerKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

javafx {
    version = "17"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))

    // JavaFX
    implementation("org.openjfx:javafx-controls:17.0.7")
    implementation("org.openjfx:javafx-fxml:17.0.7")

    // Global hotkey support
    implementation("com.github.kwhat:jnativehook:2.2.2")

    // Ktor for HTTP requests
    implementation("io.ktor:ktor-client-core:2.3.9")
    implementation("io.ktor:ktor-client-cio:2.3.9")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.9")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.9")

    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Audio processing
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("org.jflac:jflac-codec:1.5.2")

    // For easier audio format conversion
    implementation("org.jcodec:jcodec:0.2.5")

    // Audio recording
    implementation("org.xerial:sqlite-jdbc:3.41.2.2")

    // JSON processing
    implementation("com.google.code.gson:gson:2.10.1")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Environment variables
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.named<JavaExec>("run") {
    jvmArgs = listOf(
        "--add-exports=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED"
    )
}
