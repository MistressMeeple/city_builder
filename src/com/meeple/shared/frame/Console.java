package com.meeple.shared.frame;

import static org.lwjgl.nuklear.Nuklear.*;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkDrawList;
import org.lwjgl.nuklear.NkHandle;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.component.Bounds2DComponent;
import com.meeple.shared.frame.nuklear.NuklearUIComponent;
import com.meeple.shared.frame.window.Window;

public class Console {

	public enum ConsoleLoggingLevel {
		NONE(0), ERROR(1), WARN(2), GENERAL(3), DEBUG(4);

		int index;

		private ConsoleLoggingLevel(int index) {
			this.index = index;
		}

	}

	private static class basePrint extends PrintStream {
		ConsoleLoggingLevel level;
		Console console;

		public basePrint(OutputStream out, Console console, ConsoleLoggingLevel level) {
			super(out);
			this.level = level;
			this.console = console;
		}

		public void write(byte[] buf, int off, int len) {
			//super inefficient way of prepending class that called

			String full = new String(buf, off, len);
			if (!full.trim().equalsIgnoreCase("")) {
				ConsoleEntry entry = console.constructConsoleEntry(full, level);
				full = entry.toString();
			}
			super.write(full.getBytes(), 0, full.length());

		};

	};

	public static Console instance = new Console();

	public static PrintStream out = new basePrint(System.out, Console.instance, ConsoleLoggingLevel.DEBUG);
	public static PrintStream err = new basePrint(System.err, Console.instance, ConsoleLoggingLevel.ERROR);

	private static final ConcurrentLinkedQueue<ConsoleEntry> fullLog = new ConcurrentLinkedQueue<>();
	static boolean showTimestampFlag = true;
	static boolean showLevelFlag = true;
	static boolean showCallerFlag = true;
	static EnumSet<ConsoleLoggingLevel> showLevel = EnumSet.allOf(ConsoleLoggingLevel.class);

	public static int MaxLogSize = 400;
	private static String format = "HH:mm:ss";
	private static final ThreadLocal<SimpleDateFormat> formatter = ThreadLocal.withInitial(new Supplier<SimpleDateFormat>() {
		@Override
		public SimpleDateFormat get() {
			SimpleDateFormat d = new SimpleDateFormat(format);
			return d;
		}
	});

	public Console() {

	}

	public static final void changeTimeFormat(String newFormat) {
		//only update if not the same
		if (format != newFormat) {
			format = newFormat;
			//re evaluate
			formatter.remove();
		}
	}

	public ConsoleEntry constructConsoleEntry(String message, ConsoleLoggingLevel level) {
		Exception ex = new Exception();
		StackTraceElement el = ex.getStackTrace()[9];
		ConsoleEntry ce = new ConsoleEntry(message, formatter.get(), level, el);

		fullLog.add(ce);

		//this should only be run once per call but just in case some overloading or external tampering
		while (fullLog.size() > MaxLogSize) {
			fullLog.poll();
		}

		return ce;
	}

	public Iterator<ConsoleEntry> getFullConsole() {
		Iterator<ConsoleEntry> i = fullLog.iterator();
		return i;
	}

	private class ConsoleEntry {

		public final String message;
		public final SimpleDateFormat time;
		public final ConsoleLoggingLevel level;
		public final StackTraceElement caller;

		private ConsoleEntry(String message, SimpleDateFormat time, ConsoleLoggingLevel level, StackTraceElement caller) {
			super();
			this.message = message;
			this.time = time;
			this.level = level;
			this.caller = caller;
		}

		@Override
		public String toString() {
			return "[" + time.format(new Date()) + "][" + level + "][ " + caller + " ] " + message;
		}

	}

	public static class ConsoleWindow extends NuklearUIComponent {
		public ConsoleWindow(Window window) {
			title = "Console";
			render = new BiConsumer<NkContext, MemoryStack>() {

				@Override
				public void accept(NkContext context, MemoryStack stack) {
					int cols = 1;
					if (showTimestampFlag) {
						cols += 1;
					}
					if (showLevelFlag) {
						cols += 1;
					}
					if (showCallerFlag) {
						cols += 1;
					}
					nk_layout_row_dynamic(context, window.bounds.height, cols); // wrapping row
					if (showTimestampFlag) {
						if (nk_group_begin(context, "Time", NK_WINDOW_BORDER | NK_WINDOW_TITLE)) { // column 1
							nk_layout_row_dynamic(context, 30, 1); // nested row
							nk_label(context, "Time", NK_TEXT_LEFT);

							NkDrawList list = NkDrawList.mallocStack(stack);
							list.userdata(new Consumer<NkHandle>() {

								@Override
								public void accept(NkHandle t) {

								}
							});
							nk_draw_list_init(list);

							nk_group_end(context);
						}
					}
					if (showLevelFlag) {

						if (nk_group_begin(context, "Level", NK_WINDOW_BORDER | NK_WINDOW_TITLE)) { // column 2
							nk_layout_row_dynamic(context, 30, 1);
							nk_label(context, "column 2.1", NK_TEXT_LEFT);

							nk_layout_row_dynamic(context, 30, 1);
							nk_label(context, "column 2.2", NK_TEXT_LEFT);

							nk_group_end(context);
						}
					}
					if (showCallerFlag) {

						if (nk_group_begin(context, "Caller", NK_WINDOW_BORDER | NK_WINDOW_TITLE)) { // column 2
							nk_layout_row_dynamic(context, 30, 1);
							nk_label(context, "column 2.1", NK_TEXT_LEFT);

							nk_layout_row_dynamic(context, 30, 1);
							nk_label(context, "column 2.2", NK_TEXT_LEFT);

							nk_group_end(context);
						}
					}

					if (nk_group_begin(context, "Message", NK_WINDOW_BORDER | NK_WINDOW_TITLE)) { // column 2
						nk_layout_row_dynamic(context, 30, 1);
						nk_label(context, "column 2.1", NK_TEXT_LEFT);

						nk_layout_row_dynamic(context, 30, 1);
						nk_label(context, "column 2.2", NK_TEXT_LEFT);

						nk_group_end(context);
					} /*
						Iterator<ConsoleEntry> i = fullLog.iterator();
						while(i.hasNext()) {
						if (nk_group_begin(context, "column2", NK_WINDOW_BORDER)) { // column 2
							nk_layout_row_dynamic(context, 30, 1);
							nk_label(context, "column 2.1", NK_TEXT_LEFT);
						
							nk_layout_row_dynamic(context, 30, 1);
							nk_label(context, "column 2.2", NK_TEXT_LEFT);
						
							nk_group_end(context);
						}
						}*/
				}
			};
			Bounds2DComponent w = window.bounds;
			this.bounds.set(0, 0, w.width, w.height);

		}
	}

}
