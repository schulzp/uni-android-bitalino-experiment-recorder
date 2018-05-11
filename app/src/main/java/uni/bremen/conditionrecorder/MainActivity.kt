package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), ContentFragmentLifecycle {

    private val fullscreenHandler = Handler()
    private val fullScreenCallback = Runnable { setFullScreen() }

    private var content:Fragment? = null

    private var contentBackStackIdentifier:Int? = -1

    private var supportActionBarDrawerToggle:ActionBarDrawerToggle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "started with ${intent.action} (${intent.type})")

        checkRequiredFeatures()

        registerFullscreenListener()

        setContent()

    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterFullscreenListener()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        supportActionBarDrawerToggle?.syncState()
        super.onPostCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        supportActionBarDrawerToggle?.onConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (supportFragmentManager.backStackEntryCount == 1) {
            menuInflater.inflate(R.menu.menu_list_recordings, menu)
            return true
        }

        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        supportActionBarDrawerToggle?.onOptionsItemSelected(item)

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        fullscreenHandler.removeCallbacks(fullScreenCallback)

        if (hasFocus) {
            fullscreenHandler.postDelayed(fullScreenCallback, FULL_SCREEN_DELAY)
        }
    }

    override fun onBackPressed() {
        Log.d(TAG, "back pressed ${supportFragmentManager.backStackEntryCount}")

        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    internal fun getFab():FloatingActionButton = fab

    override fun onContentCreated(fragment:Fragment) {
        if (fragment is ContentFragment) {
            title = resources.getString(fragment.title)

            if (fragment.content.id < 0) {
                setDrawerEnabled(false)
                return
            } else {
                setDrawerEnabled(true)
            }

            with (navigationView.menu) {
                for (i in 0 until size()) {
                    with (getItem(i)) {
                        isEnabled = true
                        if (itemId == fragment.content.id) {
                            isEnabled = false
                        }
                    }
                }
            }
        }
    }

    override fun onContentResumed(fragment: Fragment) {
        content = fragment
        setFullScreen()
    }

    internal fun setFullScreen(enabled: Boolean = content is Fullscreen) {
        val appBarLayout = toolbar.parent as AppBarLayout

        if (enabled) {
            setSupportActionBar(null)
            appBarLayout.setExpanded(false, true)
            appBarLayout.visibility = View.GONE
            (main_fragment.layoutParams as CoordinatorLayout.LayoutParams).behavior = null
            findViewById<View>(android.R.id.content).fitsSystemWindows = false

            var flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_FULLSCREEN

            flags = flags or (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

            window.decorView.systemUiVisibility = flags
        } else {
            appBarLayout.visibility = View.VISIBLE
            appBarLayout.setExpanded(true, true)
            setSupportActionBar(toolbar)
            (main_fragment.layoutParams as CoordinatorLayout.LayoutParams).behavior = AppBarLayout.ScrollingViewBehavior()
            findViewById<View>(android.R.id.content).fitsSystemWindows = true

            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun registerFullscreenListener() {
        val decorView = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener({ visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                setFullScreen()
            }
        })
    }

    private fun unregisterFullscreenListener() {
        val decorView = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener(null)

        fullscreenHandler.removeCallbacks(fullScreenCallback)
    }

    private fun setContent() {
        setContentView(R.layout.activity_main)

        if (intent.action == Intent.ACTION_PICK && intent.type == INTENT_TYPE_DEVICE) {
            setContent(Content.DEVICES)
        } else {
            supportActionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close)

            navigationView.setNavigationItemSelectedListener { menuItem ->
                Log.d(TAG, "navigation used: $menuItem")

                // set item as selected to persist highlight
                menuItem.isChecked = true

                // close drawer when item is tapped
                drawerLayout.closeDrawers()

                when (menuItem.itemId) {
                    R.id.contentDevices -> setContent(Content.DEVICES)
                    R.id.contentRecordings -> setContent(Content.RECORDINGS)
                }

                true
            }

            setContent(Content.RECORDINGS)
        }
    }

    internal fun setContent(content:Content) {
        val tag = content.name
        val fragment: Fragment = supportFragmentManager.findFragmentByTag(tag)
                ?: when(content) {
                    Content.RECORDINGS -> {
                        ListRecordingsFragment()
                    }
                    Content.RECORDER -> {
                        RecorderFragment()
                    }
                    Content.DEVICES -> {
                        ListDevicesFragment()
                    }
                    Content.DEVICE -> {
                        DeviceFragment()
                    }
                }

        fragment.arguments = intent.extras

        contentBackStackIdentifier = supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_fragment, fragment, tag)
                .addToBackStack(tag)
                .commit()
    }

    private fun setDrawerEnabled(value:Boolean) {
        if (value) {
            supportActionBar?.setDisplayHomeAsUpEnabled(!value)
            supportActionBar?.setDisplayShowHomeEnabled(!value)

            supportActionBarDrawerToggle?.isDrawerIndicatorEnabled = value

            toolbar.setNavigationOnClickListener { _ -> drawerLayout.openDrawer(GravityCompat.START) }
        } else {
            supportActionBarDrawerToggle?.isDrawerIndicatorEnabled = value

            supportActionBar?.setDisplayHomeAsUpEnabled(!value)
            supportActionBar?.setDisplayShowHomeEnabled(!value)

            toolbar.setNavigationOnClickListener { _ -> onBackPressed() }
        }

        drawerLayout.setDrawerLockMode(
                if (value) DrawerLayout.LOCK_MODE_UNLOCKED
                else DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    private fun checkRequiredFeatures() {
        try {
            RequiredFeatures.check(this)
        } catch (e: RequiredFeatures.MissingFeatureException) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {

        const val TAG = "MainActivity"

        const val FULL_SCREEN_DELAY = 500L

        fun createPickDeviceIntent(context: Context):Intent {
            val intent = Intent(Intent.ACTION_PICK, Content.DEVICES.builder().build(), context, MainActivity::class.java)
            intent.type = INTENT_TYPE_DEVICE
            return intent
        }

        fun createViewDeviceIntent(context: Context, device:BluetoothDevice?):Intent {
            val builder = Content.DEVICES.builder()
            if (device != null) {
                builder.appendEncodedPath(device.address)
            }
            val intent = Intent(Intent.ACTION_VIEW, builder.build(), context, MainActivity::class.java)
            intent.type = INTENT_TYPE_DEVICE
            if (device != null) {
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            }
            return intent
        }

    }

}
