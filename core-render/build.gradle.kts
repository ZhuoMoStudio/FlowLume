plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.zhuomo.flowlume.render"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig { minSdk = 29 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core-config"))
    implementation(project(":core-audio"))
    implementation(libs.gdx)
    implementation(libs.gdx.backend.android)
    implementation(libs.gdx.freetype)
    implementation(libs.kotlinx.coroutines.android)
}
