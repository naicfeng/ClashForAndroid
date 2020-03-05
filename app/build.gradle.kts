import java.util.*

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
}

val rootExtra = rootProject.extra

val gCompileSdkVersion: Int by rootExtra
val gBuildToolsVersion: String by rootExtra

val gMinSdkVersion: Int by rootExtra
val gTargetSdkVersion: Int by rootExtra

val gVersionCode: Int by rootExtra
val gVersionName: String by rootExtra

val gKotlinVersion: String by rootExtra
val gKotlinCoroutineVersion: String by rootExtra
val gAppCenterVersion: String by rootExtra
val gAndroidKtxVersion: String by rootExtra
val gLifecycleVersion: String by rootExtra
val gRecyclerviewVersion: String by rootExtra
val gAppCompatVersion: String by rootExtra
val gMaterialDesignVersion: String by rootExtra
val gShizukuPreferenceVersion: String by rootExtra
val gMultiprocessPreferenceVersion: String by rootExtra

android {
    compileSdkVersion(gCompileSdkVersion)
    buildToolsVersion(gBuildToolsVersion)

    defaultConfig {
        applicationId = "com.github.kr328.clash"

        minSdkVersion(gMinSdkVersion)
        targetSdkVersion(gTargetSdkVersion)

        versionCode = gVersionCode
        versionName = gVersionName
    }

    buildTypes {
        maybeCreate("release").apply {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = true
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":service"))
    implementation(project(":design"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$gKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$gKotlinCoroutineVersion")
    implementation("androidx.lifecycle:lifecycle-extensions:$gLifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$gLifecycleVersion")
    implementation("androidx.recyclerview:recyclerview:$gRecyclerviewVersion")
    implementation("androidx.core:core-ktx:$gAndroidKtxVersion")
    implementation("androidx.appcompat:appcompat:$gAppCompatVersion")
    implementation("com.google.android.material:material:$gMaterialDesignVersion")
    implementation("moe.shizuku.preference:preference-appcompat:$gShizukuPreferenceVersion")
    implementation("moe.shizuku.preference:preference-simplemenu-appcompat:$gShizukuPreferenceVersion")
    implementation("com.microsoft.appcenter:appcenter-analytics:$gAppCenterVersion")
    implementation("com.microsoft.appcenter:appcenter-crashes:$gAppCenterVersion")
}

task("appCenterKey") {
    doFirst {
        val properties = Properties()
        properties.load(rootProject.file("local.properties").inputStream())

        val key = properties.getProperty("appcenter.key", "")

        android.buildTypes.forEach {
            it.buildConfigField("String", "APP_CENTER_KEY", "\"$key\"")
        }
    }
}

afterEvaluate {
    tasks["preBuild"].dependsOn(tasks["appCenterKey"])
}