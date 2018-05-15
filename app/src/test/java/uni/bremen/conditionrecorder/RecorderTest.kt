package uni.bremen.conditionrecorder

import org.junit.Assert.assertEquals
import org.junit.Test

class RecorderTest {

    @Test
    fun lowest() {
        assertEquals(Recorder.State.lowest(listOf(Recorder.State.CONNECTED, Recorder.State.DISCONNECTED)), Recorder.State.DISCONNECTED)
    }

}