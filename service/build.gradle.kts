plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("kotlin-kapt")
    id("kotlinx-serialization")
}

val gCompileSdkVersion: String by project
val gBuildToolsVersion: String by project

val gMinSdkVersion: String by project
val gTargetSdkVersion: String by project

val gVersionCode: String by project
val gVersionName: String by project

val gKotlinVersion: String by project
val gKotlinCoroutineVersion: String by project
val gKotlinSerializationVersion: String by project
val gRoomVersion: String by project
val gAndroidKtxVersion: String by project
val gMultiprocessPreferenceVersion: String by project

android {
    compileSdkVersion(gCompileSdkVersion)
    buildToolsVersion(gBuildToolsVersion)

    defaultConfig {
        minSdkVersion(gMinSdkVersion)
        targetSdkVersion(gTargetSdkVersion)

        versionCode = gVersionCode.toInt()
        versionName = gVersionName

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    kapt("androidx.room:room-compiler:$gRoomVersion")

    implementation(project(":core"))
    implementation(project(":common"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$gKotlinSerializationVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$gKotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$gKotlinCoroutineVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$gKotlinCoroutineVersion")
    implementation("androidx.room:room-runtime:$gRoomVersion")
    implementation("androidx.room:room-ktx:$gRoomVersion")
    implementation("androidx.core:core-ktx:$gAndroidKtxVersion")
    implementation("rikka.preference:multiprocesspreference:$gMultiprocessPreferenceVersion")
}