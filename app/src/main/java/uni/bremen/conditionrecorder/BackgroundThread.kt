package uni.bremen.conditionrecorder

import android.os.HandlerThread
import android.os.Looper

open class BackgroundThread(private val tag: String) {

    private var handlerThread: HandlerThread? = null

    @Synchronized
    internal fun start() {
        if (handlerThread == null) {
            handlerThread = object : HandlerThread(tag) {

                override fun onLooperPrepared() {
                    super.onLooperPrepared()

                    this@BackgroundThread.onLooperPrepared(looper)
                }

            }
            handlerThread?.start()
        }
    }

    @Synchronized
    internal fun stop() {
        if (handlerThread != null) {
            val moribund = handlerThread
            handlerThread = null
            moribund?.interrupt()
        }
    }

    open fun onLooperPrepared(looper: Looper) {

    }

}