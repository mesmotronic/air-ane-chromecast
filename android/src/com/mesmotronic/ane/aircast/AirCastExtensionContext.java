package com.mesmotronic.ane.aircast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.Callback;
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
	
	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private volatile ChromecastMediaRouterCallback mMediaRouterCallback = new ChromecastMediaRouterCallback();
	private String appId;
	
	private boolean autoConnect = false;
	private String lastSessionId = null;
	private String lastAppId = null;
	
	private SharedPreferences settings;
	
	private volatile ChromecastSession currentSession;
	
	final AirCastExtensionContext scope;
	
	public AirCastExtensionContext()
	{
		scope = this;
	}
	
	private void setLastSessionId(String sessionId) 
	{
		this.lastSessionId = sessionId;
		this.settings.edit().putString("lastSessionId", sessionId).apply();
	}
	
	@Override
	public void dispose()
	{
		AirCastExtension.context = null;
//		mMediaRouter.removeCallback(mediaRouteCallback);
	}
	
	/**
	 * Initialize the ANE
	 */
	private BaseFunction initNE = new BaseFunction()
	{
		@Override 
		public FREObject call(FREContext context, FREObject[] args) 
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
			
//			log("initialize " + autoJoinPolicy + " " + appId + " " + lastAppId);
//			
//			if (autoJoinPolicy.equals("origin_scoped") && appId.equals(lastAppId)) 
//			{
//				log("lastAppId " + lastAppId);
//				autoConnect = true;
//			} 
//			else if (autoJoinPolicy.equals("origin_scoped")) 
//			{
//				log("setting lastAppId " + lastAppId);
//				settings.edit().putString("lastAppId", appId).apply();
//			}
			
			activity.runOnUiThread(new Runnable() 
			{
				public void run() 
				{
					mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
					mMediaRouteSelector = new MediaRouteSelector.Builder()
						.addControlCategory(CastMediaControlIntent.categoryForCast(appId))
						.build();
					
					mMediaRouterCallback.registerCallbacks(scope);
					mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
					
					checkReceiverAvailable();
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
//			if (!isScanning)
//			{
//				mMediaRouter.addCallback(mMediaRouteSelector, mediaRouteCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
//			}
//			
//			isScanning = true;
			
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
//			if (isScanning)
//			{
//				mMediaRouter.removeCallback(mediaRouteCallback);
//			}
//			
//			isScanning = false;
			
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
				JSONObject obj = currentSession.createSessionObject();
				// TODO Convert obj to FREObject and return it?
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
							createSession(route, context);
							return;
						}
					}
					
					// TODO Return "No route found" error to AIR?
					
				}
			});
			
			return null;
		}		
	};
	
	/**
	 * Helper for the creating of a session! The user-selected RouteInfo needs to be passed to a new ChromecastSession 
	 * @param routeInfo
	 * @param callbackContext
	 */
	private void createSession(RouteInfo routeInfo, FREContext context) 
	{
		currentSession = new ChromecastSession(routeInfo, context, this, this);
		
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
					
					// TODO Somehow return data to AIR app
					
//					if (callbackContext != null) 
//					{
//						callbackContext.success(session.createSessionObject());
//					}
//					else 
//					{
//						sendJavascript("chrome.cast._.sessionJoined(" + currentSession.createSessionObject().toString() + ");");
//					}
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
	
	private void joinSession(RouteInfo routeInfo, FREContext context) 
	{
		ChromecastSession sessionJoinAttempt = new ChromecastSession(routeInfo, context, this, this);
		
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
						sendJavascript("chrome.cast._.sessionJoined(" + currentSession.createSessionObject().toString() + ");");
					} 
					catch (Exception e) 
					{
						log("wut.... " + e.getMessage() + e.getStackTrace());
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
				
				Boolean success = currentSession.loadMedia(contentId, contentType, duration, streamType, autoPlay, currentTime, metadata);
				
//				, new ChromecastSessionCallback() 
//				{
//					@Override
//					void onSuccess(Object object) 
//					{
//						if (object == null) 
//						{
//							onError("unknown");
//						}
//						else 
//						{
//							// TODO Update media status in AIR
//						}
//					}
//
//					@Override
//					void onError(String reason)
//					{
//						// TODO Error event to AIR
//					}
//				});
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
				currentSession.mediaPlay();
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
				currentSession.mediaPause();
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
				
				currentSession.mediaSeek(seekTime.longValue() * 1000, resumeState);
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
				currentSession.mediaStop();
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
				currentSession.setVolume(getDoubleFromFREObject(args[0]));
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
				currentSession.mediaSetMuted(muted);
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
	
	// INTERNAL
	
	/**
	 * Stops the session
	 * @param callbackContext
	 * @return
	 */
	public boolean sessionStop()
	{
		if (this.currentSession != null) 
		{
			this.currentSession.kill();
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
			this.currentSession.leave();
			this.currentSession = null;
			this.setLastSessionId("");
		}
		
		return true;
	}
	
	/**
	 * Checks to see how many receivers are available - emits the receiver status down to Javascript
	 */
	private void checkReceiverAvailable() 
	{
		final Activity activity = getActivity();
		
		activity.runOnUiThread(new Runnable() 
		{
			public void run() 
			{
				mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
				List<RouteInfo> routeList = mMediaRouter.getRoutes();
				boolean available = false;
				
				for (RouteInfo route: routeList) 
				{
					if (!route.getName().equals("Phone") && route.getId().indexOf("Cast") > -1) 
					{
						available = true;
						break;
					}
				}
				
				if (available || (currentSession != null && currentSession.isConnected())) 
				{
					sendJavascript("chrome.cast._.receiverAvailable()");
				}
				else 
				{
					sendJavascript("chrome.cast._.receiverUnavailable()");
				}
			}
		});
	}
	
	public boolean emitAllRoutes() 
	{
		final Activity activity = getActivity();
		
		activity.runOnUiThread(new Runnable() 
		{
			public void run() 
			{
				mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
				List<RouteInfo> routeList = mMediaRouter.getRoutes();
				
				for (RouteInfo route : routeList) 
				{
					if (!route.getName().equals("Phone") && route.getId().indexOf("Cast") > -1) 
					{
						sendJavascript("chrome.cast._.routeAdded(" + routeToJSON(route) + ")");
					}
				}
			}
		});
		
		return true;
	}
	
	/**
	 * Creates a ChromecastSessionCallback that's generic for a CallbackContext 
	 * @param callbackContext
	 * @return
	 */
//	private ChromecastSessionCallback genericCallback (final CallbackContext callbackContext) 
//	{
//		return new ChromecastSessionCallback() {
//
//			@Override
//			public void onSuccess(Object object) {
//				callbackContext.success();
//			}
//
//			@Override
//			public void onError(String reason) {
//				callbackContext.error(reason);
//			}
//			
//		};
//	};
	
	/**
	 * Called when a route is discovered
	 * @param router
	 * @param route
	 */
	protected void onRouteAdded(MediaRouter router, final RouteInfo route, FREContext context) 
	{
		if (this.autoConnect && this.currentSession == null && !route.getName().equals("Phone")) 
		{
			log("Attempting to join route " + route.getName());
			this.joinSession(route, context);
		}
		else 
		{
			log("For some reason, not attempting to join route " + route.getName() + ", " + this.currentSession + ", " + this.autoConnect);
		}
		
		if (!route.getName().equals("Phone") && route.getId().indexOf("Cast") > -1) 
		{
			sendJavascript("chrome.cast._.routeAdded(" + routeToJSON(route) + ")");
		}
		
		checkReceiverAvailable();
	}

	/**
	 * Called when a discovered route is lost
	 * @param router
	 * @param route
	 */
	protected void onRouteRemoved(MediaRouter router, RouteInfo route)
	{
		checkReceiverAvailable();
		
		if (!route.getName().equals("Phone") && route.getId().indexOf("Cast") > -1) 
		{
			sendJavascript("chrome.cast._.routeRemoved(" + routeToJSON(route) + ")");
		}
	}

	/**
	 * Called when a route is selected through the MediaRouter
	 * @param router
	 * @param route
	 */
	protected void onRouteSelected(MediaRouter router, RouteInfo route) 
	{	
		createSession(route, null);
	}

	/**
	 * Called when a route is unselected through the MediaRouter
	 * @param router
	 * @param route
	 */
	protected void onRouteUnselected(MediaRouter router, RouteInfo route)
	{
		//
	}
	
	/*
	private Cast.Listener castClientListener = new Cast.Listener() 
	{
		//
	}
	
	private MediaRouter.Callback mediaRouteCallback = new Callback() 
	{
		@Override
		public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route)
		{
			Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
				.builder(route, castClientListener);
			
			apiClient = new GoogleApiClient.Builder(this)
				.addApi(Cast.API, apiOptionsBuilder.build())
				.addConnectionCallbacks(mConnectionCallbacks)
				.addOnConnectionFailedListener(mConnectionFailedListener)
				.build();
		}
		
		@Override
		public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route)
		{
			//
		}
		
		@Override
		public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route)
		{
			//
		}
		
		@Override
		public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route)
		{
			//
		}
		
		@Override
		public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route)
		{
			//
		}
		
		@Override
		public void onRouteVolumeChanged(MediaRouter router, MediaRouter.RouteInfo route)
		{
			//
		}
		
		@Override
		public void onRoutePresentationDisplayChanged(MediaRouter router, MediaRouter.RouteInfo route)
		{
			//
		}
		
		@Override
		public void onProviderAdded(MediaRouter router, MediaRouter.ProviderInfo provider)
		{
			//
		}
		
		@Override
		public void onProviderRemoved(MediaRouter router, MediaRouter.ProviderInfo provider)
		{
			//
		}
		
		@Override
		public void onProviderChanged(MediaRouter router, MediaRouter.ProviderInfo provider)
		{
			//
		}
		
	}
	*/
	
	/**
	 * Simple helper to convert a route to JSON for passing down to the AIR side
	 * @param route
	 * @return
	 */
	private JSONObject routeToJSON(RouteInfo route) 
	{
		JSONObject obj = new JSONObject();
		
		try 
		{
			obj.put("name", route.getName());
			obj.put("id", route.getId());
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
		if (isAlive) 
		{
			sendJavascript("chrome.cast._.mediaUpdated(true, " + media.toString() +");");
		} 
		else 
		{
			sendJavascript("chrome.cast._.mediaUpdated(false, " + media.toString() +");");
		}
	}

	@Override
	public void onSessionUpdated(boolean isAlive, JSONObject session) 
	{
		if (isAlive) 
		{
			sendJavascript("chrome.cast._.sessionUpdated(true, " + session.toString() + ");");
		}
		else 
		{
			log("SESSION DESTROYYYY");
			sendJavascript("chrome.cast._.sessionUpdated(false, " + session.toString() + ");");
			this.currentSession = null;
		}
	}

	@Override
	public void onMediaLoaded(JSONObject media) 
	{
		sendJavascript("chrome.cast._.mediaLoaded(true, " + media.toString() +");");
	}

	@Override
	public void onMessage(ChromecastSession session, String namespace, String message) 
	{
		sendJavascript("chrome.cast._.onMessage('" + session.getSessionId() +"', '" + namespace + "', '" + message  + "')");
	}

	private void sendJavascript(final String javascript) 
	{
		// TODO Replace this with a function that calls AIR events
		
//		webView.post(new Runnable() 
//		{
//			@Override
//			public void run() 
//			{
//				// See: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/jsinterface-example/app/src/main/java/jsinterfacesample/android/chrome/google/com/jsinterface_example/MainFragment.java
//				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) 
//				{
//					webView.evaluateJavascript(javascript, null);
//				}
//				else 
//				{
//					webView.loadUrl("javascript:" + javascript);
//				}
//			}
//		});
	}
	
	private void log(String s) 
	{
		sendJavascript("console.log('" + s + "');");
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
