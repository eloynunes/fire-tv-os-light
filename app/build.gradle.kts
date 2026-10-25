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
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Assinado com a chave debug local: permite instalar/desinstalar via ADB sem keystore extra.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        xmlReport = true
        htmlReport = false
    }

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
