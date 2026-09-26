plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.metro)
    id("android-module-dependencies")
    id("test-module-dependencies")
    id("jacoco-module-dependencies")
}

android {

    namespace = "app.aaps.pump.tandem"

    // defaultConfig {
    //     ksp {
    //         arg("room.incremental", "true")
    //         arg("room.schemaLocation", "$projectDir/schemas")
    //     }
    // }


    // buildFeatures {
    //     compose=true
    // }
    //
    // composeOptions {
    //     kotlinCompilerExtensionVersion="1.5.3"
    // }
}


ksp {
    arg("room.incremental", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}


dependencies {
    // aaps core
    implementation(project(":core:data"))
    implementation(project(":core:interfaces"))
    implementation(project(":core:objects"))
    implementation(project(":core:keys"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(project(":shared:impl"))
    testImplementation(project(":shared:tests"))

    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // pump-common
    implementation(project(":pump:common"))

    // room database
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.rxjava3)
    ksp(libs.androidx.room.compiler)

    // pumpX2 (tandem comm library)
    implementation(libs.com.jakewharton.timber)
    implementation(libs.com.github.weliem.blessed.android)
    implementation(libs.com.github.jwoglom.pumpx2.android)

    // compose project specific
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.io.github.vanpra.compose.dialogs.datetime)

}
