package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.os.Environment
import android.util.Log
import io.reactivex.Scheduler
import uni.bremen.conditionrecorder.bitalino.BITalinoFrameMapper
import uni.bremen.conditionrecorder.bitalino.BITalinoRecorder
import uni.bremen.conditionrecorder.io.DataWriter
import java.io.File

class RecorderSession(private val service: RecorderService, private val scheduler: Scheduler) {

    private val disposables = DisposableMap()

    private val aggregator = BITalinoFrameMapper()

    private var bitalinoRecorder: BITalinoRecorder? = null

    fun create() {
        disposables["commands"] = service.bus.commands.subscribeOn(scheduler)
                .subscribe {
                    when (it) {
                        is RecorderBus.StartRecording -> startRecording()
                        is RecorderBus.StopRecording -> stopRecording()
                    }
                }
        disposables["events"] = service.bus.events.subscribeOn(scheduler)
                .subscribe {
                    when (it) {
                        is RecorderBus.SelectedDevice -> createRecorder(it.device)
                        is RecorderBus.PhaseSelected -> aggregator?.phase = it.phase
                    }
                }
        Log.d(TAG, "created")
    }

    fun destroy() {
        stopRecording()
        disposables.dispose()
        Log.d(TAG, "destroyed")
    }

    private fun startRecording() {
        if (bitalinoRecorder == null) throw IllegalStateException("no recorder available")

        Log.d(TAG, "started recording")

        val file = getDataOutputFile()
        val writer = DataWriter(file)

        aggregator.reset()

        Log.d(TAG, "writing to $file")

        val framesKey = "data.bitalino"

        disposables[framesKey] = bitalinoRecorder!!.frames
                .doOnError { error ->
                    Log.e(TAG, "data.bitalino error", error)
                }
                .doFinally {
                    disposables.remove(framesKey)?.dispose()
                    writer.close()
                }
                .subscribeOn(scheduler)
                .map(aggregator::map)
                .subscribe(writer::write)

        bitalinoRecorder!!.start()
    }

    private fun stopRecording() {
        Log.d(TAG, "stopped recording")

        bitalinoRecorder?.stop()
    }

    private fun createRecorder(device: BluetoothDevice) {
        bitalinoRecorder = BITalinoRecorder(device, service)
        bitalinoRecorder?.connect()
    }

    private fun getDataOutputFile(): File {
        val externalFilesDir = service.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File(externalFilesDir, "bitalino.csv")
    }

    companion object {

        const val TAG = "RecorderSession"

    }

}