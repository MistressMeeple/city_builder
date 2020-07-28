package com.meeple.shared.frame.nuklear;

import static org.lwjgl.nuklear.Nuklear.NK_TEXT_ALIGN_CENTERED;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_BACKGROUND;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_SCROLL_AUTO_HIDE;
import static org.lwjgl.nuklear.Nuklear.nk_button_label;
import static org.lwjgl.nuklear.Nuklear.nk_group_begin;
import static org.lwjgl.nuklear.Nuklear.nk_group_end;
import static org.lwjgl.nuklear.Nuklear.nk_label;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

import java.util.List;
import java.util.function.BiConsumer;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.component.Bounds2DComponent;
import com.meeple.shared.frame.window.ClientWindowSystem.ClientWindow;

public class NuklearMenuSystem extends NuklearManager {

	public static int topPadding = 50;
	public static int bottomPadding = 5;
	public static int buttonHeight = 35;
	public static int sub = 6;

	public enum BtnState {
		Disabled, Hidden, Visible;
	}

	public static interface Button {

		public BtnState getState();

		public String getName();

		public void onClick();
	}

	public static abstract class ButtonImpl implements Button {
		public String name;
		public BtnState state;

		public ButtonImpl(String name) {
			this.name = name;
			this.state = BtnState.Visible;
		}

		public ButtonImpl(String name, BtnState state) {

			this.name = name;
			this.state = state;
		}

		@Override
		public BtnState getState() {

			return state;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public abstract void onClick();

	}

	public static interface Menu {

		public void draw(NkContext context, MemoryStack stack);

		public boolean spacingBeforeDraw();

		public int height();

		public String getReturnName();

		public void returnClick();

		public BtnState getReturnState();

		public String getSecondaryName();

		public void secondaryClick();

		public BtnState getSecondaryState();

		public int getGroupTags();
	}

	public void setupMenu(ClientWindow window, NuklearUIComponent menuComponent, Menu menuDetails) {

//		menuComponent.container = window;
		Bounds2DComponent mainMenuContainerBounds = menuComponent.container.getBounds2DComponent();
		menuComponent.UUID = generateUUID();
		menuComponent.bounds.set((mainMenuContainerBounds.width / 10) * 1, 0, (mainMenuContainerBounds.width / 10) * 3, mainMenuContainerBounds.height);

		menuComponent.properties.add(NkWindowProperties.NO_SCROLLBAR);
		menuComponent.properties.add(NkWindowProperties.BACKGROUND);

		menuComponent.render = new BiConsumer<NkContext, MemoryStack>() {

			@Override
			public void accept(NkContext context, MemoryStack stack) {
				NkColor alpha = createColour(stack, 0, 0, 0, 0);
				NkColor alpha2 = createColour(stack, 0, 0, 0, (255 / 4));
				//TODO

				int btnGroup = (int) (buttonHeight * 4);
				int otherGroup = menuDetails.height();
				int totalSpacing = btnGroup + otherGroup;
				context.style().window().fixed_background().data().color(alpha);

				nk_layout_row_dynamic(context, topPadding, 1);
				if (nk_group_begin(context, "", 0)) {
					nk_group_end(context);
				}

				nk_layout_row_dynamic(context, buttonHeight, 1);
				nk_label(context, menuComponent.title, NK_TEXT_ALIGN_CENTERED);

				//write to bottom of page instead
				if (menuDetails.spacingBeforeDraw()) {
					nk_layout_row_dynamic(context, mainMenuContainerBounds.height - topPadding - bottomPadding - totalSpacing, 1);
					if (nk_group_begin(context, "Spacing", NK_WINDOW_SCROLL_AUTO_HIDE | NK_WINDOW_BACKGROUND)) {
						nk_group_end(context);
					}
				}

				nk_layout_row_dynamic(context, otherGroup - sub, 1);
				if (nk_group_begin(context, "Buttons", 0)) {
					menuDetails.draw(context, stack);
					nk_group_end(context);
				}

				if (!menuDetails.spacingBeforeDraw()) {
					nk_layout_row_dynamic(context, mainMenuContainerBounds.height - topPadding - bottomPadding - totalSpacing, 1);
					if (nk_group_begin(context, "Spacing", NK_WINDOW_SCROLL_AUTO_HIDE | NK_WINDOW_BACKGROUND)) {
						nk_group_end(context);
					}
				}
				{
					nk_layout_row_dynamic(context, btnGroup, 1);
					if (nk_group_begin(context, "Main", 0)) {

						nk_layout_row_dynamic(context, buttonHeight - sub, 1);
						switch (menuDetails.getSecondaryState()) {
							case Disabled:
								styledButton(context, getDisabled(context, stack), () -> {
									nk_button_label(context, menuDetails.getSecondaryName());
								});
								break;
							case Hidden:

								if (nk_group_begin(context, "btnSpacing", 0)) {
									nk_group_end(context);
								}

								break;
							case Visible:
								if (nk_button_label(context, menuDetails.getSecondaryName())) {
									menuDetails.secondaryClick();
								}
								break;
							default:
								break;

						}
						switch (menuDetails.getReturnState()) {
							case Disabled:
								styledButton(context, getDisabled(context, stack), () -> {
									nk_button_label(context, menuDetails.getReturnName());
								});
								break;
							case Hidden:

								if (nk_group_begin(context, "btnSpacing", 0)) {
									nk_group_end(context);
								}

								break;
							case Visible:
								if (nk_button_label(context, menuDetails.getReturnName())) {
									menuDetails.returnClick();
								}
								break;
							default:
								break;

						}

						context.style().window().fixed_background().data().color(alpha2);
						nk_group_end(context);
					}
				}
			}
		};

	}

	private static int UUID = 0;

	public static String generateUUID() {
		int uuid = UUID;
		UUID += 1;
		return "w." + uuid;
	}

	public static NkColor createColour(MemoryStack stack, int r, int g, int b, int a) {
		return NkColor.mallocStack(stack).set((byte) r, (byte) g, (byte) b, (byte) a);
	}

	public static NkColor getDisabled(NkContext context, MemoryStack stack) {

		NkColor normal = context.style().button().normal().data().color();
		NkColor disabled = createColour(stack, normal.r() * 2, normal.g() * 2, normal.b() * 2, 100);
		return disabled;
	}

	/**
	 * Removes all menu items from the active menu queue then navigates to the registered UI's UUID<br>
	 * Passing null to either the guis or the updateTo variables will clear the entire menu and not switch to any UI
	 * @param guis
	 * @param menuQueue
	 * @param updateTo
	 */
	/*public void setActiveNuklear(List<NuklearUIComponent> menuQueue, Map<String, NuklearUIComponent> guis, String updateTo) {
		NuklearUIComponent newUI = null;
		if (guis != null && updateTo != null) {
			newUI = guis.get(updateTo);
		}
		synchronized (menuQueue) {
			for (Iterator<NuklearUIComponent> iterator = menuQueue.iterator(); iterator.hasNext();) {
				NuklearUIComponent type = iterator.next();
				iterator.remove();
				setWindowInvisible(type, newUI);
			}
		}
		if (newUI != null) {
			navigateNuklear(menuQueue, newUI);
		}
	}
	
	public void navigateNuklear(Map<String, NuklearUIComponent> guis, List<NuklearUIComponent> menuQueue, String updateTo) {
		NuklearUIComponent update = guis.get(updateTo);
		if (update != null) {
			navigateNuklear(menuQueue, update);
		} else {
			System.out.println("No UI element found with UUID of '" + updateTo + "'");
		}
	
	}
	
	private void navigateNuklear(List<NuklearUIComponent> menuQueue, NuklearUIComponent nav) {
		List<NuklearUIComponent> queue = menuQueue;
		int queueSize = queue.size();
		if (queueSize > 0) {
			NuklearUIComponent prev = queue.get(queueSize - 1);
			setWindowInvisible(prev, nav);
		}
		setWindowVisible(nav);
		queue.add(nav);
	}
	
	public void goBackNuklear(List<NuklearUIComponent> menuQueue) {
		List<NuklearUIComponent> queue = menuQueue;
		int queueSize = queue.size();
		if (queueSize > 0) {
			NuklearUIComponent prev = queue.remove(queueSize - 1);
			NuklearUIComponent swapOut = null;
			if (queueSize > 1) {
				swapOut = queue.get(queueSize - 2);
			}
	
			setWindowInvisible(prev, swapOut);
			if (swapOut != null) {
				setWindowVisible(swapOut);
			}
		}
	
	}*/

	public static NuklearUIComponent getActiveMenu(List<NuklearUIComponent> menuQueue) {
		NuklearUIComponent current = null;
		if (!menuQueue.isEmpty()) {
			NuklearUIComponent curr = menuQueue.get(menuQueue.size() - 1);

			if (curr != null) {
				current = curr;
			}
		}
		return current;
	}

	protected void drawButtonList() {

	}

}
