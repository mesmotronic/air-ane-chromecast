package com.mesmotronic.ane.aircast.events {
	
	import flash.events.Event;

	import com.mesmotronic.ane.aircast.AirCastDevice;

	public class AirCastDeviceListEvent extends Event {

		public static const DEVICE_LIST_CHANGED:String = "AirCastDeviceListEvent.DEVICE_LIST_CHANGED";

		private var _deviceList:Vector.<AirCastDevice>;

		public function get deviceList():Vector.<AirCastDevice> { return this._deviceList.slice(); }

		public function AirCastDeviceListEvent( type:String, deviceList:Vector.<AirCastDevice> )
		{

			super(type);

			this._deviceList = deviceList;

		}

	}

}