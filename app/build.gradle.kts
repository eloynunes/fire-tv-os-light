plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.firetvoslight.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.firetvoslight.launcher"
        minSdk = 22
        targetSdk = 34
        versionCode = 4
        versionName = "0.2.0"
    }

    // Optional owner-controlled release signing; credentials never belong in Git.
    val releaseStore = providers.environmentVariable("TVLIGHT_KEYSTORE").orNull
    if (releaseStore != null) {
        signingConfigs.create("ownerRelease") {
            storeFile = file(releaseStore)
            storePassword = providers.environmentVariable("TVLIGHT_STORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("TVLIGHT_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("TVLIGHT_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (releaseStore != null) signingConfig = signingConfigs.getByName("ownerRelease")
            // Without explicit credentials, release stays unsigned.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        xmlReport = true
        htmlReport = false
    }

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
