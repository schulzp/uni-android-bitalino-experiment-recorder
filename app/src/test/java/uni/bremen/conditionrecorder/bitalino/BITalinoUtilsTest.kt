package uni.bremen.conditionrecorder.bitalino

import org.hamcrest.CoreMatchers.*;
import org.junit.Assert.*
import org.junit.Test

class BITalinoUtilsTest {

    @Test
    fun calculateBatteryPercentage() {
        assertThat(BITalinoUtils.calculateBatteryPercentage(BITalinoUtils.MIN - 1), equalTo(0.0))
        assertThat(BITalinoUtils.calculateBatteryPercentage(BITalinoUtils.MIN), equalTo(0.0))
        assertEquals(BITalinoUtils.calculateBatteryPercentage((BITalinoUtils.MIN + BITalinoUtils.MAX) / 2.0), 0.5, 0.01)
        assertThat(BITalinoUtils.calculateBatteryPercentage(BITalinoUtils.MAX), equalTo(1.0))
        assertThat(BITalinoUtils.calculateBatteryPercentage(BITalinoUtils.MAX + 1), equalTo(1.0))
    }

}