package uni.bremen.conditionrecorder

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Looper
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.*


class RecorderService : Service() {

    val bus: RecorderBus = RecorderBus()

    private val binder = Binder()

    private val disposables = DisposableMap()

    private val backgroundThread = object : BackgroundThread(RecorderService.TAG) {

        override fun onLooperPrepared(looper:Looper) {
            subscribe(AndroidSchedulers.from(looper))
        }

    }

    private var session: RecorderSession? = null

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        backgroundThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        destroySession()

        unsubscribe()

        backgroundThread.stop()
    }



    private fun subscribe(scheduler: Scheduler) {
        disposables["commands"] = bus.commands.subscribeOn(scheduler)
                .subscribe {
                    when (it) {
                        is RecorderBus.CreateSession -> createSession(scheduler)
                        is RecorderBus.DestroySession -> destroySession()
                    }
                }
    }

    private fun createSession(scheduler: Scheduler) {
        session = RecorderSession(this, scheduler)
        session?.create()
    }

    private fun destroySession() {
        session?.destroy()
        session = null
    }

    private fun unsubscribe() {
        disposables.dispose()
    }

    inner class Binder : android.os.Binder() {

        fun getService(): RecorderService = this@RecorderService

    }

    class Connection : ServiceConnection {

        private var service: RecorderService? = null

        private val disposables = LinkedList<Disposable>()

        private val queue = LinkedList<(connection: Connection, service: RecorderService) -> Any>()

        override fun onServiceConnected(source: ComponentName, binder: IBinder) {
            if (binder is RecorderService.Binder) {
                service = binder.getService()
                processQueue()
            }
        }

        override fun onServiceDisconnected(source: ComponentName) {
            service = null

            queue.clear()

            with(disposables) {
                forEach { it.dispose() }
                clear()
            }
        }

        fun whenConnected(listener: (connection: Connection, service: RecorderService) -> Any) {
            synchronized(queue) {
                queue.add(listener)
            }
            processQueue()
        }

        fun close(context: Context) = context.unbindService(this)

        private fun disposeOnClose(disposable: Disposable) {
            disposables.add(disposable)
        }

        private fun processQueue() {
            if (service != null) {
                synchronized(queue) {
                    queue.forEach { callback ->
                        val res = callback(this, service!!)
                        if (res is List<*> && res.all { it is Disposable }) {
                            res.forEach { disposeOnClose(it as Disposable) }
                        }
                        if (res is Disposable) {
                            disposeOnClose(res)
                        }
                    }
                    queue.clear()
                }
            }
        }

    }

    companion object {

        const val TAG = "RecorderService"

        fun bind(context: Context, listener: ((connection: Connection, service: RecorderService) -> Unit)? = null): Connection {
            return Connection()
                    .also { connection ->
                        context.bindService(
                                Intent(context, RecorderService::class.java),
                                connection,
                                Context.BIND_AUTO_CREATE)
                    }
                    .also { connection -> if (listener != null) connection.whenConnected(listener) }
        }

    }

}
