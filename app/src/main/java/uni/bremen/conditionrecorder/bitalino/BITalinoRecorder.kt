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
import uni.bremen.conditionrecorder.RecorderBus
import uni.bremen.conditionrecorder.RecorderDeviceFragment
import uni.bremen.conditionrecorder.RecorderService

class BITalinoRecorder(val service: RecorderService, val bluetoothDevice: BluetoothDevice) {

    var writer: BITalinoFrameWriter? = null

    private var bitalino: BITalinoCommunication? = null

    private var registerReceiverIntent: Intent? = null

    private val dataReceiver: OnBITalinoDataAvailable = OnBITalinoDataAvailable { writer?.write(it) }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (Constants.ACTION_STATE_CHANGED == action) {
                val identifier = intent.getStringExtra(Constants.IDENTIFIER)
                val state = info.plux.pluxapi.Constants.States.getStates(intent.getIntExtra(Constants.EXTRA_STATE_CHANGED, 0))

                Log.i(RecorderService.TAG, identifier + " -> " + state!!.name)

                when (state) {
                    Constants.States.NO_CONNECTION -> {
                    }
                    Constants.States.LISTEN -> {
                    }
                    Constants.States.CONNECTING -> {
                    }
                    Constants.States.CONNECTED -> {
                    }
                    Constants.States.ACQUISITION_TRYING -> {
                    }
                    Constants.States.ACQUISITION_OK -> {
                    }
                    Constants.States.ACQUISITION_STOPPING -> {
                    }
                    Constants.States.DISCONNECTED -> {
                    }
                    Constants.States.ENDED -> {
                    }
                }

                service.bus.post(RecorderBus.DeviceStateChanged(bluetoothDevice, state))

            } else if (Constants.ACTION_DATA_AVAILABLE == action) {
                if (intent.hasExtra(Constants.EXTRA_DATA)) {
                    val parcelable = intent.getParcelableExtra<Parcelable>(Constants.EXTRA_DATA)
                    if (parcelable.javaClass == BITalinoFrame::class.java) {
                        val frame = parcelable as BITalinoFrame
                        handleFrame(frame)
                    }
                }
            } else if (Constants.ACTION_COMMAND_REPLY == action) {
                val identifier = intent.getStringExtra(Constants.IDENTIFIER)

                if (intent.hasExtra(Constants.EXTRA_COMMAND_REPLY) && intent.getParcelableExtra<Parcelable>(Constants.EXTRA_COMMAND_REPLY) != null) {
                    val parcelable = intent.getParcelableExtra<Parcelable>(Constants.EXTRA_COMMAND_REPLY)
                    if (parcelable.javaClass == BITalinoState::class.java) {
                        Log.d(RecorderDeviceFragment.TAG, (parcelable as BITalinoState).toString())
                    } else if (parcelable.javaClass == BITalinoDescription::class.java) {
                        var isBITalino2 = (parcelable as BITalinoDescription).isBITalino2
                        Log.d(RecorderService.TAG, "isBITalino2: " + isBITalino2 + "; FwVersion: " + parcelable.fwVersion.toString())
                    }
                }
            }
        }

        fun handleFrame(frame: BITalinoFrame) {
            Log.d(RecorderService.TAG, "frame: $frame")
        }
    }

    fun connect() {
        var communication = Communication.getById(bluetoothDevice.type)

        Log.d(RecorderService.TAG, "communication: ${communication.name}")

        if (communication == Communication.DUAL) {
            communication = Communication.BLE
        }

        bitalino = BITalinoCommunicationFactory().getCommunication(communication, service, dataReceiver)

        registerReceiverIntent = service.registerReceiver(updateReceiver, makeUpdateIntentFilter())

        bitalino?.connect(bluetoothDevice.address)
    }

    fun start() {
        bitalino?.start(intArrayOf(0, 1, 2, 3, 4, 5), 1)
    }

    fun stop() {
        bitalino?.stop()
    }

    fun disconnect() {
        if (registerReceiverIntent != null) {
            service.unregisterReceiver(updateReceiver)
            registerReceiverIntent = null
        }

        try {
            bitalino?.closeReceivers()
            bitalino?.disconnect()
        } catch (e: BITalinoException) {
            Log.e(TAG, "failed to close/disconnect from BITalino", e)
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

    companion object {

        const val TAG = "BITalinoRecorder"

    }

}