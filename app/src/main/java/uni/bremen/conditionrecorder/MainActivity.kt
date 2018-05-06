package uni.bremen.conditionrecorder

import android.bluetooth.BluetoothDevice
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var contentBackStackIdentifier:Int? = -1

    private lateinit var supportActionBarDrawerToggle:ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        Log.d(TAG, "create main activity: ${intent.getSerializableExtra(EXTRA_SHOW_CONTENT)} ${intent.getParcelableExtra<BluetoothDevice>(DeviceFragment.EXTRA_DEVICE)}")

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

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

        setContent(intent.getSerializableExtra(EXTRA_SHOW_CONTENT) as? Content ?: Content.RECORDINGS)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        supportActionBarDrawerToggle.syncState()
        super.onPostCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        supportActionBarDrawerToggle.onConfigurationChanged(newConfig)
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
        supportActionBarDrawerToggle.onOptionsItemSelected(item)

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

            supportActionBarDrawerToggle.isDrawerIndicatorEnabled = value

            toolbar.setNavigationOnClickListener { _ -> drawerLayout.openDrawer(GravityCompat.START) }
        } else {
            supportActionBarDrawerToggle.isDrawerIndicatorEnabled = value

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

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {

        const val TAG = "MainActivity"

    }

}
