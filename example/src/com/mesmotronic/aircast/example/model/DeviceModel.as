package com.mesmotronic.aircast.example.model
{
	import com.mesmotronic.ane.aircast.AirCast;
	import com.mesmotronic.ane.aircast.AirCastDevice;
	import com.mesmotronic.ane.aircast.events.AirCastDeviceListEvent;
	
	import mx.collections.ArrayCollection;

	public class DeviceModel
	{
		public static var devices:ArrayCollection = new ArrayCollection();
		
		public static function init():void
		{
			AirCast.airCast.addEventListener(AirCastDeviceListEvent.DEVICE_LIST_CHANGED, deviceListChangedHandler);
		}
		
		private static function deviceListChangedHandler(event:AirCastDeviceListEvent):void
		{
			trace(event.deviceList.length, "devices found!");
			
			for each (var device:AirCastDevice in event.deviceList)
			{
				if (!hasDevice(device))
				{
					devices.addItem(device);
				}
			}
		}
		
		public static function hasDevice(device:AirCastDevice):Boolean
		{
			for each (var d:AirCastDevice in devices)
			{
				if (device.deviceID == d.deviceID)
				{
					return true;
				}
			}
			
			return false;
		}
	}
}