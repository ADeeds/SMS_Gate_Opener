package com.deeds;

public interface SMSReceiver {
	public boolean initialize();
	public boolean message_available();
	public simpleSMS getMessage(); 
}
