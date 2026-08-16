plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.zhuomo.flowlume.wallpaper"
    compileSdk = 35

    defaultConfig { minSdk = 29 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core-config"))
    implementation(project(":core-media"))
    implementation(project(":core-render"))
    implementation(project(":core-effects"))
    implementation(libs.gdx)
    implementation(libs.gdx.backend.android)
    implementation(libs.kotlinx.coroutines.android)
}
