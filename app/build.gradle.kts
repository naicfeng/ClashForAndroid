import java.util.*

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
}

val gCompileSdkVersion: String by project
val gBuildToolsVersion: String by project

val gMinSdkVersion: String by project
val gTargetSdkVersion: String by project

val gVersionCode: String by project
val gVersionName: String by project

val gKotlinVersion: String by project
val gKotlinCoroutineVersion: String by project
val gAppCenterVersion: String by project
val gAndroidKtxVersion: String by project
val gRecyclerviewVersion: String by project
val gAppCompatVersion: String by project
val gMaterialDesignVersion: String by project
val gShizukuPreferenceVersion: String by project
val gMultiprocessPreferenceVersion: String by project

android {
    compileSdkVersion(gCompileSdkVersion)
    buildToolsVersion(gBuildToolsVersion)

    defaultConfig {
        applicationId = "com.github.kr328.clashr"

        minSdkVersion(gMinSdkVersion)
        targetSdkVersion(gTargetSdkVersion)

        versionCode = gVersionCode.toInt()
        versionName = gVersionName
    }

    buildTypes {
        named("release") {
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
            named("release") {
                storeFile = rootProject.file(Objects.requireNonNull(properties.getProperty("storeFile")))
                storePassword = System.getenv("KEYSTORE_PWD")
                keyAlias = Objects.requireNonNull(properties.getProperty("keyAlias"))
                keyPassword = System.getenv("KEY_PWD")
            }
        }
        buildTypes {
            named("release") {
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