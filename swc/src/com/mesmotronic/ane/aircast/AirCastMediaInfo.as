package com.mesmotronic.ane.aircast
{
	[Bindable]
	public class AirCastMediaInfo 
	{
		/** A stream type of "none". */
		public static const MEDIA_STREAM_TYPE_NONE:int = 0;
		/** A buffered stream type. */
		public static const MEDIA_STREAM_TYPE_BUFFERED:int = 1;
		/** A live stream type. */
		public static const MEDIA_STREAM_TYPE_LIVE:int = 2;
		/** An unknown stream type. */
		public static const MEDIA_STREAM_TYPE_UNKNOWN:int = 99;
		
		private var _contentId:String;
		private var _streamType:int;
		private var _contentType:String;
		private var _metadata:AirCastMediaMetadata;
		private var _streamDuration:Number;
		private var _customData:Object;

		public function AirCastMediaInfo(
			contentId:String,
			streamType:int,
			contentType:String,
			metadata:AirCastMediaMetadata,
			streamDuration:Number,
			customData:Object
		)
		{
			_contentId = contentId;
			_streamType = streamType;
			_contentType = contentType;
			_metadata = metadata;
			_streamDuration = streamDuration;
			_customData = customData;
		}
		
		/** The content ID (URL) */
		public function get contentId():String { return _contentId; }
		/** The stream type. */
		public function get streamType():int { return _streamType; }
		/** The content (MIME) type. */
		public function get contentType():String { return _contentType; }
		/** The media item metadata. */
		public function get metadata():Object { return _metadata; }
		/** The length of time for the stream, in seconds. */
		public function get streamDuration():Number { return _streamDuration; }
		/** The custom data, if any. */
		public function get customData():Object { return _customData; }
		
		public static function fromJSON(jsonObject:Object):AirCastMediaInfo
		{
			return new AirCastMediaInfo
			(
				jsonObject.contentId || '',
				jsonObject.streamType || MEDIA_STREAM_TYPE_NONE,
				jsonObject.contentType || '',
				AirCastMediaMetadata.fromJSON(jsonObject.metadata || {}),
				jsonObject.streamDuration || 0,
				jsonObject.customData || {}
			);
		}
		
		public function toString():String
		{
			return '[AirCastMediaInfo contentId="'+contentId+'" streamType='+streamType+' contentType="'+contentType+'" metadata='+metadata+' streamDuration='+streamDuration+' customData='+customData+']';
		}
	}
}