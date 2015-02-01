package uk.org.g33k.digibutterapi.betalands;

import java.awt.geom.Point2D;

public class User {
	public String username;
	public String avatar;
	public String room;
	public Point2D.Double pos;
	public Point2D.Double vel;
	public boolean asleep;
	public int damage;
	
	public User(String username, String avatar, String room, Point2D.Double pos, Point2D.Double vel, boolean asleep, int damage)
	{
		this.username = username;
		this.avatar = avatar;
		this.room = room;
		this.pos = pos;
		this.vel = vel;
		this.damage = damage;
	}
	
	public User()
	{
	}
}
