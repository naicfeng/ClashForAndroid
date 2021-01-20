// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    val gKotlinVersion: String by project

    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$gKotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$gKotlinVersion")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven {
            url = java.net.URI("https://dl.bintray.com/rikkaw/MaterialPreference")
        }
        maven {
            url = java.net.URI("https://dl.bintray.com/rikkaw/Libraries")
        }
    }
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}
