import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url ="https://www.jetbrains.com/intellij-repository/releases")
        maven(url = "https://jetbrains.bintray.com/intellij-third-party-dependencies")
    }

    dependencies {
        classpath("com.squareup.sqldelight:gradle-plugin:1.4.4")
    }
}


plugins {
    kotlin("jvm") version "1.8.0"
    id("com.squareup.sqldelight") version "1.5.5"
}

group = "com.blackstone"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

dependencies {
    implementation("it.skrape:skrapeit:1.1.1")
    implementation( "com.squareup.sqldelight:sqlite-driver:1.5.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-RC")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.6")
}

sqldelight {
   database("CityDataDB") {
        packageName="com.citydata.db"
        sourceFolders = listOf("sqldelight")
   }
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}