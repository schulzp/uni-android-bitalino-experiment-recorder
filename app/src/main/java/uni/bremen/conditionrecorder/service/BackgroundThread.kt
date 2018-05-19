package uni.bremen.conditionrecorder.service

import android.os.HandlerThread

open class BackgroundThread(private val tag: String) {

    private var handlerThread: HandlerThread? = null

    @Synchronized
    internal fun start() {
        if (handlerThread == null) {
            handlerThread = object : HandlerThread(tag) {

                override fun onLooperPrepared() {
                    super.onLooperPrepared()

                    this@BackgroundThread.onHandlerThreadPrepared(this)
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

    open fun onHandlerThreadPrepared(thread: HandlerThread) {

    }

}