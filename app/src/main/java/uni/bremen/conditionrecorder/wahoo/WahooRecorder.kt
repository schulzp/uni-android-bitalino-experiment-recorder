package uni.bremen.conditionrecorder.wahoo

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.wahoofitness.connector.HardwareConnector
import com.wahoofitness.connector.HardwareConnectorEnums
import com.wahoofitness.connector.HardwareConnectorTypes
import com.wahoofitness.connector.capabilities.Capability
import com.wahoofitness.connector.capabilities.Capability.CapabilityType
import com.wahoofitness.connector.capabilities.Heartrate
import com.wahoofitness.connector.conn.connections.SensorConnection
import com.wahoofitness.connector.conn.connections.params.BTLEConnectionParams
import io.reactivex.subjects.PublishSubject
import uni.bremen.conditionrecorder.Recorder
import uni.bremen.conditionrecorder.RecorderBus
import uni.bremen.conditionrecorder.RecorderService


class WahooRecorder(private val device:BluetoothDevice, private val service: RecorderService) : Recorder {

    var data: PublishSubject<Heartrate.Data> = createObservable()
        private set(value) {
            field = value
        }

    private val callback = object : DefaultCallback() { }

    private var connector = HardwareConnector(service, callback)

    private var state: Recorder.State = Recorder.State.DISCONNECTED

    private var connection: SensorConnection? = null

    private val connectionListener = object: SensorConnection.Listener {

        override fun onSensorConnectionStateChanged(sensorConnection: SensorConnection, state: HardwareConnectorEnums.SensorConnectionState) {
            updateState(when(state) {
                HardwareConnectorEnums.SensorConnectionState.CONNECTING,
                HardwareConnectorEnums.SensorConnectionState.CONNECTED -> Recorder.State.CONNECTING
                else -> Recorder.State.DISCONNECTED
            })
        }

        override fun onSensorConnectionError(sensorConnection: SensorConnection, connectionError: HardwareConnectorEnums.SensorConnectionError) {
            Log.e(TAG, "sensor connection error: $connectionError")
        }

        override fun onNewCapabilityDetected(sensorConnection: SensorConnection, capabilityType: Capability.CapabilityType) {
            Log.i(TAG, "new capability detected: $capabilityType")

            if (capabilityType === CapabilityType.Heartrate) {
                updateState(Recorder.State.CONNECTED)

                val heartrate = sensorConnection.getCurrentCapability(CapabilityType.Heartrate) as Heartrate
                heartrate.addListener(heartrateListener)
            }
        }

    }

    private val heartrateListener = object : Heartrate.Listener {

        override fun onHeartrateData(data: Heartrate.Data) {
            if (this@WahooRecorder.state == Recorder.State.RECORDING) {
                this@WahooRecorder.data.onNext(data)
            }
        }

        override fun onHeartrateDataReset() {
            Log.d(TAG, "heart rate data reset")
        }

    }

    override fun connect() {
        val connectionParams = BTLEConnectionParams(device, HardwareConnectorTypes.SensorType.HEARTRATE)
        connection = connector.requestSensorConnection(connectionParams, connectionListener)
    }

    override fun disconnect() {
        connection?.disconnect()
        connector.shutdown()
    }

    override fun getState(): Recorder.State {
        return state
    }

    override fun start() {
        Log.d(TAG, "started")

        updateState(Recorder.State.RECORDING)
    }

    override fun stop() {
        Log.d(TAG, "stopped")

        if (state == Recorder.State.RECORDING) {
            updateState(Recorder.State.CONNECTED)
        } else {
            Log.w(TAG, "no longer connected")
        }

        data.onComplete()
        data = createObservable()
    }

    private fun updateState(state: Recorder.State) {
        this.state = state

        service.bus.events.onNext(RecorderBus.RecorderStateChanged(device, state))
    }

    private fun createObservable(): PublishSubject<Heartrate.Data> = PublishSubject.create()

    companion object {

        const val TAG = "WahooRecorder"

        const val DEFAULT_ADDRESS = "E0:BE:88:97:8E:58"

    }

}