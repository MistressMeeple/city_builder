package com.meeple.shared.frame.nuklear;

import static org.lwjgl.nuklear.Nuklear.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;

import org.lwjgl.nuklear.NkAllocator;
import org.lwjgl.nuklear.NkBuffer;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkDrawNullTexture;
import org.lwjgl.nuklear.NkDrawVertexLayoutElement;
import org.lwjgl.nuklear.NkUserFont;

public class NkContextSingleton {
	public static final String fontFolder = "resources/fonts/";
	public static final String fontExtension = ".ttf";
	public static NkUserFont default_font ;

	public static NkDrawNullTexture null_texture = NkDrawNullTexture.create();
	public static final int BUFFER_INITIAL_SIZE = 4 * 1024;
	public static final int MAX_VERTEX_BUFFER = 512 * 1024;
	public static final int MAX_ELEMENT_BUFFER = 128 * 1024;

	public static final NkAllocator ALLOCATOR;

	public static final NkDrawVertexLayoutElement.Buffer VERTEX_LAYOUT;

	static {
		ALLOCATOR = NkAllocator
			.create()
			.alloc((handle, old, size) -> nmemAllocChecked(size))
			.mfree((handle, ptr) -> nmemFree(ptr));

		VERTEX_LAYOUT = NkDrawVertexLayoutElement
			.create(4)
			.position(0)
			.attribute(NK_VERTEX_POSITION)
			.format(NK_FORMAT_FLOAT)
			.offset(0)
			.position(1)
			.attribute(NK_VERTEX_TEXCOORD)
			.format(NK_FORMAT_FLOAT)
			.offset(8)
			.position(2)
			.attribute(NK_VERTEX_COLOR)
			.format(NK_FORMAT_R8G8B8A8)
			.offset(16)
			.position(3)
			.attribute(NK_VERTEX_ATTRIBUTE_COUNT)
			.format(NK_FORMAT_COUNT)
			.offset(0)
			.flip();
	}

	public NkContext context = NkContext.create();
	public String fontName="FiraSans";
	/**
	 * It is important to store the TTF buffer externally for the STB library to use
	 */
	ByteBuffer ttf;

	public NkBuffer cmds = NkBuffer.create();
	public int vbo, vao, ebo;
	public int prog;
	public int vert_shdr;
	public int frag_shdr;
	public int uniform_tex;
	public int uniform_proj;
	public boolean hasClosed = false;

}
