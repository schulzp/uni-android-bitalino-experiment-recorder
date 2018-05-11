package uni.bremen.conditionrecorder

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.recyclerview.selection.StorageStrategy
import java.util.*

class RecordingAdapter(context: Context)
    : GenericRecycleViewAdapter<Recording, Long, RecordingAdapter.RecordingViewHolder>(context, LinkedList()) {

    override val itemKeyProvider = RecordingKeyProvider()

    override val itemKeyStorageStrategy = StorageStrategy.createLongStorage()

    override fun getItemViewResourceId(viewType: Int): Int = R.layout.item_recording

    override fun onBindViewHolder(holder: RecordingViewHolder, item:Recording, position: Int) {
        with(holder) {
            title.text = item.subject
            subtitle.text = item.condition
            date.text = item.start.toString()

            itemDetails.position = position
            itemDetails.selectionKey = item.start.time
        }
    }

    override fun createViewHolder(view: View): RecordingViewHolder {
        return RecordingViewHolder(view,
                view.findViewById(android.R.id.text1) as TextView,
                view.findViewById(android.R.id.text2) as TextView,
                view.findViewById(R.id.text3) as TextView)
    }

    inner class RecordingKeyProvider : GenericItemKeyProvider<Long>(0) {

        override fun getKey(position: Int): Long = items[position].start.time

        override fun getPosition(key: Long): Int {
            return items
                    .find { recording -> recording.start.time == key }
                    ?.let { recording -> items.indexOf(recording) } ?: -1
        }

    }

    class RecordingViewHolder(view:View, val title: TextView, val subtitle: TextView, val date: TextView)
        : GenericRecycleViewAdapter.GenericViewHolder<Long>(view, GenericItemDetails(0, -1))

}