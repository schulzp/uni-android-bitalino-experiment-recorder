package uni.bremen.conditionrecorder

abstract class Recorder {

    var state:State = State.DISCONNECTED
    protected set(value) { field = value }

    var batteryLevel = Recorder.BatteryLevel.UNKNOWN
    protected set(value) { field = value }

    enum class State {

        DISCONNECTED, CONNECTING, CONNECTED, RECORDING_STARTED, RECORDING, RECORDING_STOPPED;

        companion object {

            fun lowest(states:Iterable<State>): State {
                return states.map(State::ordinal).sorted().first().let { State.values()[it] }
            }

        }

    }

    enum class BatteryLevel {

        CRITICAL, LOW, GOOD, UNKNOWN

    }

    abstract fun connect()

    abstract fun disconnect()

    abstract fun start()

    abstract fun stop()

}