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

/*import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;*/

public class GVTesting {
	final static int ACCEPTABLE_SECONDS_TEXT_DELAY = 300;
	final static boolean DEBUG = true;
	final static String gatepass = "open sesame";
	Voice voice;
	Timer timer;
	ArrayList<String> whitelist = new ArrayList<String>();

	/*final GpioController gpio = GpioFactory.getInstance();
    final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW);*/

	public static void main(String[] args) {
		new GVTesting();
	}

	GVTesting() {

		System.out.println("Starting...");
		whitelist.add("2138675309");
		GoogleVoiceSMS gv = new GoogleVoiceSMS();
		gv.initialize();
		SMSReceiver receiver = gv;
		SMSSender sender = gv;
		while(true) {
			if (receiver.message_available()) {
				simpleSMS sms = receiver.getMessage();
				if (whitelist.contains(sms.sender)) {
					if (sms.content.trim().equalsIgnoreCase(gatepass)) {
						System.out.println("Welcome in!");
						opengate();
					}
					else {
						System.out.println("Recieved bad password from " + sms.sender);
						sender.send_message(sms.sender, "Sorry, wrong password");
					}
				}
				else {
					System.out.println("Unapproved sender! (" + sms.sender + ")");
					sender.send_message(sms.sender, "You are not on the approved senders list");
				}
			}
			else {

			}
			try {
				synchronized(this) {
					this.wait(5000);
				}
			} catch (InterruptedException e) {}
		}
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
		//pin.pulse(1250);
	}

	private String parsePhoneNumber(String raw) {
		return raw.substring(raw.length()-10);
	}

}
