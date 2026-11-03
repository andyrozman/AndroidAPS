package app.aaps.pump.tandem.common.database.data.dto

import com.jwoglom.pumpx2.pump.messages.response.qualifyingEvent.QualifyingEvent
import java.time.LocalDateTime

class TandemQualifyingEventDto(
    val dateTime : LocalDateTime,
    val name : QualifyingEvent,
    val description: String = ""
)