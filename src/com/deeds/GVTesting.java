package com.deeds;

import java.io.IOException;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.Timer;
import java.util.TimerTask;


import com.techventus.server.voice.Voice;
import com.techventus.server.voice.datatypes.Contact;
import com.techventus.server.voice.datatypes.records.SMS;
import com.techventus.server.voice.datatypes.records.SMSThread;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class GVTesting {
	final static int ACCEPTABLE_SECONDS_TEXT_DELAY = 300;
	final static boolean DEBUG = true;
	final static String username = "USERNAME@gmail.com";
	final static String password = "PASSWORD123";
	final static String gatepass = "open sesame";
	Voice voice;
	Timer timer;
	ArrayList<String> whitelist;
	
    final GpioController gpio = GpioFactory.getInstance();
    final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW);

	public static void main(String[] args) {
		new GVTesting();
	}

	GVTesting() {

		System.out.println("Starting...");
		try {
	
			voice = new Voice(username,password);
			System.out.println("Checking messages for " + voice.getPhoneNumber());
			timer = new Timer();
			whitelist = new ArrayList<String>();
			whitelist.add("2138675309");
			//if (whitelist.contains("2138675309")) System.err.println("Yes");
			timer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					try {
						checkmessages();
					} catch (IOException e) {}
				}
			}, 500, 7500);
		} catch (IOException e) { e.printStackTrace();}
	}

	private boolean checkmessages() throws IOException {
		if (DEBUG) System.out.println("Checking messages");
		for (SMSThread t : voice.getUnreadSMSThreads()) {
			if (!t.getRead()) {
				System.out.println();
				long texttime = t.getDate().getTime();
				long currenttime = System.currentTimeMillis();
				long tdiff = currenttime - texttime;
				if (ACCEPTABLE_SECONDS_TEXT_DELAY*1000 > tdiff) {
					if (DEBUG) System.out.println("Fresh message (" + tdiff/1000 + " seconds old)");
					processThread(t);
					return true;
				}
				else if (DEBUG) System.out.println("Too old (" + (currenttime-texttime)/1000 + ")");
			}
			else System.err.println("SOMETHING HAS GONE HORRIBLY WRONG");
		}
		return false;
	}
	private void processThread(SMSThread thread) throws IOException {
		SMS top = ((TreeSet<SMS>)thread.getAllSMS()).first();
		Contact sender = top.getFrom();
		String sendernumber = parsePhoneNumber(sender.getNumber());
		if (DEBUG) {
			System.out.print("New message from " + sender.getName());
			System.out.println(": " + sendernumber + " (" + sender.getNumber() + ")");
			System.out.println(top.getContent());
		}
		if (top.getContent().equalsIgnoreCase(gatepass)) {
			if (whitelist.contains(sendernumber)) {
				System.out.println("Welcome in!");
				voice.sendSMS(sendernumber, "Password accepted... welcome in!");
				opengate();
			}
			else System.err.println("Unauthorized access from number:" + sender.getNumber());
			
		}
		else {
			System.out.println("Wrong password!");
			voice.sendSMS(sendernumber, "Wrong password");
		}
		voice.markAsRead(thread.getId());
	}
	

	void opengate() {
		System.out.println("Opening gate");
		pin.pulse(1250);
	}
	
	private String parsePhoneNumber(String raw) {
		return raw.substring(raw.length()-10);
	}

}
