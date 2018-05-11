package uni.bremen.conditionrecorder

import android.content.Context
import android.content.pm.PackageManager

class RequiredFeatures {

    class MissingFeatureException(featrue:String) : Exception("Feature missing: $featrue")

    companion object {

        @Throws(MissingFeatureException::class)
        fun check(context: Context) {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                throw MissingFeatureException(PackageManager.FEATURE_BLUETOOTH)
            }

            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                throw MissingFeatureException(PackageManager.FEATURE_BLUETOOTH_LE)
            }
        }

    }

}