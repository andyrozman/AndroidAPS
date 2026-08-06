package app.aaps.pump.tandem.mobi

import com.jwoglom.pumpx2.BuildConfig

class TandemMobiPluginVersion {

    val devVersion = "4.0.0-dev-b (25.07.2026)"

    val pumpX2Version = BuildConfig.PUMPX2_VERSION
    val tandemModuleVersion = "v0.8.24.6"

    companion object {
        @JvmStatic
        val connectionFixerEnabled = true   // this is in testing for now

        @JvmStatic
        val downloadHistory = true // it seems we have some issues with history download on new versions
    }
}
