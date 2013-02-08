package com.deeds;

import java.util.Date;

public class simpleSMS {
	//I don't think these values need to be mutable
	final String sender;
	final String content;
	final Date date_sent;
	simpleSMS(String sender, String content, Date date) {
		this.sender = sender;
		this.content = content;
		this.date_sent = date;
	}
	public String getMessageSender() {
		return sender;
	}
	public String getMessageContent() {
		return content;
	}
	public Date getDateSent() {
		return date_sent;
	}
	
}
