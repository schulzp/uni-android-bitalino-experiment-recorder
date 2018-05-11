package uni.bremen.conditionrecorder

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.*
import kotlinx.android.synthetic.main.fragment_list.*


/**
 * List of recordings
 */
class ListRecordingsFragment : ContentFragment(Content.RECORDINGS, R.string.recordings), GenericRecycleViewAdapter.OnItemSelectedListener<Recording> {

    private lateinit var adapter: RecordingAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_list, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        withObserver()?.onContentCreated(this)

        with(activity as MainActivity) {
            with(getFab()) {
                setOnClickListener { _ -> setContent(Content.RECORDER) }
                show()
            }
        }

        adapter = RecordingAdapter(activity!!)

        setupList()

        for (i in 0..20) {
            adapter.add(Recording("Subject $i", "XYZ"))
        }

        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()

        withObserver()?.onContentResumed(this)
    }

    private fun setupList() {
        RecycleViewHelper.verticalList(list, activity!!).adapter = adapter
        adapter.createSelectionTracker(list, SELECTION_ID)
                .withOnItemActivatedListener(adapter.createActivationListener(this))
                .build()
    }

    override fun onItemSelected(item: Recording, details: ItemDetailsLookup.ItemDetails<*>): Boolean {
        Log.d(TAG, "got activated $item)")
        return true
    }

    companion object {

        const val SELECTION_ID = "recordings-selection"

        val TAG = Content.RECORDINGS.name

    }
}
