package com.mesmotronic.aircast.example.utils
{
	import com.mesmotronic.ane.aircast.IAirCastLogger;

	public class AirCastLogger implements IAirCastLogger
	{
		public function log(...params):void
		{
			trace.apply(null, params);
		}
	}
}
