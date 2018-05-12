package uni.bremen.conditionrecorder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_list.*


/**
 * List of recordings
 */
class ListRecordingsFragment : ContentFragment(Content.RECORDINGS, R.string.recordings), GenericRecycleViewAdapter.OnItemClickListener<Recording> {

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

        setupList()

        for (i in 0..20) {
            val item = Recording(i.toLong(),"Subject $i", "XYZ")
            adapter.add(item)
        }

        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()

        withObserver()?.onContentResumed(this)
    }

    private fun setupList() {
        adapter = RecordingAdapter(activity!!)
        adapter.onItemClickListener = this
        RecycleViewHelper.verticalList(list, activity!!).adapter = adapter
    }

    override fun onItemSelected(item: Recording): Boolean {
        Log.d(TAG, "got activated")

        return true
    }

    companion object {

        const val SELECTION_ID = "recordings-selection"

        val TAG = Content.RECORDINGS.name

    }
}
