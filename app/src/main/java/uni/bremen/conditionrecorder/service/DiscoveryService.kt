package uni.bremen.conditionrecorder.service

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.HandlerThread
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import uni.bremen.conditionrecorder.BluetoothDeviceDiscovery
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

    val isScanning = scanning.distinctUntilChanged()

    fun start(): Observable<BluetoothDevice> {
        return devices
                ?: Observable.fromIterable(discoveryProviders)
                        .map { factory -> factory() }
                        .concatMap { discovery ->
                            discovery.devices
                                    .observeOn(scheduler)
                                    .subscribeOn(scheduler)
                                    .doOnSubscribe {
                                discovery.start()
                                scanning.onNext(true)
                            }
                        }
                        .doFinally {
                            scanning.onNext(false)
                        }.cache().also { this.devices = it }
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