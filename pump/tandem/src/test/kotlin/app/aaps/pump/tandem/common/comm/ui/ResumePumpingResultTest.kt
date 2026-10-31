package app.aaps.pump.tandem.common.comm.ui

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ResumePumpingResultTest {

    @Test
    fun `no answer and not running keeps waiting`() {
        assertThat(evaluateResume(null, false)).isEqualTo(ResumeProgress.WAITING)
    }

    @Test
    fun `ok answer but still suspended keeps waiting`() {
        assertThat(evaluateResume(ResumePumpingResult(true, 0), false)).isEqualTo(ResumeProgress.WAITING)
    }

    @Test
    fun `refused answer is reported`() {
        assertThat(evaluateResume(ResumePumpingResult(false, 1), false)).isEqualTo(ResumeProgress.REFUSED)
    }

    @Test
    fun `running state means done`() {
        assertThat(evaluateResume(null, true)).isEqualTo(ResumeProgress.DONE)
        assertThat(evaluateResume(ResumePumpingResult(true, 0), true)).isEqualTo(ResumeProgress.DONE)
    }
}
