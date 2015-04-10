package uk.org.g33k.digibutterapi.betalands;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.simple.JSONObject;

public class Game {
	private ArrayList<Entity> entities;
	private int gravity = 1; //TODO
	long lastTick;
	int ticksPerSecond = 60;

	private int tileSize = 8;
	private HashMap<String, Integer> map = new HashMap<String, Integer>();
	
	public void update()
	{
		Date current = new Date();
		int ticks;
		if (lastTick == -1)
		{
			ticks = 1;
		}
		else
		{
			long diff = current.getTime() - lastTick;
			ticks = (int)Math.round(((double)diff / 1000d) * ticksPerSecond);
		}
		lastTick = current.getTime();
		
		for (Entity e : entities)
		{
			e.update(gravity, ticks);
		}
	}
	
	public int getTile(int x, int y)
	{
		return -1;
	}
	
	public void setTile(int x, int y, int val)
	{
		
	}
	
	public void loadLevel(JSONObject bits)
	{
		
	}
}
