package com.mesmotronic.ane.aircast.events 
{
	import flash.events.Event;

	public class AirCastMessageEvent extends Event 
	{
		private var _message:String;
		private var _data:Object;
		
		public function AirCastMessageEvent(ns:String, message:String)
		{
			super(ns);
			
			_message = message;
			
			try { _data = JSON.parse(_message); }
			catch (e:Error) { _data = {}; }
		}
		
		public function get ns():String
		{
			return type;
		}
		
		/**
		 * The raw message as an unparsed String
		 */
		public function get message():String 
		{
			return _message; 
		}
		
		/**
		 * The message converted into an Object using JSON.parse() 
		 */
		public function get data():Object
		{
			return _data;
		}
	}
}