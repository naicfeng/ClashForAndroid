// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    val kotlinVersion = "1.3.61"

    rootProject.extra.apply {
        this["gBuildToolsVersion"] = "29.0.3"

        this["gCompileSdkVersion"] = 29
        this["gMinSdkVersion"] = 24
        this["gTargetSdkVersion"] = 29

        this["gVersionCode"] = 10111
        this["gVersionName"] = "1.1.11"

        this["gKotlinVersion"] = kotlinVersion
        this["gKotlinCoroutineVersion"] = "1.3.3"
        this["gKotlinSerializationVersion"] = "0.14.0"
        this["gRoomVersion"] = "2.2.4"
        this["gAppCenterVersion"] = "2.5.1"
        this["gAndroidKtxVersion"] = "1.2.0"
        this["gLifecycleVersion"] = "2.2.0"
        this["gRecyclerviewVersion"] = "1.1.0"
        this["gAppCompatVersion"] = "1.1.0"
        this["gMaterialDesignVersion"] = "1.2.0-alpha05"
        this["gShizukuPreferenceVersion"] = "4.2.0"
        this["gMultiprocessPreferenceVersion"] = "1.0.0"
    }
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.0-beta01")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion")
    }
}

allprojects {
    repositories {
        google()
        jcenter()

        maven {
            url = java.net.URI("https://dl.bintray.com/rikkaw/Libraries")
        }
    }
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}
