import java.util.*
import java.security.*

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

    val signingFile = rootProject.file("keystore.properties")
    if ( signingFile.exists() ) {
        val properties = Properties().apply {
            signingFile.inputStream().use {
                load(it)
            }
        }
        signingConfigs {
            maybeCreate("release").apply {
                storeFile = rootProject.file(Objects.requireNonNull(properties.getProperty("storeFile")))
                storePassword = Objects.requireNonNull(properties.getProperty("storePassword"))
                keyAlias = Objects.requireNonNull(properties.getProperty("keyAlias"))
                keyPassword = Objects.requireNonNull(properties.getProperty("keyPassword"))
            }
        }
        buildTypes {
            maybeCreate("release").apply {
                this.signingConfig = signingConfigs.findByName("release")
            }
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":service"))
    implementation(project(":design"))
    implementation(project(":common"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$gKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$gKotlinCoroutineVersion")
    implementation("androidx.recyclerview:recyclerview:$gRecyclerviewVersion")
    implementation("androidx.core:core-ktx:$gAndroidKtxVersion")
    implementation("androidx.appcompat:appcompat:$gAppCompatVersion")
    implementation("com.google.android.material:material:$gMaterialDesignVersion")
    implementation("moe.shizuku.preference:preference-appcompat:$gShizukuPreferenceVersion")
    implementation("moe.shizuku.preference:preference-simplemenu-appcompat:$gShizukuPreferenceVersion")
    implementation("com.microsoft.appcenter:appcenter-analytics:$gAppCenterVersion")
    implementation("com.microsoft.appcenter:appcenter-crashes:$gAppCenterVersion")
}

task("injectAppCenterKey") {
    doFirst {
        val properties = Properties().apply {
            rootProject.file("local.properties").inputStream().use {
                load(it)
            }
        }

        val key = properties.getProperty("appcenter.key", "")

        android.buildTypes.forEach {
            it.buildConfigField("String", "APP_CENTER_KEY", "\"$key\"")
        }
    }
}

task("injectPackageNameBase64") {
    doFirst {
        val packageName = android.defaultConfig.applicationId ?: return@doFirst

        val base64 = Base64.getEncoder().encodeToString(packageName.toByteArray(Charsets.UTF_8))

        android.buildTypes.forEach {
            it.buildConfigField("String", "PACKAGE_NAME_BASE64", "\"$base64\"")
        }
    }
}

afterEvaluate {
    tasks["preBuild"].dependsOn(tasks["injectAppCenterKey"], tasks["injectPackageNameBase64"])
}