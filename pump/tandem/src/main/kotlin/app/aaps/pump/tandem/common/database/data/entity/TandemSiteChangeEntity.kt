package app.aaps.pump.tandem.common.database.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "site_change",
        indices = [
            Index(name = "idx_site_change_dateTime", value = ["dateTime"])
        ]
)
data class TandemSiteChangeEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var pumpSerial: Long,
    var dateTime: Long,   // EpochInMillis
    var siteLocation: String? = null,      // siteLocation = TE.Location
    var siteArrow: String? = null,         // siteArrow = TE.Arrow
    var storedInEvent: Boolean = false
)
