package app.aaps.plugins.sync.nsclientV3.workers

import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.nsclient.NSClientRepository
import app.aaps.core.interfaces.sync.NsClient
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.nssdk.interfaces.NSAndroidClient
import app.aaps.plugins.sync.nsclientV3.NSClientV3Plugin
import app.aaps.plugins.sync.nsclientV3.NsIncomingDataProcessor
import dev.zacsweers.metro.Inject
import kotlin.math.max

/**
 * This class is needed only if you are using Xdrip to fill your data to NS and then using NS BG Source.
 */
@Inject
class LegacyBgRetriever(
    private val aapsLogger: AAPSLogger,
    private val dateUtil: DateUtil,
    private val nsClientV3Plugin: NSClientV3Plugin,
    private val nsIncomingDataProcessor: NsIncomingDataProcessor,
    private val nsClientRepository: NSClientRepository
) {

    val enabled: Boolean
        get() = true

    suspend fun run(
        nsAndroidClient: NSAndroidClient
    ) {
        val isFirstLoad =
            nsClientV3Plugin.isFirstLoad(
                NsClient.Collection.ENTRIES
            )

        val lastLoaded =
            if (isFirstLoad) {
                max(
                    nsClientV3Plugin
                        .firstLoadContinueTimestamp
                        .collections
                        .entries,
                    dateUtil.now() - nsClientV3Plugin.maxAge
                )
            } else {
                max(
                    nsClientV3Plugin
                        .lastLoadedSrvModified
                        .collections
                        .entries,
                    dateUtil.now() - nsClientV3Plugin.maxAge
                )
            }

        aapsLogger.info(
            LTag.NSCLIENT,
            "ANDY-NS BG retrieval mode: legacy timestamp API " +
                "lastLoaded=$lastLoaded"
        )

        val response =
            nsAndroidClient.getSgvsNewerThan(
                lastLoaded,
                NSClientV3Plugin.RECORDS_TO_LOAD
            )

        /*
         * Calibration entries are returned through the same entries query.
         */
        if (response.calibrations.isNotEmpty()) {
            nsClientRepository.addLog(
                "◄ RCV",
                "${response.calibrations.size} calibrations from " +
                    dateUtil.dateAndTimeAndSecondsString(lastLoaded)
            )

            nsIncomingDataProcessor.processCalibrations(
                response.calibrations,
                nsClientV3Plugin.doingFullSync
            )
        }

        val sgvs = response.values

        aapsLogger.debug(
            LTag.NSCLIENT,
            "ANDY-NS LEGACY SGVS: $sgvs"
        )

        if (sgvs.isNotEmpty()) {
            val action =
                if (isFirstLoad) "RCV-F"
                else "RCV"

            nsClientRepository.addLog(
                "◄ $action",
                "${sgvs.size} SGVs from " +
                    dateUtil.dateAndTimeAndSecondsString(lastLoaded)
            )

            /*
             * Deliberately ignore the returned continue-loading value.
             *
             * Legacy mode performs one request only. The next fetch is
             * scheduled relative to the newest BG timestamp instead of
             * immediately loading another page using the same cursor.
             */
            nsIncomingDataProcessor.processSgvs(
                sgvs,
                nsClientV3Plugin.doingFullSync
            )
        } else {
            if (isFirstLoad) {
                nsClientV3Plugin
                    .lastLoadedSrvModified
                    .collections
                    .entries = lastLoaded

                nsClientV3Plugin.storeLastLoadedSrvModified()
            }

            nsClientRepository.addLog(
                "◄ RCV BG END",
                "No data from " +
                    dateUtil.dateAndTimeAndSecondsString(lastLoaded)
            )
        }

        val newestBgTimestamp =
            response.values
                .mapNotNull { it.date }
                .maxOrNull()

        if (newestBgTimestamp != null) {
            val nextCursor =
                max(
                    lastLoaded,
                    newestBgTimestamp + 1
                )

            aapsLogger.info(
                LTag.NSCLIENT,
                "ANDY-NS LEGACY advancing BG cursor " +
                    "from=$lastLoaded " +
                    "newestBg=$newestBgTimestamp " +
                    "next=$nextCursor"
            )

            nsClientV3Plugin
                .lastLoadedSrvModified
                .collections
                .entries = nextCursor

            nsClientV3Plugin.storeLastLoadedSrvModified()
        }

        nsClientV3Plugin.scheduleIrregularExecution(
            lastBgTimestamp = newestBgTimestamp
        )
    }

    /*
1.) In LoadBgRunner we add this:

val nsAndroidClient =
    nsClientV3Plugin.nsAndroidClient
        ?: return WorkOutcome.Failure("AndroidClient is null")

try {

    // LOCAL-BEGIN: xDrip/API-v1 BG compatibility
    if (legacyBgRetriever.enabled) {
        legacyBgRetriever.run(nsAndroidClient)

        storeDataForDb.storeGlucoseValuesToDb()
        storeDataForDb.storeCalibrationEntriesToDb()

        nsClientV3Plugin.lastOperationError = null

        return WorkOutcome.Success
    }
    // LOCAL-END: xDrip/API-v1 BG compatibility

    var continueLoading = true

    while (continueLoading) {


2.) In NSClientV3

a) change scheduleIrregularExecution to this (added 1 parameter)
fun scheduleIrregularExecution(
    refreshToken: Boolean = false,
    lastBgTimestamp: Long? = null
)

And change following code


val baseTimestamp =
    lastBgTimestamp
        ?: lastLoadedSrvModified.collections.entries

var origin = "5_MIN_AFTER_BG"
var toTime =
    baseTimestamp +
        T.mins(5).plus(T.secs(30)).msecs()

    */
}