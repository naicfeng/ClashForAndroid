import android.databinding.tool.ext.toCamelCase

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlinx-serialization")
}

apply(from = "clash.gradle.kts")

val gCompileSdkVersion: String by project
val gBuildToolsVersion: String by project

val gMinSdkVersion: String by project
val gTargetSdkVersion: String by project

val gVersionCode: String by project
val gVersionName: String by project

val gKotlinVersion: String by project
val gKotlinCoroutineVersion: String by project
val gKotlinSerializationVersion: String by project
val gAndroidKtxVersion: String by project

val geoipOutput = buildDir.resolve("outputs/geoip")
val golangSource = file("src/main/golang")
val golangOutput = buildDir.resolve("outputs/golang")
val nativeAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

android {
    compileSdkVersion(gCompileSdkVersion)
    buildToolsVersion(gBuildToolsVersion)

    defaultConfig {
        minSdkVersion(gMinSdkVersion)
        targetSdkVersion(gTargetSdkVersion)

        versionCode = gVersionCode.toInt()
        versionName = gVersionName

        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                abiFilters(*nativeAbis.toTypedArray())
                arguments("-DCLASH_OUTPUT=$golangOutput", "-DCLASH_SOURCE=$golangSource")
            }
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        named("main") {
            assets.srcDir(geoipOutput)
            jniLibs.srcDir(golangOutput)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
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
    android.buildTypes.forEach {
        val cName = it.name.toCamelCase()

        tasks["externalNativeBuild${cName}"].dependsOn(tasks["compileClashCore"])
        tasks["package${cName}Assets"].dependsOn(tasks["downloadGeoipDatabase"])
    }
}