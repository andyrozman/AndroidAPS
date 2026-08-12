package app.aaps.pump.tandem.mobi

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import app.aaps.core.data.model.BS
import app.aaps.core.data.plugin.PluginType
import app.aaps.core.data.pump.defs.PumpType
import app.aaps.core.data.pump.defs.TimeChangeType
import app.aaps.core.interfaces.constraints.Constraint
import app.aaps.core.interfaces.constraints.PluginConstraints
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.interfaces.plugin.PluginDescription
import app.aaps.core.interfaces.profile.Profile
import app.aaps.core.interfaces.pump.BolusProgressData
import app.aaps.core.interfaces.pump.DetailedBolusInfo
import app.aaps.core.interfaces.pump.Pump
import app.aaps.core.interfaces.pump.PumpEnactResult
import app.aaps.core.interfaces.pump.PumpInsulin
import app.aaps.core.interfaces.pump.PumpProfile
import app.aaps.core.interfaces.pump.PumpRate
import app.aaps.core.interfaces.pump.PumpSync
import app.aaps.core.interfaces.pump.PumpSync.TemporaryBasalType
import app.aaps.core.interfaces.queue.CommandQueue
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventRefreshButtonState
import app.aaps.core.interfaces.rx.events.EventRefreshOverview
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.interfaces.utils.DecimalFormatter
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy
import kotlinx.coroutines.runBlocking
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.keys.interfaces.withActivity
import app.aaps.core.keys.interfaces.withEntriesProvider
import app.aaps.core.ui.compose.icons.IcPluginTMobi
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.implementation.pump.PumpEnactResultObject
import app.aaps.pump.tandem.R
import app.aaps.pump.common.PumpPluginAbstract
import app.aaps.pump.common.data.PumpStatus
import app.aaps.pump.common.events.EventPumpConnectionParametersChanged
import app.aaps.pump.common.sync.PumpSyncStorage
import app.aaps.pump.common.utils.ProfileUtil
import app.aaps.pump.tandem.common.concurrency.PumpAvailabilitySync
import app.aaps.pump.tandem.common.concurrency.PumpDispatcherScope
import app.aaps.pump.tandem.common.concurrency.TandemDispatcher
import app.aaps.pump.tandem.common.concurrency.cancelBolus
import app.aaps.pump.tandem.common.concurrency.cancelTemporaryBasal
import app.aaps.pump.tandem.common.concurrency.deliverBolus
import app.aaps.pump.tandem.common.concurrency.executeCustomCommand
import app.aaps.pump.tandem.common.concurrency.getBasalProfile
import app.aaps.pump.tandem.common.concurrency.getBatteryLevel
import app.aaps.pump.tandem.common.concurrency.getBolus
import app.aaps.pump.tandem.common.concurrency.getConfiguration
import app.aaps.pump.tandem.common.concurrency.getPumpStatus
import app.aaps.pump.tandem.common.concurrency.getRemainingInsulin
import app.aaps.pump.tandem.common.concurrency.getTemporaryBasal
import app.aaps.pump.tandem.common.concurrency.getTime
import app.aaps.pump.tandem.common.concurrency.setBasalProfile
import app.aaps.pump.tandem.common.concurrency.setTemporaryBasal
import app.aaps.pump.tandem.common.concurrency.setTime
import app.aaps.pump.tandem.common.util.TandemPumpUtil
import kotlin.time.Duration.Companion.minutes
import app.aaps.pump.common.R as Rc

import app.aaps.pump.common.defs.*
import app.aaps.pump.common.defs.PumpDriverMode
import app.aaps.pump.common.defs.PumpDriverState
import app.aaps.pump.common.defs.PumpRunningState
import app.aaps.pump.common.defs.PumpUpdateFragmentType
import app.aaps.pump.common.driver.connector.commands.response.DataCommandResponse
import app.aaps.pump.common.driver.history.PumpHistoryPeriod
import app.aaps.pump.common.driver.refresh.PumpDataRefreshAction
import app.aaps.pump.common.driver.refresh.PumpDataRefreshCapable
import app.aaps.pump.common.driver.refresh.PumpDataRefreshType
import app.aaps.pump.common.events.EventPumpDataRefresh
import app.aaps.pump.common.events.EventPumpForceDisconnect
import app.aaps.pump.tandem.common.driver.TandemPumpStatus
import app.aaps.pump.tandem.mobi.driver.TandemMobiPumpDriverConfiguration
import app.aaps.pump.common.events.EventPumpFragmentValuesChanged
import app.aaps.pump.tandem.common.comm.history.HistoryRetriever
import app.aaps.pump.tandem.common.comm.qe.QualifyingEventHandler
import app.aaps.pump.tandem.common.comm.ui.CoreCartridgeActionsModel
import app.aaps.pump.tandem.common.data.defs.QualifyingEventsFilter
import app.aaps.pump.tandem.common.data.defs.QualifyingEventsRange
import app.aaps.pump.tandem.common.data.defs.QuickBolusType
import app.aaps.pump.tandem.common.data.defs.RefreshData
import app.aaps.pump.tandem.common.data.defs.SiteReminderPreset
import app.aaps.pump.tandem.common.data.defs.TandemNotificationType
import app.aaps.pump.tandem.common.data.defs.TandemPumpSettingType
import app.aaps.pump.tandem.common.database.data.DbDataHandler
import app.aaps.pump.tandem.common.driver.connector.def.ControlCommandResponse
import app.aaps.pump.tandem.common.driver.connector.def.TandemCustomCommand
import app.aaps.pump.tandem.common.driver.connector.response.AlarmStatusDto
import app.aaps.pump.tandem.common.driver.connector.response.AlertStatusDto
import app.aaps.pump.tandem.common.driver.connector.response.MalfunctionStatusDto
import app.aaps.pump.tandem.common.events.EventDatabaseAddQEData
import app.aaps.pump.tandem.common.events.EventHandleQualifyingEvent
import app.aaps.pump.tandem.common.events.EventRefreshPumpData
import app.aaps.pump.tandem.common.keys.TandemBooleanPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemIntPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemIntentPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemLongNonPreferenceKey
import app.aaps.pump.tandem.common.keys.TandemStringPreferenceKey
import app.aaps.pump.tandem.common.service.TandemService
import app.aaps.pump.tandem.mobi.ui.TandemUiController
import app.aaps.pump.tandem.mobi.ui.overview.MobiComposeContent
import app.aaps.pump.tandem.mobi.ui.wizard.TandemMobiConnectionWizardActivity
import com.jwoglom.pumpx2.pump.messages.models.InsulinUnit
import com.jwoglom.pumpx2.pump.messages.request.control.SetTempRateRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.PumpGlobalsRequest
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.PumpGlobalsResponse
import io.reactivex.rxjava3.kotlin.plusAssign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Created by andy on 04.01.2025.
 *
 * @author Andy Rozman (andy.rozman@gmail.com)
 */
@Singleton
class TandemMobiPumpPlugin @Inject constructor(
    aapsLogger: AAPSLogger,
    rxBus: RxBus,
    context: Context,
    preferences: Preferences,
    rh: ResourceHelper,
    val activePlugin: ActivePlugin,
    commandQueue: CommandQueue,
    fabricPrivacy: FabricPrivacy,
    val tandemPumpUtil: TandemPumpUtil,
    val pumpStatus: TandemPumpStatus,
    val qualifyingEventHandler: QualifyingEventHandler,
    dateUtil: DateUtil,
    val tandemDispatcher: TandemDispatcher,
    @Suppress("unused") val pumpAvailabilitySync: PumpAvailabilitySync, // eager-construct so observers attach
    aapsSchedulers: AapsSchedulers,
    pumpSync: PumpSync,
    pumpSyncStorage: PumpSyncStorage,
    tandemPumpDriverConfiguration: TandemMobiPumpDriverConfiguration,
    decimalFormatter: DecimalFormatter,
    val dbDataHandler: DbDataHandler,
    val historyRetriever: HistoryRetriever,
    val tandemUiController: TandemUiController,
    val resourceHelper: ResourceHelper,
    pumpEnactResultProvider: Provider<PumpEnactResult>,
    bolusProgressData: BolusProgressData
) : PumpPluginAbstract(
    pluginDescription = PluginDescription() //
        .mainType(PluginType.PUMP) //
        .composeContent { _ ->
            MobiComposeContent(
                pluginName = (activePlugin.activePumpInternal as? PluginBase)?.name ?: "",
                tandemPumpStatus = pumpStatus,
                aapsLogger = aapsLogger,
                resourceHelper = resourceHelper,
                tandemUiController = tandemUiController
            )
        }
        .icon(IcPluginTMobi)
        .pluginName(R.string.tandem_name_mobi)
        .shortName(R.string.tandem_name_mobi_short)
        .description(R.string.description_pump_tandem_mobi),
    pumpType = PumpType.TANDEM_MOBI_BT,
    rh = rh,
    aapsLogger = aapsLogger,
    commandQueue = commandQueue,
    rxBus = rxBus,
    //activePlugin = activePlugin,
    preferences = preferences,
    context = context,
    fabricPrivacy = fabricPrivacy,
    dateUtil = dateUtil,
    aapsSchedulers = aapsSchedulers,
    pumpSync = pumpSync,
    pumpSyncStorage = pumpSyncStorage,
    pumpDriverConfigurationInternal = tandemPumpDriverConfiguration,
    decimalFormatter = decimalFormatter,
    //instantiator = instantiator,
    pumpEnactResultProvider = pumpEnactResultProvider,
    bolusProgressData = bolusProgressData,  // TODO dev4 no idea what this is used for or how to integrate
    ownPreferences = listOf(
        TandemLongNonPreferenceKey::class.java,
        TandemStringPreferenceKey::class.java,
        TandemIntPreferenceKey::class.java,
        TandemBooleanPreferenceKey::class.java,
        TandemIntentPreferenceKey::class.java
    )
), Pump, PluginConstraints, PumpDataRefreshCapable /*, PumpConstraints */ {

    // variables for handling statuses and history

    private var tandemService: TandemService? = null
    private var driverMode = PumpDriverMode.Automatic

    private val versionInternal = TandemMobiPluginVersion()

    private var wantedDriverMode = PumpDriverMode.Automatic

    @Suppress("PropertyName")
    private val TAG = LTag.PUMP

    private var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun onStart() {
        aapsLogger.debug(LTag.PUMP, model().model + " started (Tandem Mobi) - $version - dev version: ${versionInternal.devVersion}")
        // dbDataHandler.databaseStatistics() // this is just in case we want to debug database

        PumpHistoryEntryGroup.doTranslation(rh)
        PumpHistoryEntryGroup.filterByGroupConfig(PumpTypeGroupConfig.tMobi)
        PumpHistoryPeriod.doTranslation(rh)
        SiteReminderPreset.doTranslation(rh)
        TandemCustomCommand.doTranslation(rh)

        super.onStart()
    }

    override suspend fun onStop() {
        coroutineScope.cancel()
        super.onStop()
    }


    // TODO dev4    see MedtronicPump:224
    // override fun updatePreferenceSummary(pref: Preference) {
    //     super.updatePreferenceSummary(pref)
    //     if (pref.key == TandemStringPreferenceKey.PumpAddress.key) {
    //         val value: String? = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.PumpAddress, null)
    //             //sp.getStringOrNull(R.string.key_tandem_address, null)
    //         pref.summary = value ?: rh.gs(app.aaps.core.ui.R.string.not_set_short)
    //         aapsLogger.info(LTag.PUMP, "TANDEMDBG: Received event that pump address changed (this means bonding/unbonding is happening)")
    //     } else if (pref.key == TandemStringPreferenceKey.PumpSerial.key) {
    //         val value: String? = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.PumpSerial, null)
    //             //sp.getStringOrNull(R.string.key_tandem_serial, null)
    //         pref.summary = value ?: rh.gs(app.aaps.core.ui.R.string.not_set_short)
    //         aapsLogger.info(LTag.PUMP, "TANDEMDBG: Received event that pump serial changed (this means bonding/unbonding is happening)")
    //     } else if (pref.key == TandemStringPreferenceKey.SharedConnectionData.key) {
    //         aapsLogger.error(TAG, "TANDEMDBG: updatePreferenceSummary for SharedConnectionData returned. ")
    //         if (tandemService!!.hasConfigurationChanged()) {
    //             tandemService!!.reconnectWithDifferentConnectionData()
    //         }
    //     } else if (pref.key == TandemStringPreferenceKey.QuickBolusTypePref.key) {
    //         val value: String? = tandemPumpUtil.getStringPreferenceOrDefaultOrNull(TandemStringPreferenceKey.QuickBolusTypePref, null)
    //         aapsLogger.error(LTag.PUMP, "Quick Bolus Setting changed: $value")
    //         if (value!=null && value.isNotEmpty()) {
    //             newQuickBolusType = QuickBolusType.valueOf(value)
    //             scheduleNextRefreshAtSameTimeAsOtherType(refreshType = PumpDataRefreshType.Custom_2,
    //                                                      refreshTypeToCopy = PumpDataRefreshType.PumpStatus)
    //         }
    //     }
    // }

    var newQuickBolusType : QuickBolusType? = null

    // PumpAbstract implementations
    override fun initPumpStatusData() {
        //pumpStatus.lastConnection = sp.getLong(TandemPumpConst.Statistics.LastGoodPumpCommunicationTime, 0L)
        if (preferences.getIfExists(TandemLongNonPreferenceKey.LastGoodPumpCommunicationTime) != null) {
            pumpStatus.lastConnection = preferences.get(TandemLongNonPreferenceKey.LastGoodPumpCommunicationTime)
            //pumpStatus.lastDataTime = pumpStatus.lastConnection
            pumpStatus.previousConnection = pumpStatus.lastConnection
        }
        aapsLogger.debug(TAG, "initPumpStatusData: " + pumpStatus)

        pumpType = PumpType.TANDEM_MOBI_BT

        // this is only thing that can change, by being configured
        // TODO pumpDescription.maxTempAbsolute = (pumpStatus.maxBasal != null) ? pumpStatus.maxBasal : 35.0d;
        aapsLogger.debug(LTag.PUMP, "pumpDescription: " + this.pumpDescription)

        pumpStatus.pumpDescription = this.pumpDescription

        // set first Tandem Pump Start
        if (preferences.getIfExists(TandemLongNonPreferenceKey.FirstPumpUse) == null)
            preferences.put(TandemLongNonPreferenceKey.FirstPumpUse, System.currentTimeMillis())

        if (preferences.getIfExists(TandemStringPreferenceKey.PumpSerial) != null) {
            val preference = preferences.get(TandemStringPreferenceKey.PumpSerial)
            if (preference.isNotEmpty()) {
                pumpStatus.serialNumber = preferences.get(TandemStringPreferenceKey.PumpSerial).toLong()
            }
        }

        if (preferences.getIfExists(TandemLongNonPreferenceKey.SiteReminderDateTime) != null) {
            val preference = preferences.get(TandemLongNonPreferenceKey.SiteReminderDateTime)
            pumpStatus.tandemSiteReminder = preference
            aapsLogger.debug(TAG, "initPumpStatusData: tandemSiteReminder pumpStatus.tandemSiteReminder: ${pumpStatus.tandemSiteReminder}")
        }

        pumpStatus.pumpType = PumpType.TANDEM_MOBI_BT
        pumpStatus.pumpDriverMode = this.wantedDriverMode
    }

    override fun onStartScheduledPumpActions() {

        disposable += rxBus
           .toObservable(EventPumpConnectionParametersChanged::class.java)
           .observeOn(aapsSchedulers.io)
           .subscribe({ reconnectAfterDataChange() },
                      { fabricPrivacy.logException(it) })
        disposable += rxBus
           .toObservable(EventPumpForceDisconnect::class.java)
           .observeOn(aapsSchedulers.io)
           .subscribe({ tandemService!!.disconnectFromPump()},
                      { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventHandleQualifyingEvent::class.java)
            .observeOn(aapsSchedulers.io)
            .subscribe({ qualifyingEventHandler.handleEventReceivedFromPump(it) },
                       { fabricPrivacy.logException(it) })
        disposable += rxBus
           .toObservable(EventPumpDataRefresh::class.java)
           .observeOn(aapsSchedulers.io)
           .subscribe({ refreshDataFull() },
                      { fabricPrivacy.logException(it) })

        disposable += rxBus
            .toObservable(EventRefreshPumpData::class.java)
            .observeOn(aapsSchedulers.io)
            .subscribe({ event ->
                            coroutineScope.launch {
                                refreshAfterUI(event)
                            }
                        },
                       { fabricPrivacy.logException(it) })
        disposable += rxBus
            .toObservable(EventDatabaseAddQEData::class.java)
            .observeOn(aapsSchedulers.io)
            .subscribe({ dbDataHandler.addQualifyingEventRecords(it.eventEntities) } ,
                       { fabricPrivacy.logException(it) })

        // check status every minute (if any status needs refresh we send readStatus command)
        startRefreshOfPumpCommands()

    }


    override fun doCustomScheduledActions() {
        if (pumpStatus.tandemSiteReminder!=null && pumpStatus.tandemSiteReminder!!>0) {
            if (System.currentTimeMillis() > pumpStatus.tandemSiteReminder!!) {
                aapsLogger.error(TAG, "Show notification and delete")
                tandemPumpUtil.sendNotification(TandemNotificationType.SiteReminder)
                pumpStatus.tandemSiteReminder = null
                preferences.remove(TandemLongNonPreferenceKey.SiteReminderDateTime)
            }
        }
    }


    private suspend fun refreshAfterUI(refreshEvent: EventRefreshPumpData) {
        if (refreshEvent.refreshEvents.contains(RefreshData.SEMAPHORE_EVENTS)) {
            if (pumpStatus.semaphoreNeedsRefresh) {
                rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Custom_2))
            }
        }

        //PUMP_CANNULA_CHANGED,
        //PUMP_SITE_CHANGED,
        //PUMP_STATE_CHANGED

        var statusChanged = false
        val currentTime = System.currentTimeMillis() - 5000

        if (refreshEvent.refreshEvents.contains(RefreshData.PUMP_STATE_CHANGED)) {
            scheduleNextRefreshWithSpecifiedTime(PumpDataRefreshType.Custom_1, currentTime)
            statusChanged = true
        }

        if (refreshEvent.refreshEvents.contains(RefreshData.PUMP_CANNULA_CHANGED) ||
            refreshEvent.refreshEvents.contains(RefreshData.PUMP_SITE_CHANGED)) {
            scheduleNextRefreshWithSpecifiedTime(PumpDataRefreshType.RemainingInsulin, currentTime)
            scheduleNextRefreshWithSpecifiedTime(PumpDataRefreshType.Custom_1, currentTime)
            scheduleNextRefreshWithSpecifiedTime(PumpDataRefreshType.GetTemporaryBasal, currentTime)
            statusChanged = true
        }

        if (statusChanged) {
            if (!commandQueue.statusInQueue()) {
                commandQueue.readStatus("Status Refresh (UI)")
            }
        }
    }


    //val preventQueueExecution: Boolean = true

    private fun reconnectAfterDataChange() {
        aapsLogger.info(LTag.PUMP, "DUB Connection data changed... validating parameters and reconnecting if possible.")
        // new pump connected, we need to reset driver
        this.isDriverInitialized = false

        if (tandemService!!.validateParameters()) {
            if (tandemService!!.connectToPump()) {
                this.firstRun = true
            }
        }
    }


    private fun refreshDataFull() {
        aapsLogger.info(LTag.PUMP, logPrefix + "refreshDataFull")
        initializePump(false)
    }

    // private fun checkInitializationState() {
    //     if (driverMode== PumpDriverMode.Demo) {
    //         this.driverInitialized = true
    //         return
    //     }
    //
    //     val pumpAddress = sp.getString(TandemPumpConst.Prefs.PumpAddress, "")
    //
    //     val pumpBondStatus = sp.getInt(TandemPumpConst.Prefs.PumpPairStatus, -1)
    //
    //     aapsLogger.debug(LTag.PUMP, "TANDEMDBG: Mobi [address=$pumpAddress,bondStatus=$pumpBondStatus]")
    //
    //     driverInitialized = (!pumpAddress.isEmpty() &&
    //         pumpBondStatus == 100 &&
    //         !tandemUtil.preventConnect)
    //
    //     aapsLogger.debug(LTag.PUMP, "TANDEMDBG: initialization status: $driverInitialized")
    //
    //     if (!driverInitialized) {
    //         pumpStatus.errorDescription = rh.gs(R.string.tandem_error_not_bonded)
    //         rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Full))
    //     } else {
    //         if (!pumpStatus.errorDescription.isNullOrEmpty()) {
    //             pumpStatus.errorDescription = null
    //             rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Configuration))
    //         }
    //     }
    //
    // }

    override val serviceClass: Class<*>?
        get() = TandemService::class.java

    val version: String = "${versionInternal.tandemModuleVersion} (${versionInternal.pumpX2Version})"

    override val pumpStatusData: PumpStatus
        get() = pumpStatus



    override fun applyBasalConstraints(absoluteRate: Constraint<Double>, profile: Profile): Constraint<Double> {
        // 1. Check if above 250% of profile rate
        val maxBasalBySettings = tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.MaxBasal)

        val allowedAmount = baseBasalRate.cU * 2.5 // 250% is max allowed

        val maxBasalRate = Math.min(allowedAmount, maxBasalBySettings.toDouble())

        if (absoluteRate.value() > maxBasalRate) {
            if (maxBasalRate == allowedAmount) {
                absoluteRate.set(maxBasalRate,
                                 rh.gs(R.string.tandem_constraint_basal_rate_max_250, maxBasalRate, baseBasalRate.cU),
                                 this)
            } else {
                absoluteRate.set(maxBasalRate,
                                 rh.gs(R.string.tandem_constraint_basal_rate_on_pump, maxBasalBySettings),
                                 this)
            }
        }

        // 2. Check if below minimum allowed basal rate
        // Effective basal rate can be 0.0u/hr, 0.1u/hr, or greater, but not between 0.0-0.1u
        // (e.g., 0.05u/hr is not allowed as an effective basal rate)
        if (absoluteRate.value() > 0 && absoluteRate.value() < 0.1) {
            absoluteRate.set(0.0,
                             rh.gs(R.string.tandem_constraint_basal_rate_min_0_1u, absoluteRate.value()),
                             this)

        }

        return absoluteRate
    }


    override fun applyBasalPercentConstraints(percentRate: Constraint<Int>, profile: Profile): Constraint<Int> {

        val maxBasalBySettings = tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.MaxBasal)

        val requestedAmount = baseBasalRate.cU * (percentRate.value() / 100.0) // 250% is max allowed

        if (requestedAmount>maxBasalBySettings) {

            val percent = (maxBasalBySettings / baseBasalRate.cU)

            if (percent > SetTempRateRequest.MAX_PERCENT) {
                percentRate.set(
                    SetTempRateRequest.MAX_PERCENT,
                    rh.gs(R.string.tandem_constraint_basal_rate_percent, percentRate.value()),
                    this
                )
            } else {
                percentRate.set(
                    percent.toInt(),
                    rh.gs(R.string.tandem_constraint_basal_rate_percent_max_pump, maxBasalBySettings),
                    this
                )
            }
        } else {
            if (percentRate.value() > SetTempRateRequest.MAX_PERCENT) {
                percentRate.set(
                    SetTempRateRequest.MAX_PERCENT,
                    rh.gs(R.string.tandem_constraint_basal_rate_percent, percentRate.value()),
                    this
                )
            }
        }

        return percentRate
    }

    override fun applyBolusConstraints(insulin: Constraint<Double>): Constraint<Double> {
        // pump-enforced minimum bolus
        val MIN_BOLUS = 0.05

        // Check minimum bolus constraint
        if (insulin.value() > 0.0 && insulin.value() < MIN_BOLUS) {
            insulin.set(
                0.0,
                rh.gs(R.string.tandem_constraint_bolus_minimum, MIN_BOLUS),
                this
            )
        }

        // maximum value of max bolus limit
        val MAX_BOLUS = 25.0
        val maxBolusBySettings = tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.MaxBolus)


        // Check maximum bolus setting
        insulin.setIfSmaller(
            maxBolusBySettings.toDouble(),
            rh.gs(R.string.tandem_constraint_bolus_maximum, maxBolusBySettings),
            this
        )

        insulin.setIfSmaller(
            MAX_BOLUS,
            rh.gs(R.string.tandem_constraint_bolus_maximum, maxBolusBySettings),
            this
        )

        return insulin

    }

    override var serviceConnection: ServiceConnection? = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            aapsLogger.info(LTag.PUMP, "Tandem Service is disconnected")
            tandemService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            aapsLogger.info(LTag.PUMP, "Tandem Service is connected")
            val mLocalBinder = service as TandemService.LocalBinder
            tandemService = mLocalBinder.serviceInstance
            //isServiceSet = true
            //tandemPump

            //tandemUtil.driverStatus = PumpDriverState.Connecting

            // var startup = Completable
            //     .timer(4, TimeUnit.SECONDS)
            //     .subscribeOn(aapsSchedulers.main) // where the work should be done
            //     .observeOn(aapsSchedulers.main) // where the data stream should be delivered
            //     .subscribe({
            //                    isDriverInitialized = tandemService!!.validateParameters()
            //                    aapsLogger.info(LTag.PUMP, "Connection parameters valid: ${isDriverInitialized}")
            //                    if (isDriverInitialized) {
            //                        aapsLogger.info(LTag.PUMP, "Trying to connect to: ${tandemService!!.pumpAddress}")
            //                        tandemService!!.connectToPump()
            //                    }
            //                }, { fabricPrivacy.logException(it)  })

            Thread {
                var driverInitialized = tandemService!!.validateParameters()
                aapsLogger.info(LTag.PUMP, "Connection parameters valid: $driverInitialized")
                if (driverInitialized) {
                    aapsLogger.info(LTag.PUMP, "Trying to connect to: ${tandemService!!.pumpAddress}")
                    tandemService!!.connectToPump()
                }
            }.start()

        }
    }

//     override fun onStart() {
//         //super.onStart()
//         initPumpStatusData()
//
// //            serviceConnection?.let { serviceConnection ->
//             val intent = Intent(context, TandemService::class.java)
//             context.bindService(intent, serviceConnectionTandem, Context.BIND_AUTO_CREATE)
//             disposable.add(
//                 rxBus
//                     .toObservable(EventAppExit::class.java)
//                     .observeOn(aapsSchedulers.io)
//                     .subscribe({ context.unbindService(serviceConnectionTandem) }, fabricPrivacy::logException)
//             )
//
//             //          }
//
//         this.serviceRunning = true
//         onStartScheduledPumpActions()
//     }


    // private val dddserviceConnection: ServiceConnection = object : ServiceConnection {
    //     override fun onServiceDisconnected(name: ComponentName) {
    //         aapsLogger.debug(LTag.PUMP, "Tandem Service is disconnected")
    //         tandemService = null
    //     }
    //
    //     override fun onServiceConnected(name: ComponentName, service: IBinder) {
    //         aapsLogger.debug(LTag.PUMP, "Tandem Service is connected")
    //         val mLocalBinder = service as TandemService.LocalBinder
    //         tandemService = mLocalBinder.serviceInstance
    //     }
    // }


    // Pump Interface
    override fun isInitialized(): Boolean {
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "isInitialized $isDriverInitialized")
        return isDriverInitialized
    }

    override fun isSuspended(): Boolean {
        val suspended = (pumpStatus.pumpRunningState == PumpRunningState.Suspended)
        //aapsLogger.debug(LTag.PUMP, "DUB isSuspended - $suspended")
        return suspended
    }

    override fun isConnected(): Boolean {
        val status = tandemPumpUtil.driverStatus
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, logPrefix +"DUB isConnected [status=$status]")
        return status == PumpDriverState.Connected || status == PumpDriverState.Handshaking || status == PumpDriverState.ExecutingCommand
        // return isServiceSet && tandemService?.isInitialized == true


        // if (!driverInitialized)
        //     return false
        //
        // // if (driverMode== PumpDriverMode.Demo) {
        // //     return true
        // // }
        //
        // val driverStatus = tandemUtil.driverStatus
        // if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "isConnected - " + driverStatus.name)
        // return driverStatus == PumpDriverState.Ready || driverStatus == PumpDriverState.ExecutingCommand
    }

    override fun isConnecting(): Boolean {
        val status = tandemPumpUtil.driverStatus
        val error = tandemPumpUtil.errorType

        //aapsLogger.debug(LTag.PUMP, logPrefix + "DUB isConnecting [status=$status]")

        val unreachable = (error!=null && error==PumpErrorType.PumpUnreachable)

        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, logPrefix + "DUB isConnecting [status=$status]")
        return status == PumpDriverState.Connecting || unreachable

        // return !isServiceSet || tandemService?.isInitialized != true

        // if (!driverInitialized)
        //     return false
        //
        // val driverStatus = tandemUtil.driverStatus
        // if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "isConnecting - " + driverStatus.name)
        //
        // return driverStatus == PumpDriverState.Connecting
    }

    // override fun connect(reason: String) {
    //     if (!driverInitialized) {
    //         return
    //     }
    //     if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "connect (reason=$reason).")
    //
    //     if (tandemUtil.preventConnect) {
    //         aapsLogger.info(LTag.PUMP, "in prevent connect mode (probably pairing in process)")
    //     } else {
    //         pumpConnectionManager.connectToPump() //deviceMac = pumpAddress, deviceBonded = pumpBonded)
    //     }
    // }

    // override fun stopConnecting() {
    //     if (!driverInitialized)
    //         return
    //     if (displayConnectionMessages)
    //         aapsLogger.debug(LTag.PUMP, "stopConnecting [PumpPluginAbstract] - default (empty) implementation.")
    // }
    //

    override fun isHandshakeInProgress(): Boolean {
        val status = tandemPumpUtil.driverStatus
        if (displayConnectionMessages)
            aapsLogger.debug(LTag.PUMP, "DUB isHandshakeInProgress - ${status==PumpDriverState.Handshaking}")

        return status == PumpDriverState.Handshaking


        // if (!driverInitialized)
        //     return false
        //
        // if (displayConnectionMessages)
        //     aapsLogger.debug(LTag.PUMP, "isHandshakeInProgress - " + tandemUtil.driverStatus.name)
        // return tandemUtil.driverStatus === PumpDriverState.Connecting
    }
    //
    // override fun finishHandshaking() {
    //     if (displayConnectionMessages)
    //         aapsLogger.debug(LTag.PUMP, "finishHandshaking [PumpPluginAbstract] - default (empty) implementation.")
    // }


    override val isFakingTempsByExtendedBoluses: Boolean
        get() = false


    override fun canHandleDST(): Boolean {
        // TODO(AndyRozman): check DST transitions
        return true
    }

    override fun isBusy(): Boolean {
        // Two orthogonal "don't dispatch right now" signals:
        //  - tandemDispatcher.isBusy(): an op is queued or in flight on PumpOpQueue's single dispatcher.
        //  - tandemPumpUtil.preventConnect: cartridge-change workflow is in progress and owns the
        //    pump comm channel. AAPS auto-reconnect must stand down for its duration.
        // Browsing Actions / Data does NOT set preventConnect — those sends are serialized by
        // the queue at USER_INITIATED priority and AAPS Loop can safely interleave.
        // PumpAvailability is intentionally NOT folded in here — mutating ops handle availability
        // via fast-fail at dispatch, not by stalling AAPS's command queue on isBusy().
        val isBusy = tandemDispatcher.isBusy() || tandemPumpUtil.preventConnect || pumpStatus.preventQueueExecution
        if (displayConnectionMessages) aapsLogger.debug(LTag.PUMP, "isBusy: $isBusy")
        return isBusy
    }

    override suspend fun getPumpStatus(reason: String) {
        var needRefresh = false

        aapsLogger.info(LTag.PUMP, "getPumpStatus [first_run=$firstRun]")

        if (firstRun) {
            needRefresh = initializePump(!isRefresh)
            firstRun = false
        } else {
            refreshAnyStatusThatNeedsToBeRefreshed()
        }

        if (needRefresh) {
            rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Full))
        }
    }


    fun resetStatusState() {
        firstRun = true
        isRefresh = true
    }


    private fun refreshAnyStatusThatNeedsToBeRefreshed(): Boolean {
        val statusRefresh = workWithStatusRefresh(
            PumpDataRefreshAction.GetData, null,
            null)!!

        if (!doWeHaveAnyStatusNeededRefereshing(statusRefresh)) {
            return false
        }

        //val refreshTypesNeededToReschedule: MutableSet<PumpDataRefreshType> = HashSet()

        //var resetTime = false
        var resetDisplay = false

        if (hasTimeDateOrTimeZoneChanged) {
            aapsLogger.info(TAG, "Refresh_PumpTime: hasTimeDateOrTimeZoneChanged")

            if (!statusRefresh.contains(PumpDataRefreshType.PumpTime)) {
                statusRefresh.put(PumpDataRefreshType.PumpTime, System.currentTimeMillis()-2000)
                aapsLogger.info(TAG, "Refresh_PumpTime: added PumpTime to change list")
            }
            //
            // checkTimeAndOptionallySetTime(readTime = true)
            //
            // // read time if changed, set new time
            // hasTimeDateOrTimeZoneChanged = false
            //
            // if (statusRefresh!!.contains(PumpDataRefreshType.PumpTime)) {
            //     statusRefresh.remove(PumpDataRefreshType.PumpTime)
            //     refreshTypesNeededToReschedule.add(PumpDataRefreshType.PumpTime)
            //     resetTime = true
            // }
        }

        // execute
        val currentTimeMillis = System.currentTimeMillis()

        aapsLogger.debug("RefreshCheck: $statusRefresh  (currentTime=$currentTimeMillis)")

        for ((key, value) in statusRefresh) {
            //aapsLogger.error("RefreshCheck: $key = $value  (currentTime=$currentTimeMillis)")
            if (value!=null && value > 0 && currentTimeMillis > value) {
                var resetTime = false
                when (key) {
                    // PumpDataRefreshType.PumpHistory      -> {
                    //     aapsLogger.info(LTag.PUMP, "Refresh_PumpHistory")
                    //     readPumpHistory()
                    // }

                    PumpDataRefreshType.PumpTime         -> {
                        aapsLogger.info(LTag.PUMP, "Refresh_PumpTime")
                        tandemDispatcher.submitDefault("getTime") { getTime() }
                        if (checkTimeAndOptionallySetTime(readTime = true)) {
                            resetDisplay = true
                        }
                        resetTime = true
                        //refreshTypesNeededToReschedule.add(key)
                        //resetTime = true
                    }

                    PumpDataRefreshType.GetTemporaryBasal -> {
                        resetTbrInDatabaseIfTbrCancelled()
                        resetTime = true
                    }

                    PumpDataRefreshType.BatteryStatus -> {
                        aapsLogger.error(LTag.PUMP, "Refresh_BatteryStatus")
                        tandemDispatcher.submitDefault("getBatteryLevel") { getBatteryLevel() }
                        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Battery))
                        //refreshTypesNeededToReschedule.add(key)
                        resetTime = true
                    }

                    PumpDataRefreshType.PumpStatus -> {
                        getFullPumpStatus(readHistory = true)
                        resetTime = true
                    }

                    // this is for simple status refresh (only PumpStatus without Alerts/Alarms or history
                    PumpDataRefreshType.Custom_1 -> {
                        aapsLogger.info(LTag.PUMP, "Refresh_Custom_1 (simple pump status after UI)")
                        tandemDispatcher.submitDefault("getPumpStatus") { getPumpStatus() }
                        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.PumpStatus))
                    }

                    PumpDataRefreshType.Custom_2 -> {
                        aapsLogger.error(LTag.PUMP, "Refresh_Custom_2 - Set QuickBolus settings on pump")

                        val stringQB = setQuickBolus()

                        tandemPumpUtil.sendNotification(notificationType = TandemNotificationType.TandemPumpSettingsUpdated,
                                                        stringQB)

                        resetTime = true
                    }

                    PumpDataRefreshType.RemainingInsulin -> {
                        aapsLogger.info(LTag.PUMP, "Refresh_RemainingInsulin")
                        tandemDispatcher.submitDefault("getRemainingInsulin") { getRemainingInsulin() }
                        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Reservoir))

                        //resetDisplay = true
                        resetTime = true
                    }

                    else -> {
                        aapsLogger.error(LTag.PUMP, "Refresh Unsupported action: $key")
                    }

                } // when

                //scheduleNextRefresh(key!!)

                if (resetTime) {
                    scheduleNextRefresh(key!!)
                }


            } // if

            // we always schedule refresh

        }

        // we always reset time
        pumpStatus.setLastCommunicationToNow()

        return resetDisplay
    }

    private fun resetTbrInDatabaseIfTbrCancelled() {

        aapsLogger.error(LTag.PUMP, "Refresh_GetTemporaryBasal: ")

        // we had TBR previously running (if not we ignore)
        if (pumpStatus.currentTempBasalInternal!=null) {
            aapsLogger.error(LTag.PUMP, "TBR was Previously Running")

            val estimatedEnd = pumpStatus.currentTempBasalInternal!!.start!! + (pumpStatus.currentTempBasalInternal!!.durationMinutes * 60 * 1000)

            if (System.currentTimeMillis() > estimatedEnd) {
                aapsLogger.error(LTag.PUMP, "Previously Running TBR expired, exiting.")
                pumpStatus.currentTempBasalInternal = null
                return
            }

            val tbr = tandemDispatcher.submitDefault("readTBR") { readTBR() }

            // if current tbr is null
            if (tbr==null) {
                aapsLogger.error(LTag.PUMP, "TBR is no longer running.")

                var tbrId : Long? = null

                if (pumpStatus.currentTempBasalInternal!!.id!=null) {
                    aapsLogger.error(LTag.PUMP, "Found id in currentTempBasalInternal")
                    tbrId = pumpStatus.currentTempBasalInternal!!.id
                } else {
                    val testTbrId = preferences.getIfExists(TandemLongNonPreferenceKey.LastTbrId)

                    if (testTbrId!=null && testTbrId>0) {
                        aapsLogger.error(LTag.PUMP, "Found id in from Preferences")
                        tbrId = testTbrId
                    }
                }

                if (tbrId!=null) {
                    runBlocking {
                        pumpSync.syncStopTemporaryBasalWithPumpId(
                            timestamp = System.currentTimeMillis(),
                            pumpSerial = serialNumber(),
                            pumpType = PumpType.TANDEM_MOBI_BT,
                            endPumpId = tandemPumpUtil.getPrefixedIdForDb(pumpEventId = tbrId, isBolus = false)
                        )
                    }

                    pumpStatus.currentTempBasalInternal = null
                    preferences.put(TandemLongNonPreferenceKey.LastTbrId, 0L)
                }
            }
        }


        // val tbr = readTBR()
        //
        // if (tbr==null) {
        //     aapsLogger.error(LTag.PUMP, "TBR is NULL.")
        //
        //
        //
        // } else {
        //     aapsLogger.error(LTag.PUMP, "TBR is $tbr")
        //
        //     if (pumpStatus.currentTempBasalInternal!=null) {
        //         var estimatedEnd = tbr.start!! + (tbr.durationMinutes * 60 * 1000)
        //
        //         if (System.currentTimeMillis() > estimatedEnd) {
        //
        //         }
        //
        //         aapsLogger.error(LTag.PUMP, "TBR was Previosuly Running")
        //     }
        //
        // }



    }

    private fun getFullPumpStatus(readHistory: Boolean) {
        aapsLogger.info(LTag.PUMP, "Refresh_PumpStatus")
        tandemDispatcher.submitDefault("getPumpStatus") { getPumpStatus() }

        readNotificationsForSemaphore()

        if (readHistory) {
            historyRetriever.downloadHistory()
        }
    }


    private fun readNotificationsForSemaphore() {

        val commandList = listOf(TandemCustomCommand.GET_ALERTS,
                                 TandemCustomCommand.GET_ALARMS,
                                 TandemCustomCommand.GET_MALFUNCTIONS)

        var notificationFound = false

        for (command in commandList) {
            val customCommandResponse = tandemDispatcher.submitDefault("getNotifications:$command") {
                executeCustomCommand(command)
            }

            if (customCommandResponse.isSuccess) {
                val valueOfResponse = customCommandResponse.value

                when(valueOfResponse) {
                    is AlertStatusDto -> {
                        if (!valueOfResponse.alerts.isEmpty()) {
                            notificationFound = true
                            break
                        }
                    }
                    is AlarmStatusDto -> {
                        if (!valueOfResponse.alarms.isEmpty()) {
                            notificationFound = true
                            break
                        }
                    }
                    is MalfunctionStatusDto -> {
                        if (valueOfResponse.hasMalfunction() && !valueOfResponse.malfunctionMatchesActiveAlertOrAlarm) {
                            notificationFound = true
                            break
                        }
                    }
                }
            }
        }

        if (pumpStatus.semaphoreNotifications != notificationFound) {
            pumpStatus.semaphoreNeedsRefresh = false
            pumpStatus.semaphoreNotifications = notificationFound
            rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Custom_2))
        }

    }


    private fun setRefreshButtonEnabled(enabled: Boolean) {
        rxBus.send(EventRefreshButtonState(enabled))
    }


    private fun initializePump(realInit: Boolean): Boolean {
        //if (isDriverInitialized) return false
        aapsLogger.info(LTag.PUMP, logPrefix + "initializePump - start")

        setRefreshButtonEnabled(false)
        //pumpState = PumpDriverState.Connected

        // time (6h) - setting time command not available
        aapsLogger.info(LTag.PUMP, "Refresh_PumpTime: onInitializePump")
        checkTimeAndOptionallySetTime(!realInit)  // we read time only if its refresh and not first init

        // read status of pump from Db
        getFullPumpStatus(readHistory = true)
        scheduleNextRefresh(PumpDataRefreshType.PumpStatus, 0)
        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Configuration))

        // remaining insulin (>50 = 4h; 50-20 = 1h; 15m) -
        tandemDispatcher.submitDefault("getRemainingInsulin") { getRemainingInsulin() }
        scheduleNextRefresh(PumpDataRefreshType.RemainingInsulin, 1)
        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Reservoir))

        // remaining power (1h) -
        tandemDispatcher.submitDefault("getBatteryLevel") { getBatteryLevel() }
        scheduleNextRefresh(PumpDataRefreshType.BatteryStatus, 2)
        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Battery))

        // site reminder
        // val reminder = preferences.get(TandemLongNonPreferenceKey.SiteReminderDateTime)
        // this.pumpStatus.tandemSiteReminder = if (reminder>0) null else reminder

        // configuration (once and then if history shows config changes)
        tandemDispatcher.submitDefault("getConfiguration") { getConfiguration() }
        checkThatSettingsAreEnforced()

        // pump info
        tandemDispatcher.submitDefault("getPumpInfo") {
            executeCustomCommand(TandemCustomCommand.GET_PUMP_INFO)
        }

        // get TBR (if needed)
        if (pumpStatus.pumpStatusMirror==null || pumpStatus.pumpStatusMirror!!.isTemporaryBasalRunning()) {
            val tbrRun = tandemDispatcher.submitDefault("readTBR") { readTBR() }

            if (tbrRun!=null && pumpStatus.currentTempBasalInternal==null) {
                pumpStatus.currentTempBasalInternal = tbrRun
            }

            rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.TBR))
        }

        // get last bolus
        tandemDispatcher.submitDefault("getBolus") { getBolus() }
        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.Bolus))

        // get basal profile
        tandemDispatcher.submitDefault("getBasalProfile") {
            getBasalProfile()
            refreshBasalRateDisplay()
        }

        pumpStatus.setLastCommunicationToNow()
        setRefreshButtonEnabled(true)

        if (!isRefresh) {
            pumpState = PumpDriverState.Initialized
        }

        isDriverInitialized = true  // means that first data was read
        firstRun = false

        return true
    }


    override fun isThisProfileSet(profile: PumpProfile): Boolean {

        aapsLogger.debug(TAG, "isThisProfileSet")

        if (!isDriverInitialized) {
            aapsLogger.warn(TAG, "Driver not initialized. Returning isThisProfileSet=true" )
            return true
        }

        if (pumpStatus.basalsByHour == null) {
            aapsLogger.debug(TAG, "  Pump Profile:     null, returning false")
            return false
        } else {
            val profileAsString = ProfileUtil.getBasalProfilesDisplayableAsStringOfArrayV2(profile, PumpType.TANDEM_MOBI_BT)
            val profileDriver = ProfileUtil.getProfilesByHourToString(pumpStatus.basalsByHour)

            aapsLogger.debug(TAG, "AAPS Profile:     $profileAsString")
            aapsLogger.debug(TAG, "Pump Profile:     $profileDriver")

            val areTheySame = profileAsString.equals(profileDriver)

            aapsLogger.debug(TAG, "Pump Profile is the same: $areTheySame")

            return areTheySame
        }
    }


    fun lastDataTime(): Long {
        return if (pumpStatus.lastConnection != 0L) {
            pumpStatus.lastConnection
        } else System.currentTimeMillis()
    }


    val baseBasalRateFlow = MutableStateFlow<PumpRate?>(null)
    override val baseBasalRate: PumpRate
        get() = if (pumpStatus.basalsByHour == null) {
                aapsLogger.debug(LTag.PUMP, "Profile is not set !")
                pumpStatus.baseBasalRate = 0.0
                baseBasalRateFlow.value = PumpRate(pumpStatus.baseBasalRate)
                baseBasalRateFlow.value!!
            } else {
                val basal = pumpStatus.basalProfileForHour
                aapsLogger.debug(LTag.PUMP, "Basal for this hour is: $basal")
                pumpStatus.baseBasalRate = basal
                baseBasalRateFlow.value = PumpRate(basal)
                baseBasalRateFlow.value!!
            }


    override fun triggerUIChange() {
        rxBus.send(EventPumpFragmentValuesChanged(PumpUpdateFragmentType.TreatmentValues))
    }


    // override fun hasService(): Boolean {
    //     return true
    // }

    private var bolusDeliveryType = BolusDeliveryType.Idle

    private enum class BolusDeliveryType {
        Idle,  //
        DeliveryPrepared,  //
        Delivering,  //
        CancelDelivery
    }


    private fun checkThatSettingsAreEnforced() {

        if (pumpStatus.settings!=null) {

            val changedItems = mutableListOf<String>()

            var maxBolus = (pumpStatus.settings!![TandemPumpSettingType.MAX_BOLUS]) as Int
            val maxBolusRequired = tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.MaxBolus)

            maxBolus /= 1000

            aapsLogger.debug(TAG, "Current Max Bolus: ${maxBolus}, Required: $maxBolusRequired")

            if (maxBolus != maxBolusRequired) {
                tandemDispatcher.submitDefault("setMaxBolus") {
                    executeCustomCommand(TandemCustomCommand.SET_MAX_BOLUS, maxBolusRequired)
                }
                changedItems.add("Max Bolus")
            }

            // note: TandemPumpSettingType stores as milliunits (long)
            val maxBasal = (pumpStatus.settings!![TandemPumpSettingType.BASAL_LIMIT]) as Long
            // convert milliunits (1000) -> units (1), truncate to int
            val maxBasalInt = InsulinUnit.from1000To1(maxBasal).toInt()

            // note: TandemIntPreferenceKey stores as units (int)
            val maxBasalRequired = tandemPumpUtil.getIntPreferenceOrDefault(TandemIntPreferenceKey.MaxBasal)


            aapsLogger.debug(TAG, "Current Max Basal: ${maxBasalInt}, Required: $maxBasalRequired")

            if (maxBasalInt != maxBasalRequired) {
                tandemDispatcher.submitDefault("setMaxBasal") {
                    executeCustomCommand(TandemCustomCommand.SET_MAX_BASAL, maxBasalRequired)
                }
                changedItems.add("Max Basal")
            }

            val controlIQEnabled = (pumpStatus.settings!![TandemPumpSettingType.CONTROL_IQ_ENABLED]) as Boolean

            if (controlIQEnabled) {
                tandemDispatcher.submitDefault("setControlIQ") {
                    executeCustomCommand(TandemCustomCommand.SET_CONTROL_IQ, false)
                }
                changedItems.add("Control IQ")
            }

            val pumpGlobalsResponse = (pumpStatus.settings!![TandemPumpSettingType.QUICK_BOLUS]) as PumpGlobalsResponse

            if (isQuickBolusIncorrectlySet(pumpGlobalsResponse)) {
                val stringQB = setQuickBolus()
                changedItems.add(stringQB)
                this.newQuickBolusType = null
            }

            // if (isSoundIncorrectlySet(pumpGlobalsResponse)) {
            //     tandemDispatcher.submitDefault("setPumpSounds") {
            //         executeCustomCommand(TandemCustomCommand.SET_PUMP_SOUNDS, PumpGlobalsResponse.AnnunciationEnum.VIBRATE)
            //     }
            // }


            if (changedItems.isNotEmpty()) {
                val changedItemsString = changedItems.joinToString(", ")

                tandemPumpUtil.sendNotification(notificationType = TandemNotificationType.TandemPumpSettingsUpdated,
                                                changedItemsString)
            }
        }
    }


    private fun isQuickBolusIncorrectlySet(pumpGlobalsResponse: PumpGlobalsResponse): Boolean {

        if (preferences.getIfExists(TandemStringPreferenceKey.QuickBolusTypePref) != null) {
            val preferenceQBString = preferences.get(TandemStringPreferenceKey.QuickBolusTypePref)

            val quickBolusType = QuickBolusType.valueOf(preferenceQBString)
            val quickBolusIncrementPump = pumpGlobalsResponse.quickBolusIncrement

            aapsLogger.info(TAG, "Quick Bolus in Settings=${quickBolusType.name} and on Pump=${quickBolusIncrementPump.name}")

            if (quickBolusType.quickBolusIncrement != quickBolusIncrementPump) {
                newQuickBolusType = quickBolusType
                return true
            } else {
                return false
            }

        } else {
            return false
        }
    }

    private fun isSoundIncorrectlySet(pumpGlobalsResponse: PumpGlobalsResponse): Boolean {
        // TODO at the moment we will set defaults to

        if (pumpGlobalsResponse.buttonAnnun != PumpGlobalsResponse.AnnunciationEnum.VIBRATE ||
            pumpGlobalsResponse.quickBolusAnnun != PumpGlobalsResponse.AnnunciationEnum.VIBRATE ||
            pumpGlobalsResponse.bolusAnnun != PumpGlobalsResponse.AnnunciationEnum.VIBRATE ||
            pumpGlobalsResponse.reminderAnnun != PumpGlobalsResponse.AnnunciationEnum.VIBRATE ||
            pumpGlobalsResponse.alertAnnun != PumpGlobalsResponse.AnnunciationEnum.VIBRATE ||
            pumpGlobalsResponse.alarmAnnun != PumpGlobalsResponse.AnnunciationEnum.VIBRATE ||
            pumpGlobalsResponse.fillTubingAnnun != PumpGlobalsResponse.AnnunciationEnum.VIBRATE) {

            return true;

        }






        return false;
    }


    private fun setQuickBolus(): String {

        tandemDispatcher.submitDefault("setQuickBolus") {
            executeCustomCommand(command = TandemCustomCommand.SET_QUICK_BOLUS,
                                 data = newQuickBolusType)
        }

        val stringQuickBolus = rh.gs(this.newQuickBolusType!!.friendlyName)

        return "Quick Bolus (${stringQuickBolus})"

    }


    private fun checkTimeAndOptionallySetTime(readTime: Boolean): Boolean {
        aapsLogger.info(LTag.PUMP, logPrefix + "checkTimeAndOptionallySetTime - Start")

        if (readTime) {
            tandemDispatcher.submitDefault("getTime") { getTime() }
        }

        try {

            if (pumpStatus.pumpTime != null) {

                val diff = Math.abs(pumpStatus.pumpTime!!.timeDifference)

                pumpStatus.pumpTime!!.displayTime(gson = tandemPumpUtil.gson,
                                                  aapsLogger = aapsLogger)

                if (diff > 60) {
                    aapsLogger.error(TAG, "Time difference between phone and pump is more than 60s ($diff)")

                    if (!pumpStatus.tandemPumpFirmware.isMobi()) {
                        tandemPumpUtil.sendNotification(TandemNotificationType.TimeDifferenceTooBig, 60, diff)
                    } else {

                        val time = tandemDispatcher.submitDefault("setTime") { setTime() }

                        if (time.isSuccess) {
                            tandemPumpUtil.sendNotification(TandemNotificationType.DateTimeUpdated)
                        } else {
                            aapsLogger.error(LTag.PUMP, "Setting time on pump failed.")
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            aapsLogger.error(TAG, "Setting time on pump failed.")
        } finally {
            setRefreshButtonEnabled(false)
            scheduleNextRefresh(PumpDataRefreshType.PumpTime, 0)
        }

        return true
    }


    override fun deliverBolus(detailedBolusInfo: DetailedBolusInfo): PumpEnactResult = tandemDispatcher.submitMutating(
        name = "deliverBolus",
        maxDuration = 10.minutes,
        unavailable = { e ->
            PumpEnactResultObject(rh).success(false).enacted(false)
                .comment(rh.gs(Rc.string.pump_cmd_err_bolus_could_not_be_delivered) + " (${e.availability})")
        }
    ) {
        deliverBolusBody(detailedBolusInfo)
    }

    private fun PumpDispatcherScope.deliverBolusBody(detailedBolusInfo: DetailedBolusInfo): PumpEnactResult {
        aapsLogger.info(LTag.PUMP, logPrefix + "deliverBolus - " + BolusDeliveryType.DeliveryPrepared)
        return if (detailedBolusInfo.insulin > pumpStatus.reservoirRemainingUnits) {
            PumpEnactResultObject(rh) //
                .success(false) //
                .enacted(false) //
                .comment(
                    rh.gs(
                        Rc.string.pump_cmd_err_bolus_could_not_be_delivered_no_insulin,
                        pumpStatus.reservoirRemainingUnits,
                        detailedBolusInfo.insulin
                    )
                )
        } else try {
            setRefreshButtonEnabled(false)

            aapsLogger.error(TAG, "Bolus initiation: $detailedBolusInfo")

            bolusDeliveryType = BolusDeliveryType.DeliveryPrepared

            val commandResponse = deliverBolus(detailedBolusInfo)

            if (commandResponse.isSuccess) {
                val now = System.currentTimeMillis()

                val bolusData = commandResponse.value!!

                aapsLogger.error(TAG, "Bolus Data after Bolus: ${tandemPumpUtil.gson.toJson(bolusData)}")

                detailedBolusInfo.bolusTimestamp = now

                if (!bolusData.fullyDelivered) {
                    detailedBolusInfo.insulin = bolusData.amountImmediateDelivered!!
                }

                pumpStatus.lastBolusData = bolusData

                // we subtract insulin, exact amount will be visible with next remainingInsulin update.
                pumpStatus.reservoirRemainingUnits -= detailedBolusInfo.insulin

                incrementStatistics(if (detailedBolusInfo.bolusType == BS.Type.SMB)
                    TandemLongNonPreferenceKey.SmbBoluses
                else
                    TandemLongNonPreferenceKey.StandardBoluses)

                if (detailedBolusInfo.carbs > 0.0) {
                    runBlocking {
                        pumpSync.syncCarbsWithTimestamp(
                            timestamp = now,
                            amount = detailedBolusInfo.carbs,
                            pumpId = tandemPumpUtil.getPrefixedIdForDb(pumpEventId = bolusData.bolusId!!, isBolus = true),
                            pumpType = pumpType,
                            pumpSerial = serialNumber()
                        )
                    }
                }

                if (detailedBolusInfo.insulin > 0.0) {
                    runBlocking {
                        pumpSync.syncBolusWithPumpId(
                            timestamp = now,
                            amount = PumpInsulin(detailedBolusInfo.insulin),
                            pumpId = tandemPumpUtil.getPrefixedIdForDb(pumpEventId = bolusData.bolusId!!, isBolus = true),
                            pumpType = pumpType,
                            pumpSerial = serialNumber(),
                            type = detailedBolusInfo.bolusType
                        )
                    }
                }

                //readPumpHistoryAfterAction(bolusInfo = detailedBolusInfo)

                bolusDeliveryType = BolusDeliveryType.Idle

                PumpEnactResultObject(rh).success(true) //
                    .enacted(true) //
                    .bolusDelivered(detailedBolusInfo.insulin)
            } else {
                PumpEnactResultObject(rh) //
                    .success(false) //
                    .enacted(false) //
                    .comment(rh.gs(Rc.string.pump_cmd_err_bolus_could_not_be_delivered))
            }
        } finally {
            finishAction("Bolus")
        }
    }


    override fun stopBolusDelivering() {
        // CRITICAL — must run during the gating bolus workflow.
        tandemDispatcher.submitCritical("stopBolusDelivering") {
            stopBolusDeliveringBody()
        }
    }

    private fun PumpDispatcherScope.stopBolusDeliveringBody() {
        aapsLogger.debug(TAG, "stopBolusDelivering")

        if (bolusDeliveryType==BolusDeliveryType.Delivering ||
            bolusDeliveryType==BolusDeliveryType.DeliveryPrepared) {

            bolusDeliveryType = BolusDeliveryType.CancelDelivery // we don't want to come here twice

            aapsLogger.info(TAG, "Cancelling Bolus")
            val cancelBolusResponse = cancelBolus(BolusData())

            if (cancelBolusResponse.isSuccess) {
                bolusDeliveryType = BolusDeliveryType.Idle
            } else {
                aapsLogger.warn(TAG, "Bolus couldn't be cancelled")
            }

        } else {
            aapsLogger.debug(TAG, "Bolus delivery Type is $bolusDeliveryType, cancelBolus will be skipped.")
        }
    }


    private val isLoggingEnabled: Boolean
        get() = true

    //setTempBasalPercent(percent: Int, durationInMinutes: Int, enforceNew: Boolean, tbrType: TemporaryBasalType)

    // if enforceNew===true current temp basal is canceled and new TBR set (duration is prolonged),
    // if false and the same rate is requested enacted=false and success=true is returned and TBR is not changed
    override suspend fun setTempBasalPercent(percent: Int, durationInMinutes: Int,
                                     enforceNew: Boolean, tbrType: TemporaryBasalType
    ): PumpEnactResult = tandemDispatcher.submitMutating(
        name = "setTempBasalPercent",
        maxDuration = 2.minutes,
        unavailable = { e ->
            PumpEnactResultObject(rh).success(false).enacted(false)
                .comment(rh.gs(Rc.string.pump_cmd_err_tbr_could_not_be_delivered) + " (${e.availability})")
        }
    ) {
        setTempBasalPercentBody(percent, durationInMinutes, enforceNew, tbrType)
    }

    private fun PumpDispatcherScope.setTempBasalPercentBody(percent: Int, durationInMinutes: Int,
                                        enforceNew: Boolean, tbrType: TemporaryBasalType
    ): PumpEnactResult {
        setRefreshButtonEnabled(false)

        return try {
            aapsLogger.info(LTag.PUMP, "TBR setTempBasalPercent: rate: $percent %, duration=$durationInMinutes [enforce=$enforceNew,tbrType=${tbrType.name}]"  )

            // read current TBR
            val tbrCurrent = readTBR()  // if null is returned no TBR running
            if (tbrCurrent != null) {
                aapsLogger.info(LTag.PUMP, "TBR Current Basal: $tbrCurrent")
            }

            if (!enforceNew && tbrCurrent!=null) {
                aapsLogger.info(LTag.PUMP, "enforceNEW = false")
                if (tandemPumpUtil.isSame(tbrCurrent.insulinRate, percent)) {
                    aapsLogger.info(LTag.PUMP, "enforceNEW = false, same = true")
                    aapsLogger.info(LTag.PUMP, logPrefix + "TBR setTempBasalPercent - No enforceNew and same rate. Exiting.")
                    return pumpEnactResultProvider.get().success(true).enacted(false)
                }
                // if not the same rate, we continue to next step
            }

            // if TBR is running we will cancel it.
            if (tbrCurrent!=null) {
                aapsLogger.info(LTag.PUMP, "setTempBasalPercent - TBR running - so canceling it.")

                if (!sendCancelTbrToPump()) {
                    aapsLogger.error(logPrefix + "setTempBasalPercent - Cancel TBR failed.")
                    return pumpEnactResultProvider.get().success(false).enacted(false)
                        .comment(rh.gs(Rc.string.pump_cmd_err_cant_cancel_tbr_stop_op))
                }
            }

            // now start new TBR
            val commandResponse  = setTemporaryBasal(percent, durationInMinutes)

            aapsLogger.info(LTag.PUMP, logPrefix + "setTempBasalPercent - setTBR. Response: " + commandResponse)
            if (commandResponse.isSuccess) {

                val controlCommandResponse: TempBasalPair = commandResponse.value as TempBasalPair

                pumpStatus.currentTempBasalInternal = controlCommandResponse

                preferences.put(key = TandemLongNonPreferenceKey.LastTbrId,
                                value = controlCommandResponse.id!!)

                runBlocking {
                    pumpSync.syncTemporaryBasalWithPumpId(
                        timestamp = controlCommandResponse.start!!,
                        isAbsolute = false,
                        pumpSerial = serialNumber(),
                        pumpType = PumpType.TANDEM_MOBI_BT,
                        type = tbrType,
                        duration = (controlCommandResponse.durationMinutes * 60 * 1000).toLong(),
                        rate = PumpRate(percent.toDouble()),
                        pumpId = tandemPumpUtil.getPrefixedIdForDb(pumpEventId = controlCommandResponse.id!!, isBolus = false)
                    )
                }

                incrementStatistics(statsKey = TandemLongNonPreferenceKey.TbrsSet)

                pumpEnactResultProvider.get().success(true).enacted(true) //
                    .percent(percent).duration(durationInMinutes)
            } else {
                pumpEnactResultProvider.get().success(false).enacted(false) //
                    .comment(rh.gs(Rc.string.pump_cmd_err_tbr_could_not_be_delivered))
            }
        } finally {
            finishAction("TBR")
        }
    }

    override suspend fun setTempBasalAbsolute(absoluteRate: Double, durationInMinutes: Int,
                                      enforceNew: Boolean, tbrType: TemporaryBasalType
    ): PumpEnactResult = tandemDispatcher.submitMutating(
        name = "setTempBasalAbsolute",
        maxDuration = 2.minutes,
        unavailable = { e ->
            PumpEnactResultObject(rh).success(false).enacted(false)
                .comment(rh.gs(Rc.string.pump_cmd_err_tbr_could_not_be_delivered) + " (${e.availability})")
        }
    ) {
        aapsLogger.info(LTag.PUMP, "TBR setTempBasalAbsolute called with a rate of $absoluteRate for $durationInMinutes min [enforce=$enforceNew,tbrType=${tbrType.name}].")
        val unroundedPercentage = ((absoluteRate / baseBasalRate.cU) * 100).toInt()
        aapsLogger.info(LTag.PUMP, "TBR abs=$absoluteRate,base=${pumpStatus.basalProfileForHour},percent: $unroundedPercentage")
        // Already inside a queued op — call body directly to avoid nested submit (deadlock).
        setTempBasalPercentBody(unroundedPercentage, durationInMinutes, enforceNew, tbrType)
    }


    private fun finishAction(overviewKey: String?) {
        //if (overviewKey != null) rxBus.send(EventRefreshOverview(overviewKey, false))
        //triggerUIChange()
        //setRefreshButtonEnabled(true)
    }



    private fun PumpDispatcherScope.readTBR(): TempBasalPair? {
        val temporaryBasalResponse = getTemporaryBasal()

        aapsLogger.info(LTag.PUMP, "TBR readTBR $temporaryBasalResponse")

        return if (temporaryBasalResponse.value!=null) {
            val tbr = temporaryBasalResponse.value!!
            tbr
        } else {
            null
        }
    }


    override suspend fun cancelTempBasal(enforceNew: Boolean): PumpEnactResult = tandemDispatcher.submitMutating(
        name = "cancelTempBasal",
        maxDuration = 1.minutes,
        unavailable = { e ->
            PumpEnactResultObject(rh).success(false).enacted(false)
                .comment(rh.gs(Rc.string.pump_cmd_err_cant_cancel_tbr) + " (${e.availability})")
        }
    ) {
        cancelTempBasalBody(enforceNew)
    }

    private fun PumpDispatcherScope.cancelTempBasalBody(enforceNew: Boolean): PumpEnactResult {
        return try {
            aapsLogger.info(TAG, "TBR cancelTempBasal - started")
            setRefreshButtonEnabled(false)
            val tbrCurrent = readTBR()

            if (tbrCurrent==null) {
                aapsLogger.info(TAG, "cancelTempBasal - TBR is not running exiting.")
                pumpStatus.clearTbr()
                return pumpEnactResultProvider.get().success(true).enacted(false)
            }

            //val commandResponseCancel = cancelTemporaryBasal()

            if (sendCancelTbrToPump()) {
                aapsLogger.info(TAG, "cancelTempBasal - Cancel TBR successful.")

                // //readPumpHistoryAfterAction(tempBasalInfo = TempBasalPair(insulinRate = 0.0, isPercent = true, durationMinutes = 0))
                // pumpStatus.clearTbr()
                //
                // val controlCommandResponse = commandResponseCancel.value as ControlCommandResponse
                //
                //
                // pumpSync.syncStopTemporaryBasalWithPumpId(
                //     timestamp = System.currentTimeMillis(),
                //     pumpSerial = serialNumber(),
                //     pumpType = PumpType.TANDEM_MOBI_BT,
                //     endPumpId = getPrefixedIdForDb(pumpEventId = controlCommandResponse.id.toLong(), isBolus =false)
                // )
                //
                // pumpStatus.currentTempBasalInternal = null

                pumpEnactResultProvider.get().success(true).enacted(true) //
                    .isTempCancel(true)
            } else {
                aapsLogger.info(TAG, "cancelTempBasal - Cancel TBR failed.")
                pumpEnactResultProvider.get().success(false).enacted(false) //
                    .comment(rh.gs(Rc.string.pump_cmd_err_cant_cancel_tbr))
            }
        } finally {
            finishAction("TBR")
        }
    }


    private fun PumpDispatcherScope.sendCancelTbrToPump(): Boolean {

        val commandResponseCancel = cancelTemporaryBasal()

        if (commandResponseCancel.isSuccess) {
            aapsLogger.info(TAG, "cancelTempBasal - Cancel TBR successful.")

            pumpStatus.clearTbr()

            val controlCommandResponse = commandResponseCancel.value as ControlCommandResponse

            runBlocking {
                pumpSync.syncStopTemporaryBasalWithPumpId(
                    timestamp = System.currentTimeMillis(),
                    pumpSerial = serialNumber(),
                    pumpType = PumpType.TANDEM_MOBI_BT,
                    endPumpId = tandemPumpUtil.getPrefixedIdForDb(pumpEventId = controlCommandResponse.id.toLong(), isBolus = false)
                )
            }

            pumpStatus.currentTempBasalInternal = null
            preferences.put(TandemLongNonPreferenceKey.LastTbrId, 0L)
        }

        return commandResponseCancel.isSuccess
    }

    override fun serialNumber(): String {
        return pumpStatus.serialNumber.toString()
    }


    override fun generateTempId(objectA: Any): Long {
        return 0L
    }


    override suspend fun setNewBasalProfile(profile: PumpProfile): PumpEnactResult = tandemDispatcher.submitMutating(
        name = "setNewBasalProfile",
        maxDuration = 2.minutes,
        unavailable = { e ->
            PumpEnactResultObject(rh).success(false).enacted(false)
                .comment(rh.gs(Rc.string.pump_cmd_err_basal_profile_could_not_be_set) + " (${e.availability})")
        }
    ) {
        setNewBasalProfileBody(profile)
    }


    private fun PumpDispatcherScope.setNewBasalProfileBody(profile: PumpProfile): PumpEnactResult {
        aapsLogger.info(LTag.PUMP, "setNewBasalProfile - start")
        return try {
            setRefreshButtonEnabled(false)
            val resultCommandResponse: DataCommandResponse<Boolean?>

            resultCommandResponse = setBasalProfile(profile)

            aapsLogger.info(LTag.PUMP, logPrefix + "Basal Profile was set: " + resultCommandResponse)

            if (resultCommandResponse.isSuccess) {
                pumpStatus.basalsByHour = ProfileUtil.getArrayOfHourlyBasals(profile)
                pumpEnactResultProvider.get()
                    .success(true).enacted(true)
            } else {
                pumpEnactResultProvider.get()
                    .success(false).enacted(false)
                    .comment(rh.gs(Rc.string.pump_cmd_err_basal_profile_could_not_be_set))
            }
        } finally {
            finishAction("Set Basal Profile")
            refreshBasalRateDisplay()  // this forces refresh of basal value
        }
    }


    private fun refreshBasalRateDisplay() {
        val bbr = baseBasalRate
    }


    init {
        displayConnectionMessages = false // TODO can be removed in future
    }


    override fun getRefreshTime(pumpDataRefreshType: PumpDataRefreshType): Int {
        return (
            when(pumpDataRefreshType) {
                PumpDataRefreshType.RemainingInsulin -> {
                    val remaining = pumpStatus.reservoirRemainingUnits
                    if (remaining > 50) 60 else if (remaining > 20) 30 else 15
                }
                PumpDataRefreshType.BatteryStatus    -> {
                    val power = pumpStatus.batteryRemaining ?: 0
                    if (power > 30) 55 else if (power > 20) 30 else 15
                }
                PumpDataRefreshType.PumpTime         -> 300
                PumpDataRefreshType.PumpStatus       -> 5
                else                                 -> -1
        })
    }


    override fun isInPreventConnectMode(): Boolean {
        return tandemPumpUtil.preventConnect
    }


    // TODO(jwoglom): we will need this to call changetimedate not sure if relevan
    override suspend fun timezoneOrDSTChanged(timeChangeType: TimeChangeType) {
        aapsLogger.warn(LTag.PUMP, logPrefix + "Time or TimeZone changed (type=$timeChangeType). ")
        this.timeChangeType = timeChangeType
        this.hasTimeDateOrTimeZoneChanged = true
        val timeRefresh = System.currentTimeMillis() + (20*1000)  // 20s in future
        scheduleNextRefreshWithSpecifiedTime(PumpDataRefreshType.PumpTime, timeRefresh)
        // TODO timezoneOrDSTChanged: this might not work as expected, needs to be tested again...
    }


    // TODO Preferences:
    //    - add MIN_RESERVOIR2 confirmation not implemented yet, but might be needed

    // TODO getPreferenceScreenContent doesn't fully work (withEntriesProvider problem)

    override fun getPreferenceScreenContent(): PreferenceSubScreenDef =
        PreferenceSubScreenDef(
            key = "tandem_tmobi_settings",
            titleResId = R.string.tandem_mobi_pump_settings,
            icon = pluginDescription.icon,
            items = listOf(
                TandemBooleanPreferenceKey.UseSharedConnection,
                TandemStringPreferenceKey.SharedConnectionData,
                TandemIntentPreferenceKey.PumpPairing.withActivity(TandemMobiConnectionWizardActivity::class.java),
                TandemIntPreferenceKey.MaxBolus,
                TandemIntPreferenceKey.MaxBasal,
                TandemStringPreferenceKey.QualifyingEventsFilterPref,
                TandemStringPreferenceKey.QualifyingEventsRangePref,
                // TandemStringPreferenceKey.QualifyingEventsFilterPref.withEntriesProvider(provider = { context: Context ->
                //     QualifyingEventsFilter.entries.associate { it.name to context.getString(it.friendlyName) }
                // }),
                // TandemStringPreferenceKey.QualifyingEventsRangePref.withEntriesProvider(provider = { context: Context ->
                //     QualifyingEventsRange.entries.associate { it.name to context.getString(it.friendlyName) }
                // }),
                TandemBooleanPreferenceKey.DisplayDriverVersion,
                TandemBooleanPreferenceKey.ShowCargoOfUnknownEntries,
                TandemBooleanPreferenceKey.AutoConfirmLowBasalDelivery,
                TandemStringPreferenceKey.QuickBolusTypePref
                // TandemStringPreferenceKey.QuickBolusTypePref.withEntriesProvider(provider = { context: Context ->
                //     QuickBolusType.entries.associate { it.name to context.getString(it.friendlyName) }
                // })
            )
        )


}