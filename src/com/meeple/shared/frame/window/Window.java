package com.meeple.shared.frame.window;

import static org.lwjgl.opengl.GL11.*;

import org.joml.Vector4f;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import com.meeple.shared.frame.component.Bounds2DComponent;
import com.meeple.shared.frame.component.EmptyComponent;
import com.meeple.shared.frame.component.HasBounds2D;

public class Window extends EmptyComponent implements HasBounds2D {
	public long windowID = 0;
	public String title = "Default Title";
	public Long monitor = MemoryUtil.NULL;
	public Long share = MemoryUtil.NULL;
	public boolean vSync = true;
	public GLCapabilities capabilities;
	public boolean created = false;
	public Thread loopThread;
	public boolean shouldClose = false;
	public boolean hasClosed = false;
	public int clearType = GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT;
	public int frameBufferSizeX;
	public int frameBufferSizeY;

	//final, so can never be null. all these components have methods to set the internal representations
	public final Bounds2DComponent bounds = new Bounds2DComponent();
	public final WindowHints hints = WindowHints.defaultWindowHints;
	public final WindowEvents events = new WindowEvents();
	public final Vector4f clearColour = new Vector4f(0, 0, 0, 0);
	public final MirroredWindowCallbacks callbacks = new MirroredWindowCallbacks();
	/**
	 * Normally which nuklear window has focus 
	 */
	public transient Object currentFocus = null;
	//	public final Map<String, Object> properties = new HashMap<>();

	@Override
	public Bounds2DComponent getBounds2DComponent() {
		return bounds;
	}

}
