import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing. The keystore and both passwords live outside every repo, in
// ~/.android/n3d-release.properties, so nothing secret is ever committed. The
// lookup has to sit at the top level: inside the android {} block `java` binds
// to the Android DSL's own member and java.util.Properties stops resolving.
// Without that file the release build still assembles — just unsigned.
val releaseKeyProps = Properties()
val releaseKeyPropsFile = File(System.getProperty("user.home"), ".android/n3d-release.properties")
if (releaseKeyPropsFile.exists()) releaseKeyPropsFile.inputStream().use { releaseKeyProps.load(it) }
val hasReleaseKey = releaseKeyProps.getProperty("storeFile")?.let { path -> File(path).exists() } == true


android {
    namespace = "com.n3d.netlab"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.n3d.netlab"
        // BlurMaskFilter — which is what actually draws the neumorphic shadows —
        // is only honoured by the hardware-accelerated canvas from API 28.
        // Below that every card would render with hard edges.
        minSdk = 29
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        if (hasReleaseKey) create("release") {
            storeFile = File(releaseKeyProps.getProperty("storeFile"))
            storePassword = releaseKeyProps.getProperty("storePassword")
            keyAlias = releaseKeyProps.getProperty("keyAlias")
            keyPassword = releaseKeyProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        // So the About card and the API's User-Agent both read the version
        // from one place instead of drifting apart.
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation("junit:junit:4.13.2")
    // Android stubs org.json out of the unit-test classpath, so the real
    // implementation is added here: the progress blob is the one thing this
    // app and the website have to agree on byte for byte, and a codec that
    // cannot be tested is a codec nobody checks.
    testImplementation("org.json:json:20240303")
}
