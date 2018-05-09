package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import io.reactivex.Observable
import io.reactivex.rxkotlin.zipWith


class RecorderBus : GenericBus<GenericBus.Instance, RecorderBus.Event, RecorderBus.Command>() {

    override val commandSubject = subject
            .filter { it is Command }
            .map { it as Command }

    override val eventSubject = subject
            .filter { it is Event }
            .map { it as Event }

    val bitalinoRecoringStared = eventSubject.filter { it is RecorderBus.BitalinoRecordingStarted }.map { it as RecorderBus.BitalinoRecordingStarted }
    val bitalinoRecordingStopped = eventSubject.filter { it is RecorderBus.BitalinoRecordingStopped }.map { it as RecorderBus.BitalinoRecordingStopped }

    val videoRecordingStarted = eventSubject.filter { it is RecorderBus.VideoRecordingStarted }.map { it as RecorderBus.VideoRecordingStarted }
    val videoRecordingStopped = eventSubject.filter { it is RecorderBus.VideoRecordingStopped }.map { it as RecorderBus.VideoRecordingStopped }

    val recordingStarted = videoRecordingStarted.zipWith(bitalinoRecoringStared, { v, s -> RecorderBus.RecordingStared(s, v) })
    val recordingStopped = videoRecordingStopped.zipWith(bitalinoRecordingStopped, { v, s -> RecorderBus.RecordingStopped(s, v) })

    val deviceSelected =  eventSubject
            .filter { event -> event is RecorderBus.SelectedDevice }
            .map { event -> event as RecorderBus.SelectedDevice }

    val deviceStateChange =  eventSubject
            .filter { event -> event is RecorderBus.DeviceStateChanged }
            .map { event -> event as RecorderBus.DeviceStateChanged }

    val recording = Observable.merge(recordingStarted, recordingStopped)

    interface Event : GenericBus.Instance
    interface Command : GenericBus.Instance

    class RecordingStared(val bitalinoRecordingStarted: BitalinoRecordingStarted, val videoRecordingStarted: VideoRecordingStarted) : Event
    class RecordingStopped(val bitalinoRecordingStopped: BitalinoRecordingStopped, val videoRecordingStopped: VideoRecordingStopped) : Event

    class BitalinoRecordingStarted : Event
    class BitalinoRecordingStopped : Event

    class VideoRecordingStarted : Event
    class VideoRecordingStopped(val path:String) : Event

    class StartRecording : Command
    class StopRecording : Command

    class SelectDevice : Command
    class SelectedDevice(val device: BluetoothDevice) : Event

    class DeviceStateChanged(val device: BluetoothDevice, val state: Any) : Event

}