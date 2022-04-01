package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.data.EventDto
import info.nightscout.androidaps.plugins.pump.ypsopump.data.HistoryEntryType

class GetAlarms(hasAndroidInjector: HasAndroidInjector?, eventSequenceNumber: Int?) : GetDataListAbstract<EventDto>(hasAndroidInjector!!, null, eventSequenceNumber, false) {

    override fun getIndexUuid(): YpsoGattCharacteristic {
        return YpsoGattCharacteristic.ALARM_ENTRY_INDEX
    }

    override fun getCountUuid(): YpsoGattCharacteristic {
        return YpsoGattCharacteristic.ALARM_ENTRY_COUNT
    }

    override fun getValueUuid(): YpsoGattCharacteristic {
        return YpsoGattCharacteristic.ALARM_ENTRY_VALUE
    }

    override fun decodeEntry(data: ByteArray): EventDto? {
        return ypsoPumpDataConverter.decodeEvent(data, HistoryEntryType.Alarm)
    }

    override fun getEntryType(): String {
        return "Alarm"
    }

    override fun isEntryInRange(event: EventDto): Boolean {
        return if (targetDate != null) {
            event.dateTime.toATechDate() >= targetDate!!
        } else if (eventSequenceNumber != null) {
            event.eventSequenceNumber > eventSequenceNumber!!
        } else
            true
    }

}