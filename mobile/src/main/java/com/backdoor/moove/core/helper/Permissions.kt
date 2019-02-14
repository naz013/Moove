package com.backdoor.moove.core.helper

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build


/**
 * Copyright 2015 Nazar Suhovich
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Helper class for checking app permissions on Android 6.0 Marshmallow and above.
 */

@TargetApi(Build.VERSION_CODES.M)
object Permissions {

    /**
     * Permission constants.
     */
    val READ_CONTACTS = Manifest.permission.READ_CONTACTS
    val GET_ACCOUNTS = Manifest.permission.GET_ACCOUNTS

    val READ_CALENDAR = Manifest.permission.READ_CALENDAR
    val WRITE_CALENDAR = Manifest.permission.WRITE_CALENDAR

    val WRITE_EXTERNAL = Manifest.permission.WRITE_EXTERNAL_STORAGE
    val READ_EXTERNAL = Manifest.permission.READ_EXTERNAL_STORAGE

    val ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
    val ACCESS_COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION

    val READ_PHONE_STATE = Manifest.permission.READ_PHONE_STATE
    val CALL_PHONE = Manifest.permission.CALL_PHONE

    val SEND_SMS = Manifest.permission.SEND_SMS

    val MANAGE_DOCUMENTS = Manifest.permission.MANAGE_DOCUMENTS

    fun checkPermission(a: Activity, vararg permissions: String): Boolean {
        if (!Module.isMarshmallow) {
            return true
        }
        var res = true
        for (string in permissions) {
            if (a.checkSelfPermission(string) != PackageManager.PERMISSION_GRANTED) {
                res = false
            }
        }
        return res
    }

    fun checkPermission(a: Activity, permission: String): Boolean {
        return !Module.isMarshmallow || a.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(a: Activity, requestCode: Int, vararg permission: String) {
        if (!Module.isMarshmallow) {
            return
        }
        val size = permission.size
        if (size == 1) {
            a.requestPermissions(permission, requestCode)
        } else {
            val array = arrayOfNulls<String>(size)
            System.arraycopy(permission, 0, array, 0, size)
            a.requestPermissions(array, requestCode)
        }
    }

    fun showInfo(a: Activity, permission: String) {
        if (!Module.isMarshmallow) {
            return
        }
        a.shouldShowRequestPermissionRationale(permission)
    }
}