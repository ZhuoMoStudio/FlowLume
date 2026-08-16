plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.zhuomo.flowlume.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zhuomo.flowlume"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    // 云端签名：GitHub Actions 注入 -PKEYSTORE_FILE 等属性（Secrets 解码）
    signingConfigs {
        create("release") {
            val keyFile = providers.gradleProperty("KEYSTORE_FILE").orNull
            if (keyFile != null) storeFile = file(keyFile)
            storePassword = providers.gradleProperty("KEYSTORE_PASSWORD").getOrElse("")
            keyAlias = providers.gradleProperty("KEY_ALIAS").getOrElse("")
            keyPassword = providers.gradleProperty("KEYSTORE_KEY_PASSWORD").getOrElse("")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // 骨架阶段暂不混淆；后续开启并补充 proguard 规则
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (providers.gradleProperty("KEYSTORE_FILE").isPresent) {
                signingConfigs.getByName("release")
            } else null
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":wallpaper"))
    implementation(project(":core-config"))
    implementation(project(":core-media"))
    implementation(project(":core-audio"))
    implementation(project(":core-timer"))
    implementation(project(":core-render"))
    implementation(project(":core-effects"))
    implementation(project(":core-ui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.gdx.backend.android)
    implementation(libs.kotlinx.coroutines.android)
}
