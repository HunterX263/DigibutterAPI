package uk.org.g33k.digibutterapi.betalands;

import java.awt.Point;

public class Entity {
	private Point size = new Point(16, 16);
	private Point pos = new Point(0, 0);
	private Point last = new Point(0, 0);
	private Point vel = new Point(0, 0);
	private Point accel = new Point(0, 0);
	private Point maxVel = new Point(100, 100);
	
	private int gravityFactor = 1;
	private int bounciness = 0;
	private int health = 10;
	
	public void update()
	{
		
	}
	
	private void getNewVelocity()
	{
		
	}
	
	private void handleMovementTrace()
	{
		
	}
}
