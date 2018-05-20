package uni.bremen.conditionrecorder.bitalino

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Parcelable
import android.util.Log
import info.plux.pluxapi.Communication
import info.plux.pluxapi.Constants
import info.plux.pluxapi.bitalino.*
import info.plux.pluxapi.bitalino.bth.OnBITalinoDataAvailable
import io.reactivex.subjects.PublishSubject
import uni.bremen.conditionrecorder.Recorder
import uni.bremen.conditionrecorder.RecorderBus
import uni.bremen.conditionrecorder.service.RecorderService

class BITalinoRecorder(device: BluetoothDevice, service: RecorderService) : Recorder(device, service) {

    var frames: PublishSubject<BITalinoFrame> = createObservable()
        private set(value) {
            field = value
        }

    private var bitalino: BITalinoCommunication? = null

    private val dataReceiver = OnBITalinoDataAvailable { frame -> frames.onNext(frame) }

    private var registeredReceiver = false

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (Constants.ACTION_STATE_CHANGED == action) {
                val identifier = intent.getStringExtra(Constants.IDENTIFIER)
                val state = info.plux.pluxapi.Constants.States.getStates(intent.getIntExtra(Constants.EXTRA_STATE_CHANGED, 0))

                Log.i(TAG, identifier + " -> " + state.name)

                updateState(when (state) {
                    Constants.States.NO_CONNECTION,
                    Constants.States.DISCONNECTED,
                    Constants.States.LISTEN,
                    Constants.States.ENDED -> Recorder.State.DISCONNECTED
                    Constants.States.CONNECTING -> Recorder.State.CONNECTING
                    Constants.States.CONNECTED -> Recorder.State.CONNECTED
                    Constants.States.ACQUISITION_TRYING -> Recorder.State.RECORDING_STARTED
                    Constants.States.ACQUISITION_OK -> Recorder.State.RECORDING
                    Constants.States.ACQUISITION_STOPPING -> Recorder.State.RECORDING_STOPPED
                })
            } else if (Constants.ACTION_DATA_AVAILABLE == action) {
                if (intent.hasExtra(Constants.EXTRA_DATA)) {
                    val parcelable = intent.getParcelableExtra<Parcelable>(Constants.EXTRA_DATA)
                    if (parcelable.javaClass == BITalinoFrame::class.java) {
                        val frame = parcelable as BITalinoFrame
                        handle(frame)
                    }
                }
            } else if (Constants.ACTION_COMMAND_REPLY == action) {
                val identifier = intent.getStringExtra(Constants.IDENTIFIER)

                if (intent.hasExtra(Constants.EXTRA_COMMAND_REPLY) && intent.getParcelableExtra<Parcelable>(Constants.EXTRA_COMMAND_REPLY) != null) {
                    val parcelable = intent.getParcelableExtra<Parcelable>(Constants.EXTRA_COMMAND_REPLY)
                    if (parcelable is BITalinoState) {
                        handle(parcelable)
                    } else if (parcelable is BITalinoDescription) {
                        handle(parcelable)
                    }
                }
            }
        }

        private fun handle(parcelable: BITalinoDescription) {
            var isBITalino2 = (parcelable as BITalinoDescription).isBITalino2
            Log.d(RecorderService.TAG, "isBITalino2: " + isBITalino2 + "; FwVersion: " + parcelable.fwVersion.toString())

            if (state == State.CONNECTED) {
                try {
                    bitalino?.state()
                } catch (e: Exception) {
                    Log.w(TAG, "failed to read state")
                }
            }
        }

        private fun handle(frame: BITalinoFrame) {
            Log.d(RecorderService.TAG, "frame available: $frame")
        }

        private fun handle(state: BITalinoState) {
            if (state.battery < state.batThreshold) {
                batteryLevel = BatteryLevel.CRITICAL
            } else if (state.battery < 600) {
                batteryLevel = BatteryLevel.LOW
            } else {
                batteryLevel = BatteryLevel.GOOD
            }

            updateState()
        }
    }

    override fun connect() {
        var communication = Communication.getById(device.type)

        Log.d(TAG, "communication: ${communication.name}")

        if (communication == Communication.DUAL) {
            communication = Communication.BLE
        }

        bitalino = BITalinoCommunicationFactory().getCommunication(communication, service, dataReceiver)

        service.registerReceiver(updateReceiver, makeUpdateIntentFilter())
        registeredReceiver = true

        bitalino?.connect(device.address)
    }

    override fun start() {
        Log.d(TAG, "started")
        service.bus.events.onNext(RecorderBus.BitalinoRecordingStarted())

        bitalino?.start(intArrayOf(0, 1, 2, 3, 4, 5), 100)
    }

    override fun stop() {
        Log.d(TAG, "stopped")
        service.bus.events.onNext(RecorderBus.BitalinoRecordingStopped())

        frames.onComplete()
        frames = createObservable()

        try {
            bitalino?.stop()
        } catch (e: BITalinoException) {
            Log.w(TAG, "failed to stop communication: ${e.message}")
        }
    }

    override fun disconnect() {
        if (registeredReceiver) {
            service.unregisterReceiver(updateReceiver)
            registeredReceiver = false
        }

        try {
            bitalino?.disconnect()
        } catch (e: BITalinoException) {
            Log.w(TAG, "failed to disconnect: ${e.message}")
        }

        try {
            bitalino?.closeReceivers()
        } catch (e: BITalinoException) {
            Log.w(TAG, "failed to close receivers: ${e.message}")
        }
    }

    private fun makeUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.ACTION_STATE_CHANGED)
        intentFilter.addAction(Constants.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(Constants.ACTION_EVENT_AVAILABLE)
        intentFilter.addAction(Constants.ACTION_DEVICE_READY)
        intentFilter.addAction(Constants.ACTION_COMMAND_REPLY)
        return intentFilter
    }


    private fun createObservable(): PublishSubject<BITalinoFrame> = PublishSubject.create()

    companion object {

        const val TAG = "BITalinoRecorder"

    }

}