package info.nightscout.interfaces.pump

import androidx.appcompat.app.AppCompatActivity

interface BlePreCheck {

    fun prerequisitesCheck(activity: AppCompatActivity): Boolean

    fun prerequisitesCheck(activity: AppCompatActivity, additionalPrerequisties: List<String>?): Boolean

}