package com.mesmotronic.ane.aircast.events 
{	
	import com.mesmotronic.ane.aircast.AirCastDevice;
	
	import flash.events.Event;

	public class AirCastDeviceEvent extends Event 
	{
		public static const DEVICE_CONNECTING:String = "deviceConnecting";
		public static const DEVICE_CONNECTED:String = "deviceConnected";
		public static const DEVICE_DISCONNECTED:String = "deviceDisconnected";
		
		private var _device:AirCastDevice;
		
		public function get device():AirCastDevice
		{ 
			return _device; 
		}
		
		public function AirCastDeviceEvent(type:String, device:AirCastDevice=null)
		{
			super(type);
			_device = device;
		}
	}
}