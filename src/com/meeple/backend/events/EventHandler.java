package com.meeple.backend.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import com.meeple.backend.entity.EntityBase;
import com.meeple.shared.CollectionSuppliers.SetSupplier;
import com.meeple.shared.frame.FrameUtils;

public class EventHandler {

	//	private List<EventListener<Event>> listeners = Collections.synchronizedList(new ArrayList<>());
	private List<EventBase> eventsQueue = Collections.synchronizedList(new ArrayList<>());

	private Map<Class<? extends EventBase>, Set<Consumer<? extends EventBase>>> listeners = new ConcurrentHashMap<>();
	private Supplier<Set<Consumer<? extends EventBase>>> listenerSetSuppler = new SetSupplier<>();

	public <E extends EventBase> void registerListener(Class<E> e, Consumer<E> listener) {
		FrameUtils.addToSetMap(listeners, e, listener, listenerSetSuppler);
	}

	public void sendEventAsync(EventBase event) {
		SwingUtilities.invokeLater(() ->
		{
			sendEventNow(event);
		});
	}

	public void sendEventAsync(Runnable runWhen, EventBase event) {
		SwingUtilities.invokeLater(() ->
		{
			runWhen.run();
			sendEventNow(event);
		});
	}

	public <T extends EntityBase> void sendEventNow(EventBase event) {

		Set<Consumer<? extends EventBase>> set = listeners.get(event.getClass());
		if (set != null && !set.isEmpty()) {
			for (Iterator<Consumer<? extends EventBase>> i = set.iterator(); i.hasNext();) {
				Consumer<? extends EventBase> listener = i.next();
				((Consumer<EventBase>) listener).accept(event);
			}

		}
	}

	/**
	 * Using this method you have to call either {@link #processQueueAsync()} or {@link #processQueueNow()} to actually use the event queue
	 * @param event to add to the queue
	 */
	public void queueEvent(EventBase event) {
		this.eventsQueue.add(event);
	}

	public void processQueueNow() {
		synchronized (eventsQueue) {
			for (Iterator<EventBase> i = eventsQueue.iterator(); i.hasNext();) {
				EventBase event = i.next();
				sendEventNow(event);
				i.remove();

			}
		}
	}

	public void processQueueAsync() {
		SwingUtilities.invokeLater(() ->
		{
			processQueueNow();
		});

	}
}
