package com.mesmotronic.ane.aircast;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREExtension;

public class AirCastExtension implements FREExtension
{
	public static String TAG = "AirCast";
	
	private static AirCastExtensionContext context;
	
	@Override
	public FREContext createContext(String extId) 
	{
		context = new AirCastExtensionContext();
		return context;
	}
	
	@Override
	public void dispose()
	{
		context = null;
	}
	
	@Override
	public void initialize()
	{
		// Nothing to do here
	}
	
}
