package com.meeple.backend;

import com.meeple.shared.frame.FrameUtils;

public final class FrameTimings {

	private long prev = System.nanoTime();
	public long totalNanos = 0;
	public float totalSeconds = 0f;
	public long deltaNanos;
	public float deltaSeconds;
	public float fps;

	public void tick() {
		//NOTE delta calculation per frame
		{
			long curr = System.nanoTime();
			deltaNanos = curr - prev;
			deltaSeconds = FrameUtils.nanosToSeconds(deltaNanos);
			fps = 1f / deltaSeconds;
			prev = curr;
			totalNanos += deltaNanos;
			totalSeconds += deltaSeconds;
		}
	}
}
