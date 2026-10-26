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

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Artefato sem assinatura; assinatura explícita após validação local.
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
