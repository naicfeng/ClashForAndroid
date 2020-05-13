apply(from = "clash.gradle.kts")

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlinx-serialization")
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
val gKotlinSerializationVersion: String by rootExtra
val gAndroidKtxVersion: String by rootExtra

val clashCoreOutput = buildDir.resolve("extraSources")

android {
    compileSdkVersion(gCompileSdkVersion)
    buildToolsVersion(gBuildToolsVersion)

    defaultConfig {
        minSdkVersion(gMinSdkVersion)
        targetSdkVersion(gTargetSdkVersion)

        versionCode = gVersionCode
        versionName = gVersionName

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        maybeCreate("release").apply {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        maybeCreate("main").apply {
            assets.srcDir(clashCoreOutput.resolve("assets"))
            jniLibs.srcDir(clashCoreOutput.resolve("jniLibs"))
            java.srcDir(clashCoreOutput.resolve("classes"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":common"))
    implementation("androidx.core:core-ktx:$gAndroidKtxVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$gKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$gKotlinCoroutineVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$gKotlinSerializationVersion")
}

repositories {
    mavenCentral()
}

afterEvaluate {
    tasks["clean"].dependsOn(tasks["resetGolangPathMode"])
    tasks["preBuild"].dependsOn(tasks["extractSources"], tasks["downloadGeoipDatabase"])
}