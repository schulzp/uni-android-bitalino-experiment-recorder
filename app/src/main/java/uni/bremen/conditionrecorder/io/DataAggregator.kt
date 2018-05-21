package uni.bremen.conditionrecorder.io

import com.google.common.util.concurrent.AtomicDouble
import info.plux.pluxapi.bitalino.BITalinoFrame
import java.util.concurrent.atomic.AtomicInteger

class DataAggregator {

    var phase = AtomicInteger(DEFAULT_PHASE)

    val value = AtomicDouble(DEFAULT_VALUE)

    fun map(frame:BITalinoFrame):Array<*> = arrayOf(
            frame.analogArray[0],
            frame.analogArray[1],
            frame.analogArray[2],
            frame.analogArray[3],
            frame.analogArray[4],
            frame.analogArray[5],
            value.getAndSet(DEFAULT_VALUE),
            phase.get())

    companion object {

        const val DEFAULT_PHASE = 0

        const val DEFAULT_VALUE = -1.0

    }

}