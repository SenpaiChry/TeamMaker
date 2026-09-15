import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")

    id("com.google.gms.google-services")
}

// Firma release: le password vivono in keystore.properties (gitignored).
// Se il file non esiste, teniamo signingConfig nullo: le build debug funzionano lo
// stesso, la build release fallisce con messaggio esplicito.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.teammaker.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.teammaker.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // BuildConfig e' usato da UpdateUtility per confrontare la versione installata
    // con quella su GitHub Releases (BuildConfig.VERSION_CODE / VERSION_NAME).
    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.flexbox)
    implementation(libs.fastexcel)

    // Firebase: il BOM allinea le versioni di tutte le librerie Firebase,
    // quindi le singole vanno importate SENZA versione esplicita.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    implementation(libs.firebase.auth)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
