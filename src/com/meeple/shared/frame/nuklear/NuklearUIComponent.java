package com.meeple.shared.frame.nuklear;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.component.Bounds2DComponent;
import com.meeple.shared.frame.component.HasBounds2D;

public class NuklearUIComponent implements HasBounds2D {
	public HasBounds2D container;
	/**
	 * Bounds in x-y and w-h
	 */
	public Bounds2DComponent bounds = new Bounds2DComponent();
	public String UUID = null;
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
	public boolean hasFocus = false;

	@Override
	public Bounds2DComponent getBounds2DComponent() {
		return bounds;
	}

	public boolean isVisible() {
		return visible;
	}
	/*
		public void render(NkContextSingleton context) {
	
			if (visible) {
				try (MemoryStack stack = stackPush()) {
					NkRect rect = NkRect.mallocStack(stack);
					nk_rect(bounds.posX, bounds.posY, bounds.width, bounds.height, rect);
					if (nk_begin(context.context, "title", rect, NkWindowProperties.flags(properties))) {
						render.accept(context.context, stack);
					}
					hasFocus = nk_window_has_focus(context.context);
					nk_end(context.context);
				}
	
			}
		}*/
}
