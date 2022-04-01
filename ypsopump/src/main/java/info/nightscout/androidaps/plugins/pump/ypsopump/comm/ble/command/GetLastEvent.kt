package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.data.DetailedBolusInfo
import info.nightscout.androidaps.plugins.pump.common.defs.TempBasalPair
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.data.EventDto
import info.nightscout.androidaps.plugins.pump.ypsopump.data.HistoryEntryType
import info.nightscout.androidaps.plugins.pump.ypsopump.defs.YpsoPumpEventType
import info.nightscout.shared.logging.LTag
import java.util.*

class GetLastEvent(hasAndroidInjector: HasAndroidInjector?,
                   var bolusInfo: DetailedBolusInfo? = null,
                   var tempBasalInfo: TempBasalPair? = null) : GetDataListAbstract<EventDto>(hasAndroidInjector!!, null, 0, false) {

    override fun getIndexUuid(): YpsoGattCharacteristic {
        return YpsoGattCharacteristic.EVENT_ENTRY_INDEX
    }

    override fun getCountUuid(): YpsoGattCharacteristic {
        return YpsoGattCharacteristic.EVENT_ENTRY_COUNT
    }

    override fun getValueUuid(): YpsoGattCharacteristic {
        return YpsoGattCharacteristic.EVENT_ENTRY_VALUE
    }

    override fun decodeEntry(data: ByteArray): EventDto? {
        return ypsoPumpDataConverter.decodeEvent(data, HistoryEntryType.Event)
    }

    override fun getEntryType(): String {
        return "Event"
    }

    override fun isEntryInRange(event: EventDto): Boolean {

        val type = event.entryType
        val found: Boolean

        if (bolusInfo != null) {
            found = (type == YpsoPumpEventType.BOLUS_NORMAL ||
                type == YpsoPumpEventType.BOLUS_NORMAL_RUNNING ||
                type == YpsoPumpEventType.BOLUS_NORMAL_ABORT)
        } else {
            found = (type == YpsoPumpEventType.TEMPORARY_BASAL ||
                type == YpsoPumpEventType.TEMPORARY_BASAL_RUNNING ||
                type == YpsoPumpEventType.TEMPORARY_BASAL_ABORT)
        }

        aapsLogger.debug(LTag.PUMPCOMM, String.format(Locale.ROOT, "Looking for bolus entry: %b, found: %b", bolusInfo != null,
            found))

        if (found) {
            cancelProcessing = true
        }

        // it should be the first record, so we will probably come here only once, but for TBRs it could actually
        // be non-first record

        return found
    }

}