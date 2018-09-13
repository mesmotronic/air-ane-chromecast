package com.mesmotronic.ane.aircast
{
	/**
	 * A class that holds status information about some media.
	 */
	[Bindable]
	public class AirCastMediaStatus 
	{
		/** Constant indicating unknown player state. */
		public static const MEDIA_PLAYER_STATE_UNKNOWN:int = 0;
		/** Constant indicating that the media player is idle. */
		public static const MEDIA_PLAYER_STATE_IDLE:int = 1;
		/** Constant indicating that the media player is playing. */
		public static const MEDIA_PLAYER_STATE_PLAYING:int = 2;
		/** Constant indicating that the media player is paused. */
		public static const MEDIA_PLAYER_STATE_PAUSED:int = 3;
		/** Constant indicating that the media player is buffering. */
		public static const MEDIA_PLAYER_STATE_BUFFERING:int = 4;

		/** Constant indicating that the player currently has no idle reason. */
		public static const MEDIA_PLAYER_IDLE_REASON_NONE:int = 0;
		/** Constant indicating that the player is idle because playback has finished. */
		public static const MEDIA_PLAYER_IDLE_REASON_FINISHED:int = 1;
		/** Constant indicating that the player is idle because playback has been cancelled in response to a STOP command. */
		public static const MEDIA_PLAYER_IDLE_REASON_CANCELLED:int = 2;
		/** Constant indicating that the player is idle because playback has been interrupted by a LOAD command. */
		public static const MEDIA_PLAYER_IDLE_REASON_INTERRUPTED:int = 3;
		/** Constant indicating that the player is idle because a playback error has occurred. */
		public static const MEDIA_PLAYER_IDLE_REASON_ERROR:int = 4;

		private var _mediaSessionID:int;
		private var _playerState:int;
		private var _idleReason:int;
		private var _playbackRate:Number;
		private var _mediaInformation:AirCastMediaInfo;
		private var _streamPosition:Number;
		private var _volume:Number;
		private var _isMuted:Boolean;
		private var _customData:Object;

		public function AirCastMediaStatus(	mediaSessionID:int,
											playerState:int,
											idleReason:int,
											playbackRate:Number,
											mediaInformation:AirCastMediaInfo,
											streamPosition:Number,
											volume:Number,
											isMuted:Boolean,
											customData:Object
										)
		{
			_mediaSessionID = mediaSessionID;
			_playerState = playerState;
			_idleReason = idleReason;
			_playbackRate = playbackRate;
			_mediaInformation = mediaInformation;
			_streamPosition = streamPosition;
			_volume = volume;
			_isMuted = isMuted;
			_customData = customData;
		}

		/** The media session ID for this item. */
		public function get mediaSessionID():int { return _mediaSessionID; }
		/** The current player state. */
		public function get playerState():int { return _playerState; }
		/** The current idle reason. This value is only meaningful if the player state is MEDIA_PLAYER_STATE_IDLE. */
		public function get idleReason():int { return _idleReason; }
		/**
		 * Gets the current stream playback rate. This will be negative if the stream is seeking
		 * backwards, 0 if the stream is paused, 1 if the stream is playing normally, and some other
		 * postive value if the stream is seeking forwards.
		 */
		public function get playbackRate():Number { return _playbackRate; }
		/** The GCKMediaInformation for this item. */
		public function get mediaInformation():AirCastMediaInfo { return _mediaInformation; }
		/** The current stream position, as an NSTimeInterval from the start of the stream. */
		public function get streamPosition():Number { return _streamPosition; }
		/** The stream's volume. */
		public function get volume():Number { return _volume; }
		/** The stream's mute state. */
		public function get isMuted():Boolean { return _isMuted; }
		/** Any custom data that is associated with the media item. */
		public function get customData():Object { return _customData; }

		public static function fromJSON(jsonObject:Object):AirCastMediaStatus
		{
			return new AirCastMediaStatus
			(
				jsonObject.mediaSessionID || 0,
				jsonObject.playerState || MEDIA_PLAYER_STATE_UNKNOWN,
				jsonObject.idleReason || MEDIA_PLAYER_IDLE_REASON_NONE,
				jsonObject.playbackRate || 1,
				AirCastMediaInfo.fromJSON(jsonObject.mediaInformation || {}),
				jsonObject.streamPosition || 0,
				jsonObject.volume || 1,
				jsonObject.isMuted || false,
				jsonObject.customData || {}
			);
		}

	}
}