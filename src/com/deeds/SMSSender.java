package com.deeds;

public interface SMSSender {

	public boolean initialize();
	public boolean send_message(String recipient, String message);

}
