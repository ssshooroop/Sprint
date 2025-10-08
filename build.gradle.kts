// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    extra.apply {
        set("compose_ui_version", "1.5.4")
        set("wear_compose_version", "1.2.1")
        set("kotlin_version", "1.8.10")
        set("hilt_version", "2.48")
        set("room_version", "2.6.0")
    }

    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.10.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${rootProject.extra.get("kotlin_version")}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${rootProject.extra.get("hilt_version")}")
    }
}

plugins {
    id("com.android.application") version "8.10.1" apply false
    id("com.android.library") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}