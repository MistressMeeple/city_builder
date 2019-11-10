package com.meeple.shared.frame.nuklear;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.component.Bounds2DComponent;
import com.meeple.shared.frame.component.HasBounds2D;

public class NuklearUIComponent implements HasBounds2D {
	public HasBounds2D container;
	public Bounds2DComponent bounds = new Bounds2DComponent();
	public String UUID = "W" + System.currentTimeMillis();
	public String title = "Default Nuklear Window";
	public EnumSet<NkWindowProperties> properties = EnumSet.noneOf(NkWindowProperties.class);

	public BiConsumer<NkContext, MemoryStack> preRender;
	public BiConsumer<NkContext, MemoryStack> render;
	public boolean visible = false;
	/**
	 * Whether or not the component is a menu or a game element
	 */
	public boolean isMenuElement = true;
	//	public final Map<String, Object> additionalProperties = new HashMap<>();

	public Set<Runnable> open = Collections.synchronizedSet(new HashSet<>());
	public Set<Consumer<NuklearUIComponent>> close = Collections.synchronizedSet(new HashSet<>());

	@Override
	public Bounds2DComponent getBounds2DComponent() {
		return bounds;
	}

	public boolean isVisible() {
		return visible;
	}
}
