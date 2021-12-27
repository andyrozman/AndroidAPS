package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.data.EventDto
import info.nightscout.androidaps.plugins.pump.ypsopump.data.HistoryEntryType

class GetEvents(hasAndroidInjector: HasAndroidInjector?,
                targetDate: Long? = null,
                eventSequenceNumber: Int? = null,
                includeEventSequence: Boolean = false) : GetDataListAbstract<EventDto>(hasAndroidInjector!!, targetDate, eventSequenceNumber, includeEventSequence) {

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
        var inRange: Boolean = false

        if (targetDate != null) {
            inRange = event.dateTime.toATechDate() >= targetDate!!
        } else if (eventSequenceNumber != null) {
            if (includeEventSequence)
                inRange = event.eventSequenceNumber >= eventSequenceNumber!!
            else
                inRange = event.eventSequenceNumber > eventSequenceNumber!!
        } else
            return true

        if (!inRange) {
            cancelProcessing = true
        }

        return inRange
    }

}