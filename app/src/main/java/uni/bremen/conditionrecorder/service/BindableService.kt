package uni.bremen.conditionrecorder.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.HandlerThread
import android.os.IBinder

open class BindableService : Service() {

    private val binder = Binder()

    private val backgroundThread = object : BackgroundThread(javaClass.name) {

        override fun onHandlerThreadPrepared(thread: HandlerThread) {
            this@BindableService.onHandlerThreadPrepared(thread)
        }

    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        backgroundThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        backgroundThread.stop()
    }

    protected open fun onHandlerThreadPrepared(thread: HandlerThread) { }

    inner class Binder : android.os.Binder() {

        fun <B : BindableService>getService(): B = this@BindableService as B

    }

    companion object {

        fun <S : BindableService>bind(
                context: Context,
                type:Class<S>)
                : BindableServiceConnection<S> {
            return BindableServiceConnection<S>(context)
                    .also { connection ->
                        context.bindService(
                                Intent(context, type),
                                connection, Context.BIND_AUTO_CREATE)
                    }

        }

    }

}