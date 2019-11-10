package com.meeple.shared.frame.component;

import com.meeple.shared.frame.wrapper.Wrapper;
import com.meeple.shared.frame.wrapper.WrapperImpl;

/**
 * This class holds the desired frame time in nano-seconds (optionally calculated from desired framerate). 
 * set {@link #desiredFrameTime} to manually set the frame time or set {@link #desiredFrameRate} to calculate
 * Tries to put the thread to sleep for the calculated time. 
 *  
 * @author Megan
 *
 */
public class FrameTimeManager implements Runnable {

	private static final long nanoToMilli = 1000 * 1000;
	private static final long milliToSeconds = 1000 * nanoToMilli;

	/**
	 * If {@link #desiredFrameTime} is not set, then this will set the desired FPS 
	 */
	public long desiredFrameRate = 60;
	/**
	 * Sets the nano-seconds to sleep to maintain a stable framerate. 
	 */
	public Long desiredFrameTime = null;
	private long time = System.nanoTime();

	public Wrapper<Double> getWrapper() {
		Wrapper<Double> wrapper = new WrapperImpl<>();
		wrapper.setWrapped(1d / desiredFrameRate);
		return wrapper;
	}

	/**
	 * This calls thread.sleep<br>
	 * if thread was interupted then catches it and fowards it.
	 */
	@Override

	public void run() {

		long diff = diff();
		if (diff > 0) {
			int ns = (int) (diff % nanoToMilli);
			long ms = diff / nanoToMilli;
			try {
				Thread.sleep(ms, ns);
			} catch (InterruptedException err) {
				Thread.currentThread().interrupt();
			}
		}
		time = System.nanoTime();

	}

	private long diff() {

		long newTime, frameTime;
		long diff;
		newTime = System.nanoTime();
		frameTime = newTime - time;
		if (desiredFrameTime == null) {
			desiredFrameTime = milliToSeconds / desiredFrameRate;
		}
		diff = desiredFrameTime - frameTime;
		return diff;
	}

}
