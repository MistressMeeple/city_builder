package com.meeple.citybuild;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Vector2i;

import com.meeple.citybuild.Buildings.BuildingInstance;
import com.meeple.citybuild.WorldGenerator.TileTypes;
import com.meeple.shared.frame.component.FrameTimeManager;

public class LevelData implements Serializable {

	private static final long serialVersionUID = -4303939847435592039L;

	/**
	 * how long the level has been active in nanos
	 */
	public long activeTime = 0;

	public static final int chunkSize = 2;
	public static final float tileSize = 2;

	/**
	 * Name of the save. 
	 */
	public String name;
	/**
	 * Levels random
	 */
	public Random random;
	/**
	 * Chunk storage
	 */
	public Map<Vector2i, Chunk> chunks = new HashMap<>();
	/**
	 * Holds the "dictionary" of all the tile types. <br>
	 * This is usually populated at the world generation and never touched.
	 */
	public Map<Byte, Object> tileTypes = new HashMap<>();
	/**
	 * ID indexed map of all the unique buildings. <br>
	 * This is added to as soon as a new building is placed. 
	 */
	public Map<Byte, BuildingInstance> buildings = new HashMap<>();
	/**
	 * All the entities 
	 */
	public Set<Entity> entities = new HashSet<>();

	/**
	 * Chunk contains a set of {@link Tile}
	 * @author Megan
	 *
	 */
	public class Chunk implements Serializable {
		private static final long serialVersionUID = 5810527395504498634L;

		/**
		 * Map of the tiles used in the chunk
		 */
		public Tile[][] tiles;

		public Chunk() {
			tiles = new Tile[chunkSize][chunkSize];
			for (int x = 0; x < chunkSize; x++) {
				for (int y = 0; y < chunkSize; y++) {
					tiles[x][y] = new Tile();
				}
			}
		}

		/**
		 * A single tile holds 2 pieces of information. <br>
		 * <ol>
		 * 	<li>the tile type, eg building-type-hospital - links to {@link SaveFile#tiles}</li>
		 * 	<li>the building ID - links to {@link SaveFile#buildings}</li>
		 * </ol>
		 * @author Megan
		 */
		public class Tile implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = 4696596418608513610L;
			public TileTypes type;
			public byte UUID = -1;
		}

	}

	public final FrameTimeManager frameTimeManager = new FrameTimeManager();
	public final Object gamePauseLock = new Object();
	public final AtomicBoolean quit = new AtomicBoolean(false), pause = new AtomicBoolean(true);

}
