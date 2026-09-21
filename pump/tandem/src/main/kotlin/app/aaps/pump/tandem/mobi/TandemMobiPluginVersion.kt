package app.aaps.pump.tandem.mobi

import com.jwoglom.pumpx2.BuildConfig

class TandemMobiPluginVersion {

    val devVersion = "4.0.0-dev-b (04.09.2026)"

    val pumpX2Version = BuildConfig.PUMPX2_VERSION
    val tandemModuleVersion = "v0.8.32.4"

    companion object {
        @JvmStatic
        val connectionFixerEnabled = false   // this is in testing for now

        @JvmStatic
        val downloadHistory = true // it seems we have some issues with history download on new versions
    }
}
