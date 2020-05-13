plugins {
    id("com.android.library")
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
val gAndroidKtxVersion: String by rootExtra
val gAppCompatVersion: String by rootExtra
val gMaterialDesignVersion: String by rootExtra

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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$gKotlinVersion")
    implementation("androidx.appcompat:appcompat:$gAppCompatVersion")
    implementation("androidx.core:core-ktx:$gAndroidKtxVersion")
    implementation("com.google.android.material:material:$gMaterialDesignVersion")
}
