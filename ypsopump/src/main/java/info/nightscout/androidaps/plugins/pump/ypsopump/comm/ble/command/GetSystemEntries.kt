package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.data.EventDto
import info.nightscout.androidaps.plugins.pump.ypsopump.data.HistoryEntryType

class GetSystemEntries(hasAndroidInjector: HasAndroidInjector?, targetDate: Long?, eventSequenceNumber: Int?) : GetDataListAbstract<EventDto>(hasAndroidInjector!!, targetDate, eventSequenceNumber) {

    override fun getIndexUuid(): YpsoGattCharacteristic {
        return YpsoGattCharacteristic.SYSTEM_ENTRY_INDEX
    }

    override fun getCountUuid(): YpsoGattCharacteristic {
        return YpsoGattCharacteristic.SYSTEM_ENTRY_COUNT
    }

    override fun getValueUuid(): YpsoGattCharacteristic {
        return YpsoGattCharacteristic.SYSTEM_ENTRY_VALUE
    }

    override fun decodeEntry(data: ByteArray): EventDto? {
        return ypsoPumpDataConverter.decodeEvent(data, HistoryEntryType.SystemEntry)
    }

    override fun getEntryType(): String {
        return "System Entry"
    }

    override fun isEntryInRange(decodedObject: EventDto): Boolean {
        return if (targetDate != null) {
            decodedObject.dateTime.toATechDate() >= targetDate!!
        } else if (eventSequenceNumber != null) {
            decodedObject.eventSequenceNumber > eventSequenceNumber!!
        } else
            true
    }

}