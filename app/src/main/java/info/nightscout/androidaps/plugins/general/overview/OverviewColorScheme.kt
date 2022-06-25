package info.nightscout.androidaps.plugins.general.overview

import androidx.annotation.ColorRes
import info.nightscout.androidaps.R

/**
 * Created by andy on 3/2/19.
 */
enum class OverviewColorScheme(
    @ColorRes var newBackground: Int,
    @ColorRes var newTextColor: Int,
    @ColorRes var oldBackground: Int,
    @ColorRes var oldTextColor: Int
) {

    TempTargetNotSet(R.color.ribbonDefault,
                     R.color.ribbonTextDefault,
                     R.color.tempTargetDisabledBackground,
                     R.color.white),
    TempTargetSet(
        R.color.ribbonWarning,
        R.color.ribbonTextWarning,
        R.color.tempTargetBackground,
        R.color.black
    ),

    //    APS_Loop_Enabled(R.color.ribbonDefault, R.color.ribbonTextDefault, R.color.loopenabled, R.color.black),
    //    APS_SuperBolus(R.color.ribbonWarning, R.color.ribbonTextWarning, R.color.looppumpsuspended, R.color.white),
    //    APS_Loop_Disconnected(R.color.ribbonCritical, R.color.ribbonTextCritical, R.color.looppumpsuspended, R.color.white),
    //    APS_Loop_Suspended(R.color.ribbonWarning, R.color.ribbonTextWarning, R.color.looppumpsuspended, R.color.white),
    //    APS_Pump_Suspended(R.color.ribbonWarning, R.color.ribbonTextWarning, R.color.loopenabled, R.color.white),
    //    APS_Loop_Disabled(R.color.ribbonCritical, R.color.ribbonTextCritical, R.color.loopdisabled, R.color.white),
    ProfileNormal(R.color.ribbonDefault, R.color.ribbonTextDefault, R.color.loopOpened, R.color.white),
    ProfileChanged(R.color.ribbonWarning, R.color.ribbonTextWarning, R.color.iobPredAS, R.color.white);

    @ColorRes fun getBackground(isNew: Boolean): Int {
        return if (isNew) newBackground else oldBackground
    }

    @ColorRes fun getTextColor(isNew: Boolean): Int {
        return if (isNew) newTextColor else oldTextColor
    }
}