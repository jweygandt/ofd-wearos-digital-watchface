# Philosophy

The first IPhone had a paltry resolution of 320x480, the Galaxy Watch 5 has a resolution of 450x450,
about 30% more pixels. The first digital watch used 7 segment LED displays. So why are we treating
it as just a watch? The word "watch" is about as descriptive for a Galaxy Watch 5 as "phone" is to a
modern Android phone.

If you think about it the "watch face" is simply the place for a default program to run and be
displayed when nothing else is running on the watch (e.g. media player, exercise...). And it has 2
main modes AMBIENT and INTERACTIVE. So while it displays date and time, it should also be a
dashboard of information plus a primary interaction panel (e.g. app launcher).

So with that in mind, I'm breaking some rules and hacking away...

# Recent Changes

1/15/22 Did a big cleanup and refactor, even changed the application id. It looks better than the
1/14 code, IT COMPILES, BUT HAS NOT BEEN TESTED, so likely will have some bugs.

1/14/22 At the moment the code is really hacked - very much so!, and will be cleaned up considerably
later, but I'm feeling a need to push the changes to the cloud.

# Play/Pause

A really big feature being added is the ability to pause/resume music with different methods.
Imagine listening to Spotify, on a ski slope, mittens on. You really cannot navigate to Spotify, if
needed, and then have the precision to tap the pause button. Right now by placing Virutal Play/Pause
Complication in position 7 you can toggle it to 3 modes: 1)stop-disabled, 2)play-tapenabled, 3)
pause-tap and visibility enabled. So with tapenabled you simply tap the screen anywhere other than
complicaiton 7 or the music playing icon and it will toggle play/pause. With visibility enabled,
when the watch face becomes visible play pauses, and when not visible play resumes. Allowing you to
run spotify the app, and use buttons to bring up watch face, and (customised) double press to go to
last app. Pause/resume by the button push. I should note it needs the phone app installed on the
phone, and it need to be started, but not necessarlly visible
(actually that is a big hack right now as well)

# Virtual Complications

In the sense of location aware complications (described below), battery and the play/pause are
special as well. So I have created my own version of Virtual Complications (VComps). Still a work in
progress.

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
    * calendar, which seems to have data hidden, thanks Samsung for your special API only you know
      about!
  * 2 range
    * Steps and AQI
  * 1 large full image background
    * Photo images of the moon phase

* Currently this is for an audience of 1, so many shortcuts, if others are interested, I'm
  interested in hearing from them.

NOTE - this is still a HACK and WORK IN PROGRESS, but you are welcome to look at, copy, and perhaps
suggest changes. I do use the watch face daily. I have not given much thought to the previews and
various app/complication icons in editor mode, as I only use that briefly.

I noticed that images, called monochromatic... are really in full color, and that color could be
supported. So why not.

I do wish Samsung did better with the complications they deliver, when used with 3rd party watch
faces, but they don't! So I'm also doing some complications myself. You can look at HonestHeartRate.
Their calendar complication works differently in their watch faces as well!!

As to editing - That seems more complex than it should. For a while I was using the phone app, and
it worked, then suddenly most of the complications disappeared, even for Samsung's watchfaces! So I
have buttons. It's functional

I tries https://github.com/google/where-am-i and realized that locations don't update very
reliabily. Tested on Galaxy Watch 5. Perhaps some power/permission management for complications,
likely througly undocumented (if not I'd like to read about it). I have seen that location works
fine, if Priority is set correctly (e.g. LocationRequest.PRIORITY_HIGH_ACCURACY), if it is run in
the process of the WatchFace. Unfortunately that means the complications cannot be standalone.

Included are Sunrise/Sunset and 2 different AQI methods. I'm currently using OpenWeatherAQI.
Eventually I'd like to do a weather complication that uses location all the time, but does not
report to 3rd parties. There is also some long text complications for testing.

Also I decided to have some fun with rendering the range complications, by providing some URL like
parameters, see the AQI complications. TODO - I should actually pull these parameters out into first
class VirtualComplication arguments of sorts.

Thanks to:

* https://github.com/odbol/air-quality-complication
* https://github.com/google/where-am-i
* https://github.com/SebastianSarbu/PizzaWatchFace - for going down the path of "lots of
  complications"
* https://github.com/VladimirWrites/AnalogWatchFace - for simply having some additional open source
  samples of wear-os
* Of course google and wear-os and samples
* Less to Samsung for doing thier own version of Virtual Complications, although I'm sure they
  partnered with Google for wear-os, and of course the hardware

So still lots to do, but I'm making it public as there are so few wear os open source projects so
far.

You will need a res/values/api_kes.xml like this (in .gitignore):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="purpleair_api_key_read">XXX</string>
    <string name="purpleair_api_key_write">XXX</string>

    <string name="openweather_appid">XXX</string>
</resources>
.```
