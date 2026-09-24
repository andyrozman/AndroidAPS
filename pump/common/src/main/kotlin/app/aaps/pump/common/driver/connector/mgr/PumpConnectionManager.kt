package app.aaps.pump.common.driver.connector.mgr

import android.content.Context
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.profile.Profile
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.sharedPreferences.SP

import app.aaps.pump.common.data.BasalProfileDto
import app.aaps.pump.common.driver.connector.PumpConnectorInterface

import app.aaps.pump.common.defs.PumpConfigurationTypeInterface
import app.aaps.pump.common.driver.connector.commands.data.AdditionalResponseDataInterface
import app.aaps.pump.common.driver.connector.commands.data.CustomCommandTypeInterface
import app.aaps.pump.common.driver.connector.commands.response.DataCommandResponse
import app.aaps.pump.common.driver.connector.defs.PumpCommandType
import app.aaps.pump.common.data.PumpStatus
import app.aaps.pump.common.data.PumpTimeDifferenceDto
import app.aaps.pump.common.defs.BolusData
import app.aaps.pump.common.defs.TempBasalPair
import app.aaps.pump.common.utils.PumpUtil
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class PumpConnectionManager constructor(
    val pumpStatus: PumpStatus,
    val pumpUtil: PumpUtil,
    val sp: SP,
    val aapsLogger: AAPSLogger,
    val rxBus: RxBus,
    val context: Context
) {

    var inConnectMode = false
    var inDisconnectMode = false
    
    val TAG = LTag.PUMPCOMM

    abstract fun connectToPump(): Boolean

    abstract fun disconnectFromPump(): Boolean

    abstract fun setCurrentPumpCommandType(commandType: PumpCommandType, customCommandType: CustomCommandTypeInterface?)

    abstract fun resetDriverStatus()

    abstract fun getConnector(commandType: PumpCommandType?): PumpConnectorInterface

    abstract fun postProcessConfiguration(valueMap: MutableMap<PumpConfigurationTypeInterface, Any>?)

    open fun resetFirmwareVersion() {
    }

    open fun determineFirmwareVersion() {
    }


    fun disconnectFromPumpX(): Boolean {
        return getConnector(null).disconnectFromPump()
    }



    fun deliverBolus(detailedBolusInfo: DetailedBolusInfo?): DataCommandResponse<BolusData?> {

        val responseData: DataCommandResponse<BolusData?> = getConnectorData(PumpCommandType.SetBolus)
        {
            getConnector(PumpCommandType.SetBolus).sendBolus(detailedBolusInfo!!)
        }

        checkAdditionalResponseData(PumpCommandType.SetBolus, responseData as DataCommandResponse<AdditionalResponseDataInterface?>)

        return responseData
    }


    fun getBolus(): DataCommandResponse<BolusData?> {

        val responseData: DataCommandResponse<BolusData?> = getConnectorData(PumpCommandType.GetBolus)
        {
            getConnector(PumpCommandType.GetBolus).getBolus()
        }

        checkAdditionalResponseData(PumpCommandType.GetBolus, responseData as DataCommandResponse<AdditionalResponseDataInterface?>)

        return responseData
    }

    fun cancelBolus(bolusData: BolusData?): DataCommandResponse<AdditionalResponseDataInterface?> {

        val responseData: DataCommandResponse<AdditionalResponseDataInterface?> = getConnectorData(PumpCommandType.CancelBolus)
        {
            getConnector(PumpCommandType.CancelBolus).cancelBolus(bolusData)
        }

        checkAdditionalResponseData(PumpCommandType.CancelBolus, responseData)

        return responseData
    }


    fun getTemporaryBasal(): DataCommandResponse<TempBasalPair?> {

        val responseData: DataCommandResponse<TempBasalPair?> = getConnectorData(PumpCommandType.GetTemporaryBasal)
        {
            getConnector(PumpCommandType.GetTemporaryBasal).retrieveTemporaryBasal()
        }

        checkAdditionalResponseData(PumpCommandType.GetTemporaryBasal, responseData as DataCommandResponse<AdditionalResponseDataInterface?>)

        return responseData
    }


    fun setTemporaryBasal(value: Int, duration: Int): DataCommandResponse<TempBasalPair?> {

        val responseData: DataCommandResponse<TempBasalPair?> = getConnectorData(PumpCommandType.SetTemporaryBasal)
        {
            getConnector(PumpCommandType.SetTemporaryBasal).sendTemporaryBasal(value, duration)
        }

        checkAdditionalResponseData(PumpCommandType.SetTemporaryBasal, responseData as DataCommandResponse<AdditionalResponseDataInterface?>)

        return responseData
    }


    fun getPumpStatus(): DataCommandResponse<AdditionalResponseDataInterface?> {

        val responseData: DataCommandResponse<AdditionalResponseDataInterface?> = getConnectorData(PumpCommandType.GetPumpStatus)
        {
            getConnector(PumpCommandType.GetPumpStatus).getPumpStatus()
        }

        checkAdditionalResponseData(PumpCommandType.GetPumpStatus, responseData)

        return responseData
    }



    fun setBasalProfile(profile: Profile?): DataCommandResponse<Boolean?> {

        val responseData: DataCommandResponse<Boolean?> = getConnectorData(PumpCommandType.SetBasalProfile)
        {
            getConnector(PumpCommandType.SetBasalProfile).sendBasalProfile(profile!!)
        }

        //checkAdditionalResponseData(PumpCommandType.SetBasalProfile, responseData)

        if (responseData.isSuccess) {
            
        }


        return responseData
    }


    fun cancelTemporaryBasal(): DataCommandResponse<AdditionalResponseDataInterface?> {
        val responseData: DataCommandResponse<AdditionalResponseDataInterface?> = getConnectorData(PumpCommandType.CancelTemporaryBasal)
        {
            getConnector(PumpCommandType.CancelTemporaryBasal).cancelTemporaryBasal() // TODO refactor this not to use AAPS object
        }

        checkAdditionalResponseData(PumpCommandType.CancelTemporaryBasal, responseData)

        return responseData
    }

    fun getRemainingInsulin(): DataCommandResponse<Double?> {

        val responseData: DataCommandResponse<Double?> = getConnectorData(PumpCommandType.GetRemainingInsulin)
        {
            getConnector(PumpCommandType.GetRemainingInsulin).retrieveRemainingInsulin()
        }

        if (responseData.isSuccess) {
            pumpStatus.reservoirRemainingUnits = responseData.value!!
        }

        return responseData

    }


    fun getConfiguration(): DataCommandResponse<MutableMap<PumpConfigurationTypeInterface, Any>?> {

        val responseData: DataCommandResponse<MutableMap<PumpConfigurationTypeInterface, Any>?> = getConnectorData(PumpCommandType.GetSettings)
        {
            getConnector(PumpCommandType.GetSettings).retrieveConfiguration()
        }

        if (responseData.isSuccess) {
            postProcessConfiguration(responseData.value)
        }

        return responseData

    }


    fun getBasalProfile(): DataCommandResponse<BasalProfileDto?> {

        val responseData: DataCommandResponse<BasalProfileDto?> = getConnectorData(PumpCommandType.GetBasalProfile)
        {
            getConnector(PumpCommandType.GetBasalProfile).retrieveBasalProfile()
        }

        if (responseData.isSuccess) {
            postProcessBasalProfile(responseData.value!!);
        }

        return responseData

    }


    fun getTime(): DataCommandResponse<PumpTimeDifferenceDto?> {

        val responseData: DataCommandResponse<PumpTimeDifferenceDto?> = getConnectorData(PumpCommandType.GetTime)
        {
            getConnector(PumpCommandType.GetTime).getTime()
        }

        if (responseData.isSuccess) {
            postProcessTime(responseData.value)
        }

        return responseData
    }


    fun setTime(): DataCommandResponse<Boolean?> {

        val responseData: DataCommandResponse<Boolean?> = getConnectorData(PumpCommandType.SetTime)
        {
            getConnector(PumpCommandType.SetTime).setTime()
        }

        //checkAdditionalResponseData(PumpCommandType.SetTime, responseData)

        return responseData

    }


    fun getBatteryLevel(): DataCommandResponse<Int?> {

        val responseData: DataCommandResponse<Int?> = getConnectorData(PumpCommandType.GetBatteryStatus)
        {
            getConnector(PumpCommandType.GetBatteryStatus).retrieveBatteryStatus()
        }

        if (responseData.isSuccess) {
            pumpStatus.batteryRemaining = responseData.value!!
        }

        return responseData
    }


    fun executeCustomCommand(command: CustomCommandTypeInterface, data: Any? = null): DataCommandResponse<AdditionalResponseDataInterface?> {

        val responseData: DataCommandResponse<AdditionalResponseDataInterface?> = getConnectorData(PumpCommandType.CustomCommand, command)
        {
            getConnector(PumpCommandType.CustomCommand).executeCustomCommand(command, data)
        }

        if (responseData.isSuccess) {
            postProcessCustomCommand(command, responseData)
        }

        return responseData
    }


    open fun postProcessTime(value: PumpTimeDifferenceDto?) {
        pumpStatus.pumpTime  = value
    }


    open fun postProcessBasalProfile(value: BasalProfileDto) {
        pumpStatus.basalsByHour = value.basalPatterns
        if (value.basalName!=null) {
            pumpStatus.activeProfileName = value.basalName!!
        }
    }


    open fun postProcessCustomCommand(command: CustomCommandTypeInterface, responseData: DataCommandResponse<AdditionalResponseDataInterface?>) {

    }


    private inline fun <reified T>  getConnectorData(commandType: PumpCommandType,
                                                     customCommandType: CustomCommandTypeInterface? = null,
                                                     getData: () -> DataCommandResponse<T>
    ): DataCommandResponse<T> {

        val commandTypeDescription = if (customCommandType==null) {
            commandType.name
        } else {
            commandType.name + " ${customCommandType.getKey()}"
        }

        aapsLogger.info(TAG, "TANDEMDBG: Executing Command: ${commandTypeDescription}  - START ")

        // TODO check if command is available, if not return error

        setCurrentPumpCommandType(commandType, customCommandType)
        val response = getData()
        resetDriverStatus()

        if (response.isSuccess) {
            aapsLogger.info(TAG, "Command was SUCCESSFUL - Data: ${response.value}")
        } else {
            aapsLogger.error(TAG, "Command FAILED with description ${response.errorDescription}")
        }

        aapsLogger.info(TAG, "Executing Command: ${commandTypeDescription}  - END ")

        return response
    }


    private fun checkAdditionalResponseData(commandType: PumpCommandType, responseData: DataCommandResponse<AdditionalResponseDataInterface?>) {
        if (responseData.value!=null) {
            processAdditionalResponseData(commandType, responseData)
        }
    }


    abstract fun processAdditionalResponseData(commandType: PumpCommandType, responseData: DataCommandResponse<AdditionalResponseDataInterface?>)

}