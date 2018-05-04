package uni.bremen.conditionrecorder

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    enum class Content { LIST, RECORDER }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { _ ->
            showContent(Content.RECORDER)
        }

        showContent(Content.LIST)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_list_recordings, menu)
        return true
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

    private fun showContent(content:Content) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        val fragment: Fragment? = when(content) {
            Content.LIST -> {
                ListRecordingsFragment()
            }
            Content.RECORDER -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                RecorderFragment()
            }
        }

        if (fragment != null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_fragment, fragment)
                    .commit()
        }
    }

}
