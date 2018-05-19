package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.os.Environment
import android.util.Log
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import uni.bremen.conditionrecorder.bitalino.BITalinoFrameMapper
import uni.bremen.conditionrecorder.bitalino.BITalinoRecorder
import uni.bremen.conditionrecorder.io.DataWriter
import uni.bremen.conditionrecorder.service.RecorderService
import uni.bremen.conditionrecorder.wahoo.WahooRecorder
import java.io.File
import java.util.*

class RecorderSession(private val service: RecorderService, private val scheduler: Scheduler) {

    enum class State {
        NOT_READY, READY, RECORDING;

        companion object {

            fun valueOf(state: Recorder.State):State {
                return when(state) {
                    Recorder.State.CONNECTED -> State.READY
                    Recorder.State.RECORDING -> State.RECORDING
                    else -> State.NOT_READY
                }
            }
        }
    }

    private val disposables = CompositeDisposable()

    private val aggregator = BITalinoFrameMapper()

    val recorders = HashMap<BluetoothDevice, Recorder>()

    fun create() {
        disposables.add(service.bus.commands.observeOn(scheduler).subscribeOn(scheduler)
                .subscribe {
                    when (it) {
                        is RecorderBus.StartRecording -> startRecording()
                        is RecorderBus.StopRecording -> stopRecording()
                    }
                })
        disposables.add(service.bus.events.observeOn(scheduler).subscribeOn(scheduler)
                .subscribe {
                    when (it) {
                        is RecorderBus.RecorderStateChanged -> updateState(it)
                        is RecorderBus.SelectedDevice -> createRecorder(it.device)
                        is RecorderBus.PhaseSelected -> aggregator.phase.set(it.phase)
                    }
                })
        Log.d(TAG, "created")
    }

    fun destroy() {
        stopRecording()
        recorders.values.forEach(Recorder::disconnect)
        disposables.dispose()
        Log.d(TAG, "destroyed")
    }

    private fun startRecording() {
        if (recorders.isEmpty()) throw IllegalStateException("no recorder available")

        Log.d(TAG, "started recording")

        aggregator.reset()

        recorders.values.forEach(this::startRecorder)
    }

    private fun startRecorder(recorder: Recorder) {
        var disposable: Disposable? = null

        if (recorder is BITalinoRecorder) {
            val file = getDataOutputFile()
            val writer = DataWriter(file)
            Log.d(TAG, "writing to $file")

            disposable = recorder.frames
                    .doOnError { error ->
                        Log.e(TAG, "data.bitalino error", error)
                    }
                    .doFinally {
                        disposable?.dispose()
                        writer.close()
                    }
                    .observeOn(scheduler)
                    .subscribeOn(scheduler)
                    .map(aggregator::map)
                    .subscribe(writer::write)
        } else if (recorder is WahooRecorder) {
            disposable = recorder.data
                    .doOnError {error ->
                        Log.e(TAG, "data.wahoo error", error)
                    }
                    .doFinally {
                        disposable?.dispose()
                    }
                    .observeOn(scheduler)
                    .subscribeOn(scheduler)
                    .subscribe { data ->
                        Log.v(TAG, "heart rate data: $data")
                        aggregator.value.set(data.heartrate.asEventsPerSecond())
                    }
        }

        recorder.start()
    }

    private fun stopRecording() {
        Log.d(TAG, "stopped recording")

        recorders.values.forEach(Recorder::stop)
    }

    private fun createRecorder(device: BluetoothDevice) {
        val recorder = buildRecorder(device)
        recorders[device] = recorder
        recorder.connect()
    }

    private fun buildRecorder(device: BluetoothDevice): Recorder {
        if (device.address.endsWith("37")) {
            return BITalinoRecorder(device, service)
        }
        if (device.address.endsWith("58")) {
            return WahooRecorder(device, service)
        }
        throw IllegalArgumentException("unkown device type: $device")
    }

    private fun getDataOutputFile(): File {
        val externalFilesDir = service.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val time = System.currentTimeMillis()
        return File(externalFilesDir, "data-$time.csv")
    }

    private fun updateState(stateChanged: RecorderBus.RecorderStateChanged) {
        val lowest = Recorder.State.lowest(recorders.values.map(Recorder::getState))

        val state = State.valueOf(lowest)

        service.bus.events.onNext(RecorderBus.RecorderSessionStateChanged(state))
    }

    companion object {

        const val TAG = "RecorderSession"

    }

}