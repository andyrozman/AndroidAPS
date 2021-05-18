package info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.command

import dagger.android.HasAndroidInjector
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.ble.defs.YpsoGattCharacteristic
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.EventDto
import info.nightscout.androidaps.plugins.pump.ypsopump.comm.data.HistoryEntryType

class GetEvents(hasAndroidInjector: HasAndroidInjector?) : GetDataListAbstract<EventDto>(hasAndroidInjector!!) {

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


}