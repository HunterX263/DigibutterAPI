package uk.org.g33k.digibutterapi.betalands;

import java.util.ArrayList;

public class Game {
	private ArrayList<Entity> entities;

	private int tileSize = 8;
	private int[][] map;
	
	public void update()
	{
		for (Entity e : entities)
		{
			e.update();
		}
	}
}
