// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.ofd.digital.alpha.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.location.LocationRequest
import com.patloew.colocation.CoGeocoder
import com.patloew.colocation.CoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class LocationViewModel(private val loc : String, private val applicationContext: Context) {
    private val coGeocoder = CoGeocoder.from(applicationContext)
    private val coLocation = CoLocation.from(applicationContext)

    var iid = ictr.incrementAndGet()
    var callcnt = AtomicInteger()

    suspend fun readLocationResult(): LocationResult {
        if (applicationContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return PermissionError
        }

        return withContext(Dispatchers.IO) {
            var callid = callcnt.incrementAndGet()
            Log.d(TAG, "starting to get location:${loc}:${iid}:${callid}")
            val location =
                coLocation.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
            Log.d(TAG, "location:${iid}:${callid}="+location)

            if (location == null) {
                NoLocation
            } else {
                val address = coGeocoder.getAddressFromLocation(location)

                if (address == null) {
                    Unknown
                } else {
                    ResolvedLocation(location, address)
                }
            }
        }
    }
    companion object{
        var TAG = "LocationViewModel"
        var ictr = AtomicInteger(1)
    }
}
