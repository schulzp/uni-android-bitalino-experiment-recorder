package uni.bremen.conditionrecorder.bitalino

import info.plux.pluxapi.bitalino.BITalinoState

class BITalinoUtils {

    companion object {

        const val N = 10.0

        const val VCC = 3.3

        const val MIN = 3.4

        const val MAX = 3.8

        /**
         * ABAT [~3.4 V : ~3.8 V]
         *
         * ABATV = 2*(ABATB * Vcc / (2^n - 1))
         *
         * Where:
         * ABATV – Battery level in Volts (V)
         * ABATB – ADC value for ABAT obtained from BITalino
         * Vcc – Operating Voltage (V)
         * n – number of bits (bit)
         *
         * Values:
         * Vcc = 3.3 (V)
         * n = See Number of Bits section
         * Number of Bits: The number of bits for each channel depends on its position in the "acquire channel" request (ex. BITalino().start([0,2,3,5])):
         * - If it is one of the four initial channels, its resolution will be: 10 bit.
         * - If it is one of the two last channels, its resolution will be: 6 bit.
         *
         * http://forum.bitalino.com/viewtopic.php?t=43#p528
         */
        fun calculateBatteryVoltLevel(state:BITalinoState)
                : Double = 2 * (state.battery * VCC / (Math.pow(2.0, N) - 1))

        fun calculateBatteryPercentage(voltLevel:Double)
                : Double = Math.max(0.0, Math.min(1.0, (voltLevel - MIN) / (MAX - MIN)))

    }

}