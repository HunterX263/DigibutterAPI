package uk.org.g33k.digibutterapi;

import java.util.Date;

public class Topic {
	public int id;
	public String username;
	public Date date;
	public String title;
	public String message;
	
	public Topic(int id, String username, Date date, String title, String message)
	{
		this.id = id;
		this.username = username;
		this.date = date;
		this.title = title;
		this.message = message;
	}
	
	public Topic() {}
}
