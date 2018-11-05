package uni.bremen.conditionrecorder.bitalino

import info.plux.pluxapi.bitalino.BITalinoFrame
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

class BITalinoFrameSanityCheck {

    private val maxNumberOfZeroFrames = 10

    fun observe(frames:Observable<BITalinoFrame>): Observable<String> {
        return observerZeroFrames(frames)
    }

    private fun observerZeroFrames(frames: Observable<BITalinoFrame>): Observable<String> {
        return frames.map { frame -> frame.analogArray.sum() }
                .filter { sum -> sum == 0 }
                .buffer(1, TimeUnit.SECONDS)
                .map { buffer -> buffer.size }
                .filter { numberOfZeroFrames -> numberOfZeroFrames > maxNumberOfZeroFrames }
                .map { bufferSize -> "found $bufferSize 0 frames" }
    }

}