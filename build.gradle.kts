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

// Plugins are resolved from the buildscript classpath above (legacy style),
// so the app module applies them by id without a version. Declaring them again
// here with versions would conflict ("plugin already on the classpath").

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}