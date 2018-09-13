package com.mesmotronic.ane.aircast.events 
{	
	import com.mesmotronic.ane.aircast.AirCastMediaStatus;
	
	import flash.events.Event;
	
	public class AirCastMediaEvent extends Event 
	{
		public static const MEDIA_STATUS_CHANGED:String = "mediaStatusChanged";

		private var _status:String;
		private var _mediaStatus:AirCastMediaStatus;
		
		public function AirCastMediaEvent(type:String, status:String, mediaStatus:AirCastMediaStatus=null)
		{
			super(type);
			
			_status = status;
			_mediaStatus = mediaStatus;
		}
		
		public function get status():String 
		{
			return _status; 
		}
		
		public function get mediaStatus():AirCastMediaStatus 
		{
			return _mediaStatus; 
		}
	}
}
