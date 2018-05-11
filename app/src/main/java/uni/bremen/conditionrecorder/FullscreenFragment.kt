package uni.bremen.conditionrecorder

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager


open class FullscreenFragment : Fragment(), Runnable {

    private val _handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFullscreen(activity!!)
        if (Build.VERSION.SDK_INT > 10) {
            registerSystemUiVisibility()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT > 10) {
            unregisterSystemUiVisibility()
        }
        exitFullscreen(activity!!)
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            _handler.removeCallbacks(this)
            _handler.postDelayed(this, 300)
        } else {
            _handler.removeCallbacks(this)
        }
    }

    fun onKeyDown(keyCode: Int) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            _handler.removeCallbacks(this)
            _handler.postDelayed(this, 500)
        }
    }

    override fun onStop() {
        _handler.removeCallbacks(this)
        super.onStop()
    }

    override fun run() {
        setFullscreen(activity!!)
    }

    private fun setFullscreen(activity: Activity) {
        if (Build.VERSION.SDK_INT > 10) {
            var flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_FULLSCREEN

            if (isImmersiveAvailable) {
                flags = flags or (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }

            activity.window.decorView.systemUiVisibility = flags
        } else {
            activity.window
                    .setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    private fun exitFullscreen(activity: Activity) {
        if (Build.VERSION.SDK_INT > 10) {
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        } else {
            activity.window
                    .setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                            WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun registerSystemUiVisibility() {
        val decorView = activity!!.window.decorView
        decorView.setOnSystemUiVisibilityChangeListener({ visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                setFullscreen(activity!!)
            }
        })
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun unregisterSystemUiVisibility() {
        val decorView = activity!!.window.decorView
        decorView.setOnSystemUiVisibilityChangeListener(null)
    }

    companion object {

        val isImmersiveAvailable: Boolean
            get() = android.os.Build.VERSION.SDK_INT >= 19

    }
}