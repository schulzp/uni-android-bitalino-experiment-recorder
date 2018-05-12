package uni.bremen.conditionrecorder

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.*
import info.plux.pluxapi.BTHDeviceScan
import info.plux.pluxapi.Constants
import kotlinx.android.synthetic.main.fragment_list.*

class ListDevicesFragment : ContentFragment(Content.DEVICES, R.string.devices) {

    private lateinit var listAdapter: DeviceListAdapter
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private var isScanning: Boolean = false
    private val handler = Handler()

    private var bthDeviceScan: BTHDeviceScan? = null
    private var isScanDevicesUpdateReceiverRegistered = false

    private val onItemClickListener = object:GenericRecycleViewAdapter.OnItemClickListener<DeviceListAdapter.StatefulBluetoothDevice<*>> {

        override fun onItemSelected(item: DeviceListAdapter.StatefulBluetoothDevice<*>): Boolean {
            if (isScanning) {
                bthDeviceScan?.stopScan()
                isScanning = false
            }

            if (activity?.intent?.action == Intent.ACTION_PICK && activity?.intent?.type == INTENT_TYPE_DEVICE) {
                var result = Intent()
                result.putExtra(BluetoothDevice.EXTRA_DEVICE, item.device)
                activity?.setResult(Activity.RESULT_OK, result)
                activity?.finish()
            } else {
                startActivity(MainActivity.createViewDeviceIntent(context!!, item.device))
            }

            return true
        }
    }

    private val scanDevicesUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (action == Constants.ACTION_MESSAGE_SCAN) {
                val bluetoothDevice = intent.getParcelableExtra<BluetoothDevice>(Constants.EXTRA_DEVICE_SCAN)

                if (bluetoothDevice != null) {
                    listAdapter.add(DeviceListAdapter.BITalinoBluetoothDevice(bluetoothDevice))
                    listAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

        bluetoothAdapter = bluetoothManager?.adapter
                ?: throw RequiredFeatures.MissingFeatureException(PackageManager.FEATURE_BLUETOOTH)

        requestLocationPermissions()

        bthDeviceScan = BTHDeviceScan(this.context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_list, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        withObserver()?.onContentCreated(this)

        with(activity as MainActivity) {
            getFab().hide()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_list_devices, menu)
        if (!isScanning) {
            menu.findItem(R.id.menu_stop).isVisible = false
            menu.findItem(R.id.menu_scan).isVisible = true
            menu.findItem(R.id.menu_refresh).actionView = null
        } else {
            menu.findItem(R.id.menu_stop).isVisible = true
            menu.findItem(R.id.menu_scan).isVisible = false
            menu.findItem(R.id.menu_refresh)
                    .setActionView(R.layout.actionbar_indeterminate_progress)
        }
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> {
                listAdapter.clear()
                scanDevice(true)
            }
            R.id.menu_stop -> scanDevice(false)
        }
        return true
    }



    override fun onResume() {
        super.onResume()

        withObserver()?.onContentResumed(this)

        context?.registerReceiver(scanDevicesUpdateReceiver, IntentFilter(Constants.ACTION_MESSAGE_SCAN))
        isScanDevicesUpdateReceiverRegistered = true

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bluetoothAdapter.isEnabled) {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }

        setupList()

        for (device in bluetoothAdapter.bondedDevices) {
            listAdapter.add(DeviceListAdapter.BITalinoBluetoothDevice(device))
        }

        //scanDevice(true)
    }

    private fun setupList() {
        listAdapter = DeviceListAdapter(activity!!)
        listAdapter.onItemClickListener = onItemClickListener

        RecycleViewHelper.verticalList(list, activity!!).adapter = listAdapter
    }

    override fun onPause() {
        super.onPause()
        scanDevice(false)
        listAdapter.clear()
    }

    override fun onDestroy() {
        super.onDestroy()

        bthDeviceScan?.closeScanReceiver()

        if (isScanDevicesUpdateReceiverRegistered) {
            context?.unregisterReceiver(scanDevicesUpdateReceiver)
        }
    }

    private fun scanDevice(enable: Boolean) {
        if (enable) {
            // Stops isScanning after a pre-defined scan period.
            handler.postDelayed({
                isScanning = false
                bthDeviceScan!!.stopScan()
                activity?.invalidateOptionsMenu()
            }, SCAN_PERIOD)

            isScanning = true
            bthDeviceScan!!.doDiscovery()
        } else {
            isScanning = false
            bthDeviceScan!!.stopScan()
        }
        activity?.invalidateOptionsMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            activity?.finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        if (requestCode == REQUEST_COARSE_LOCATION_PERMISSIONS) {
            if (grantResults.size == COARSE_LOCATION_PERMISSIONS.size) {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance(getString(R.string.permission_request))
                                .show(childFragmentManager, FRAGMENT_DIALOG)
                        break
                    }
                }
            } else {
                ErrorDialog.newInstance(getString(R.string.permission_request))
                        .show(childFragmentManager, FRAGMENT_DIALOG)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun shouldShowRequestPermissionRationale(permissions: Array<String>) =
            permissions.any { shouldShowRequestPermissionRationale(it) }

    private fun requestLocationPermissions() {
        if (shouldShowRequestPermissionRationale(COARSE_LOCATION_PERMISSIONS)) {
            ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
        } else {
            requestPermissions(COARSE_LOCATION_PERMISSIONS, REQUEST_COARSE_LOCATION_PERMISSIONS)
        }
    }

    companion object {

        const val SELECTION_ID = "selected-devices"

        val TAG = Content.DEVICES.name

        fun newInstance():ListDevicesFragment = ListDevicesFragment()

    }

}