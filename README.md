# Goals:
* No data collection by 3rd parties, why should a watch face and complications send data to others?
* Digital time to the second, with day-of-week and date
* Location aware complications (seen more below)
* As many complications as reasonably possible
  * 4 for display short text and icon
    * Weather, sunrise/sunset, battery, heartrate (I use mine, not Samsungs)
  * 4 for icon only, generally tap-to-launch
    * app and contacts shortcuts
  * 1 large multi-line text
    * calendar, which seems to have data hidden, thanks Samsung for your special API only you know about!
  * 2 range 
    * Steps and AQI
  * 1 large full image background
    * Photo images of the moon phase
    
* Currently this is for an audience of 1, so many shortcuts if others are interested, I'm interested in hearing from them. 

NOTE - this is still a HACK and WORK IN PROGRESS, but you are welcome to look at,
copy, and perhaps suggest changes. I do use the watch face daily.

I noticed that images, called monochromatic... are really in full color, and that
color could be supported. So why not.

I do wish Samsung did better with the complications they deliver, when used with
3rd party watch faces, but they don't! So I'm also doing some complications myself.
You can look at HonestHeartRate. Their calendar complication works differently in their
watch faces as well!!

As to editing - That seems more complex than it should. For a while I was using
the phone app, and it worked, then suddenly most of the complications disappeared,
even for Samsung's watchfaces! So I have buttons. It's functional

I tries https://github.com/google/where-am-i and realized that locations don't update very reliabily.
Tested on Galaxy Watch 5. Perhaps some power/permission management for complications, likely througly 
undocumented (if not I'd like to read about it). I have seen that location works fine, if Priority
is set correctly (e.g. LocationRequest.PRIORITY_HIGH_ACCURACY), if it is run in the process of the
WatchFace. Unfortunately that means the complications cannot be standalone.

Included are Sunrise/Sunset and 2 different AQI methods. I'm currently using OpenWeatherAQI. Eventually
I'd like to do a weather complication that uses location all the time, but does not report to 3rd 
parties. There is also some long text complications for testing.

Also I decided to have some fun with rendering the range complications, by providing some URL like
parameters, see the AQI complications. 

Thanks to:
* https://github.com/odbol/air-quality-complication
* https://github.com/google/where-am-i

So still lots to do, but I'm making it public as there are so few wear os
open source projects so far.

You will need a res/values/api_kes.xml like this (in .gitignore):
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="purpleair_api_key_read">XXX</string>
    <string name="purpleair_api_key_write">XXX</string>

    <string name="openweather_appid">XXX</string>
</resources>
.```
