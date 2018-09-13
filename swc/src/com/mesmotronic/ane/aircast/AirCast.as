package com.mesmotronic.ane.aircast
{
	import com.mesmotronic.ane.aircast.events.AirCastDeviceEvent;
	import com.mesmotronic.ane.aircast.events.AirCastDeviceListEvent;
	import com.mesmotronic.ane.aircast.events.AirCastMediaEvent;
	import com.mesmotronic.ane.aircast.events.AirCastMessageEvent;
	
	import flash.events.Event;
	import flash.events.EventDispatcher;
	import flash.events.StatusEvent;
	import flash.external.ExtensionContext;
	import flash.system.Capabilities;
	
	[Event(name="deviceConnecting", type="com.mesmotronic.ane.aircast.events.AirCastDeviceEvent")]
	[Event(name="deviceConnected", type="com.mesmotronic.ane.aircast.events.AirCastDeviceEvent")]
	[Event(name="deviceDisconnected", type="com.mesmotronic.ane.aircast.events.AirCastDeviceEvent")]
	[Event(name="deviceScanStarted", type="com.mesmotronic.ane.aircast.events.AirCastDeviceListEvent")]
	[Event(name="deviceScanStopped", type="com.mesmotronic.ane.aircast.events.AirCastDeviceListEvent")]
	[Event(name="deviceListChanged", type="com.mesmotronic.ane.aircast.events.AirCastDeviceListEvent")]
	[Event(name="mediaStatusChanged", type="com.mesmotronic.ane.aircast.events.AirCastMediaEvent")]
	
	[Bindable]
	public class AirCast extends EventDispatcher 
	{
		// Static
		
		public static const NS_PREFIX:String = 'urn:x-cast:';
		
		private static var _airCast:AirCast;
		
		public static function get isSupported():Boolean
		{
			return !!airCast.context;
		}
		
		public static function get airCast():AirCast
		{
			if (!_airCast)
			{
				_airCast = new AirCast(new Singleton());
			}
			
			return _airCast;
		}
		
		// Local
		
		public var defaultNS:String = '';
		public var logEnabled:Boolean = false;
		
		protected var context:ExtensionContext;
		protected var isInitialized:Boolean;
		
		private var _isScanning:Boolean = false;
		private var _isConnecting:Boolean = false;
		private var _deviceList:Vector.<AirCastDevice> = new Vector.<AirCastDevice>;
		private var _connectedDevice:AirCastDevice;
		private var _status:String = '';
		private var _mediaStatus:AirCastMediaStatus;
		
		public function AirCast(singleton:Singleton)
		{
			if (!singleton) throw new Error('AirCast is a singleton!');
			
			var version:String = Capabilities.version.substr(0,3);
			
			switch (version)
			{
				case 'AND':
				case 'IOS':
				{
					context = ExtensionContext.createExtensionContext('com.mesmotronic.ane.aircast', '');
					
					if (context)
					{
						context.addEventListener(StatusEvent.STATUS, statusHandler);
					}
					
					break;
				}
			}
		}
		
		[Bindable(event="change")]
		public function get isConnecting():Boolean
		{
			return _isConnecting;
		}
		
		/** 
		 * Returns true if connected to a receiver 
		 */
		[Bindable(event="change")]
		public function get isConnected():Boolean
		{
			if (!isSupported) return false;
			return context.call('isConnected');
		}
		
		/** 
		 * Returns true if media is loaded on the receiver
		 */
		[Bindable(event="mediaStatusChanged")]
		public function get isPlaying():Boolean
		{
			if (!isSupported) return false;
			return context.call('isPlayingMedia');
		}
		
		[Bindable(event="change")]
		public function get isScanning():Boolean
		{
			return _isScanning;
		}
		
		/**
		 * The most recently received device list
		 */
		[Bindable(event="change")]
		public function get deviceList():Vector.<AirCastDevice> 
		{
			return _deviceList; 
		}
		
		/**
		 * The receiver currently connected to your app
		 */
		[Bindable(event="change")]
		public function get connectedDevice():AirCastDevice 
		{
			return _connectedDevice; 
		}
		
		/**
		 * The most recently received device status
		 */
		[Bindable(event="mediaStatusChanged")]
		public function get status():String
		{
			return _status;
		}
		
		/**
		 * The most recently received media status
		 */
		[Bindable(event="mediaStatusChanged")]
		public function get mediaStatus():AirCastMediaStatus
		{
			return _mediaStatus;
		}
		
		/** 
		 * initialize the cast sender with the receiver app ID: 
		 * 
		 * <p>If no app ID is set, we use the default Chromecast receiver app</p>  
		 */
		public function init(appId:String='CC1AD845', customNS:String=''):void
		{
			if (!isSupported || isInitialized) return;
			
			log("AirCast initialized using app ID "+appId);
			context.call('initNE', appId);
			
			defaultNS = customNS;
			isInitialized = true;
			
			// Both ANEs now start scanning automatically
			_isScanning = true;
		}
		
		/** 
		 * Start scanning for receivers on the network. 
		 */
		public function startScan():void
		{
			if (!isSupported) return;
			
			log("Scanning for devices...");
			
			_isScanning = true;
			
			dispatchEvent(new AirCastDeviceListEvent(AirCastDeviceListEvent.DEVICE_SCAN_STARTED));
			dispatchEvent(new Event(Event.CHANGE));
			
			context.call('scan');
		}
		
		/** 
		 * Stop any ongoing device scan 
		 */
		public function stopScan():void
		{
			if (!isSupported) return;
			
			log("Stopping scanning for devices");
			
			_isScanning = false;
			
			dispatchEvent(new AirCastDeviceListEvent(AirCastDeviceListEvent.DEVICE_SCAN_STOPPED));
			dispatchEvent(new Event(Event.CHANGE));
			
			context.call('stopScan');
		}
		
		/** 
		 * Search for a device with given ID and connect to it
		 * 
		 * A return of true does not mean we connected to the receiver:
		 * listen for AirCastDeviceEvent.DEVICE_CONNECTED to know
		 * when we successfully connected to a receiver
		 *  
		 * @returns false if the device could not be found, true otherwise
		 */
		public function connect(deviceId:String):Boolean
		{
			if (!isSupported || !deviceId) return false;
			
			_isConnecting = context.call('connectToDevice', deviceId);
			
			log(_isConnecting ? "Connecting to device "+deviceId : "Unable to connect to device "+deviceId);
			
			if (_isConnecting)
			{
				dispatchEvent(new AirCastDeviceEvent(AirCastDeviceEvent.DEVICE_CONNECTING));
				dispatchEvent(new Event(Event.CHANGE));
			}
			
			return _isConnecting;
		}

		/** 
		 * Disconnect your app from the receiver
		 * 
		 * <p>The AirCastDeviceEvent.DEVICE_DISCONNECTED event is dispatched 
		 * when disconnection is complete</p>
		 */
		public function disconnect():void
		{
			if (!isSupported) return;
			
			log("Disconnecting from device");
			context.call('disconnectFromDevice') ;
		}
		
		/** 
		 * Load media on the device supplied metadata 
		 */
		public function load(url:String, thumbnailUrl:String='', title:String='', 
			subtitle:String='', contentType:String='', startTime:Number=0.0, autoPlay:Boolean=true):Boolean
		{
			if (!isSupported) return false;
			
			log("Loading "+url);
			return context.call('loadMedia', url, thumbnailUrl, title, subtitle, contentType, startTime, autoPlay);
		}

		/**
		 * Play the current media on the receiver 
		 */
		public function play():void
		{
			if (!isSupported) return;
			
			log("Play");
			context.call('playCast');
		}

		/**
		 * Pause the current media on the receiver 
		 */
		public function pause():void
		{
			if (!isSupported) return;
			
			log("Pause");
			context.call('pauseCast');
		}
		
		/**
		 * Stop the current media on the receiver 
		 */
		public function stop():void
		{
			if (!isSupported) return;
			
			log("Stop");
			context.call('stopCast');
		}
		
		/** 
		 * Request an update of media playback status from the receiver
		 * 
		 * TODO Should this request an update? Or return an update? :-/
		 * TODO Has this been implemented for iOS? 
		 */
		public function updateMediaStatus():AirCastMediaStatus
		{
			try
			{
				var json:String = context.call('updateStatsFromDevice') as String;
				return AirCastMediaStatus.fromJSON(JSON.parse(json).mediaStatus);
			}
			catch (e:Error)
			{
				log("Unable to update media status:", e.message);
			}
			
			return null;
		}
		
		/** 
		 * Sets the position of the playback on the receiver
		 */
		public function seek(position:Number):void
		{
			if (!isSupported) return;
			
			log("seek "+position);
			context.call('seek', position);
		}
		
		/** 
		 * Sets the volume on the receiver
		 */
		public function setVolume(value:Number):void
		{
			if (!isSupported) return;
			
			log("setting volume to "+value);
			context.call('setVolume', value);
		}
		
		/** 
		 * Send a custom message to the receiver
		 * 
		 * @param	message		The message to send 
		 * @param	ns 			The namespace to use for sending the message, e.g. urn:x-cast:com.example.cast,
		 * 						if it's not specified, it uses the value in defaultNS (if specified) 
		 */
		public function sendMessage(message:*, ns:String=''):void
		{
			if (!isSupported) return;
			
			ns || (ns = defaultNS);
			
			if (ns.indexOf(NS_PREFIX) != 0)
			{
				throw new Error('Namespaces must begin with "'+NS_PREFIX+'"');
			}
			
			if (!(message is String))
			{
				try
				{
					message = JSON.stringify(message);
				}
				catch(e:Error)
				{
					throw new Error('Invalid message');
				}
			}
			
			context.call('sendCustomEvent', message, ns);
		}
		
		/**
		 * Events from the ANE
		 */
		protected function statusHandler(event:StatusEvent):void
		{
			var jsonData:Object;
			
			switch (event.code)
			{
				case "AirCast.deviceListChanged":
				{
					log(event.code+": "+event.level);
					
					_deviceList = new Vector.<AirCastDevice>();
					
					try
					{
						jsonData = JSON.parse(event.level) as Array;
						
						for each (var device:Object in jsonData)
						{
							_deviceList.push(AirCastDevice.fromJSON(device));
						}
					}
					catch (e:Error) 
					{
						log(e.message);
					}
					
					dispatchEvent(new AirCastDeviceListEvent(AirCastDeviceListEvent.DEVICE_LIST_CHANGED, _deviceList.slice()));
					break;
				}
					
				case "AirCast.didConnectToDevice":
				{
					log(event.code+": "+event.level);
					
					_isConnecting = false;
					
					try
					{
						jsonData = JSON.parse(event.level);
						
						_connectedDevice = AirCastDevice.fromJSON(jsonData);
						
						// Workaround to initialise message channel on iOS implementation
						if (defaultNS)
						{
							sendMessage({type:'PING'});
						}
						
						dispatchEvent(new AirCastDeviceEvent(AirCastDeviceEvent.DEVICE_CONNECTED, _connectedDevice));
					}
					catch (e:Error) 
					{
						log(e.message);
					}
					
					dispatchEvent(new Event(Event.CHANGE));
					break;
				}
				
				case "AirCast.didDisconnect":
				{
					log(event.code+": "+event.level);
					
					var disconnectedDevice:AirCastDevice = connectedDevice;
					
					_connectedDevice = null;
					_isConnecting = false;
					_status = '';
					_mediaStatus = null;
					
					if (disconnectedDevice)
					{
						dispatchEvent(new AirCastDeviceEvent(AirCastDeviceEvent.DEVICE_DISCONNECTED, disconnectedDevice));
					}
					
					dispatchEvent(new Event(Event.CHANGE));						
					
					break;
				}
				
				case "AirCast.didReceiveMediaStateChange":
				{
					log(event.code+": "+event.level);
					
					try
					{
						jsonData = JSON.parse(event.level);
						
						// e.g. "Ready To Cast"
						var status:String = jsonData.status || '';
						
						// Information about the current state of the loaded media (if any)
						var mediaStatus:AirCastMediaStatus = jsonData.mediaStatus
							? AirCastMediaStatus.fromJSON(jsonData.mediaStatus)
							: null;
						
						_status = status;
						_mediaStatus = mediaStatus;
						
						dispatchEvent(new AirCastMediaEvent(AirCastMediaEvent.MEDIA_STATUS_CHANGED, status, mediaStatus));
					}
					catch (e:Error) 
					{
						log(e.message);
					}
					
					break;
				}
				
				case "AirCast.didReceiveCustomEvent":
				{
					log(event.code+": "+event.level);
					
					try
					{
						jsonData = JSON.parse(event.level);
						dispatchEvent(new AirCastMessageEvent(jsonData.protocol, jsonData.event));
					}
					catch (e:Error) 
					{
						log(e.message);
					}
					
					break;
				}
				
				case "LOGGING":
				{
					log(event.level);
					break;
				}
					
				default:
				{
					log("Unknown status received:", event.code);
					break;
				}
			}
		}
		
		protected function log(...messages):void
		{
			if (logEnabled)
			{
				messages.unshift(this);
				trace.apply(null, messages);
			}
		}
	}
}

class Singleton {}