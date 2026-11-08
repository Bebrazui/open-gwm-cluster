plugins {
    id("com.android.application")
}

android {
    namespace = "org.opengwm.split"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "org.opengwm.split"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-alpha"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
}
