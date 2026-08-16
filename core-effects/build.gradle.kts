plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.zhuomo.flowlume.effects"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig { minSdk = 29 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core-config"))
    implementation(project(":core-audio"))
    implementation(project(":core-render"))
    // ParticleBatch 超类型 Disposable 来自 gdx
    implementation(libs.gdx)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
