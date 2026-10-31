package app.aaps.pump.tandem.common.comm.ui

/** Answer of the pump to a resume request. Make a new object for each answer. */
class ResumePumpingResult(val isOk: Boolean, val status: Int)

/** Why a resume did not work. */
enum class ResumeFailure {
    NOT_SENT,
    PUMP_REFUSED,
    NO_ANSWER
}

/** What we know about a resume request while we wait. */
enum class ResumeProgress {
    WAITING,
    DONE,
    REFUSED
}

/** Running state wins. Then a "no" from the pump. Else we keep waiting. */
fun evaluateResume(result: ResumePumpingResult?, isRunning: Boolean): ResumeProgress = when {
    isRunning                         -> ResumeProgress.DONE
    result != null && !result.isOk    -> ResumeProgress.REFUSED
    else                              -> ResumeProgress.WAITING
}
