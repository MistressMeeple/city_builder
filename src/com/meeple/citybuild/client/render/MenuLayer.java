package com.meeple.citybuild.client.render;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryStack.*;

import org.apache.log4j.Logger;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.system.MemoryStack;

import com.meeple.shared.frame.nuklear.NkContextSingleton;
import com.meeple.shared.frame.nuklear.NkWindowProperties;
import com.meeple.shared.frame.nuklear.NuklearManager;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem;
import com.meeple.shared.frame.nuklear.NuklearUIComponent;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.BtnState;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.Button;
import com.meeple.shared.frame.nuklear.NuklearMenuSystem.Menu;

public abstract class MenuLayer extends Renderable {
	/*
	public static Logger logger = Logger.getLogger(MenuLayer.class);
	
	@Override
	public void render() {
	Button continueBtn = new Button() {
	
	@Override
	public BtnState getState() {
	return BtnState.Visible;
	}
	
	@Override
	public String getName() {
	return "continue";
	}
	
	@Override
	public void onClick() {
	logger.trace("continue game click");
	}
	
	};
	Button loadBtn = new Button() {
	
	@Override
	public BtnState getState() {
	return BtnState.Visible;
	}
	
	@Override
	public String getName() {
	return "Load Game";
	}
	
	@Override
	public void onClick() {
	logger.trace("Load game click");
	}
	
	};
	Button newBtn = new Button() {
	
	@Override
	public BtnState getState() {
	return BtnState.Visible;
	}
	
	@Override
	public String getName() {
	return "New Game";
	}
	
	@Override
	public void onClick() {
	logger.trace("NEW game click");
	}
	};
	Menu mainMenuDetails = new Menu() {
	
	@Override
	public boolean spacingBeforeDraw() {
	return true;
	}
	
	@Override
	public void returnClick() {
	window.shouldClose = true;
	
	}
	
	@Override
	public void secondaryClick() {
	menuSystem.navigateNuklear(window.registeredNuklear, window.menuQueue, optionsMenu.UUID);
	
	}
	
	@Override
	public int height() {
	return (int) (NuklearMenuSystem.buttonHeight * (0.5f + 3));
	}
	
	@Override
	public BtnState getReturnState() {
	return BtnState.Visible;
	}
	
	@Override
	public BtnState getSecondaryState() {
	return BtnState.Visible;
	}
	
	@Override
	public String getSecondaryName() {
	return "Options";
	}
	
	@Override
	public String getReturnName() {
	return "Quit";
	}
	
	@Override
	public void draw(NkContext context, MemoryStack stack) {
	
	nk_layout_row_dynamic(context, NuklearMenuSystem.buttonHeight - NuklearMenuSystem.sub, 1);
	
	for (Button b : new Button[] { continueBtn, loadBtn, newBtn }) {
	if (b.getState() != BtnState.Disabled) {
	if (nk_button_label(context, b.getName())) {
	b.onClick();
	}
	} else {
	NuklearManager.styledButton(context, NuklearMenuSystem.getDisabled(context, stack), () -> {
	nk_button_label(context, b.getName());
	});
	}
	}
	
	}
	
	@Override
	public int getGroupTags() {
	return 0;
	}
	
	};
	
	}
	
	public void accept(NkContext context, MemoryStack stack) {
	NkColor alpha = createColour(stack, 0, 0, 0, 0);
	NkColor alpha2 = createColour(stack, 0, 0, 0, (255 / 4));
	
	int btnGroup = (int) (NuklearMenuSystem.buttonHeight * 4);
	int otherGroup = (int) (NuklearMenuSystem.buttonHeight * (0.5f + 3));
	int totalSpacing = btnGroup + otherGroup;
	context.style().window().fixed_background().data().color(alpha);
	
	{
	//padding section to put the buttons on bottom
	nk_layout_row_dynamic(context, NuklearMenuSystem.topPadding, 1);
	if (nk_group_begin(context, "", 0)) {
	nk_group_end(context);
	}
	}
	nk_layout_row_dynamic(context, NuklearMenuSystem.buttonHeight, 1);
	nk_label(context, menuComponent.title, NK_TEXT_ALIGN_CENTERED);
	
	nk_layout_row_dynamic(context, mainMenuContainerBounds.height - topPadding - bottomPadding - totalSpacing, 1);
	if (nk_group_begin(context, "Spacing", NK_WINDOW_SCROLL_AUTO_HIDE | NK_WINDOW_BACKGROUND)) {
	nk_group_end(context);
	}
	
	nk_layout_row_dynamic(context, otherGroup - sub, 1);
	if (nk_group_begin(context, "Buttons", 0)) {
	
	nk_layout_row_dynamic(context, NuklearMenuSystem.buttonHeight - NuklearMenuSystem.sub, 1);
	
	for (Button b : new Button[] { continueBtn, loadBtn, newBtn }) {
	if (b.getState() != BtnState.Disabled) {
	if (nk_button_label(context, b.getName())) {
	b.onClick();
	}
	} else {
	NuklearManager.styledButton(context, NuklearMenuSystem.getDisabled(context, stack), () -> {
	nk_button_label(context, b.getName());
	});
	}
	}
	
	nk_group_end(context);
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
	
	*/	public abstract void render(NkContext context, MemoryStack stack);

	public abstract void onButtonClick();
}
