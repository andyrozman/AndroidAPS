plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("android-module-dependencies")
    id("test-module-dependencies")
    id("jacoco-module-dependencies")
}

// buildscript {
//     ext {
//         jwoglom_pumpx2_version = 'v1.2.9'
//     }
// }

android {

    namespace = "app.aaps.pump.tandem"

    defaultConfig {
        kapt {
            arguments {
                arg("room.incremental", "true")
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:interfaces"))
    implementation(project(":core:libraries"))
    implementation(project(":core:objects"))
    implementation(project(":core:utils"))
    implementation(project(":core:ui"))
    implementation(project(":core:validators"))
    implementation(project(":core:keys"))

    //implementation project(':pump:pump-core')
    implementation(project(":pump:pump-common"))

    api(Libs.AndroidX.fragment)
    api(Libs.AndroidX.navigationFragment)

    api(Libs.AndroidX.Room.room)
    api(Libs.AndroidX.Room.runtime)
    api(Libs.AndroidX.Room.rxJava3)
    kapt(Libs.AndroidX.Room.compiler)
    kapt(Libs.Dagger.compiler)
    kapt(Libs.Dagger.androidProcessor)


    implementation("com.github.jwoglom.pumpX2:pumpx2-android:v1.3.0")
    implementation("com.github.jwoglom.pumpX2:pumpx2-messages:v1.3.0")
    implementation("com.github.jwoglom.pumpX2:pumpx2-shared:v1.3.0")
    implementation("com.github.weliem:blessed-android:2.2.0")
    implementation("com.jakewharton.timber:timber:5.0.1")

    // implementation("com.github.jwoglom.pumpX2:pumpx2-android:$jwoglom_pumpx2_version")
    // implementation("com.github.jwoglom.pumpX2:pumpx2-messages:$jwoglom_pumpx2_version")
    // implementation("com.github.jwoglom.pumpX2:pumpx2-shared:$jwoglom_pumpx2_version")


}
