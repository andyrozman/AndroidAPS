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
    implementation(project(":core:data"))
    implementation(project(":core:interfaces"))
    //implementation(project(":core:libraries"))
    implementation(project(":core:objects"))
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))
    //implementation(project(":core:validators"))
    implementation(project(":core:keys"))
    implementation(project(":implementation"))

    implementation(project(":pump:common"))
    implementation(project(":shared:tests"))

    testImplementation(project(":shared:tests"))
    testImplementation(project(":shared:impl"))

    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.rxjava3)

    // api(libs.androidx.room)
    // api(libs.androidx.room.runtime)
    // api(libs.androidx.room.rxjava3)

    //ksp(libs.androidx.room.compiler)
    // ksp(libs.com.google.dagger.compiler)
    // ksp(libs.com.google.dagger.hilt.compiler)
    // ksp(libs.com.google.dagger.android.processor)

    // compose dependencies
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))

    //implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.foundation)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.io.github.vanpra.compose.dialogs.datetime)

    // pumpX2
    implementation(libs.com.jakewharton.timber)
    implementation(libs.com.github.weliem.blessed.android)
    implementation(libs.com.github.jwoglom.pumpx2.android)

}
