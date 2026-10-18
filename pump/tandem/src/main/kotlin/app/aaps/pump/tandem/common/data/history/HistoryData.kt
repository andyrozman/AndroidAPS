package app.aaps.pump.tandem.common.data.history

import com.jwoglom.pumpx2.pump.messages.response.historyLog.HistoryLog

data class HistorySummaryDto(
    var serialNumber: Int,  // serial
    var startRecord: Long, // first history record on pump
    var modifiedStartRecord: Long, // first history record in database (we keep only 1 month of history, so we would never retrieve anything over 1.5 month
    var lastRecord: Long, // current last record in database
    var missedRanges: ArrayList<HistoryRange> = arrayListOf(),
    var activeProcessing: MutableCollection<HistoryRequestInfo> = arrayListOf()
)


data class HistoryRange(var start: Long, var end: Long) {

    fun getItemAmount(): Int {
        return ((end - start) +1).toInt()
    }

}


data class HistoryRequestInfo(
    val startSequence: Long,
    val endSequence: Long,
    val numberOfLogs: Int = (endSequence - startSequence +1).toInt(),
    val downloadStatus: HistoryDownloadStatus = HistoryDownloadStatus.Initializing,
    val historyLogMap: MutableMap<Long, HistoryLog> = mutableMapOf()
) {
    fun isInRange(sequence: Long): Boolean {
        return sequence in startSequence..endSequence
    }

    // keeps only records of the requested range, returns how many were ignored
    fun addLogs(logs: Collection<HistoryLog>): Int {
        var ignored = 0
        for (log in logs) {
            if (isInRange(log.sequenceNum)) {
                historyLogMap[log.sequenceNum] = log
            } else {
                ignored++
            }
        }
        return ignored
    }

    fun chunkComplete(): Boolean {
        return chunkHasAllItems() ||
            ( historyLogMap.containsKey(startSequence) && historyLogMap.containsKey(endSequence) )
    }

    // every sequence number of the range must be here, a count alone is not enough
    fun chunkHasAllItems(): Boolean {
        return historyLogMap.keys.count { isInRange(it) }.toLong() == (endSequence - startSequence + 1)
    }

    override fun toString(): String {
        return "HistoryRequestInfo [startSequence=$startSequence, endSequence=$endSequence, numberOfLogs=$numberOfLogs, downloadStatus=${downloadStatus.name}]";
    }
}

enum class HistoryDownloadStatus {
    Initializing,
    Waiting,
    Downloading,
    Complete
}