package com.mesmotronic.ane.aircast
{
	[Bindable]
	public class AirCastImage
	{
		private var _url:String;
		private var _width:int;
		private var _height:int;

		public function AirCastImage(url:String, width:int, height:int)
		{
			_url = url;
			_width = width;
			_height = height;
		}
		
		public function get url():String { return _url; }
		public function get width():int { return _width; }
		public function get height():int { return _height; }

		public static function fromJSON(jsonObject:Object):AirCastImage
		{
			return new AirCastImage
			(
				jsonObject.url,
				jsonObject.width,
				jsonObject.height
			);
		}
	}
}