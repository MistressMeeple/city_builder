package com.meeple.shared.frame.thread;

/**Class used to managed when a thread should close. Used by {@link ThreadManager}*/
public interface ThreadCloseManager {
	/**
	 * Check whether the thread loop should exit. <br>
	 * while(check)...
	 * @return true if thread should keep running, false if not 
	 */
	public boolean check();
}
