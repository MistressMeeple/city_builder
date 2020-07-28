package com.meeple;

import org.lwjgl.glfw.GLFW;

import com.meeple.shared.frame.window.UserInput;
import com.meeple.shared.frame.window.UserInput.EventOrigin;

public class ClientOptions {
	private static final String settingsFile = "";
	private static final String _player = "player";
	private static final String _menu = "menu";
	private static final String connect = ".";

	public final UserInput.KeyBinding playerFoward, playerBack, playerLeft, playerRight, playerCrouch, playerJump, playerSprint, playerInteract,openMenu;
	

	public ClientOptions(UserInput userInput) {
		//generic camera controls
		playerFoward = userInput.new KeyBinding(_player + connect + "foward", GLFW.GLFW_KEY_W, EventOrigin.Keyboard);
		playerBack = userInput.new KeyBinding(_player + connect + "back", GLFW.GLFW_KEY_S, EventOrigin.Keyboard);
		playerLeft = userInput.new KeyBinding(_player + connect + "left", GLFW.GLFW_KEY_A, EventOrigin.Keyboard);
		playerRight = userInput.new KeyBinding(_player + connect + "right", GLFW.GLFW_KEY_D, EventOrigin.Keyboard);

		//first person controls
		playerCrouch = userInput.new KeyBinding(_player + connect + "crouch", GLFW.GLFW_KEY_LEFT_CONTROL, EventOrigin.Keyboard);
		playerJump = userInput.new KeyBinding(_player + connect + "jump", GLFW.GLFW_KEY_SPACE, EventOrigin.Keyboard);
		playerSprint = userInput.new KeyBinding(_player + connect + "sprint", GLFW.GLFW_KEY_LEFT_SHIFT, EventOrigin.Keyboard);

		playerInteract = userInput.new KeyBinding(_player + connect + "interact", GLFW.GLFW_MOUSE_BUTTON_LEFT, EventOrigin.Mouse);
		
		openMenu = userInput.new KeyBinding(_menu+connect+"pause", GLFW.GLFW_KEY_ESCAPE, EventOrigin.Keyboard);

	}

}
