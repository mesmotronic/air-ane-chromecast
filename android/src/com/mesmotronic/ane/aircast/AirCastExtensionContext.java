package com.mesmotronic.ane.aircast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;
import com.adobe.fre.FREWrongThreadException;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;

public class AirCastExtensionContext 
	extends FREContext 
	implements ChromecastOnMediaUpdatedListener, ChromecastOnSessionUpdatedListener
{
	private static final String SETTINGS_NAME = "AirCastSettings";
	
	private static final String EVENT_DEVICE_LIST_CHANGED = "AirCast.deviceListChanged";
	private static final String EVENT_CONNECT_TO_DEVICE = "AirCast.didConnectToDevice";
	private static final String EVENT_DISCONNECTED = "AirCast.didDisconnect";
	private static final String EVENT_RECEIVED_MEDIA_STATE_CHANGE = "AirCast.didReceiveMediaStateChange";
	private static final String EVENT_RECEIVED_CUSTOM_EVENT = "AirCast.didReceiveCustomEvent";
	private static final String EVENT_LOGGING = "LOGGING";
	
	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private volatile ChromecastMediaRouterCallback mMediaRouterCallback = new ChromecastMediaRouterCallback();
	private String appId;
	
	private boolean isScanning = false;
	private boolean autoConnect = false;
	private String lastSessionId = null;
	private String lastAppId = null;
	
	private SharedPreferences settings;
	
	private volatile ChromecastSession currentSession;
	
	public AirCastExtensionContext() {}
	
	/*
	 * NATIVE EXTENSION API
	 */
	
	/**
	 * Initialize the ANE
	 */
	private BaseFunction initNE = new BaseFunction()
	{
		@Override 
		public FREObject call(final FREContext context, FREObject[] args) 
		{
			final Activity activity = getActivity();
			
			settings = activity.getSharedPreferences(SETTINGS_NAME, 0);
			lastSessionId = settings.getString("lastSessionId", "");
			lastAppId = settings.getString("lastAppId", "");
			
			appId = getStringFromFREObject(args[0]);
			
			mMediaRouter = MediaRouter.getInstance(activity);
			
			mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(CastMediaControlIntent.categoryForCast(appId))
				.build();
			
			log("initialize AirCast with app ID "+appId);
			
			activity.runOnUiThread(new Runnable() 
			{
				public void run() 
				{
					mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
					mMediaRouteSelector = new MediaRouteSelector.Builder()
						.addControlCategory(CastMediaControlIntent.categoryForCast(appId))
						.build();
					
					mMediaRouterCallback.registerCallbacks(AirCastExtensionContext.this);
					mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
					
					isScanning = true;
					
					emitAllRoutes();
				}
			});
			
			return null;
		}
	};
	
	/**
	 * TODO Implement this!
	 */
	private BaseFunction scan = new BaseFunction() 
	{
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			if (!isScanning)
			{
				mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
			}
			
			isScanning = true;
			
			emitAllRoutes();
			
			return null;
		}	
	};
	
	/**
	 * TODO Implement this!
	 */
	private BaseFunction stopScan = new BaseFunction() 
	{
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			if (isScanning)
			{
				mMediaRouter.removeCallback(mMediaRouterCallback);
			}
			
			isScanning = false;
			
			return null;
		}
	};
	
	/**
	 * Connect to a Chromecast device
	 */
	private BaseFunction connectToDevice = new BaseFunction() 
	{
		@Override 
		public FREObject call(final FREContext context, FREObject[] args) 
		{
			final String routeId = getStringFromFREObject(args[0]);
			
			if (currentSession != null)
			{
				// TODO Should we disconnect and create a new session?
				
				return null;
			}
			
			setLastSessionId("");
			
			final Activity activity = getActivity();
			
			activity.runOnUiThread(new Runnable()
			{
				public void run() 
				{
					mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
					final List<RouteInfo> routeList = mMediaRouter.getRoutes();
					
					for (RouteInfo route : routeList) 
					{
						if (route.getId().equals(routeId)) 
						{
							createSession(route);
							return;
						}
					}
					
					// TODO Dispatch "No route found" error?
					
				}
			});
			
			return null;
		}		
	};
	
	/**
	 * Helper for the creating of a session! The user-selected RouteInfo needs to be passed to a new ChromecastSession 
	 * @param route
	 * @param callbackContext
	 */
	private void createSession(final RouteInfo route) 
	{
		currentSession = new ChromecastSession(route, this, this, this);
		
		// Launch the app.
		currentSession.launch(appId, new ChromecastSessionCallback() 
		{
			@Override
			void onSuccess(Object object) 
			{
				ChromecastSession session = (ChromecastSession) object;
				
				if (object == null) 
				{
					onError("unknown");
				}
				else if (session == currentSession)
				{
					setLastSessionId(currentSession.getSessionId());
					currentSession.createSessionObject();
					
					dispatchStatus(EVENT_CONNECT_TO_DEVICE, routeToJSON(route));
				}
			}
			
			@Override
			void onError(String reason) 
			{
				if (reason != null) 
				{
					// log("createSession onError " + reason);
					// TODO Somehow return error to AIR app
				} 
				else 
				{
					// TODO Somehow return unknown error to AIR app
				}
			}
			
		});
	}
	
	private void joinSession(RouteInfo routeInfo) 
	{
		ChromecastSession sessionJoinAttempt = new ChromecastSession(routeInfo, this, this, this);
		
		sessionJoinAttempt.join(this.appId, this.lastSessionId, new ChromecastSessionCallback() 
		{
			@Override
			void onSuccess(Object object) 
			{
				if (currentSession == null) 
				{
					try 
					{
						currentSession = (ChromecastSession) object;
						setLastSessionId(currentSession.getSessionId());
						
						dispatchStateChange();
						
//						sendJavascript("chrome.cast._.sessionJoined(" + currentSession.createSessionObject().toString() + ");");
					} 
					catch (Exception e) 
					{
						log(e.getMessage());
					}
				}
			}
			
			@Override
			void onError(String reason) 
			{
//				log("sessionJoinAttempt error " +reason);
			}
			
		});
	}
	
	/**
	 * Disconnect from Chromecast device
	 */
	private BaseFunction disconnectFromDevice = new BaseFunction() 
	{
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			mMediaRouter.selectRoute(null);
			return null;
		}		
	};
	
	/**
	 * Load media on the connected Chromecast device
	 */
	private BaseFunction loadMedia = new BaseFunction() 
	{
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			if (currentSession != null) 
			{
				// url, thumbnailURL, title, desc, mimeType, startTime, autoPlay;
				
				String contentId = getStringFromFREObject(args[0]);
				String contentType = getStringFromFREObject(args[4]);
				Long duration = MediaInfo.UNKNOWN_DURATION;
				String streamType = ""; // TODO Make this auto detect?
				Boolean autoPlay = getBooleanFromFREObject(args[6]);
				Double currentTime = getDoubleFromFREObject(args[5]); 
				
				Map<String, String> matadataMap = new HashMap<String, String>();
				matadataMap.put("title", getStringFromFREObject(args[2]));
				matadataMap.put("subtitle", getStringFromFREObject(args[3]));
				matadataMap.put("thumbnail", getStringFromFREObject(args[1]));
				
				JSONObject metadata = new JSONObject(matadataMap);
				
				currentSession.loadMedia
				(
					contentId, 
					contentType, 
					duration, 
					streamType, 
					autoPlay, 
					currentTime, 
					metadata,
				
					new ChromecastSessionCallback() 
					{
						@Override
						void onSuccess(Object obj) 
						{
							if (obj == null) 
							{
								onError("unknown");
							}
							else 
							{
								dispatchStatus(EVENT_RECEIVED_MEDIA_STATE_CHANGE, (JSONObject)obj);
							}
						}
						
						@Override
						void onError(String reason)
						{
							// TODO Error event to AIR
						}
					}
				);
			}
			else 
			{
				// TODO Error event to AIR; "session_error"
			}
			
			return null;
		}		
	};
	
	/**
	 * Are we currently connected to a Chromecast device?
	 */
	private BaseFunction isConnected = new BaseFunction() 
	{
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			try 
			{
				return FREObject.newObject(currentSession != null);
			}
			catch (FREWrongThreadException e) 
			{
				return null;
			}
		}
	};
	
	/**
	 * TODO Implement this
	 */
	private BaseFunction isPlayingMedia = new BaseFunction() 
	{ 
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			return null;
		}
	};
	
	/**
	 * Play on the current media in the current session
	 */
	private BaseFunction playCast = new BaseFunction()
	{ 
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			if (currentSession != null) 
			{
				currentSession.mediaPlay(stateChangeCallback);
			}
			
			return null;
		}
	};
	
	/**
	 * Pause on the current media in the current session
	 */
	private BaseFunction pauseCast = new BaseFunction() 
	{ 
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			if (currentSession != null) 
			{
				currentSession.mediaPause(stateChangeCallback);
			}
			
			return null;
		}		
	};
	
	/**
	 * TODO Implement this
	 */
	private BaseFunction updateStatsFromDevice = new BaseFunction() 
	{ 
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			return null;
		}		
	};
	
	/**
	 * Seeks the current media in the current session
	 */
	private BaseFunction seek = new BaseFunction()
	{ 
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			if (currentSession != null) 
			{
				Double seekTime = getDoubleFromFREObject(args[0]); 
				String resumeState = "";
				
				currentSession.mediaSeek(seekTime.longValue() * 1000, resumeState, stateChangeCallback);
			}
			
			return null;
		}
	};
	
	/**
	 * Stop the currently playing media
	 */
	private BaseFunction stopCast = new BaseFunction() 
	{ 
		@Override public FREObject call(FREContext context, FREObject[] args) 
		{
			if (currentSession != null) 
			{
				currentSession.mediaStop(stateChangeCallback);
			}
			
			return null;
		}
	};
	
	/**
	 * Set volume on device
	 */
	private BaseFunction setVolume = new BaseFunction() 
	{ 
		@Override public FREObject call(FREContext context, FREObject[] args) 
		{
			if (currentSession != null) 
			{
				currentSession.setVolume(getDoubleFromFREObject(args[0]), stateChangeCallback);
			} 
			
			return null;
		}
	};
	
	/**
	 * Mute the device
	 */
	private BaseFunction setMuted = new BaseFunction() 
	{ 
		@Override public FREObject call(FREContext context, FREObject[] args) 
		{
			if (currentSession != null) 
			{
				Boolean muted = getBooleanFromFREObject(args[0]);
				currentSession.mediaSetMuted(muted, stateChangeCallback);
			}
			
			return null;
		}
	};
	
	/**
	 * Send a custom event to the device
	 */
	private BaseFunction sendCustomEvent = new BaseFunction() 
	{ 
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			String namespace = getStringFromFREObject(args[0]);
			String message = getStringFromFREObject(args[1]);
			
			if (currentSession != null) 
			{
				currentSession.sendMessage(namespace, message, new ChromecastSessionCallback() 
				{
					@Override
					void onSuccess(Object object) 
					{
						// TODO Success event in AIR
					}
					
					@Override
					void onError(String reason) 
					{
						// TODO Error event in AIR
					}
				});
			}
			
			return null;
		}
	};
	
	/**
	 * Adds a listener to a specific namespace
	 */
	private BaseFunction addMessageListener = new BaseFunction() 
	{ 
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
		{
			String namespace = getStringFromFREObject(args[0]);
			
			if (currentSession != null) 
			{
				currentSession.addMessageListener(namespace);
			}
			
			return null;
		}
	};
	
	
	/*
	 * INTERNAL FUNCTIONS
	 */
	
	private void setLastSessionId(String sessionId) 
	{
		this.lastSessionId = sessionId;
		this.settings.edit().putString("lastSessionId", sessionId).apply();
	}
	
	@Override
	public void dispose()
	{
		AirCastExtension.context = null;
		mMediaRouter.removeCallback(mMediaRouterCallback);
	}
	
	/**
	 * Stops the session
	 * @param callbackContext
	 * @return
	 */
	public boolean sessionStop()
	{
		if (this.currentSession != null) 
		{
			this.currentSession.kill(stateChangeCallback);
			this.currentSession = null;
			this.setLastSessionId("");
		}
		
		return true;
	}

	/**
	 * Stops the session
	 * @param callbackContext
	 * @return
	 */
	public boolean sessionLeave() 
	{
		if (this.currentSession != null) 
		{
			this.currentSession.leave(stateChangeCallback);
			this.currentSession = null;
			this.setLastSessionId("");
		}
		
		return true;
	}
	
	public void emitAllRoutes() 
	{
		final Activity activity = getActivity();
		
		activity.runOnUiThread(new Runnable() 
		{
			public void run() 
			{
				mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
				List<RouteInfo> routeList = mMediaRouter.getRoutes();
				
				JSONArray devices = new JSONArray();
				
				for (RouteInfo route : routeList) 
				{
					if (!route.getName().equals("Phone") && route.getId().indexOf("Cast") > -1) 
					{
						devices.put(routeToJSON(route));
					}
				}
				
				dispatchStatus(EVENT_DEVICE_LIST_CHANGED, devices);
			}
		});
	}
		
	/**
	 * Called when a route is discovered
	 * @param router
	 * @param route
	 */
	protected void onRouteAdded(MediaRouter router, final RouteInfo route, FREContext context) 
	{
//		if (this.autoConnect && this.currentSession == null && !route.getName().equals("Phone")) 
//		{
//			log("Attempting to join route " + route.getName());
//			this.joinSession(route, context);
//		}
//		else 
//		{
//			log("For some reason, not attempting to join route " + route.getName() + ", " + this.currentSession + ", " + this.autoConnect);
//		}
//		
//		if (!route.getName().equals("Phone") && route.getId().indexOf("Cast") > -1) 
//		{
//			sendJavascript("chrome.cast._.routeAdded(" + routeToJSON(route) + ")");
//		}
		
		emitAllRoutes();
	}

	/**
	 * Called when a discovered route is lost
	 * @param router
	 * @param route
	 */
	protected void onRouteRemoved(MediaRouter router, RouteInfo route)
	{
		emitAllRoutes();
	}

	/**
	 * Called when a route is selected through the MediaRouter
	 * 
	 * @param router
	 * @param route
	 */
	protected void onRouteSelected(MediaRouter router, RouteInfo route) 
	{	
		createSession(route);
	}
	
	/**
	 * Called when a route is unselected through the MediaRouter
	 * 
	 * @param router
	 * @param route
	 */
	protected void onRouteUnselected(MediaRouter router, RouteInfo route)
	{
		dispatchStatus(EVENT_DISCONNECTED, routeToJSON(route));
	}
	
	/**
	 * Converts RouteInfo data into a format consistent with the iOS elements
	 * of the ANE ready to be returned to AIR
	 * 
	 * @param route
	 * @return
	 */
	private JSONObject routeToJSON(RouteInfo route) 
	{
		JSONObject obj = new JSONObject();
		
		try 
		{
			obj.put("ipAddress", "");
			obj.put("servicePort", "");
			obj.put("deviceID", route.getId());
			obj.put("friendlyName", route.getName());
			obj.put("manufacturer", "");
			obj.put("modelName", "");
			obj.put("icons", new JSONArray());
		}
		catch (JSONException e) 
		{
			e.printStackTrace();
		}
		
		return obj;
	}
	
	@Override
	public void onMediaUpdated(boolean isAlive, JSONObject media) 
	{
		dispatchStateChange();
		
//		if (isAlive) 
//		{
//			sendJavascript("chrome.cast._.mediaUpdated(true, " + media.toString() +");");
//		} 
//		else 
//		{
//			sendJavascript("chrome.cast._.mediaUpdated(false, " + media.toString() +");");
//		}
	}

	@Override
	public void onSessionUpdated(boolean isAlive, JSONObject session) 
	{
		if (isAlive) 
		{
			dispatchStateChange();
//			sendJavascript("chrome.cast._.sessionUpdated(true, " + session.toString() + ");");
		}
		else 
		{
//			sendJavascript("chrome.cast._.sessionUpdated(false, " + session.toString() + ");");
			this.currentSession = null;
		}
	}

	@Override
	public void onMediaLoaded(JSONObject media) 
	{
		dispatchStateChange();
//		sendJavascript("chrome.cast._.mediaLoaded(true, " + media.toString() +");");
	}

	@Override
	public void onMessage(ChromecastSession session, String namespace, String message) 
	{
		dispatchStateChange();
//		sendJavascript("chrome.cast._.onMessage('" + session.getSessionId() +"', '" + namespace + "', '" + message  + "')");
	}
	
	/**
	 * Default callback for any action that results in the media state changing
	 * 
	 * TODO Implement this
	 */
	private ChromecastSessionCallback stateChangeCallback = new ChromecastSessionCallback()
	{
		@Override
		void onSuccess(Object object) 
		{
			dispatchStateChange();
		}
		
		@Override
		void onError(String reason) 
		{
			// TODO Dispatch errors back to AIR?
		}
	};
	
	/**
	 * Dispatches the current media state back to AIR
	 */
	private void dispatchStateChange()
	{
		dispatchStatus(EVENT_RECEIVED_MEDIA_STATE_CHANGE, currentSession.createSessionObject());
	}
	
	/**
	 * Dispatch a status event to AIR
	 * 
	 * @param context
	 * @param level
	 * @param code
	 */
	private void dispatchStatus(String type, String data)
	{
		dispatchStatusEventAsync(type, data);
	}
	
	private void dispatchStatus(String type, JSONObject data)
	{
		dispatchStatus(type, data.toString());
	}
	
	private void dispatchStatus(String type, JSONArray data)
	{
		dispatchStatus(type, data.toString());
	}
	
	private void log(String s) 
	{
		dispatchStatus(EVENT_LOGGING, s);
	}
	
	@Override
	public Map<String, FREFunction> getFunctions()
	{
		Map<String, FREFunction> functions = new HashMap<String, FREFunction>();
		
		functions.put("initNE", initNE);
		functions.put("scan", scan);
		functions.put("stopScan", stopScan);
		functions.put("connectToDevice", connectToDevice);
		functions.put("disconnectFromDevice", disconnectFromDevice);
		functions.put("loadMedia", loadMedia);
		functions.put("isConnected", isConnected);
		functions.put("isPlayingMedia", isPlayingMedia);
		functions.put("playCast", playCast);
		functions.put("pauseCast", pauseCast);
		functions.put("updateStatsFromDevice", updateStatsFromDevice);
		functions.put("seek", seek);
		functions.put("stopCast", stopCast);
		functions.put("setVolume", setVolume);
		functions.put("setMuted", setMuted);
		functions.put("sendCustomEvent", sendCustomEvent);
		
		return functions;	
	}
	
}
