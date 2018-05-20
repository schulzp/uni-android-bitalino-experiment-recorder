package uni.bremen.conditionrecorder.wahoo

import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.util.Log
import com.wahoofitness.connector.HardwareConnector
import com.wahoofitness.connector.HardwareConnectorEnums
import com.wahoofitness.connector.HardwareConnectorTypes
import com.wahoofitness.connector.capabilities.Battery
import com.wahoofitness.connector.capabilities.Capability
import com.wahoofitness.connector.capabilities.Capability.CapabilityType
import com.wahoofitness.connector.capabilities.Heartrate
import com.wahoofitness.connector.conn.connections.SensorConnection
import com.wahoofitness.connector.conn.connections.params.BTLEConnectionParams
import io.reactivex.subjects.PublishSubject
import uni.bremen.conditionrecorder.Recorder
import uni.bremen.conditionrecorder.RecorderBus
import uni.bremen.conditionrecorder.service.RecorderService



class WahooRecorder(device:BluetoothDevice, service: RecorderService) : Recorder(device, service) {

    var data: PublishSubject<Heartrate.Data> = createObservable()
        private set(value) {
            field = value
        }

    private val callback = object : DefaultCallback() { }

    private var connector = HardwareConnector(service, callback)

    private var connection: SensorConnection? = null

    private val connectionListener = object: SensorConnection.Listener {

        override fun onSensorConnectionStateChanged(sensorConnection: SensorConnection, state: HardwareConnectorEnums.SensorConnectionState) {
            updateState(when(state) {
                HardwareConnectorEnums.SensorConnectionState.CONNECTING -> Recorder.State.CONNECTING
                HardwareConnectorEnums.SensorConnectionState.CONNECTED ->
                    if (state == Recorder.State.CONNECTED)
                        Recorder.State.CONNECTED
                    else
                        Recorder.State.CONNECTING
                else -> Recorder.State.DISCONNECTED
            })
        }

        override fun onSensorConnectionError(sensorConnection: SensorConnection, connectionError: HardwareConnectorEnums.SensorConnectionError) {
            Log.e(TAG, "sensor connection error: $connectionError")
        }

        override fun onNewCapabilityDetected(sensorConnection: SensorConnection, capabilityType: Capability.CapabilityType) {
            Log.i(TAG, "new capability detected: $capabilityType")

            when (capabilityType) {
                CapabilityType.Heartrate -> {
                    updateState(Recorder.State.CONNECTED)
                    val heartrate = sensorConnection.getCurrentCapability(capabilityType) as Heartrate
                    heartrate.addListener(heartrateListener)
                }
                CapabilityType.Battery -> {
                    val battery = sensorConnection.getCurrentCapability(capabilityType) as Battery
                    battery.addListener(batteryListener)
                }
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

    private val batteryListener = object : Battery.Listener {

        override fun onBatteryData(data: Battery.Data?) {
            Log.d(TAG, "battery level: ${data?.batteryLevel}")
            this@WahooRecorder.batteryLevel = Recorder.BatteryLevel.valueOf(data?.batteryLevel?.name ?: BatteryLevel.UNKNOWN.name)
            updateState()
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

    private fun createObservable(): PublishSubject<Heartrate.Data> = PublishSubject.create()

    companion object {

        const val TAG = "WahooRecorder"

        const val DEFAULT_ADDRESS = "E0:BE:88:97:8E:58"

    }

}