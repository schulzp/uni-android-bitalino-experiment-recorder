package uni.bremen.conditionrecorder

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import uni.bremen.conditionrecorder.bitalino.BITalinoRecordingSession
import java.util.*


class RecorderService : Service() {

    val bus: RecorderBus = RecorderBus()

    private val binder = Binder()

    private val disposables: MutableMap<String, Disposable> = HashMap()

    private var handlerThread: HandlerThread? = null

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        startThread()
    }

    override fun onDestroy() {
        super.onDestroy()

        bitalinoRecordingSession?.close()

        disposables.values.forEach { it.dispose() }
        disposables.clear()

        stopThread()
    }

    fun createSession(bluetoothDevice: BluetoothDevice) {
        bitalinoRecordingSession = BITalinoRecordingSession(this, bluetoothDevice)
    }

    private var bitalinoRecordingSession: BITalinoRecordingSession? = null

    private fun startRecording() {
        Log.d(TAG, "started recording")

        bitalinoRecordingSession?.start()
    }

    private fun stopRecording() {
        Log.d(TAG, "stopped recording")

        bitalinoRecordingSession?.close()
    }

    @Synchronized
    private fun startThread() {
        if (handlerThread == null) {
            handlerThread = BackgroundThread(TAG)
            handlerThread?.start()
        }
    }

    @Synchronized
    private fun stopThread() {
        if (handlerThread != null) {
            val moribund = handlerThread
            handlerThread = null
            moribund?.interrupt()
        }
    }

    inner class Binder : android.os.Binder() {

        fun getService(): RecorderService = this@RecorderService

    }

    inner class BackgroundThread(tag: String) : HandlerThread(tag) {

        override fun onLooperPrepared() {
            val scheduler = AndroidSchedulers.from(looper)

            disposables["commands"] = bus.commandSubject.subscribeOn(scheduler)
                    .subscribe {
                        when (it) {
                            is RecorderBus.StartRecording -> startRecording()
                            is RecorderBus.StopRecording -> stopRecording()
                        }
                    }

            disposables["devices"] = bus.eventSubject.subscribeOn(scheduler)
                    .filter { it is RecorderBus.SelectedDevice }
                    .map { it as RecorderBus.SelectedDevice }
                    .map { it.device }
                    .subscribe { createSession(it) }
        }

    }

    class Connection : ServiceConnection {

        private var service: RecorderService? = null

        private val disposables = LinkedList<Disposable>()

        private val queue = LinkedList<(connection: Connection, service: RecorderService) -> Any>()

        override fun onServiceConnected(source: ComponentName, binder: IBinder) {
            if (binder is RecorderService.Binder) {
                service = binder.getService()
                processQueue()
            }
        }

        override fun onServiceDisconnected(source: ComponentName) {
            service = null

            queue.clear()

            with(disposables) {
                forEach { it.dispose() }
                clear()
            }
        }

        fun whenConnected(listener: (connection: Connection, service: RecorderService) -> Any) {
            synchronized(queue) {
                queue.add(listener)
            }
            processQueue()
        }

        fun close(context: Context) = context.unbindService(this)

        private fun disposeOnClose(disposable: Disposable) {
            disposables.add(disposable)
        }

        private fun processQueue() {
            if (service != null) {
                synchronized(queue) {
                    queue.forEach { callback ->
                        val res = callback(this, service!!)
                        if (res is List<*> && res.all { it is Disposable }) {
                            res.forEach { disposeOnClose(it as Disposable) }
                        }
                        if (res is Disposable) {
                            disposeOnClose(res)
                        }
                    }
                    queue.clear()
                }
            }
        }

    }

    companion object {

        const val TAG = "RecorderService"

        fun bind(context: Context, listener: ((connection: Connection, service: RecorderService) -> Unit)? = null): Connection {
            return Connection()
                    .also { connection ->
                        context.bindService(
                                Intent(context, RecorderService::class.java),
                                connection,
                                Context.BIND_AUTO_CREATE)
                    }
                    .also { connection -> if (listener != null) connection.whenConnected(listener) }
        }

    }

}
