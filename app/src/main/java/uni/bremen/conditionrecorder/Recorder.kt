package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import uni.bremen.conditionrecorder.service.RecorderService

abstract class Recorder(protected val device:BluetoothDevice, protected val service:RecorderService) {

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

    protected fun updateState(state: State = this.state) {
        this.state = state
        service.bus.events.onNext(RecorderBus.RecorderStateChanged(device, state, batteryLevel))
    }

}