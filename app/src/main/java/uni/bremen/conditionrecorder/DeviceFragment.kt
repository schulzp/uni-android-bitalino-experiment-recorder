package uni.bremen.conditionrecorder

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import info.plux.pluxapi.Communication
import info.plux.pluxapi.bitalino.*
import info.plux.pluxapi.bitalino.bth.OnBITalinoDataAvailable
import info.plux.pluxapi.Constants
import info.plux.pluxapi.Constants.*

class DeviceFragment : Fragment(), OnBITalinoDataAvailable, View.OnClickListener {

    private var bluetoothDevice: BluetoothDevice? = null

    private var bitalino: BITalinoCommunication? = null
    private var isBITalino2 = false


    private var handler: Handler? = null

    private val currentState = States.DISCONNECTED

    private var isUpdateReceiverRegistered = false

    /*
     * UI elements
     */
    private var nameTextView: TextView? = null
    private var addressTextView: TextView? = null
    private var elapsedTextView: TextView? = null
    private var stateTextView: TextView? = null

    private var connectButton: Button? = null
    private var disconnectButton: Button? = null
    private var startButton: Button? = null
    private var stopButton: Button? = null

    private var bitalinoLinearLayout: LinearLayout? = null
    private var stateButton: Button? = null
    private var digital1RadioButton: RadioButton? = null
    private var digital2RadioButton: RadioButton? = null
    private var digital3RadioButton: RadioButton? = null
    private var digital4RadioButton: RadioButton? = null
    private var triggerButton: Button? = null
    private var batteryThresholdSeekBar: SeekBar? = null
    private var batteryThresholdButton: Button? = null
    private var pwmSeekBar: SeekBar? = null
    private var pwmButton: Button? = null
    private var resultsTextView: TextView? = null

    private var isDigital1RadioButtonChecked = false
    private var isDigital2RadioButtonChecked = false

    private val alpha = 0.25f

    /*
     * Local Broadcast
     */
    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_STATE_CHANGED == action) {
                val identifier = intent.getStringExtra(IDENTIFIER)
                val state = info.plux.pluxapi.Constants.States.getStates(intent.getIntExtra(EXTRA_STATE_CHANGED, 0))

                Log.i(TAG, identifier + " -> " + state!!.name)

                stateTextView!!.text = state!!.name

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
            } else if (ACTION_DATA_AVAILABLE == action) {
                if (intent.hasExtra(EXTRA_DATA)) {
                    val parcelable = intent.getParcelableExtra<Parcelable>(EXTRA_DATA)
                    if (parcelable.javaClass == BITalinoFrame::class.java) { //BITalino
                        val frame = parcelable as BITalinoFrame
                        resultsTextView!!.text = frame.toString()
                    }
                }
            } else if (ACTION_COMMAND_REPLY == action) {
                val identifier = intent.getStringExtra(IDENTIFIER)

                if (intent.hasExtra(EXTRA_COMMAND_REPLY) && intent.getParcelableExtra<Parcelable>(EXTRA_COMMAND_REPLY) != null) {
                    val parcelable = intent.getParcelableExtra<Parcelable>(EXTRA_COMMAND_REPLY)
                    if (parcelable.javaClass == BITalinoState::class.java) { //BITalino
                        Log.d(TAG, (parcelable as BITalinoState).toString())
                        resultsTextView!!.text = parcelable.toString()
                    } else if (parcelable.javaClass == BITalinoDescription::class.java) { //BITalino
                        isBITalino2 = (parcelable as BITalinoDescription).isBITalino2
                        resultsTextView!!.text = "isBITalino2: " + isBITalino2 + "; FwVersion: " + parcelable.fwVersion.toString()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "on create: ($arguments) ${arguments?.getParcelable<BluetoothDevice>(EXTRA_DEVICE)}")

        bluetoothDevice = arguments?.getParcelable(EXTRA_DEVICE)

        handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                val bundle = msg.data
                val frame = bundle.getParcelable<BITalinoFrame>(FRAME)

                Log.d(TAG, frame!!.toString())

                if (frame != null) { //BITalino
                    resultsTextView!!.text = frame.toString()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_device, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        setUIElements()
    }

    override fun onResume() {
        super.onResume()

        activity?.registerReceiver(updateReceiver, makeUpdateIntentFilter())
        isUpdateReceiverRegistered = true
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isUpdateReceiverRegistered) {
            activity?.unregisterReceiver(updateReceiver)
            isUpdateReceiverRegistered = false
        }

        if (bitalino != null) {
            bitalino!!.closeReceivers()
            try {
                bitalino!!.disconnect()
            } catch (e: BITalinoException) {
                e.printStackTrace()
            }

        }
    }

    /*
     * UI elements
     */
    private fun initView() {
        with(view as View) {
            nameTextView = findViewById(R.id.device_name_text_view)
            addressTextView = findViewById(R.id.mac_address_text_view)
            elapsedTextView = findViewById(R.id.elapsed_time_Text_view)
            stateTextView = findViewById(R.id.state_text_view)

            connectButton = findViewById(R.id.connect_button)
            disconnectButton = findViewById(R.id.disconnect_button)
            startButton = findViewById(R.id.start_button)
            stopButton = findViewById(R.id.stop_button)

            //bitalino ui elements
            bitalinoLinearLayout = findViewById(R.id.bitalino_linear_layout)
            stateButton = findViewById(R.id.state_button)
            digital1RadioButton = findViewById(R.id.digital_1_radio_button)
            digital2RadioButton = findViewById(R.id.digital_2_radio_button)
            digital3RadioButton = findViewById(R.id.digital_3_radio_button)
            digital4RadioButton = findViewById(R.id.digital_4_radio_button)
            triggerButton = findViewById(R.id.trigger_button)
            batteryThresholdSeekBar = findViewById(R.id.battery_threshold_seek_bar)
            batteryThresholdButton = findViewById(R.id.battery_threshold_button)
            pwmSeekBar = findViewById(R.id.pwm_seek_bar)
            pwmButton = findViewById(R.id.pwm_button)
            resultsTextView = findViewById(R.id.results_text_view)
        }
    }

    private fun setUIElements() {
        if (bluetoothDevice!!.name == null) {
            nameTextView!!.text = "BITalino"
        } else {
            nameTextView!!.text = bluetoothDevice!!.name
        }
        addressTextView!!.text = bluetoothDevice!!.address
        stateTextView!!.setText(currentState.name)

        var communication = Communication.getById(bluetoothDevice!!.type)
        Log.d(TAG, "Communication: " + communication.name)
        if (communication == Communication.DUAL) {
            communication = Communication.BLE
        }

        bitalino = BITalinoCommunicationFactory().getCommunication(communication, context, this)

        connectButton!!.setOnClickListener(this)
        disconnectButton!!.setOnClickListener(this)
        startButton!!.setOnClickListener(this)
        stopButton!!.setOnClickListener(this)
        stateButton!!.setOnClickListener(this)
        digital1RadioButton!!.setOnClickListener(this)
        digital2RadioButton!!.setOnClickListener(this)
        digital3RadioButton!!.setOnClickListener(this)
        digital4RadioButton!!.setOnClickListener(this)
        triggerButton!!.setOnClickListener(this)
        batteryThresholdButton!!.setOnClickListener(this)
        pwmButton!!.setOnClickListener(this)
    }

    private fun makeUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_STATE_CHANGED)
        intentFilter.addAction(ACTION_DATA_AVAILABLE)
        intentFilter.addAction(ACTION_EVENT_AVAILABLE)
        intentFilter.addAction(ACTION_DEVICE_READY)
        intentFilter.addAction(ACTION_COMMAND_REPLY)
        return intentFilter
    }

    /*
     * Callbacks
     */

    override fun onBITalinoDataAvailable(bitalinoFrame: BITalinoFrame) {
        val message = handler!!.obtainMessage()
        val bundle = Bundle()
        bundle.putParcelable(FRAME, bitalinoFrame)
        message.data = bundle
        handler!!.sendMessage(message)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.connect_button -> try {
                bitalino!!.connect(bluetoothDevice!!.address)
            } catch (e: BITalinoException) {
                e.printStackTrace()
            }

            R.id.disconnect_button -> try {
                bitalino!!.disconnect()
            } catch (e: BITalinoException) {
                e.printStackTrace()
            }

            R.id.start_button -> try {
                bitalino!!.start(intArrayOf(0, 1, 2, 3, 4, 5), 1)
            } catch (e: BITalinoException) {
                e.printStackTrace()
            }

            R.id.stop_button -> try {
                bitalino!!.stop()
            } catch (e: BITalinoException) {
                e.printStackTrace()
            }

            R.id.state_button -> try {
                bitalino!!.state()
            } catch (e: BITalinoException) {
                e.printStackTrace()
            }

            R.id.trigger_button -> {
                val digitalChannels: IntArray
                if (isBITalino2) {
                    digitalChannels = IntArray(2)
                } else {
                    digitalChannels = IntArray(4)
                }

                digitalChannels[0] = if (digital1RadioButton!!.isChecked) 1 else 0
                digitalChannels[1] = if (digital2RadioButton!!.isChecked) 1 else 0

                if (!isBITalino2) {
                    digitalChannels[2] = if (digital3RadioButton!!.isChecked) 1 else 0
                    digitalChannels[3] = if (digital4RadioButton!!.isChecked) 1 else 0
                }

                try {
                    bitalino!!.trigger(digitalChannels)
                } catch (e: BITalinoException) {
                    e.printStackTrace()
                }

            }
            R.id.digital_1_radio_button -> {
                digital1RadioButton!!.isChecked = !isDigital1RadioButtonChecked
                isDigital1RadioButtonChecked = digital1RadioButton!!.isChecked
            }
            R.id.digital_2_radio_button -> {
                digital2RadioButton!!.isChecked = !isDigital2RadioButtonChecked
                isDigital2RadioButtonChecked = digital2RadioButton!!.isChecked
            }

            R.id.digital_3_radio_button -> digital3RadioButton!!.isChecked = !digital3RadioButton!!.isChecked
            R.id.digital_4_radio_button -> digital4RadioButton!!.isChecked = !digital4RadioButton!!.isChecked

            R.id.battery_threshold_button -> try {
                bitalino!!.battery(batteryThresholdSeekBar!!.progress)
            } catch (e: BITalinoException) {
                e.printStackTrace()
            }

            R.id.pwm_button -> try {
                bitalino!!.pwm(pwmSeekBar!!.progress)
            } catch (e: BITalinoException) {
                e.printStackTrace()
            }

        }
    }

    companion object {
        val TAG = Content.DEVICE.name

        const val EXTRA_DEVICE = "info.plux.pluxapi.sampleapp.DeviceActivity.EXTRA_DEVICE"
        const val FRAME = "info.plux.pluxapi.sampleapp.DeviceActivity.Frame"

        fun newInstance():DeviceFragment = DeviceFragment()
    }
}
