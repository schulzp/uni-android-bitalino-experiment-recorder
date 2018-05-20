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
import com.wahoofitness.connector.capabilities.Battery
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_recorder_device_list.*
import uni.bremen.conditionrecorder.rx.onMainThread
import uni.bremen.conditionrecorder.service.BindableServiceConnection
import uni.bremen.conditionrecorder.service.DiscoveryService
import uni.bremen.conditionrecorder.service.RecorderService
import uni.bremen.conditionrecorder.wahoo.WahooDiscovery
import uni.bremen.conditionrecorder.wahoo.WahooRecorder


class RecorderDeviceListFragment : Fragment() {

    private lateinit var adapter:DeviceListAdapter

    private lateinit var recorderServiceConnection: BindableServiceConnection<RecorderService>
    private lateinit var discoveryServiceConnection: BindableServiceConnection<DiscoveryService>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_recorder_device_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupList()
    }

    override fun onResume() {
        super.onResume()

        recorderServiceConnection = RecorderService.bind(activity!!)
        recorderServiceConnection.service.subscribe { recorderService ->
            recorderService.bus.deviceSelected
                    .compose(onMainThread())
                    .map(RecorderBus.SelectedDevice::device)
                    .subscribe(this::addDevice)
            recorderService.bus.deviceStateChange
                    .compose(onMainThread())
                    .subscribe { change -> updateDevice(change.device, change.state, change.batteryLevel) }

            discoveryServiceConnection = DiscoveryService.bind(activity!!)
            discoveryServiceConnection.service.subscribe { discoveryService ->
                discoveryService.start()
                        .compose(onMainThread())
                        .map { RecorderBus.SelectedDevice(it) }
                        .subscribe(recorderService.bus.events::onNext)
            }
        }

        empty.setOnClickListener {
            startActivityForResult(MainActivity.createPickDeviceIntent(activity!!), INTENT_REQUEST_PICK_DEVICE)
        }
    }

    override fun onPause() {
        super.onPause()

        recorderServiceConnection.close()
        discoveryServiceConnection.close()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == INTENT_REQUEST_PICK_DEVICE && resultCode == Activity.RESULT_OK) {
            var device = data?.extras?.getParcelable<BluetoothDevice>(EXTRA_DEVICE)
            if (device != null) {
                recorderServiceConnection.service.subscribe { service ->
                    service.bus.events.onNext(RecorderBus.SelectedDevice(device))
                }
            }
        }
    }

    private fun addDevice(device: BluetoothDevice) {
        adapter.add(DeviceListAdapter.RecorderBluetoothDevice(device))
        adapter.notifyDataSetChanged()
    }

    private fun updateDevice(device: BluetoothDevice, state:Recorder.State, batteryLevel: Recorder.BatteryLevel) {
        val statefulBluetoothDevice = adapter.find(device)
        if (statefulBluetoothDevice != null) {
            if (statefulBluetoothDevice is DeviceListAdapter.RecorderBluetoothDevice) {
                statefulBluetoothDevice.state = state
                statefulBluetoothDevice.batteryLevel = batteryLevel
            }

            val position = adapter.indexOf(statefulBluetoothDevice)

            list?.findViewHolderForAdapterPosition(position)
                    ?.let { it as DeviceListAdapter.DeviceViewHolder }
                    ?.let { adapter.onBindViewHolder(it, statefulBluetoothDevice, position) }
        }
    }

    private fun setupList() {
        adapter = DeviceListAdapter(activity!!)
        adapter.compact = true
        RecycleViewHelper.verticalList(list, activity!!).adapter = adapter
    }

}