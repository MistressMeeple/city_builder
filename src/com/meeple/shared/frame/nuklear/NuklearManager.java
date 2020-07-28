package com.meeple.shared.frame.nuklear;

import static org.lwjgl.nuklear.Nuklear.NK_BUTTON_LEFT;
import static org.lwjgl.nuklear.Nuklear.NK_BUTTON_MIDDLE;
import static org.lwjgl.nuklear.Nuklear.NK_SYMBOL_TRIANGLE_DOWN;
import static org.lwjgl.nuklear.Nuklear.NK_SYMBOL_TRIANGLE_UP;
import static org.lwjgl.nuklear.Nuklear.NK_TEXT_ALIGN_LEFT;
import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_BORDER;
import static org.lwjgl.nuklear.Nuklear.nk_button_symbol_label;
import static org.lwjgl.nuklear.Nuklear.nk_fill_rect;
import static org.lwjgl.nuklear.Nuklear.nk_group_begin;
import static org.lwjgl.nuklear.Nuklear.nk_group_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_stroke_line;
import static org.lwjgl.nuklear.Nuklear.nk_stroke_rect;
import static org.lwjgl.nuklear.Nuklear.nk_style_pop_color;
import static org.lwjgl.nuklear.Nuklear.nk_style_push_color;
import static org.lwjgl.system.MemoryUtil.memASCII;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.apache.log4j.Logger;
import org.joml.Rectanglef;
import org.joml.Vector2f;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkCommandBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkImage;
import org.lwjgl.nuklear.NkPluginFilterI;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL46;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

public class NuklearManager {

	public static Logger logger = Logger.getLogger(NuklearManager.class);

	/*	public static Runnable globalEventsHandler(NkContextSingleton context, ActiveWindowsComponent windows) {
	
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
								glfwSetInputMode(window.windowID, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
							} else if (mouse.grabbed()) {
								float prevX = mouse.prev().x();
								float prevY = mouse.prev().y();
								glfwSetCursorPos(window.windowID, prevX, prevY);
								mouse.pos().x(prevX);
								mouse.pos().y(prevY);
							} else if (mouse.ungrab()) {
								glfwSetInputMode(window.windowID, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
							}
							//						}
						}
					}
	
					nk_input_end(context.context);
				}
			};
			return r;
		}
	
		public void registerUI(Map<String, NuklearUIComponent> windows, NuklearUIComponent UI) {
			if (UI.UUID == null || UI.UUID.isEmpty()) {
				UI.UUID = NuklearMenuSystem.generateUUID();
				logger.trace("UUID of window '" + UI.title + "' was null. ");
			}
			windows.put(UI.UUID, UI);
	
		}
	
		
			private void renderGUIs(NkContextSingleton context, Window window, Collection<NuklearUIComponent> uis) {
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
		
		public void create(ClientWindow window, Map<String, NuklearUIComponent> windows) {
	
			addWindowCallbacks(window.nkContext, window);
	
			window.events.postCreation.add(new Runnable() {
	
				@Override
				public void run() {
	
					//assume we have context from manager
					setupContext(window.nkContext);
					setupFont(window.nkContext);
				}
			});
	
			//TODO not render here
			//		window.events.frameStart.add(() -> renderGUIs(context, window, windows.guis.values()));
			window.events.render
				.add(
					(delta) -> {
	
						 IMPORTANT: `nk_glfw_render` modifies some global OpenGL state
						* with blending, scissor, face culling, depth test and viewport and
						* defaults everything back into a default state.
						* Make sure to either a.) save and restore or b.) reset your own state after
						* rendering the UI.
						boolean blend = glGetBoolean(GL_BLEND);
						boolean cull = glGetBoolean(GL_CULL_FACE);
						boolean depth = glGetBoolean(GL_DEPTH_TEST);
						boolean scissor = glGetBoolean(GL_SCISSOR_TEST);
						
	
						render(window.nkContext, window, NK_ANTI_ALIASING_ON, NkContextSingleton.MAX_VERTEX_BUFFER, NkContextSingleton.MAX_ELEMENT_BUFFER);
	
						if (blend) {
							glEnable(GL_BLEND);
						} else {
							glDisable(GL_BLEND);
						}
						if (cull) {
							glEnable(GL_CULL_FACE);
						} else {
							glDisable(GL_CULL_FACE);
						}
						if (depth) {
							glEnable(GL_DEPTH_TEST);
						} else {
							glDisable(GL_DEPTH_TEST);
						}
	
						if (scissor) {
							glEnable(GL_SCISSOR_TEST);
						} else {
							glDisable(GL_SCISSOR_TEST);
						}
	
						return false;
					});
			window.events.preCleanup.add(new Runnable() {
	
				@Override
				public void run() {
					shutdown(window.nkContext);
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
	
				nk_buffer_init_fixed(vbuf, vertices, max_vertex_buffer);
				nk_buffer_init_fixed(ebuf, elements, max_element_buffer);
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
	
								glfwSetClipboardString(window.windowID, str);
							}
						})
						.paste((handle, edit) -> {
							long text = nglfwGetClipboardString(window.windowID);
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
	
	*/

	public static boolean collapsableGroup(NkContext ctx, String title, int btnHeight, boolean show, int subMenuHeight, Runnable inner) {

		if (show) {
			nk_layout_row_dynamic(ctx, btnHeight, 1);
			if (nk_button_symbol_label(ctx, NK_SYMBOL_TRIANGLE_UP, title, NK_TEXT_ALIGN_LEFT)) {
				show = false;
			}
			nk_layout_row_dynamic(ctx, subMenuHeight, 1);
			if (nk_group_begin(ctx, title + "submenu", NK_WINDOW_BORDER)) {
				inner.run();
				nk_group_end(ctx);
			}
		} else {
			nk_layout_row_dynamic(ctx, btnHeight, 1);
			if (nk_button_symbol_label(ctx, NK_SYMBOL_TRIANGLE_DOWN, title, NK_TEXT_ALIGN_LEFT)) {
				show = true;
			}
		}
		return show;
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

	/*public static void textAreaPre(ByteBuffer textBuffer, Wrapper<String> text, int max) {
	
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
	*/
	public static String textArea(NkContext ctx, MemoryStack stack, String string, int maxLen, int flags, NkPluginFilterI filter) {
		ByteBuffer buffer = stack.calloc(maxLen);
		memASCII(string, true, buffer);
		Nuklear.nk_edit_string_zero_terminated(ctx, flags, buffer, maxLen - 1, filter);
		return memASCII(buffer);
	}

	public static void setNkColour(NkColor colour, int r, int g, int b) {
		byte br = (byte) r,
			bg = (byte) g,
			bb = (byte) b;
		colour.r(br).g(bg).b(bb);
	}

	public static void setNkColour(NkColor colour, int r, int g, int b, int a) {
		byte br = (byte) r,
			bg = (byte) g,
			bb = (byte) b,
			ba = (byte) a;
		colour.r(br).g(bg).b(bb).a(ba);
	}

	private static boolean nkGraphBehaivior(NkContext ctx, Vector2f position, Rectanglef graphValues) {
		boolean update = false;
		NkRect bounds = NkRect.create();
		boolean canMove = false;
		boolean reset = false;
		Nuklear.nk_widget_bounds(ctx, bounds);
		if (Nuklear.nk_widget_has_mouse_click_down(ctx, NK_BUTTON_LEFT, true)) {
			canMove = true;
		}
		if (Nuklear.nk_widget_has_mouse_click_down(ctx, NK_BUTTON_MIDDLE, true)) {
			reset = true;
		}
		float x = position.y;
		float y = position.x;

		/* update location */
		if (Nuklear.nk_input_is_mouse_down(ctx.input(), NK_BUTTON_LEFT) && canMove || reset) {

			float ax = (ctx.input().mouse().pos().x() - bounds.x()) / bounds.w();
			float ay = (ctx.input().mouse().pos().y() - bounds.y()) / bounds.h();
			x = ax;
			y = ay;
			if (reset) {
				x = 0.5f;
				y = 0.5f;
				update |= true;
			} else

			if (Nuklear.nk_input_is_mouse_hovering_rect(ctx.input(), bounds)) {
				update = update | true;
			} else {
				//clamp instead

				//min %
				if (x < 0) {
					x = 0;
				}
				if (x > 1) {
					x = 1;
				}
				if (y < 0) {
					y = 0;
				}
				if (y > 1) {
					y = 1;
				}

				update = update | true;
			}
			x = graphValues.minX + (graphValues.maxX - graphValues.minX) * x;
			y = graphValues.minY + (graphValues.maxY - graphValues.minY) * y;

		}
		if (update && (position.x != x || position.y != y)) {

			position.x = x;
			position.y = y;
		}
		return update;
	}

	private static void nkGraphDraw(NkContext ctx, Vector2f position, Rectanglef graphValues) {
		try (MemoryStack stack = MemoryStack.stackPush()) {

			NkRect bounds = NkRect.callocStack(stack);
			Nuklear.nk_widget(bounds, ctx);
			NkCommandBuffer o = Nuklear.nk_window_get_canvas(ctx);
			nk_fill_rect(o, bounds, ctx.style().chart().rounding(), ctx.style().chart().background().data().color());
			nk_stroke_rect(o, bounds, ctx.style().chart().rounding(), ctx.style().chart().border(), ctx.style().chart().border_color());

			nk_stroke_line(
				o,
				bounds.w() / 2 + bounds.x(),
				bounds.y(),
				bounds.w() / 2 + bounds.x(),
				bounds.y() + bounds.w(),
				1.0f,
				ctx.style().chart().border_color());
			nk_stroke_line(o, bounds.x(), bounds.h() / 2 + bounds.y(), bounds.x() + bounds.w(), bounds.h() / 2 + bounds.y(), 1.0f, ctx.style().chart().border_color());

			/* draw cross-hair */

			{
				float midX = (graphValues.maxX - graphValues.minX);
				float midY = (graphValues.maxY - graphValues.minY);

				float dx = (position.x - graphValues.minX) / midX;
				float dy = (position.y - graphValues.minY) / midY;

				Vector2f draw = new Vector2f();
				draw.x = ((bounds.w() * dx) + bounds.x());
				draw.y = ((bounds.h() * dy) + bounds.y());

				float crosshair_size = 7.0f;
				NkColor white = NkColor.create();
				NuklearManager.setNkColour(white, 255, 255, 255, 255);
				NuklearManager.nk_crosshair(ctx, draw, crosshair_size, white);
			}
		}
	}

	public static boolean nk_graph(NkContext ctx, Vector2f position, Rectanglef graphValues) {
		boolean update = nkGraphBehaivior(ctx, position, graphValues);
		nkGraphDraw(ctx, position, graphValues);
		return update;
	}


	public static void nk_crosshair(NkContext ctx, Vector2f position, float crosshair_size, NkColor colour) {
		NkCommandBuffer o = Nuklear.nk_window_get_canvas(ctx);
		nk_stroke_line(o, position.x() - crosshair_size, position.y(), position.x() - 2, position.y(), 1.0f, colour);
		nk_stroke_line(o, position.x() + crosshair_size + 1, position.y(), position.x() + 3, position.y(), 1.0f, colour);
		nk_stroke_line(o, position.x(), position.y() + crosshair_size + 1, position.x(), position.y() + 3, 1.0f, colour);
		nk_stroke_line(o, position.x(), position.y() - crosshair_size, position.x(), position.y() - 2, 1.0f, colour);
	}
}
