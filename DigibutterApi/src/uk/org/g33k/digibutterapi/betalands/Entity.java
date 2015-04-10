package uk.org.g33k.digibutterapi.betalands;

import java.awt.Point;

public class Entity {
	private Point size = new Point(16, 16);
	private Point pos = new Point(0, 0);
	private Point last = new Point(0, 0);
	private Point vel = new Point(0, 0);
	private Point accel = new Point(0, 0);
	private Point friction = new Point(0, 0);
	private Point maxVel = new Point(100, 100);
	
	private int gravityFactor = 1;
	private int bounciness = 0;
	private int health = 10;
	
	public void update(int gravity, int tick)
	{
		last.x = pos.x;
		last.y = pos.y;
		
		vel.y += gravity * tick * gravityFactor;
		vel.x = getNewVelocity(vel.x, accel.x, friction.x, maxVel.x, tick);
		vel.y = getNewVelocity(vel.y, accel.y, friction.y, maxVel.y, tick);
		
		int mx = vel.x * tick;
		int my = vel.y * tick;
		//TODO
		
		handleMovementTrace();
	}
	
	private int getNewVelocity(int vel, int accel, int friction, int max, int tick)
	{
		if (accel > 0)
		{
			return Math.max(-max, Math.min(max, vel + accel * tick));
		}
		else if (friction > 0)
		{
			int delta = friction * tick;
			if (vel - delta > 0)
			{
				return vel - delta;
			}
			else if (vel + delta < 0)
			{
				return vel + delta;
			}
			else
			{
				return 0;
			}
		}
		else
		{
			return Math.max(-max, Math.min(max, vel));
		}
	}
	
	private void handleMovementTrace()
	{
		
	}
}
