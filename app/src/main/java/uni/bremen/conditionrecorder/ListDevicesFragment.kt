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
import android.support.v4.app.ListFragment
import android.util.Log
import android.view.*
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import info.plux.pluxapi.BTHDeviceScan
import info.plux.pluxapi.Constants
import java.util.*

import info.plux.pluxapi.Constants.*

class ListDevicesFragment : ListFragment() {

    private lateinit var deviceListAdapter: DeviceListAdapter
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var mScanning: Boolean = false
    private lateinit var mHandler: Handler

    private var bthDeviceScan: BTHDeviceScan? = null
    private var isScanDevicesUpdateReceiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        mHandler = Handler()


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        //        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
        //            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        //            finish();
        //        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        mBluetoothAdapter = bluetoothManager!!.adapter

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this.context, "Error - Bluetooth not supported", Toast.LENGTH_SHORT).show()
            activity?.finish()
            return
        }

        requestLocationPermissions()

        bthDeviceScan = BTHDeviceScan(this.context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        with(activity as MainActivity) {
            title = resources.getString(R.string.devices)
            setDrawerEnabled(false)
            updateDrawerMenu(R.id.contentDevices)

            with(getFab()) {
                hide()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_list_devices, menu)
        if (!mScanning) {
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
                deviceListAdapter!!.clear()
                scanDevice(true)
            }
            R.id.menu_stop -> scanDevice(false)
        }
        return true
    }

    override fun onResume() {
        super.onResume()

        context?.registerReceiver(scanDevicesUpdateReceiver, IntentFilter(Constants.ACTION_MESSAGE_SCAN))
        isScanDevicesUpdateReceiverRegistered = true

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter!!.isEnabled) {
            if (!mBluetoothAdapter!!.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }

        // Initializes list view adapter.
        deviceListAdapter = DeviceListAdapter()
        setListAdapter(deviceListAdapter)
        scanDevice(true)
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

    override fun onPause() {
        super.onPause()
        scanDevice(false)
        deviceListAdapter!!.clear()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (bthDeviceScan != null) {
            bthDeviceScan!!.closeScanReceiver()
        }

        if (isScanDevicesUpdateReceiverRegistered) {
            context?.unregisterReceiver(scanDevicesUpdateReceiver)
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val device = deviceListAdapter.getDevice(position) ?: return
        val intent = Intent(this.context, MainActivity::class.java)
        intent.putExtra(EXTRA_SHOW_CONTENT, Content.DEVICE)
        intent.putExtra(DeviceFragment.EXTRA_DEVICE, device)
        if (mScanning) {
            bthDeviceScan?.stopScan()
            mScanning = false
        }
        startActivity(intent)
    }

    private fun scanDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler!!.postDelayed({
                mScanning = false
                bthDeviceScan!!.stopScan()
                activity?.invalidateOptionsMenu()
            }, SCAN_PERIOD)

            mScanning = true
            bthDeviceScan!!.doDiscovery()
        } else {
            mScanning = false
            bthDeviceScan!!.stopScan()
        }
        activity?.invalidateOptionsMenu()
    }

    private val scanDevicesUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            if (action == Constants.ACTION_MESSAGE_SCAN) {
                val bluetoothDevice = intent.getParcelableExtra<BluetoothDevice>(Constants.EXTRA_DEVICE_SCAN)

                if (bluetoothDevice != null) {
                    deviceListAdapter!!.addDevice(bluetoothDevice)
                    deviceListAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    // Adapter for holding devices found through scanning.
    private inner class DeviceListAdapter : BaseAdapter() {
        private val devices: ArrayList<BluetoothDevice> = ArrayList()
        private val mInflator: LayoutInflater? = activity?.getLayoutInflater()

        fun addDevice(device: BluetoothDevice) {
            if (!devices.contains(device)) {
                devices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice? {
            return devices[position]
        }

        fun clear() {
            devices.clear()
        }

        override fun getCount(): Int {
            return devices.size
        }

        override fun getItem(i: Int): Any {
            return devices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            var view = view
            val viewHolder: ViewHolder
            // General ListView optimization code.
            if (view == null) {
                view = mInflator?.inflate(R.layout.listitem_device, null)
                viewHolder = ViewHolder()
                viewHolder.deviceAddress = view!!.findViewById(R.id.device_address) as TextView
                viewHolder.deviceName = view.findViewById(R.id.device_name) as TextView
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }

            val device = devices[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.length > 0) {
                viewHolder.deviceName!!.text = deviceName
            } else {
                viewHolder.deviceName!!.text = "BITalino"
            }
            viewHolder.deviceAddress!!.text = device.address

            return view
        }
    }

    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
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
        val TAG = Content.DEVICES.name

        fun newInstance():ListDevicesFragment = ListDevicesFragment()
    }

}