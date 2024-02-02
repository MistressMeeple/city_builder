package com.meeple.citybuild.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.joml.Vector2i;
import org.joml.Vector3f;

import com.meeple.citybuild.server.LevelData.Chunk;
import com.meeple.citybuild.server.LevelData.Chunk.Tile;
import com.meeple.shared.Delta;
import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;
import com.meeple.shared.utils.CollectionSuppliers;
import com.meeple.shared.utils.FrameUtils;

public abstract class GameManager {
	private static Logger logger = Logger.getLogger(GameManager.class);

	static final String LevelFolder = "saves/";
	static final String LevelExt = ".sv";
	static final long wait = 10l;
	public LevelData level;
	WorldGenerator worldGen = new WorldGenerator();
	private Thread levelThread;

	public synchronized void newGame(long seed) {
		level = new LevelData();
		level.random = new Random(seed);
		worldGen.create(level, seed);

		logger.trace("todo: new game ");
	}

	/**
	 * Reads the provided {@linkplain File} and converts to a
	 * {@linkplain LevelData}.<br>
	 * Returns null if failed to read.
	 * 
	 * @param fileIn
	 * @return Level read from file or null
	 */
	public synchronized void loadLevel(File fileIn) {
		if (fileIn == null) {
			logger.warn("no file given to load from");
			return;
		}
		logger.trace("Loading level from file: " + fileIn.toString());
		try (ObjectInputStream oos = new ObjectInputStream(new FileInputStream(fileIn))) {
			level = (LevelData) oos.readObject();
		} catch (FileNotFoundException err) {
			logger.error("File not found while loading", err);
		} catch (IOException err) {
			logger.error("IO Exception while loading", err);
		} catch (ClassNotFoundException err) {
			logger.error("Class not found while loading", err);
		}
	}

	public synchronized void saveGame() {

		if (level != null) {
			String name = level.name;
			if (name == null || name.isEmpty()) {

				Date date = new Date();
				name = "Save_" + DateFormat.getInstance().format(date);
				name = name.replace('/', '_');
				name = name.replace(':', '_');

			}
			File fileOut = new File(LevelFolder + name + LevelExt);
			fileOut.getParentFile().mkdirs();
			logger.trace("Saving level to file: " + fileOut.toString());
			logger.error("we arent saving!");
			if (false) {
				try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileOut))) {
					// oos.writeObject(level.getWrapped());
				} catch (FileNotFoundException err) {
					logger.error("File not found while loading", err);
				} catch (IOException err) {
					logger.error("IO Exception while loading", err);
				}
			}
		} else {
			logger.error("Cannot save game, no save game data to save");
		}

	}

	public synchronized void startGame() {
		levelThread = Thread.currentThread();
		if (level == null) {
			logger.error("No game loaded. cannot start game");
			return;
		}
		Wrapper<Long> prev = new WrapperImpl<>();
		Delta delta = new Delta();

		try {
			while (!Thread.currentThread().isInterrupted() && !level.quit.get()) {
				if (!level.pause.get()) {

					/// Time management
					long curr = System.nanoTime();
					delta.nanos = curr - prev.getOrDefault(curr);
					delta.seconds = FrameUtils.nanosToSeconds(delta.nanos);
					delta.totalNanos += delta.nanos;
					prev.set(curr);
					// logger.trace("level tick");

					if (level.frameTimeManager != null)
						level.frameTimeManager.run();
				} else {

					if (level.gamePauseLock == null) {
						Thread.sleep(wait);
					} else {
						synchronized (level.gamePauseLock) {
							while (level.pause.get()) {
								level.gamePauseLock.wait(wait);
							}
							prev.set(null);
						}

					}
				}
			}
			logger.trace("Level thread closing normally");
		} catch (InterruptedException err) {

			logger.trace("Level thread interupted", err);
		} finally {
			saveGame();
		}

	}

	public void pauseGame() {
		if (level == null) {
			logger.error("No game loaded. cannot pause game");
			return;
		}
		if (level.pause.compareAndSet(false, true)) {
			logger.trace("Pausing game");
		}
	}

	public void resumeGame() {
		if (level == null) {
			logger.error("No game loaded. cannot resume game");
			return;
		}
		if (level.pause.compareAndSet(true, false)) {
			logger.trace("Resuming game");
			synchronized (level.gamePauseLock) {
				level.gamePauseLock.notifyAll();
			}
		}
	}

	public void quitGame() {
		if (level == null) {
			logger.error("No game loaded. cannot quit game");
			return;
		}
		level.quit.compareAndSet(false, true);
		logger.trace("Quiting game");
		synchronized (level.gamePauseLock) {
			level.gamePauseLock.notifyAll();
		}
		try {
			levelThread.interrupt();
		} catch (Exception e) {
			// silent catch
		}
	}

	/**
	 * Finds the chunk that contains the world coord parameter
	 * 
	 * @param level       to search
	 * @param worldCoords position to find
	 * @return Chunk that owns the coords
	 */
	public Chunk getChunk(Vector3f worldCoords) {
		Vector2i chunkLoc = new Vector2i(chunk(worldCoords.x), chunk(worldCoords.y));
		Chunk c = level.chunks.get(chunkLoc);
		return c;
	}

	/**
	 * Finds the tile that contains the world coord passed
	 * 
	 * @param level       to search
	 * @param worldCoords to find the tile
	 * @return Tile that owns the coords
	 */
	public Tile getTile(Vector3f worldCoords) {
		Tile result = null;
		Chunk c = getChunk(worldCoords);
		if (c != null) {
			Vector2i index = new Vector2i(tileIndex(worldCoords.x), tileIndex(worldCoords.y));
			result = c.tiles[index.x][index.y];
		}
		return result;
	}

	public Tile getTile(Chunk c, Vector2i index) {
		Tile result = null;
		if (index != null) {
			result = c.tiles[index.x][index.y];
		}
		return result;
	}

	public Vector2i getTileIndex(Chunk c, Vector3f worldCoords) {
		Vector2i index = null;
		if (c != null) {
			index = new Vector2i(tileIndex(worldCoords.x), tileIndex(worldCoords.y));
		}
		return index;
	}

	/**
	 * Searches the sphere for all entities and returns a set containing any found.
	 * 
	 * @param level       level to search
	 * @param worldCoords sphere center
	 * @param radius      of sphere
	 * @return set of entities found
	 */
	public Set<Entity> getEntities(Vector3f worldCoords, float radius) {
		Set<Entity> result = new CollectionSuppliers.SetSupplier<Entity>().get();
		for (Entity e : level.entities) {
			if (e.position.distance(worldCoords) <= radius) {
				result.add(e);
			}
		}
		logger.warn("not implimented");
		return result;
	}

	public static int chunk(float world) {
		float calc = world / (LevelData.chunkSize * LevelData.tileSize);
		if (calc < 0) {
			calc -= 1f;
		}
		int ret = (int) calc;
		return ret;
	}

	public static int tileIndex(float chunk) {
		float calc = chunk % (LevelData.chunkSize * LevelData.tileSize);
		float calc2 = calc / LevelData.tileSize;
		int calc3 = (int) calc2;
		int result = calc3;
		if (chunk < 0) {
			result += LevelData.chunkSize - 1;
		}
		return result;
	}

	public abstract void levelTick(Delta delta);

}
