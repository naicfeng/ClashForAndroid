// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    val kotlinVersion = "1.3.72"

    rootProject.extra.apply {
        this["gBuildToolsVersion"] = "29.0.3"

        this["gCompileSdkVersion"] = 29
        this["gMinSdkVersion"] = 24
        this["gTargetSdkVersion"] = 29

        this["gVersionCode"] = 10214
        this["gVersionName"] = "1.2.14R"

        this["gKotlinVersion"] = kotlinVersion
        this["gKotlinCoroutineVersion"] = "1.3.6"
        this["gKotlinSerializationVersion"] = "0.20.0"
        this["gRoomVersion"] = "2.2.5"
        this["gAppCenterVersion"] = "2.5.1"
        this["gAndroidKtxVersion"] = "1.2.0"
        this["gRecyclerviewVersion"] = "1.1.0"
        this["gAppCompatVersion"] = "1.1.0"
        this["gMaterialDesignVersion"] = "1.1.0"
        this["gShizukuPreferenceVersion"] = "4.2.0"
        this["gMultiprocessPreferenceVersion"] = "1.0.0"
    }
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.0-rc01")
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
