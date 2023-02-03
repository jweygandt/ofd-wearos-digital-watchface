package com.ofd.apis

interface APILocation {
    val latitude: Double
    val longitude: Double
    suspend fun getShortAddress(): String
}

class FakeLocation : APILocation {
    override val latitude: Double
        get() = 37.0
    override val longitude: Double
        get() = -122.0

    override suspend fun getShortAddress(): String {
        return "Faky Island"
    }
}
