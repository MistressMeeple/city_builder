package com.meeple.backend;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.GLFWManager;
import com.meeple.shared.frame.GLFWThread;
import com.meeple.shared.frame.component.FrameTimeManager;
import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.nuklear.NuklearManager;
import com.meeple.shared.frame.thread.ThreadManager.Builder;
import com.meeple.shared.frame.window.Window;
import com.meeple.shared.frame.window.WindowManager;
import com.meeple.shared.frame.window.WindowMonitorBoundsSystem;

public class Display {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
//			new Display().swingInit();
			new Display().glInit();
		});
	}

	public void swingInit() {
		JFrame frame = new JFrame("foo");
		frame.add(new JPanel());
		frame.setSize(400, 400);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void glInit() {

		ExecutorService service = Executors.newCachedThreadPool();
		try (GLFWManager glManager = new GLFWManager(); WindowManager windowManager = new WindowManager()) {

			NkContextSingleton nkContext = new NkContextSingleton();
			FrameTimeManager renderTimeManager = new FrameTimeManager();
			FrameTimeManager eventTimeManager = new FrameTimeManager();
			renderTimeManager.desiredFrameRate = eventTimeManager.desiredFrameRate = 60;
			
			Window window = new Window();
			window.bounds.size(400, 400);
			new WindowMonitorBoundsSystem().centerBoundsInMonitor(0, window.bounds);
			AtomicInteger quitCountdown = new AtomicInteger(1);
			window.loopThread = new GLFWThread(window, quitCountdown, renderTimeManager, true, new Runnable[] {});

			windowManager.create(window);
			Builder t = windowManager.generateManagerRunnable(quitCountdown, NuklearManager.globalEventsHandler(nkContext, windowManager.getActiveWindows()), eventTimeManager, window);
			service.execute(() -> window.loopThread.start());
			t.build().run();
		}
		FrameUtils.shutdownService(service, 1l, TimeUnit.SECONDS);

	}
}
