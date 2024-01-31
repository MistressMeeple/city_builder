package com.meeple.shared.frame.thread;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiConsumer;

import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;
import com.meeple.shared.utils.CollectionSuppliers;
import com.meeple.shared.utils.FrameUtils;

public class ThreadManager {
	public static class Builder {
		ThreadCloseManager quit;
		UncaughtExceptionHandler exceptionHandler;
		Set<BiConsumer<Long, Float>> timedConsumers = new CollectionSuppliers.SetSupplier<BiConsumer<Long, Float>>().get();
		Set<Runnable> untimedConsumers = new CollectionSuppliers.SetSupplier<Runnable>().get();

		public Builder() {

		}

		public Builder setQuit(ThreadCloseManager quit) {
			this.quit = quit;
			return this;
		}

		public Builder add(Runnable r) {
			untimedConsumers.add(r);
			return this;
		}

		public Builder add(BiConsumer<Long, Float> c) {
			timedConsumers.add(c);
			return this;
		}

		public Runnable build() {

			Runnable r = new Runnable() {

				@Override
				public void run() {
					if (timedConsumers.isEmpty()) {
						untimedThreadLoop(quit, untimedConsumers);
					} else {
						timedThreadLoop(quit, timedConsumers, untimedConsumers);
					}

				}
			};
			return r;

		}
	}

	public void debugThreads() {

		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		synchronized (threadSet) {
			String out = "\r\n";
			for (Iterator<Thread> iterator = threadSet.iterator(); iterator.hasNext();) {
				Thread t = iterator.next();
				String name = t.getName();
				Thread.State state = t.getState();
				int priority = t.getPriority();
				String type = t.isDaemon() ? "Daemon" : "Normal";

				out += String.format("%-20s \t %s \t %d \t %s\n", name, state, priority, type);
			}
			System.out.println(out);
		}
	}

	/**
	 * This just returns a a runnable that encapsulates the {@link #untimedThreadLoop(ThreadCloseManager, Runnable...)} method.
	 * @param container Runnable to return
	 * @param quit the ThreadCloseManager to be used
	 * @param runnables an array of runnables to be run from the loop
	 */
	/*	public static void createRunnable( ThreadCloseManager quit, Runnable... runnables) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					untimedThreadLoop(quit, runnables);
				}
			};
			container.setWrapped(r);
		}
	
		@SafeVarargs
		public static void createRunnable( ThreadCloseManager quit, BiConsumer<Long, Float>... runnables) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					timedThreadLoop(quit, runnables);
				}
			};
			container.setWrapped(r);
		}*/

	private static void untimedThreadLoop(ThreadCloseManager quit, Set<Runnable> runnables) {
		if (!runnables.isEmpty()) {
			while (quit.check() && !Thread.currentThread().isInterrupted()) {
				FrameUtils.iterateRunnable(runnables, false);
			}
		} else {
			System.out.println("Thread had no tick events, not starting a loop. ");
		}
	}

	private static void timedThreadLoop(ThreadCloseManager quit, Set<BiConsumer<Long, Float>> timedConsumers, Set<Runnable> runnables) {
		Wrapper<Long> prev = new WrapperImpl<>(System.nanoTime());
		if (!timedConsumers.isEmpty()) {
			while (quit.check() && !Thread.currentThread().isInterrupted()) {
				///Time management
				long curr = System.nanoTime();
				long delta = curr - prev.getOrDefault(System.nanoTime());
				float deltaSeconds = FrameUtils.nanosToSeconds(delta);

				prev.set(curr);

				FrameUtils.iterateBiConsumer(timedConsumers, delta, deltaSeconds, false);
				FrameUtils.iterateRunnable(runnables, false);
			}
		} else {
			System.out.println("Thread had no tick events, not starting a loop. ");
		}
	}
}
