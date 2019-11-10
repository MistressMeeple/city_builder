package com.meeple.shared.frame.window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.meeple.shared.Tickable;

public class WindowEvents {

	public Set<Runnable> frameStart = Collections.synchronizedSet(new HashSet<>());
	public Set<Runnable> preClear = Collections.synchronizedSet(new HashSet<>());
	public List<Tickable> render = Collections.synchronizedList(new ArrayList<>());
	public Set<Runnable> frameEnd = Collections.synchronizedSet(new HashSet<>());

	public Set<Runnable> preCreation = Collections.synchronizedSet(new HashSet<>());
	public Set<Runnable> postCreation = Collections.synchronizedSet(new HashSet<>());

	public Set<Runnable> preCleanup = Collections.synchronizedSet(new HashSet<>());
	public Set<Runnable> postCleanup = Collections.synchronizedSet(new HashSet<>());

	public Set<Runnable> preThreadStart = Collections.synchronizedSet(new HashSet<>());
	/**
	 * called AFTER capabilities have been set
	 */
	public Set<Runnable> postThreadStart = Collections.synchronizedSet(new HashSet<>());
	public Set<Runnable> threadEnd = Collections.synchronizedSet(new HashSet<>());

}
