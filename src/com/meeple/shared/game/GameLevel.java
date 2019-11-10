package com.meeple.shared.game;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joml.Vector4f;

import com.meeple.shared.Tickable;

/**
 * Abstract class that holds base game level information. extend this class to impliment custom information like the world/actual game save data. 
 * @author Megan
 *
 */
public abstract class GameLevel implements Serializable {
	/**
	 * generated serial UUID
	 */
	private static final long serialVersionUID = -4303939847435592039L;

	/**
	 * how long the level has been active in nanos
	 */
	public long activeTime = 0;

	/**
	 * used when rendering, the clear colour of the window when rendering level
	 */
	public final Vector4f clearColour = new Vector4f();
	/**
	 * 
	 */
	public Random random;
	/**
		* Avoid directly using this as it might not be in sync lock. instead use {@link #addTick(Tickable)}
		*/

	transient public final Set<Tickable> ticks = Collections.synchronizedSet(new HashSet<>());
	transient protected final Set<Tickable> tickAddQueue = Collections.synchronizedSet(new HashSet<>());
	/**
		* Synchronous check to which tick set to add to
		*/
	transient public AtomicBoolean ticksClosed = new AtomicBoolean();

	public void addTick(Tickable tick) {
		if (ticksClosed.get()) {
			tickAddQueue.add(tick);
		} else {
			ticks.add(tick);
		}
	}

	public void postTick() {
		synchronized (tickAddQueue) {
			for (Iterator<Tickable> i = tickAddQueue.iterator(); i.hasNext();) {
				Tickable t = i.next();
				ticks.add(t);
			}
		}
	}

}
