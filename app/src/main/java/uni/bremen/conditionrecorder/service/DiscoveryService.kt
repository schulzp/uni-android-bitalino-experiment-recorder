package uni.bremen.conditionrecorder.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.HandlerThread
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import uni.bremen.conditionrecorder.BluetoothDeviceDiscovery
import uni.bremen.conditionrecorder.RequiredFeatures
import uni.bremen.conditionrecorder.bitalino.BITalinoDiscovery
import uni.bremen.conditionrecorder.wahoo.WahooDiscovery
import java.util.*

class DiscoveryService : BindableService() {

    private lateinit var scheduler: Scheduler

    private var devices:Observable<BluetoothDevice>? = null

    private val discoveryProviders = LinkedList<() -> BluetoothDeviceDiscovery>()

    init {
        discoveryProviders.add({ BITalinoDiscovery(this) })
        discoveryProviders.add({ WahooDiscovery(this) })
    }

    private val scanning = PublishSubject.create<Boolean>()

    private val backgroundTransformer = ObservableTransformer<BluetoothDevice, BluetoothDevice> {
        it.subscribeOn(scheduler).observeOn(scheduler)
    }

    val isScanning = scanning.distinctUntilChanged()

    fun start(): Observable<BluetoothDevice> {
        return devices
                ?: Observable.fromIterable(discoveryProviders)
                        .map { factory -> factory() }
                        .concatMap { discovery ->
                            discovery.devices
                                    .compose(backgroundTransformer)
                                    .doOnSubscribe {
                                discovery.start()
                                scanning.onNext(true)
                            }
                        }
                        .doFinally {
                            scanning.onNext(false)
                        }
                        .mergeWith(getKnownDevices())
                        .distinct()
                        .cache()
                        .also { this.devices = it }
    }

    private fun getKnownDevices(): Observable<BluetoothDevice> {
        return Observable
                .fromIterable(getBluetoothAdapter().bondedDevices)
                .filter { it.address == "20:16:02:14:75:37" }
    }

    private fun getBluetoothAdapter(): BluetoothAdapter {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager
                ?.adapter
                ?: throw RequiredFeatures.MissingFeatureException(PackageManager.FEATURE_BLUETOOTH)
    }

    override fun onHandlerThreadPrepared(thread: HandlerThread) {
        scheduler = AndroidSchedulers.from(thread.looper)
    }

    companion object {

        const val TAG = "DiscoveryService"

        fun bind(context: Context) : BindableServiceConnection<DiscoveryService> {
            return BindableService.bind(context, DiscoveryService::class.java)
        }

    }

}