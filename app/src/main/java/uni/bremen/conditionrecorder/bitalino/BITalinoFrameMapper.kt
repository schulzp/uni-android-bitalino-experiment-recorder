package uni.bremen.conditionrecorder.bitalino

import info.plux.pluxapi.bitalino.BITalinoFrame

class BITalinoFrameMapper {

    var phase:Int = 0

    var value:Int = 0

    fun map(frame:BITalinoFrame):Array<*> = arrayOf(
            frame.analogArray[0],
            frame.analogArray[1],
            frame.analogArray[2],
            frame.analogArray[3],
            frame.analogArray[4],
            frame.analogArray[5],
            value,
            phase)

    fun reset() {
        phase = DEFAULT_PHASE
        value = DEFAULT_VALUE
    }

    companion object {

        const val DEFAULT_PHASE = 0

        const val DEFAULT_VALUE = 0

    }

}