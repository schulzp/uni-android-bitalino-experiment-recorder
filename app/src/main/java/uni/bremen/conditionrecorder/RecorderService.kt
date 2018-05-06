package uni.bremen.conditionrecorder

import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.zipWith
import java.util.*

class RecorderService : Service() {

    val recorderBus:RecorderBus = RecorderBus()

    val signalRecoringStared = recorderBus.eventSubject.filter { it is RecorderBus.SignalRecordingStarted }.map { it as RecorderBus.SignalRecordingStarted }
    val signalRecordingStopped = recorderBus.eventSubject.filter { it is RecorderBus.SignalRecordingStopped }.map { it as RecorderBus.SignalRecordingStopped }

    val videoRecordingStarted = recorderBus.eventSubject.filter { it is RecorderBus.VideoRecordingStarted }.map { it as RecorderBus.VideoRecordingStarted }
    val videoRecordingStopped = recorderBus.eventSubject.filter { it is RecorderBus.VideoRecordingStopped }.map { it as RecorderBus.VideoRecordingStopped }

    val recordingStarted = videoRecordingStarted.zipWith(signalRecoringStared, { v, s -> RecorderBus.RecordingStared(s, v) })
    val recordingStopped = videoRecordingStopped.zipWith(signalRecordingStopped, { v, s -> RecorderBus.RecordingStopped(s, v) })

    val recording = Observable.merge(recordingStarted, recordingStopped)

    private val binder = Binder()

    private val disposables:MutableMap<String, Disposable> = HashMap()

    override fun onBind(intent: Intent): IBinder = binder

    fun onResume() {
        disposables["commands"] = recorderBus.commandSubject.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
            when (it) {
                is RecorderBus.StartRecording -> startRecording()
                is RecorderBus.StopRecording -> stopRecording()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        disposables.values.forEach { it.dispose() }
        disposables.clear()
    }

    private fun startRecording() {
        Log.d(TAG, "started recording")
    }

    private fun stopRecording() {
        Log.d(TAG, "stopped recording")
    }

    inner class Binder : android.os.Binder() {

        fun getService():RecorderService = this@RecorderService

    }

    class Connection : ServiceConnection {

        var service:RecorderService? = null

        private val disposables = LinkedList<Disposable>()

        private val listeners = LinkedList<(service:RecorderService) -> Unit>()

        override fun onServiceConnected(source: ComponentName, binder: IBinder) {
            Log.d(TAG, "service connected")
            if (binder is RecorderService.Binder) {
                service = binder.getService()
                notifyConnected()
            }
        }

        override fun onServiceDisconnected(source: ComponentName) {
            service = null

            listeners.clear()

            with(disposables) {
                forEach { it.dispose() }
                clear()
            }
        }

        fun connected(listener: (service:RecorderService) -> Unit) {
            listeners.add(listener)
            notifyConnected()
        }

        fun dispose(disposable: Disposable) {
            disposables.add(disposable)
        }

        fun close(activity: Activity) = activity.unbindService(this)

        private fun notifyConnected() {
            if (service != null) {
                listeners.forEach { it.invoke(service!!) }
            }
        }

    }

    companion object {

        const val TAG = "RecorderService"

        fun bind(activity: Activity):Connection {
            return Connection().also { activity.bindService(Intent(activity, RecorderService::class.java), it, Context.BIND_AUTO_CREATE) }
        }

    }

}
