package uni.bremen.conditionrecorder

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * A placeholder fragment containing a simple view.
 */
class ListRecordingsFragment : Fragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        with(activity as MainActivity) {
            with(getFab()) {
                title = resources.getString(R.string.recordings)
                setFullScreen(false)
                setDrawerEnabled(true)
                updateDrawerMenu(R.id.contentRecordings)

                setOnClickListener { _ ->
                    setContent(Content.RECORDER)
                }
                show()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_list_recordings, container, false)
    }

    companion object {
        val TAG = Content.RECORDINGS.name

        fun newInstance():ListRecordingsFragment = ListRecordingsFragment()
    }
}
