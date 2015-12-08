AirCast: Chromecast ANE for Adobe AIR
=====================================

A native extension for Adobe AIR that enables you to cast audio and video content to Chromecast devices from iOS apps, with Android (hopefully) coming soon.

Requires Adobe AIR 16+. 

iOS
---

Based on [Renaud Bardet's seemingly abandoned iOS-only ANE project](https://github.com/renaudbardet/ANE-chromecast), support for iOS is now largely complete, including:

* Support for [default Chromecast receiver app](https://developers.google.com/cast/docs/receiver_apps#default), no registration required
* Upgrade to the latest Google Cast SDK
* Support for amd64 and Adobe AIR 16, required for submission to the App Store

Android
-------

The current Android implementation, based on based on [cordova-chromecast](https://github.com/videostream/cordova-chromecast), is (in theory at least) nearly complete, but there are a few (hopefully minor) dependency issues to be resolved. 

Can you help?
