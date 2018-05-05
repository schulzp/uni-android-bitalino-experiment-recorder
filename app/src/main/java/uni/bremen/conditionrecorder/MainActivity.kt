package uni.bremen.conditionrecorder

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.support.design.widget.NavigationView



class MainActivity : AppCompatActivity() {

    enum class Content { RECORDINGS, RECORDER, DEVICES }

    private val contentStack:Deque<Content> = ArrayDeque()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        supportActionBar.also {
            it?.setHomeButtonEnabled(true)
            it?.setTitle("Moin!")
            it?.setSubtitle("Tolle Wurst...")
        }

        fab.setOnClickListener { _ ->
            setContent(Content.RECORDER)
        }

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        var menuId:Int = when (contentStack.peek()) {
            Content.RECORDINGS -> R.menu.menu_list_recordings
            else -> -1
        }

        if (menuId > 0) {
            menuInflater.inflate(menuId, menu)
            return true
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setContent(content:Content) {
        var orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        val fragment: Fragment = when(content) {
            Content.RECORDINGS -> {
                fab.show()
                ListRecordingsFragment.newInstance()
            }
            Content.RECORDER -> {
                fab.hide()
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                RecorderFragment.newInstance()
            }
            Content.DEVICES -> {
                fab.hide()
                ListDevicesFragment.newInstance()
            }
        }

        requestedOrientation = orientation
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_fragment, fragment)
                .commit()

        this.contentStack.push(content)
    }

    override fun onBackPressed() {
        if (!contentStack.isEmpty()) {
            setContent(contentStack.pop())
        } else {
            super.onBackPressed()
        }
    }

}
