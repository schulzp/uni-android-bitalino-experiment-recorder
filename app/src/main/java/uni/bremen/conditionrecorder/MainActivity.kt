package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.AppBarLayout
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
import kotlinx.android.synthetic.main.activity_main.*
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE




class MainActivity : AppCompatActivity() {

    private var contentBackStackIdentifier:Int? = -1

    private var supportActionBarDrawerToggle:ActionBarDrawerToggle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "started with ${intent.action} (${intent.type})")

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (intent.action == Intent.ACTION_PICK && intent.type == INTENT_TYPE_DEVICE) {
            setContent(Content.DEVICES)
        } else {
            supportActionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close)

            navigationView.setNavigationItemSelectedListener { menuItem ->
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

    internal fun setDrawerEnabled(value:Boolean) {
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

    internal fun getFab():FloatingActionButton = fab

    internal fun setContent(content:Content) {
        val tag = content.name
        val fragment: Fragment = supportFragmentManager.findFragmentByTag(tag)
                ?: when(content) {
            Content.RECORDINGS -> {
                ListRecordingsFragment.newInstance()
            }
            Content.RECORDER -> {
                RecorderFragment.newInstance()
            }
            Content.DEVICES -> {
                ListDevicesFragment.newInstance()
            }
            Content.DEVICE -> {
                DeviceFragment.newInstance()
            }
        }

        fragment.arguments = intent.extras

        contentBackStackIdentifier = supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_fragment, fragment, tag)
                .addToBackStack(tag)
                .commit()
    }

    internal fun updateDrawerMenu(currentId: Int) {
        with (navigationView.menu) {
            for (i in 0 until size()) {
                with (getItem(i)) {
                    isEnabled = true
                    if (itemId == currentId) {
                        isEnabled = false
                    }
                }
            }
        }
    }

    private var fullscreen: Boolean = false

    internal fun setFullScreen(enabled: Boolean) {
        this.fullscreen = enabled
        val flags = if (enabled)
            (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        else
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)


        val appBarLayout = toolbar.parent as AppBarLayout
        if (enabled) {
            appBarLayout.setExpanded(false, true)
            appBarLayout.visibility = View.GONE
        } else {
            appBarLayout.visibility = View.VISIBLE
            appBarLayout.setExpanded(true, true)
        }

        window.decorView.setOnSystemUiVisibilityChangeListener(View.OnSystemUiVisibilityChangeListener { visibility ->
            // Note that system bars will only be "visible" if none of the
            // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                // TODO: The system bars are visible. Make any desired
                // adjustments to your UI, such as showing the action bar or
                // other navigational controls.
            } else {
                // TODO: The system bars are NOT visible. Make any desired
                // adjustments to your UI, such as hiding the action bar or
                // other navigational controls.
            }

            main_fragment.invalidate()
        })
        window.decorView.systemUiVisibility = flags
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            setFullScreen(fullscreen)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {

        const val TAG = "MainActivity"

        fun createPickDeviceIntent(context: Context):Intent {
            val intent = Intent(Intent.ACTION_PICK, URI_DEVICES, context, MainActivity::class.java)
            intent.type = INTENT_TYPE_DEVICE
            return intent
        }

        fun createViewDeviceIntent(context: Context, device:BluetoothDevice?):Intent {
            val intent = Intent(Intent.ACTION_VIEW, URI_DEVICE, context, MainActivity::class.java)
            intent.type = INTENT_TYPE_DEVICE
            if (device != null) {
                intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            }
            return intent
        }

    }

}
