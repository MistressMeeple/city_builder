package com.meeple.shared.frame.nuklear;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12C.*;
import static org.lwjgl.opengl.GL13C.*;
import static org.lwjgl.opengl.GL14C.*;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.*;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.apache.log4j.Logger;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.nuklear.NkBuffer;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkConvertConfig;
import org.lwjgl.nuklear.NkDrawCommand;
import org.lwjgl.nuklear.NkImage;
import org.lwjgl.nuklear.NkMouse;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkUserFont;
import org.lwjgl.nuklear.NkUserFontGlyph;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.opengl.GL46;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.window.ActiveWindowsComponent;
import com.meeple.shared.frame.window.MirroredWindowCallbacks;
import com.meeple.shared.frame.window.Window;
import com.meeple.shared.frame.wrapper.Wrapper;

public class NuklearManager {

	public static Logger logger = Logger.getLogger(NuklearManager.class);

	public class RegisteredGUIS {
		public final Map<String, NuklearUIComponent> guis = Collections.synchronizedMap(new HashMap<>());
	}

	public static Runnable globalEventsHandler(NkContextSingleton context, ActiveWindowsComponent windows) {

		Runnable r = new Runnable() {
			@Override
			public void run() {
				nk_input_begin(context.context);
				glfwPollEvents();
				//TODO check if neccessary 

				NkMouse mouse = context.context.input().mouse();
				List<Window> list = windows.windows;

				synchronized (list) {
					Iterator<Window> i = list.iterator();
					while (i.hasNext()) {
						Window window = i.next();
						//						Boolean isNuklearWindow = (Boolean) window.properties.getOrDefault("isNuklearWindow", null);
						//						if (isNuklearWindow != null && isNuklearWindow) {
						if (mouse.grab()) {
							glfwSetInputMode(window.getID(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
						} else if (mouse.grabbed()) {
							float prevX = mouse.prev().x();
							float prevY = mouse.prev().y();
							glfwSetCursorPos(window.getID(), prevX, prevY);
							mouse.pos().x(prevX);
							mouse.pos().y(prevY);
						} else if (mouse.ungrab()) {
							glfwSetInputMode(window.getID(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
						}
						//						}
					}
				}

				nk_input_end(context.context);
			}
		};
		return r;
	}

	public void registerUI(RegisteredGUIS windows, NuklearUIComponent UI) {
		if (UI.UUID == null || UI.UUID.isEmpty()) {
			UI.UUID = NuklearMenuSystem.generateUUID();
			logger.trace("UUID of window '" + UI.title + "' was null. ");
		}
		windows.guis.put(UI.UUID, UI);

	}

	public void renderGUIs(NkContextSingleton context, Window window, Collection<NuklearUIComponent> uis) {
		Object o = null;
		if (uis != null && !uis.isEmpty()) {
			synchronized (uis) {
				Iterator<NuklearUIComponent> i = uis.iterator();
				while (i.hasNext()) {
					NuklearUIComponent ui = i.next();
					if (ui.visible) {
						try (MemoryStack stack = stackPush()) {
							BiConsumer<NkContext, MemoryStack> r = ui.preRender;
							if (r != null) {
								r.accept(context.context, stack);
							}
							NkRect rect = NkRect.mallocStack(stack);

							if (nk_begin(
								context.context,
								ui.title,
								nk_rect(ui.bounds.posX, ui.bounds.posY, ui.bounds.width, ui.bounds.height, rect),
								NkWindowProperties.flags(ui.properties))) {
								ui.render.accept(context.context, stack);
							}
							if (nk_window_has_focus(context.context)) {
								o = ui;
							}
							nk_end(context.context);
						}

					}
				}
			}
		}
		window.currentFocus = o;
	}

	public void create(NkContextSingleton context, Window window, RegisteredGUIS windows) {

		addWindowCallbacks(context, window);

		window.events.postCreation.add(new Runnable() {

			@Override
			public void run() {

				//assume we have context from manager
				setupContext(context);
				setupFont(context);
			}
		});

		//TODO not render here
		window.events.frameStart.add(() -> renderGUIs(context, window, windows.guis.values()));
		window.events.render
			.add(
				(delta) -> {

					/*
					 * IMPORTANT: `nk_glfw_render` modifies some global OpenGL state
					 * with blending, scissor, face culling, depth test and viewport and
					 * defaults everything back into a default state.
					 * Make sure to either a.) save and restore or b.) reset your own state after
					 * rendering the UI.
					 */
					render(context, window, NK_ANTI_ALIASING_ON, NkContextSingleton.MAX_VERTEX_BUFFER, NkContextSingleton.MAX_ELEMENT_BUFFER);
					return false;
				});
		window.events.preCleanup.add(new Runnable() {

			@Override
			public void run() {
				shutdown(context);
			}
		});
		//		window.properties.put("isNuklearWindow", true);
	}

	public void setWindowVisible(NuklearUIComponent ui) {
		FrameUtils.iterateRunnable(ui.open, false);
		ui.visible = true;

	}

	public void setWindowInvisible(NuklearUIComponent ui, NuklearUIComponent updatingTo) {
		ui.visible = false;
		FrameUtils.iterateConsumer(ui.close, updatingTo, false);
	}

	private void render(NkContextSingleton context, Window window, int AA, int max_vertex_buffer, int max_element_buffer) {

		try (MemoryStack stack = stackPush()) {

			// setup global state
			glEnable(GL_BLEND);
			glBlendEquation(GL_FUNC_ADD);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glDisable(GL_CULL_FACE);
			glDisable(GL_DEPTH_TEST);
			glEnable(GL_SCISSOR_TEST);
			glActiveTexture(GL_TEXTURE0);

			// setup program
			glUseProgram(context.prog);
			glUniform1i(context.uniform_tex, 0);
			glUniformMatrix4fv(
				context.uniform_proj,
				false,
				stack
					.floats(
						2.0f / window.bounds.width,
						0.0f,
						0.0f,
						0.0f,
						0.0f,
						-2.0f / window.bounds.height,
						0.0f,
						0.0f,
						0.0f,
						0.0f,
						-1.0f,
						0.0f,
						-1.0f,
						1.0f,
						0.0f,
						1.0f));

			// convert from command queue into draw list and draw to screen

			// allocate vertex and element buffer
			glBindVertexArray(context.vao);
			glBindBuffer(GL_ARRAY_BUFFER, context.vbo);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, context.ebo);
			glBufferData(GL_ARRAY_BUFFER, max_vertex_buffer, GL_STREAM_DRAW);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, max_element_buffer, GL_STREAM_DRAW);
			// load draw vertices & elements directly into vertex + element buffer

			//TODO this is making index out of range error
			ByteBuffer vertices = Objects.requireNonNull(glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY, max_vertex_buffer, null));
			ByteBuffer elements = Objects.requireNonNull(glMapBuffer(GL_ELEMENT_ARRAY_BUFFER, GL_WRITE_ONLY, max_element_buffer, null));

			// fill convert configuration
			NkConvertConfig config = NkConvertConfig
				.callocStack(stack)
				.vertex_layout(NkContextSingleton.VERTEX_LAYOUT)
				.vertex_size(20)
				.vertex_alignment(4)
				.null_texture(NkContextSingleton.null_texture)
				.circle_segment_count(22)
				.curve_segment_count(22)
				.arc_segment_count(22)
				.global_alpha(1.0f)
				.shape_AA(AA)
				.line_AA(AA);

			// setup buffers to load vertices and elements
			NkBuffer vbuf = NkBuffer.mallocStack(stack);
			NkBuffer ebuf = NkBuffer.mallocStack(stack);

			nk_buffer_init_fixed(vbuf, vertices/*, max_vertex_buffer*/);
			nk_buffer_init_fixed(ebuf, elements/*, max_element_buffer*/);
			nk_convert(context.context, context.cmds, vbuf, ebuf, config);
		}
		glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
		glUnmapBuffer(GL_ARRAY_BUFFER);

		// iterate over and execute each draw command
		float fb_scale_x = (float) window.frameBufferSizeX / (float) window.bounds.width;
		float fb_scale_y = (float) window.frameBufferSizeY / (float) window.bounds.height;

		long offset = NULL;
		for (NkDrawCommand cmd = nk__draw_begin(context.context, context.cmds); cmd != null; cmd = nk__draw_next(cmd, context.cmds, context.context)) {
			if (cmd.elem_count() == 0) {
				continue;
			}
			glBindTexture(GL_TEXTURE_2D, cmd.texture().id());
			glScissor(
				(int) (cmd.clip_rect().x() * fb_scale_x),
				(int) ((window.bounds.height - (int) (cmd.clip_rect().y() + cmd.clip_rect().h())) * fb_scale_y),
				(int) (cmd.clip_rect().w() * fb_scale_x),
				(int) (cmd.clip_rect().h() * fb_scale_y));
			glDrawElements(GL_TRIANGLES, cmd.elem_count(), GL_UNSIGNED_SHORT, offset);
			offset += cmd.elem_count() * 2;
		}
		nk_clear(context.context);

	}

	private void detatchAndDelete(NkContextSingleton context) {
		glDetachShader(context.prog, context.vert_shdr);
		glDetachShader(context.prog, context.frag_shdr);
		glDeleteShader(context.vert_shdr);
		glDeleteShader(context.frag_shdr);
		glDeleteProgram(context.prog);
		glDeleteTextures(NkContextSingleton.default_font.texture().id());
		glDeleteTextures(NkContextSingleton.null_texture.texture().id());
		glDeleteBuffers(context.vbo);
		glDeleteBuffers(context.ebo);
		nk_buffer_free(context.cmds);
	}

	private void shutdown(NkContextSingleton context) {
		context.hasClosed = true;
		Objects.requireNonNull(context.context.clip().copy()).free();
		Objects.requireNonNull(context.context.clip().paste()).free();
		nk_free(context.context);
		detatchAndDelete(context);
		Objects.requireNonNull(NkContextSingleton.default_font.query()).free();
		Objects.requireNonNull(NkContextSingleton.default_font.width()).free();

		Objects.requireNonNull(NkContextSingleton.ALLOCATOR.alloc()).free();
		Objects.requireNonNull(NkContextSingleton.ALLOCATOR.mfree()).free();
	}

	private void addWindowCallbacks(NkContextSingleton context, Window window) {
		MirroredWindowCallbacks cb = window.callbacks;
		cb.scrollCallbackSet.add(new GLFWScrollCallbackI() {

			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				try (MemoryStack stack = stackPush()) {
					NkVec2 scroll = NkVec2
						.mallocStack(stack)
						.x((float) xoffset)
						.y((float) yoffset);
					nk_input_scroll(context.context, scroll);

				}
			}
		});
		cb.charCallbackSet.add(new GLFWCharCallbackI() {

			@Override
			public void invoke(long window, int codepoint) {
				nk_input_unicode(context.context, codepoint);

			}
		});
		cb.keyCallbackSet.add((windowID, key, scancode, action, mods) -> {
			boolean press = action == GLFW_PRESS;
			switch (key) {

				case GLFW_KEY_DELETE:
					nk_input_key(context.context, NK_KEY_DEL, press);
					break;
				case GLFW_KEY_ENTER:
					nk_input_key(context.context, NK_KEY_ENTER, press);
					break;
				case GLFW_KEY_TAB:
					nk_input_key(context.context, NK_KEY_TAB, press);
					break;
				case GLFW_KEY_BACKSPACE:
					nk_input_key(context.context, NK_KEY_BACKSPACE, press);
					break;
				case GLFW_KEY_UP:
					nk_input_key(context.context, NK_KEY_UP, press);
					break;
				case GLFW_KEY_DOWN:
					nk_input_key(context.context, NK_KEY_DOWN, press);
					break;
				case GLFW_KEY_HOME:
					nk_input_key(context.context, NK_KEY_TEXT_START, press);
					nk_input_key(context.context, NK_KEY_SCROLL_START, press);
					break;
				case GLFW_KEY_END:
					nk_input_key(context.context, NK_KEY_TEXT_END, press);
					nk_input_key(context.context, NK_KEY_SCROLL_END, press);
					break;
				case GLFW_KEY_PAGE_DOWN:
					nk_input_key(context.context, NK_KEY_SCROLL_DOWN, press);
					break;
				case GLFW_KEY_PAGE_UP:
					nk_input_key(context.context, NK_KEY_SCROLL_UP, press);
					break;
				case GLFW_KEY_LEFT_SHIFT:
				case GLFW_KEY_RIGHT_SHIFT:
					nk_input_key(context.context, NK_KEY_SHIFT, press);
					break;
				case GLFW_KEY_LEFT_CONTROL:
				case GLFW_KEY_RIGHT_CONTROL:
					if (press) {
						nk_input_key(context.context, NK_KEY_COPY, glfwGetKey(windowID, GLFW_KEY_C) == GLFW_PRESS);
						nk_input_key(context.context, NK_KEY_PASTE, glfwGetKey(windowID, GLFW_KEY_P) == GLFW_PRESS);
						nk_input_key(context.context, NK_KEY_CUT, glfwGetKey(windowID, GLFW_KEY_X) == GLFW_PRESS);
						nk_input_key(context.context, NK_KEY_TEXT_UNDO, glfwGetKey(windowID, GLFW_KEY_Z) == GLFW_PRESS);
						nk_input_key(context.context, NK_KEY_TEXT_REDO, glfwGetKey(windowID, GLFW_KEY_R) == GLFW_PRESS);
						nk_input_key(context.context, NK_KEY_TEXT_WORD_LEFT, glfwGetKey(windowID, GLFW_KEY_LEFT) == GLFW_PRESS);
						nk_input_key(context.context, NK_KEY_TEXT_WORD_RIGHT, glfwGetKey(windowID, GLFW_KEY_RIGHT) == GLFW_PRESS);
						nk_input_key(context.context, NK_KEY_TEXT_LINE_START, glfwGetKey(windowID, GLFW_KEY_B) == GLFW_PRESS);
						nk_input_key(context.context, NK_KEY_TEXT_LINE_END, glfwGetKey(windowID, GLFW_KEY_E) == GLFW_PRESS);
					} else {
						nk_input_key(context.context, NK_KEY_LEFT, glfwGetKey(windowID, GLFW_KEY_LEFT) == GLFW_PRESS);
						nk_input_key(context.context, NK_KEY_RIGHT, glfwGetKey(windowID, GLFW_KEY_RIGHT) == GLFW_PRESS);
						nk_input_key(context.context, NK_KEY_COPY, false);
						nk_input_key(context.context, NK_KEY_PASTE, false);
						nk_input_key(context.context, NK_KEY_CUT, false);
						nk_input_key(context.context, NK_KEY_SHIFT, false);
					}
					break;
			}
		});
		cb.cursorPosCallbackSet.add((windowID, xpos, ypos) -> nk_input_motion(context.context, (int) xpos, (int) ypos));

		cb.mouseButtonCallbackSet.add((windowID, button, action, mods) -> {
			try (MemoryStack stack = stackPush()) {
				DoubleBuffer cx = stack.mallocDouble(1);
				DoubleBuffer cy = stack.mallocDouble(1);

				glfwGetCursorPos(windowID, cx, cy);

				int x = (int) cx.get(0);
				int y = (int) cy.get(0);

				int nkButton;
				switch (button) {
					case GLFW_MOUSE_BUTTON_RIGHT:
						nkButton = NK_BUTTON_RIGHT;
						break;
					case GLFW_MOUSE_BUTTON_MIDDLE:
						nkButton = NK_BUTTON_MIDDLE;
						break;
					default:
						nkButton = NK_BUTTON_LEFT;
				}
				nk_input_button(context.context, nkButton, x, y, action == GLFW_PRESS);

			}
		});

		nk_init(context.context, NkContextSingleton.ALLOCATOR, null);

		context.context
			.clip(
				it -> it
					.copy((handle, text, len) -> {
						if (len == 0) {
							return;
						}

						try (MemoryStack stack = stackPush()) {
							ByteBuffer str = stack.malloc(len + 1);
							memCopy(text, memAddress(str), len);
							str.put(len, (byte) 0);

							glfwSetClipboardString(window.getID(), str);
						}
					})
					.paste((handle, edit) -> {
						long text = nglfwGetClipboardString(window.getID());
						if (text != NULL) {
							nnk_textedit_paste(edit, text, nnk_strlen(text));
						}
					}));
	}

	private void setupContext(NkContextSingleton context) {

		String NK_SHADER_VERSION = Platform.get() == Platform.MACOSX ? "#version 150\n" : "#version 300 es\n";
		String vertex_shader =
			NK_SHADER_VERSION +
				"uniform mat4 ProjMtx;\n" +
				"in vec2 Position;\n" +
				"in vec2 TexCoord;\n" +
				"in vec4 Color;\n" +
				"out vec2 Frag_UV;\n" +
				"out vec4 Frag_Color;\n" +
				"void main() {\n" +
				"   Frag_UV = TexCoord;\n" +
				"   Frag_Color = Color;\n" +
				"   gl_Position = ProjMtx * vec4(Position.xy, 0, 1);\n" +
				"}\n";
		String fragment_shader =
			NK_SHADER_VERSION +
				"precision mediump float;\n" +
				"uniform sampler2D Texture;\n" +
				"in vec2 Frag_UV;\n" +
				"in vec4 Frag_Color;\n" +
				"out vec4 Out_Color;\n" +
				"void main(){\n" +
				"   Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n" +
				"}\n";

		nk_buffer_init(context.cmds, NkContextSingleton.ALLOCATOR, NkContextSingleton.BUFFER_INITIAL_SIZE);
		context.prog = glCreateProgram();
		context.vert_shdr = glCreateShader(GL_VERTEX_SHADER);
		context.frag_shdr = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(context.vert_shdr, vertex_shader);
		glShaderSource(context.frag_shdr, fragment_shader);
		glCompileShader(context.vert_shdr);
		glCompileShader(context.frag_shdr);
		if (glGetShaderi(context.vert_shdr, GL_COMPILE_STATUS) != GL_TRUE) {
			throw new IllegalStateException();
		}
		if (glGetShaderi(context.frag_shdr, GL_COMPILE_STATUS) != GL_TRUE) {
			throw new IllegalStateException();
		}
		glAttachShader(context.prog, context.vert_shdr);
		glAttachShader(context.prog, context.frag_shdr);
		glLinkProgram(context.prog);
		if (glGetProgrami(context.prog, GL_LINK_STATUS) != GL_TRUE) {
			throw new IllegalStateException();
		}

		context.uniform_tex = glGetUniformLocation(context.prog, "Texture");
		context.uniform_proj = glGetUniformLocation(context.prog, "ProjMtx");
		int attrib_pos = glGetAttribLocation(context.prog, "Position");
		int attrib_uv = glGetAttribLocation(context.prog, "TexCoord");
		int attrib_col = glGetAttribLocation(context.prog, "Color");

		{
			// buffer setup
			context.vbo = glGenBuffers();
			context.ebo = glGenBuffers();
			context.vao = glGenVertexArrays();

			glBindVertexArray(context.vao);
			glBindBuffer(GL_ARRAY_BUFFER, context.vbo);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, context.ebo);

			glEnableVertexAttribArray(attrib_pos);
			glEnableVertexAttribArray(attrib_uv);
			glEnableVertexAttribArray(attrib_col);

			glVertexAttribPointer(attrib_pos, 2, GL_FLOAT, false, 20, 0);
			glVertexAttribPointer(attrib_uv, 2, GL_FLOAT, false, 20, 8);
			glVertexAttribPointer(attrib_col, 4, GL_UNSIGNED_BYTE, true, 20, 16);
		}

		{
			// null texture setup
			int nullTexID = glGenTextures();

			NkContextSingleton.null_texture.texture().id(nullTexID);
			NkContextSingleton.null_texture.uv().set(0.5f, 0.5f);

			glBindTexture(GL_TEXTURE_2D, nullTexID);
			try (MemoryStack stack = stackPush()) {
				glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 1, 1, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, stack.ints(0xFFFFFFFF));
			}
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		}

		glBindTexture(GL_TEXTURE_2D, 0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		glBindVertexArray(0);

	}

	private void setupFont(NkContextSingleton context) {
		int BITMAP_W = 1024;
		int BITMAP_H = 1024;

		int FONT_HEIGHT = 18;
		int fontTexID = glGenTextures();

		STBTTFontinfo fontInfo = STBTTFontinfo.create();
		STBTTPackedchar.Buffer cdata = STBTTPackedchar.create(95);

		float scale;
		float descent;
		if (context.ttf == null) {
			try (MemoryStack stack = stackPush()) {

				try {
					context.ttf = IOUtil.ioResourceToByteBuffer(NkContextSingleton.fontFolder + context.fontName + NkContextSingleton.fontExtension, 512 * 1024);
				} catch (IOException err) {
					err.printStackTrace();
				}
			}
		}
		try (MemoryStack stack = stackPush()) {

			stbtt_InitFont(fontInfo, context.ttf);
			scale = stbtt_ScaleForPixelHeight(fontInfo, FONT_HEIGHT);

			IntBuffer d = stack.mallocInt(1);
			stbtt_GetFontVMetrics(fontInfo, null, d, null);
			descent = d.get(0) * scale;

			ByteBuffer bitmap = memAlloc(BITMAP_W * BITMAP_H);

			STBTTPackContext pc = STBTTPackContext.mallocStack(stack);
			stbtt_PackBegin(pc, bitmap, BITMAP_W, BITMAP_H, 0, 1, NULL);
			stbtt_PackSetOversampling(pc, 4, 4);
			stbtt_PackFontRange(pc, context.ttf, 0, FONT_HEIGHT, 32, cdata);
			stbtt_PackEnd(pc);

			// Convert R8 to RGBA8
			ByteBuffer texture = memAlloc(BITMAP_W * BITMAP_H * 4);
			for (int i = 0; i < bitmap.capacity(); i++) {
				texture.putInt((bitmap.get(i) << 24) | 0x00FFFFFF);
			}
			texture.flip();

			glBindTexture(GL_TEXTURE_2D, fontTexID);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, BITMAP_W, BITMAP_H, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, texture);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

			memFree(texture);
			memFree(bitmap);
		}
		if (NkContextSingleton.default_font == null) {
			NkContextSingleton.default_font = NkUserFont.create();

			NkContextSingleton.default_font
				.width((handle, h, text, len) -> {
					float text_width = 0;
					try (MemoryStack stack = stackPush()) {
						IntBuffer unicode = stack.mallocInt(1);

						int glyph_len = nnk_utf_decode(text, memAddress(unicode), len);
						int text_len = glyph_len;

						if (glyph_len == 0) {
							return 0;
						}

						IntBuffer advance = stack.mallocInt(1);
						while (text_len <= len && glyph_len != 0) {
							if (unicode.get(0) == NK_UTF_INVALID) {
								break;
							}

							/* query currently drawn glyph information */
							stbtt_GetCodepointHMetrics(fontInfo, unicode.get(0), advance, null);
							text_width += advance.get(0) * scale;

							/* offset next glyph */
							glyph_len = nnk_utf_decode(text + text_len, memAddress(unicode), len - text_len);
							text_len += glyph_len;
						}
					}
					return text_width;
				})
				.height(FONT_HEIGHT)
				.query((handle, font_height, glyph, codepoint, next_codepoint) -> {
					try (MemoryStack stack = stackPush()) {
						FloatBuffer x = stack.floats(0.0f);
						FloatBuffer y = stack.floats(0.0f);

						STBTTAlignedQuad q = STBTTAlignedQuad.mallocStack(stack);
						IntBuffer advance = stack.mallocInt(1);

						stbtt_GetPackedQuad(cdata, BITMAP_W, BITMAP_H, codepoint - 32, x, y, q, false);
						stbtt_GetCodepointHMetrics(fontInfo, codepoint, advance, null);

						NkUserFontGlyph ufg = NkUserFontGlyph.create(glyph);

						ufg.width(q.x1() - q.x0());
						ufg.height(q.y1() - q.y0());
						ufg.offset().set(q.x0(), q.y0() + (FONT_HEIGHT + descent));
						ufg.xadvance(advance.get(0) * scale);
						ufg.uv(0).set(q.s0(), q.t0());
						ufg.uv(1).set(q.s1(), q.t1());
					}
				})
				.texture(
					it -> it
						.id(fontTexID));
		}

		nk_style_set_font(context.context, NkContextSingleton.default_font);
	}

	public NkImage createImage(String imageLocation) {
		ByteBuffer imageBuffer;
		try {
			imageBuffer = IOUtil.ioResourceToByteBuffer(imageLocation, 8 * 1024);
		} catch (IOException e) {
			logger.error("Failed to load resource");
			return null;
		}
		try (MemoryStack stack = MemoryStack.stackPush();) {
			IntBuffer w = stack.mallocInt(1);
			IntBuffer h = stack.mallocInt(1);
			IntBuffer comp = stack.mallocInt(1);

			// These are the properties we need to define a texture in OpenGL
			if (!STBImage.stbi_info_from_memory(imageBuffer, w, h, comp)) {
				logger.error("Failed to read image information: " + STBImage.stbi_failure_reason());
			} else {
				logger.warn("image loaded with reason: " + STBImage.stbi_failure_reason());
			}
			ByteBuffer buffer = STBImage.stbi_load_from_memory(imageBuffer, w, h, comp, 4);

			int textureWidth = w.get(0);
			int textureHeight = h.get(0);

			// Generate OpenGL texture
			int textureID = GL46.glGenTextures();
			GL46.glBindTexture(GL46.GL_TEXTURE_2D, textureID);
			GL46.glPixelStorei(GL46.GL_UNPACK_ALIGNMENT, 1);

			// Set the texture parameters
			GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_LINEAR);
			GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_LINEAR_MIPMAP_LINEAR);
			GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_REPEAT);
			GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_REPEAT);

			// Store the texture data
			GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_RGBA, textureWidth, textureHeight, 0, GL46.GL_RGBA, GL46.GL_UNSIGNED_BYTE, buffer);

			// Enable mipmapping
			GL46.glGenerateMipmap(GL46.GL_TEXTURE_2D);

			// Unbind the texture
			GL46.glBindTexture(GL46.GL_TEXTURE_2D, 0);
			System.out.println(textureID);
			NkImage img = NkImage.callocStack(stack);
			img.handle(it -> it.id(textureID));

			return img;

		}
	}

	public static void styledButton(NkContext context, NkColor disabled, Runnable r) {
		nk_style_push_color(context, context.style().button().hover().data().color(), disabled);
		nk_style_push_color(context, context.style().button().normal().data().color(), disabled);
		nk_style_push_color(context, context.style().button().active().data().color(), disabled);
		r.run();
		nk_style_pop_color(context);
		nk_style_pop_color(context);
		nk_style_pop_color(context);
	}

	public static void textAreaPre(ByteBuffer textBuffer, Wrapper<String> text, int max) {

		textBuffer.clear();
		byte[] arr = text.getWrapped().getBytes();
		if (arr.length > max) {
			arr = Arrays.copyOfRange(arr, 0, max - 2);
		}
		textBuffer.put(arr);
		if (arr[arr.length - 1] != 0) {
			textBuffer.put((byte) 0);
		}
		textBuffer.flip();
	}

	public static void textAreaPost(Wrapper<String> text, ByteBuffer textBuffer) {

		if (textBuffer.hasRemaining()) {
			byte[] arr = new byte[textBuffer.remaining()];
			textBuffer.get(arr);
			text.setWrapped(new String(arr));
		} else {
			text.setWrapped("");
		}
	}
	/*
		protected NkColor createNKColor(MemoryStack stack, int r, int g, int b, int a) {
			byte br = (byte) r,
				bg = (byte) g,
				bb = (byte) b,
				ba = (byte) a;
			return NkColor.mallocStack(stack).set(br, bg, bb, ba);
		}*/
}
