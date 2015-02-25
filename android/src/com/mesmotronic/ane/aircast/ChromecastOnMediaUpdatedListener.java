package com.mesmotronic.ane.aircast;

import org.json.JSONObject;

public interface ChromecastOnMediaUpdatedListener 
{
	void onMediaLoaded(JSONObject media);
	void onMediaUpdated(boolean isAlive, JSONObject media);
}