package uni.bremen.conditionrecorder


class RecorderBus : GenericBus<GenericBus.Instance, RecorderBus.Event, RecorderBus.Command>() {

    override val commandSubject = subject
            .filter { it is Command }
            .map { it as Command }

    override val eventSubject = subject
            .filter { it is Event }
            .map { it as Event }

    interface Aware {
        var recorderBus: RecorderBus
    }

    interface Event : GenericBus.Instance
    interface Command : GenericBus.Instance

    class RecordingStared(val signalRecordingStarted: SignalRecordingStarted, val videoRecordingStarted: VideoRecordingStarted) : Event
    class RecordingStopped(val signalRecordingStopped: SignalRecordingStopped, val videoRecordingStopped: VideoRecordingStopped) : Event

    class SignalRecordingStarted : Event
    class SignalRecordingStopped : Event

    class VideoRecordingStarted : Event
    class VideoRecordingStopped(val path:String) : Event

    class StartRecording : Command
    class StopRecording : Command

}