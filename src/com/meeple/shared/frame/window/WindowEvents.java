package com.meeple.shared.frame.window;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.meeple.shared.Tickable;
import com.meeple.shared.frame.OGL.GLContext;
import com.meeple.shared.utils.CollectionSuppliers;

public class WindowEvents {

	//rendering events
	public Set<Consumer<GLContext>> frameStart = new CollectionSuppliers.SetSupplier<Consumer<GLContext>>().get();
	public Set<Consumer<GLContext>> preClear = new CollectionSuppliers.SetSupplier<Consumer<GLContext>>().get();
	public List<Tickable> render = new CollectionSuppliers.ListSupplier<Tickable>().get();
	public Set<Consumer<GLContext>> frameEnd = new CollectionSuppliers.SetSupplier<Consumer<GLContext>>().get();

	//creation events
	public Set<Consumer<GLContext>> preCreation = new CollectionSuppliers.SetSupplier<Consumer<GLContext>>().get();
	public Set<Consumer<GLContext>> postCreation = new CollectionSuppliers.SetSupplier<Consumer<GLContext>>().get();

	//destruction events
	public Set<Consumer<GLContext>> preCleanup = new CollectionSuppliers.SetSupplier<Consumer<GLContext>>().get();
	public Set<Runnable> postCleanup = new CollectionSuppliers.SetSupplier<Runnable>().get();

}
