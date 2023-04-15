import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  kotlin("jvm") version "1.8.20"
  id("org.sonarqube") version "4.0.0.2929"
  id("org.jlleitschuh.gradle.ktlint") version "11.3.1"
  application
}

group = "io.github.myuwono"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("io.arrow-kt:arrow-core:1.2.0-RC")
  implementation("io.arrow-kt:arrow-fx-coroutines:1.2.0-RC")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

  testImplementation("io.kotest:kotest-runner-junit5:5.5.5")
  testImplementation("io.kotest:kotest-property:5.5.5")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.3.1")
  testImplementation("io.kotest.extensions:kotest-property-arrow:1.3.1")
}

tasks {
  test {
    useJUnitPlatform()
    testLogging {
      setExceptionFormat("full")
      setEvents(listOf("passed", "skipped", "failed", "standardOut", "standardError"))
    }
  }

  compileKotlin {
    compilerOptions {
      freeCompilerArgs.addAll(
        "-progressive",
        "-java-parameters",
        "-Xcontext-receivers",
        "-opt-in=kotlin.time.ExperimentalTime",
        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        "-opt-in=kotlin.RequiresOptIn"
      )

      languageVersion.set(KotlinVersion.KOTLIN_2_0)
      apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
  }
}

kotlin {
  jvmToolchain(17)
}

application {
  mainClass.set("MainKt")
}
