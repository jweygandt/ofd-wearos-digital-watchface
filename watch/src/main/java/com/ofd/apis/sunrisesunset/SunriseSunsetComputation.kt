package com.ofd.apis.sunrisesunset

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import org.shredzone.commons.suncalc.MoonTimes
import org.shredzone.commons.suncalc.SunTimes

// DO NOT CHANGE THESE WITHOUT FIXING SunriseSetActivity
enum class SSTYPE { ASTRONOMICAL, NAUTICAL, CIVIL, TOTAL, MOON }
enum class SSDIR { UP, DOWN }

data class SSData(val type: SSTYPE, val dir: SSDIR, val time: ZonedDateTime?) : Comparable<SSData> {
    override fun compareTo(other: SSData): Int {

        if (time == null && other.time == null) {
            return if (dir == other.dir) {
                if (dir == SSDIR.UP) type.compareTo(other.type)
                else -type.compareTo(other.type)
            } else dir.compareTo(other.dir)
        }

        if (time == null) {
            return if (dir == other.dir) {
                if (dir == SSDIR.UP) 1 else -1
            } else {
                dir.compareTo(other.dir)
            }
        } else if (other.time == null) {
            return if (other.dir == dir) {
                if (other.dir == SSDIR.UP) -1 else 1
            } else {
                dir.compareTo(other.dir)
            }
        }
        return time.compareTo(other.time)
    }
}


fun main(args: Array<String>) {
//    val dateTime = ZonedDateTime.now(ZoneId.of("CET")).truncatedTo(ChronoUnit.DAYS)
    val dateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
    println("now=" + dateTime)
    println()
//    val lat = 69.65 //37.22
//    val lng = 18.95 //-121.967
    val lat = 37.22
    val lng = -121.967
    compute(dateTime, lat, lng)
}

public fun compute(dateTime: ZonedDateTime, lat: Double, lng: Double) : List<SSData> {
    val data = mutableListOf<SSData>()
    var times = SunTimes.compute().on(dateTime)   // set a date
        .oneDay()
        .at(lat, lng)   // set a location
        .execute();     // get the results
    System.out.println("Sunrise: " + times.getRise());
    System.out.println("Sunset: " + times.getSet());
    data.add(SSData(SSTYPE.TOTAL, SSDIR.UP, times.rise))
    data.add(SSData(SSTYPE.TOTAL, SSDIR.DOWN, times.set))
    println()

    times = SunTimes.compute().on(dateTime)   // set a date
        .oneDay()
        .at(lat, lng)   // set a location
        .twilight(SunTimes.Twilight.CIVIL).execute();     // get the results
    System.out.println("Sunrise: " + times.getRise());
    System.out.println("Sunset: " + times.getSet());
    data.add(SSData(SSTYPE.CIVIL, SSDIR.UP, times.rise))
    data.add(SSData(SSTYPE.CIVIL, SSDIR.DOWN, times.set))
    println()

    times = SunTimes.compute().on(dateTime)   // set a date
        .oneDay()
        .at(lat, lng)   // set a location
        .twilight(SunTimes.Twilight.NAUTICAL).execute();     // get the results
    System.out.println("Sunrise: " + times.getRise());
    System.out.println("Sunset: " + times.getSet());
    data.add(SSData(SSTYPE.NAUTICAL, SSDIR.UP, times.rise))
    data.add(SSData(SSTYPE.NAUTICAL, SSDIR.DOWN, times.set))
    println()

    times = SunTimes.compute().on(dateTime)   // set a date
        .oneDay()
        .at(lat, lng)   // set a location
        .twilight(SunTimes.Twilight.ASTRONOMICAL).execute();     // get the results
    System.out.println("Sunrise: " + times.getRise());
    System.out.println("Sunset: " + times.getSet());
    data.add(SSData(SSTYPE.ASTRONOMICAL, SSDIR.UP, times.rise))
    data.add(SSData(SSTYPE.ASTRONOMICAL, SSDIR.DOWN, times.set))
    println()

    var mtimes = MoonTimes.compute().on(dateTime)   // set a date
        .oneDay()
        .at(lat, lng)   // set a location
        .execute();     // get the results
    System.out.println("Sunrise: " + mtimes.getRise());
    System.out.println("Sunset: " + mtimes.getSet());
    data.add(SSData(SSTYPE.MOON, SSDIR.UP, mtimes.rise))
    data.add(SSData(SSTYPE.MOON, SSDIR.DOWN, mtimes.set))
    println()

    val r =    data.sorted()
    r.forEach { println(it) }
    return r
}
