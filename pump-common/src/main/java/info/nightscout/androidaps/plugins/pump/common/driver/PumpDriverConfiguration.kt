package info.nightscout.androidaps.plugins.pump.common.driver

interface PumpDriverConfiguration {

    fun getPumpBLESelector(): PumpBLESelectorInterface

}