package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.os.Environment
import android.util.Log
import io.reactivex.Scheduler
import uni.bremen.conditionrecorder.bitalino.BITalinoFrameWriter
import uni.bremen.conditionrecorder.bitalino.BITalinoRecorder
import java.io.File

class RecorderSession(private val service: RecorderService, private val scheduler: Scheduler) {

    private val disposables = DisposableMap()

    private var bitalinoRecorder: BITalinoRecorder? = null

    fun create() {
        disposables["commands"] = service.bus.commandSubject.subscribeOn(scheduler)
                .subscribe {
                    when (it) {
                        is RecorderBus.StartRecording -> startRecording()
                        is RecorderBus.StopRecording -> stopRecording()
                    }
                }

        disposables["devices"] = service.bus.eventSubject.subscribeOn(scheduler)
                .filter { it is RecorderBus.SelectedDevice }
                .map { it as RecorderBus.SelectedDevice }
                .map { it.device }
                .subscribe { createSession(it) }

        Log.d(TAG, "created")
    }

    fun destroy() {
        stopRecording()
        disposables.dispose()
        Log.d(TAG, "destroyed")
    }

    private fun startRecording() {
        Log.d(TAG, "started recording")

        bitalinoRecorder?.start()
    }

    private fun stopRecording() {
        Log.d(TAG, "stopped recording")

        bitalinoRecorder?.stop()
        bitalinoRecorder?.writer?.close()
    }

    private fun createSession(bluetoothDevice: BluetoothDevice) {
        bitalinoRecorder = BITalinoRecorder(service, bluetoothDevice)
        val file = getBitalinoOutputFile()
        Log.d(TAG, "writing to $file")
        bitalinoRecorder?.writer = BITalinoFrameWriter(file)
        bitalinoRecorder?.connect()
    }

    private fun getBitalinoOutputFile(): File {
        val externalFilesDir = service.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File(externalFilesDir, "bitalino.csv")
    }

    companion object {

        const val TAG = "RecorderSession"

    }

}