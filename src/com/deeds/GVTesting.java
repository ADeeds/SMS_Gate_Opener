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
	final static String gatepass = "open sesame";
	Voice voice;
	Timer timer;
	ArrayList<String> whitelist = new ArrayList<String>();

	final GpioController gpio = GpioFactory.getInstance();
	//Uses pin 1 (which is NOT in the top right corner, see the diagram at http://elinux.org/RPi_Low-level_peripherals
    final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW);

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
					//Approved sender and correct password
					if (sms.content.trim().equalsIgnoreCase(gatepass)) {
						System.out.println("Welcome in!");
						opengate();
					}
					//Approved sender; bad password
					else {
						System.out.println("Recieved bad password from " + sms.sender);
						sender.send_message(sms.sender, "Sorry, wrong password");
					}
				}
				//Unapproved sender
				else {
					System.out.println("Unapproved sender! (" + sms.sender + ")");
					sender.send_message(sms.sender, "You are not on the approved senders list");
				}
			}
			else {

			}
			try {
				synchronized(this) {
					this.wait(5000);			//Wait for 5 seconds before next check
				}
			} catch (InterruptedException e) {}
		}
	}

	void opengate() {
		System.out.println("Opening gate");
		pin.pulse(1250);
	}

	private String parsePhoneNumber(String raw) {
		return raw.substring(raw.length()-10);
	}

}
