package app.aaps.pump.tandem.common.events

import app.aaps.core.interfaces.rx.events.Event
import app.aaps.pump.tandem.common.data.defs.TandemPumpApiVersion

/**
 * Event for tracking Tandem pump pairing status through the wizard flow
 */
sealed class EventTandemPairingStatus : Event() {
    object PairingStarted : EventTandemPairingStatus()
    object WaitingForCode : EventTandemPairingStatus()
    data class PairingInProgress(val progressPercent: Int, val progressLabel: String = "") : EventTandemPairingStatus()
    data class PairingSuccess(val pumpSerial: String, val pumpName: String, val pumpApiVersion: TandemPumpApiVersion) : EventTandemPairingStatus()
    data class PairingFailed(val error: PairingError) : EventTandemPairingStatus()
}

/**
 * Types of pairing errors that can occur
 */
sealed class PairingError {
    object IncorrectPIN : PairingError()
    object ConnectionTimeout : PairingError()
    object BluetoothError : PairingError()
    object UnknownError : PairingError()
}
