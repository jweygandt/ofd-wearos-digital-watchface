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
package com.ofd.watchface.location

import android.location.Location
import android.text.format.DateUtils
import com.ofd.apis.APILocation
import com.patloew.colocation.CoGeocoder

sealed class LocationResult {
    companion object

//    fun describeLocation(): String {
//        return when (this) {
//            is ResolvedLocation -> getAddressDescription()
//            else -> "Unknown"
//        }
//
//    }
}

object Unknown : LocationResult()

object PermissionError : LocationResult()

object NoLocation : LocationResult()

data class LocationError(val e: Exception) : LocationResult()

data class ResolvedLocation(val location: Location?, private val coGeocoder: CoGeocoder?) :
    LocationResult(), APILocation {

    override val latitude: Double
        get() = if (location != null) location.latitude else 0.0

    override val longitude: Double
        get() = if (location != null) location.longitude else 0.0

    val timeAgo: CharSequence
        get() = if (location != null) DateUtils.getRelativeTimeSpanString(location.time) else "Error"

    val valid = location != null

    suspend fun getAddress() =
        if (location != null && coGeocoder != null) coGeocoder.getAddressFromLocation(
            location
        ) else null

    override suspend fun getShortAddress(): String {
        val address = getAddress()
        if (address == null) return "Null Island"
        val city = address.locality
        if (city != null) return city
        val state = address.adminArea
        if(state != null) return state
        return "Null Island"
    }

    suspend fun getAddressDescription(): String {
        val address = getAddress()
        if (address == null) return "Null Island"
        val state = address.adminArea
        val city = address.locality
        if (state != null && city != null) return city + ", " + state
        return "Null Island"
    }
}
