/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("Constants")

package uni.bremen.conditionrecorder

import android.Manifest
import android.net.Uri

const val REQUEST_VIDEO_PERMISSIONS = 1
const val REQUEST_ENABLE_BT = 2
const val REQUEST_COARSE_LOCATION_PERMISSIONS = 3

const val SCAN_PERIOD: Long = 10000

const val FRAGMENT_DIALOG = "dialog"

const val INTENT_REQUEST_PICK_DEVICE = 0

const val INTENT_TYPE_DEVICE = "device"

const val URI_SCHEME = "content"
const val URI_AUTHORITY = "condition-recorder"

fun uri():Uri.Builder {
    return Uri.Builder().scheme(URI_SCHEME).authority(URI_AUTHORITY)
}

val VIDEO_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
val COARSE_LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)