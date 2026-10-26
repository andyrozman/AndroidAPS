package app.aaps.pump.tandem.common.data.defs

import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.keys.interfaces.TextRef
import app.aaps.pump.tandem.R

enum class SiteReminderPreset(var resourceId: Int, var hours: Int) {

    IN_1_5_DAYS(R.string.reminder_in_1_5_days, 36),
    IN_2_DAYS(R.string.reminder_in_2_days, 48),
    IN_2_5_DAYS(R.string.reminder_in_2_5_days, 60),
    IN_3_DAYS(R.string.reminder_in_3_days, 72),
    IN_4_DAYS(R.string.reminder_in_4_days, 72),
    IN_5_DAYS(R.string.reminder_in_5_days, 72),
    IN_6_DAYS(R.string.reminder_in_6_days, 72),
    IN_7_DAYS(R.string.reminder_in_7_days, 72),
    IN_8_DAYS(R.string.reminder_in_8_days, 72)

    ;

    fun getDisplayValue(): String {
        return if (translated==null)
            name
        else
            translated!!

    }


    var translated: String? = null
        private set

    override fun toString(): String {
        if (translated!=null) {
            return translated!!
        } else {
            return name
        }
    }

    companion object {

        @JvmStatic private var translatedList: MutableList<SiteReminderPreset>? = null

        fun doTranslation(rh: ResourceHelper) {
            if (translatedList != null) return
            translatedList = ArrayList()
            for (reminderPreset in SiteReminderPreset.entries) {
                reminderPreset.translated = rh.gs(TextRef.AndroidRes(reminderPreset.resourceId))
                (translatedList as ArrayList<SiteReminderPreset>).add(reminderPreset)
            }
        }


        fun getTranslatedList(rh: ResourceHelper): List<SiteReminderPreset> {
            if (translatedList == null) doTranslation(rh)

            return translatedList!!
        }
    }

}