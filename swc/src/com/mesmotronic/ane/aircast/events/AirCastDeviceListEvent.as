package com.mesmotronic.ane.aircast.events 
{	
	import com.mesmotronic.ane.aircast.AirCastDevice;
	
	import flash.events.Event;

	public class AirCastDeviceListEvent extends Event 
	{
		public static const DEVICE_SCAN_STARTED:String = "deviceScanStarted";
		public static const DEVICE_SCAN_STOPPED:String = "deviceScanStopped";
		public static const DEVICE_LIST_CHANGED:String = "deviceListChanged";
		
		private var _deviceList:Vector.<AirCastDevice>;
		
		public function AirCastDeviceListEvent(type:String, deviceList:Vector.<AirCastDevice>=null)
		{
			super(type);
			_deviceList = deviceList;
		}
		
		public function get deviceList():Vector.<AirCastDevice> 
		{
			if (_deviceList)
			{
				return _deviceList.slice();
			}
			
			return null;
		}
	}
}
