package uk.org.g33k.digibutterapi;

import java.util.Date;

public class Reply {
	public String username;
	public Date date;
	public String message;
	
	public Reply(String username, Date date, String message)
	{
		this.username = username;
		this.date = date;
		this.message = message;
	}
	
	public Reply() {}
}
