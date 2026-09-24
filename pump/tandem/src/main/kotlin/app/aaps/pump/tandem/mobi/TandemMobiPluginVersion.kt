package app.aaps.pump.tandem.mobi

import com.jwoglom.pumpx2.BuildConfig

class TandemMobiPluginVersion {

    val devVersion = "4.0.0-dev-c (24.09.2026)"

    val pumpX2Version = BuildConfig.PUMPX2_VERSION
    val tandemModuleVersion = "v0.9.1.0"

    companion object {
        @JvmStatic
        val connectionFixerEnabled = false   // this is in testing for now

        @JvmStatic
        val downloadHistory = true // it seems we have some issues with history download on new versions
    }
}
