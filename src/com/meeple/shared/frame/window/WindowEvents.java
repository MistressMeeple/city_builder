package com.meeple.shared.frame.window;

import java.util.List;
import java.util.Set;

import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.Tickable;

public class WindowEvents {

	//rendering events
	public Set<Runnable> frameStart = new CollectionSuppliers.SetSupplier<Runnable>().get();
	public Set<Runnable> preClear = new CollectionSuppliers.SetSupplier<Runnable>().get();
	public List<Tickable> render = new CollectionSuppliers.ListSupplier<Tickable>().get();
	public Set<Runnable> frameEnd = new CollectionSuppliers.SetSupplier<Runnable>().get();

	//creation events
	public Set<Runnable> preCreation = new CollectionSuppliers.SetSupplier<Runnable>().get();
	public Set<Runnable> postCreation = new CollectionSuppliers.SetSupplier<Runnable>().get();

	//destruction events
	public Set<Runnable> preCleanup = new CollectionSuppliers.SetSupplier<Runnable>().get();
	public Set<Runnable> postCleanup = new CollectionSuppliers.SetSupplier<Runnable>().get();

}
