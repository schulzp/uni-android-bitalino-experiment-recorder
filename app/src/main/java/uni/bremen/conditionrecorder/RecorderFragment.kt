package uni.bremen.conditionrecorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_recorder.*
import java.io.File


/**
 * A placeholder fragment containing a simple view.
 */
class RecorderFragment : Fragment() {

    private val TAG:String = "RecorderFragment"

    private var cameraDevice:CameraDevice? = null

    private val permissions = listOf(Manifest.permission.CAMERA)

    private val ORIENTATIONS: SparseIntArray = SparseIntArray()
    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    protected var cameraCaptureSession: CameraCaptureSession? = null
    protected var captureRequest: CaptureRequest? = null
    protected var captureRequestBuilder: CaptureRequest.Builder? = null

    private val file: File? = null
    private var imageReader: ImageReader? = null
    private var cameraId: String? = null
    private var imageDimension: Size? = null

    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_recorder, container, false)
    }

    private val textureListener: TextureView.SurfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            Log.i(TAG, "surface available")
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Transform you image captured size according to the surface width and height
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened")
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            closeCamera()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            closeCamera()
        }
    }
    val captureCallbackListener: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            Toast.makeText(this@RecorderFragment.context, "Saved:$file", Toast.LENGTH_SHORT).show()
            createCameraPreview()
        }
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread?.start()
        mBackgroundHandler = Handler(mBackgroundThread?.looper)
    }

    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try {
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "failed to stop background thread", e)
        }
    }

    private fun createCameraPreview() {
        try {
            val texture = previewOutput?.surfaceTexture;
            texture?.setDefaultBufferSize(imageDimension?.width as Int, imageDimension?.height as Int)
            val surface = Surface(texture);
            captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(surface)
            cameraDevice?.createCaptureSession(listOf(surface), object:CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession:CameraCaptureSession) {
                            //The camera is already closed
                            if (null == cameraDevice) {
                                return
                            }
                            // When the session is ready, we start displaying the preview.
                            this@RecorderFragment.cameraCaptureSession = cameraCaptureSession;
                            updatePreview()
                        }

                        override fun onConfigureFailed(cameraCaptureSession:CameraCaptureSession) {
                            Toast.makeText(this@RecorderFragment.context, "Configuration change", Toast.LENGTH_SHORT).show()
                        }
                    }, null)
            } catch (e: CameraAccessException) {
                 Log.e(TAG,"failed to create preview", e)
            }
    }

    private fun adjustAspectRatio(size:Size) {
        val viewWidth = previewOutput.width
        val viewHeight = previewOutput.height
        val aspectRatio = size.height.toDouble() / size.width

        val newWidth: Int
        val newHeight: Int
        if (viewHeight > (viewWidth * aspectRatio).toInt()) {
            // limited by narrow width; restrict height
            newWidth = viewWidth
            newHeight = (viewWidth * aspectRatio).toInt()
        } else {
            // limited by short height; restrict width
            newWidth = (viewHeight / aspectRatio).toInt()
            newHeight = viewHeight
        }
        val xoff = (viewWidth - newWidth) / 2f
        val yoff = (viewHeight - newHeight) / 2f
        Log.v(TAG, "video=" + size.width + "x" + size.height +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff)

        val txform = Matrix()
        previewOutput.getTransform(txform)
        txform.setScale(newWidth.toFloat() / viewWidth, newHeight.toFloat() / viewHeight)
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff)
        previewOutput.setTransform(txform)
    }

    private fun openCamera() {
        val manager = this@RecorderFragment.activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Log.e(TAG, "is camera open")
        try {
            cameraId = manager.cameraIdList[0]
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            if (imageDimension != null) {
                adjustAspectRatio(imageDimension as Size)
            }

            // Add permission for camera and let user grant the permission
            if (!checkPermissions()) {
                return
            }
            manager.openCamera(cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "failed to access camera", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "insufficient permissions", e)
        }

        Log.e(TAG, "openCamera X")
    }

    private fun updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return")
        }
        captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSession?.setRepeatingRequest(captureRequestBuilder?.build(), null, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "failed to update preview", e)
        }

    }

    private fun closeCamera() {
        cameraDevice?.close()
        cameraDevice = null

        imageReader?.close()
        imageReader = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode < permissions.size) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(this@RecorderFragment.context, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show()
                this@RecorderFragment.activity.finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e(TAG, "onResume")
        startBackgroundThread()
        if (previewOutput != null && previewOutput?.isAvailable as Boolean) {
            openCamera()
        } else {
            Log.i(TAG,"setting texture listener: $previewOutput -> $textureListener")
            previewOutput?.surfaceTextureListener = textureListener
        }
    }

    override fun onPause() {
        Log.e(TAG, "onPause")
        //closeCamera();
        stopBackgroundThread()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        closeCamera()
    }

    private fun checkPermissions(): Boolean {
        return permissions.mapIndexed { index, permission ->
            if (ContextCompat.checkSelfPermission(this.context, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions( this.activity, arrayOf(permission), index)
                return false
            }
            return true
        }.all { granted -> granted }
    }

}
