package com.deeds;


import com.techventus.server.voice.Voice;
import com.techventus.server.voice.datatypes.Contact;
import com.techventus.server.voice.datatypes.records.SMS;
import com.techventus.server.voice.datatypes.records.SMSThread;

import java.io.IOException;

import java.util.LinkedList;

public class GoogleVoiceSMS implements SMSReceiver, SMSSender {
	final int storage_time = 3600; //Store the last hour's messages in the DB so we don't duplicate
	final String username = "USERNAME@gmail.com", password = "PASSWORD";
	boolean logged_in = false;
	//We need to remember which texts we've responded to, because when a message thread is marked as unread, we don't
	//know which *specific* messages are unread. If we remember all of the messages that we've handled from the last
	//hour or so, we won't respond twice (since we never take action when a receieved message is older than 10 mins)
	LinkedList<SMS> pending = new LinkedList<SMS>(), recent = new LinkedList<SMS>();

	Voice voice;

	GoogleVoiceSMS() { 
		initialize();
	}

	public boolean initialize() {
		if (!logged_in) {
			try {
				voice = new Voice(username,password);
				logged_in = voice.isLoggedIn();
			} catch (IOException e) {
				return false;
			}
		}
		return logged_in;
	}

	//This keeps the recent list from getting too crazy the longer the program runs.
	private boolean clearRecent(long currenttime) {
		SMS top = recent.peekFirst();
		if (top == null) return false;
		else {
			long texttime = top.getDateTime().getTime();
			long tdiff = (currenttime - texttime)/1000;
			if (tdiff > storage_time) {
				recent.pop();
				return true;
			}
			else {
				return false;
			}
		}
	}
	private boolean isOld(SMS s, long currenttime) {
		while(clearRecent(currenttime));   //Returns true until there's no more old texts in the recent list
		long texttime = s.getDateTime().getTime();
		long tdiff = (currenttime - texttime)/1000;
		if (tdiff > 600) return true;
		else {
			for (SMS c : recent) if (c.compareTo(s) == 0) return true;
			return false;
		}
	}

	private void checkMessages() throws IOException  {
		long currenttime = System.currentTimeMillis();
		for (SMSThread t : voice.getUnreadSMSThreads()) {
			if (!t.getRead()) {				
				for (SMS s : t.getAllSMS()) {
					if (!isOld(s, currenttime)) {
						recent.add(s);
						pending.add(s);
					}
					//else System.out.println("Already handled : " + s);
				}
			}
			else System.err.println("SOMETHING HAS GONE HORRIBLY WRONG");
		}
	}
	public boolean message_available() {
		try {
			checkMessages();
		} catch (IOException e) {}
		return !pending.isEmpty();
	}
	private String parsePhoneNumber(String raw) {
		return raw.substring(raw.length()-10);
	}
	public simpleSMS getMessage() {
		SMS message = pending.pop();
		String number = parsePhoneNumber(message.getFrom().getNumber());
		return new simpleSMS(number, message.getContent(), message.getDateTime());
	}

	@Override
	public boolean send_message(String recipient, String message) {
		try {
			voice.sendSMS(recipient, message);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
