package uni.bremen.conditionrecorder

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import info.plux.pluxapi.Constants
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_recorder_device_list.*


class RecorderDeviceListFragment : Fragment() {

    private lateinit var adapter:DeviceListAdapter

    private lateinit var recorderServiceConnection:RecorderService.Connection

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_recorder_device_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupList()
    }

    override fun onResume() {
        super.onResume()

        recorderServiceConnection = RecorderService.bind(context!!) { _, service ->
            val scheduler = AndroidSchedulers.mainThread()

            val selected = service.bus.deviceSelected.subscribeOn(scheduler)
                    .subscribe { selected -> addDevice(selected.device) }
            val updated = service.bus.deviceStateChange.subscribeOn(scheduler)
                    .subscribe { change -> updateDevice(change.device, change.state) }

            Handler().postDelayed({ addDefaultDevices(service) }, 100)

            listOf(selected, updated)
        }

        empty.setOnClickListener {
            startActivityForResult(MainActivity.createPickDeviceIntent(context!!), INTENT_REQUEST_PICK_DEVICE)
        }
    }

    override fun onPause() {
        super.onPause()

        recorderServiceConnection.close(context!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == INTENT_REQUEST_PICK_DEVICE && resultCode == Activity.RESULT_OK) {
            var device = data?.extras?.getParcelable<BluetoothDevice>(EXTRA_DEVICE)
            if (device != null) {
                recorderServiceConnection.whenConnected { _, service ->
                    service.bus.events.onNext(RecorderBus.SelectedDevice(device))
                }
            }
        }
    }

    private fun addDevice(device: BluetoothDevice) {
        adapter.add(DeviceListAdapter.BITalinoBluetoothDevice(device))
        adapter.notifyDataSetChanged()
    }

    private fun updateDevice(device: BluetoothDevice, state:Any?) {
        val statefulBluetoothDevice = adapter.find(device)
        if (statefulBluetoothDevice != null) {
            when (statefulBluetoothDevice) {
                is DeviceListAdapter.BITalinoBluetoothDevice -> {
                    //statefulBluetoothDevice.state = state as? Constants.States
                }
            }

            val position = adapter.indexOf(statefulBluetoothDevice)

            list.findViewHolderForAdapterPosition(position)
                    ?.let { it as DeviceListAdapter.DeviceViewHolder }
                    ?.let { adapter.onBindViewHolder(it, statefulBluetoothDevice, position) }
        }
    }

    private fun setupList() {
        adapter = DeviceListAdapter(activity!!)
        RecycleViewHelper.verticalList(list, activity!!).adapter = adapter
    }

    private fun addDefaultDevices(service: RecorderService) {
        val bluetoothManager = service.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
                ?: throw RequiredFeatures.MissingFeatureException(PackageManager.FEATURE_BLUETOOTH)

        bluetoothAdapter.bondedDevices
                .filter { it.address == "20:16:02:14:75:37" }
                .forEach { service.bus.events.onNext(RecorderBus.SelectedDevice(it)) }
    }

}