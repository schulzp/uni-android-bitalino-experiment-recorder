package uni.bremen.conditionrecorder

import android.app.Activity
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.zipWith
import uni.bremen.conditionrecorder.bitalino.BITalinoRecordingSession
import java.util.*

class RecorderService : Service() {

    val bus: RecorderBus = RecorderBus()

    val bitalinoRecoringStared = bus.eventSubject.filter { it is RecorderBus.BitalinoRecordingStarted }.map { it as RecorderBus.BitalinoRecordingStarted }
    val bitalinoRecordingStopped = bus.eventSubject.filter { it is RecorderBus.BitalinoRecordingStopped }.map { it as RecorderBus.BitalinoRecordingStopped }

    val videoRecordingStarted = bus.eventSubject.filter { it is RecorderBus.VideoRecordingStarted }.map { it as RecorderBus.VideoRecordingStarted }
    val videoRecordingStopped = bus.eventSubject.filter { it is RecorderBus.VideoRecordingStopped }.map { it as RecorderBus.VideoRecordingStopped }

    val recordingStarted = videoRecordingStarted.zipWith(bitalinoRecoringStared, { v, s -> RecorderBus.RecordingStared(s, v) })
    val recordingStopped = videoRecordingStopped.zipWith(bitalinoRecordingStopped, { v, s -> RecorderBus.RecordingStopped(s, v) })

    val recording = Observable.merge(recordingStarted, recordingStopped)

    private val binder = Binder()

    private val disposables: MutableMap<String, Disposable> = HashMap()

    private val handlerThread = HandlerThread(TAG)

    private fun looper():Scheduler = AndroidSchedulers.from(handlerThread.looper)

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        disposables["commands"] = bus.commandSubject.subscribeOn(looper()).subscribe {
            when (it) {
                is RecorderBus.StartRecording -> startRecording()
                is RecorderBus.StopRecording -> stopRecording()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        bitalinoRecordingSession?.close()

        disposables.values.forEach { it.dispose() }
        disposables.clear()
    }

    fun startSession(bluetoothDevice: BluetoothDevice) {
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


    inner class Binder : android.os.Binder() {

        fun getService(): RecorderService = this@RecorderService

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

        fun close(activity: Activity) = activity.unbindService(this)

        private fun disposeOnClose(disposable: Disposable) {
            disposables.add(disposable)
        }

        private fun processQueue() {
            if (service != null) {
                synchronized(queue) {
                    queue.forEach { callback ->
                        val res = callback(this, service!!)
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
