package com.backdoor.moove.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat

import com.backdoor.moove.core.consts.Prefs
import com.backdoor.moove.core.helper.SharedPrefs

/**
 * Copyright 2016 Nazar Suhovich
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
 */

class LocationTracker(private val mContext: Context?, private val mCallback: Callback?) : LocationListener {
    private var mLocationManager: LocationManager? = null

    init {
        updateListener()
    }

    override fun onLocationChanged(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        mCallback?.onChange(latitude, longitude)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        updateListener()
    }

    override fun onProviderEnabled(provider: String) {
        updateListener()
    }

    override fun onProviderDisabled(provider: String) {
        updateListener()
    }

    fun removeUpdates() {
        mLocationManager!!.removeUpdates(this)
    }

    private fun updateListener() {
        if (mContext == null) {
            return
        }
        if (ActivityCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mLocationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val prefs = SharedPrefs.getInstance(mContext)
        val time = (prefs!!.loadInt(Prefs.TRACK_TIME) * 1000).toLong()
        val distance = prefs.loadInt(Prefs.TRACK_DISTANCE)
        if (mLocationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, time, distance.toFloat(), this)
        } else {
            mLocationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, time, distance.toFloat(), this)
        }
    }

    interface Callback {
        fun onChange(lat: Double, lon: Double)
    }
}