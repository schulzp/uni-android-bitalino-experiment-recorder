package uni.bremen.conditionrecorder

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable

class RecorderFragment : Fragment() {

    private val recorderBus = RecorderBus()

    private val disposables:MutableMap<String, Disposable> = HashMap()

    override fun onResume() {
        super.onResume()

        disposables["video.events"] = recorderBus.eventSubject.subscribeOn(AndroidSchedulers.mainThread()).subscribe {
            if (it is RecorderBus.VideoRecordingStopped) {
                showToast(it.path)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        disposables.values.forEach { it.dispose() }
        disposables.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?)
            : View? = inflater.inflate(R.layout.fragment_recorder, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.beginTransaction()
                .replace(R.id.videoPlaceholder, RecorderVideoFragment.newInstance(recorderBus))
                .replace(R.id.controlsPlaceholder, RecorderControlsFragment.newInstance(recorderBus))
                .commit()
    }

    private fun showToast(message : String) = Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

    companion object {
        fun newInstance():RecorderFragment = RecorderFragment()
    }

}