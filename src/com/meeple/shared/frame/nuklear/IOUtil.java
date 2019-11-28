package com.meeple.shared.frame.nuklear;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.lwjgl.BufferUtils;

public final class IOUtil {

	private IOUtil() {
	}

	public static void writeToBuffer(InputStream stream, ByteBuffer buffer) throws IOException {
		try (ReadableByteChannel rbc = Channels.newChannel(stream)) {
			while (true) {
				int bytes = rbc.read(buffer);
				if (bytes == -1) {
					break;
				}
				if (buffer.remaining() == 0) {
					buffer = resizeBuffer(buffer, buffer.capacity() * 3 / 2); // 50%
					System.out.println(buffer.capacity() + " " + buffer.remaining() + " " + buffer.hasRemaining());
				}
			}
		}
	}

	private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
		ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
		buffer.flip();
		newBuffer.put(buffer);
		return newBuffer;
	}

	/**
	 * Reads the specified resource and returns the raw data as a ByteBuffer.
	 *
	 * @param resource   the resource to read
	 * @param bufferSize the initial buffer size
	 *
	 * @return the resource data
	 *
	 * @throws IOException if an IO error occurs
	 */
	public static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
		ByteBuffer buffer;
		URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
		if (url == null)
			throw new IOException("Classpath resource not found: " + resource);
		File file = new File(url.getFile());
		if (file.isFile()) {
			FileInputStream fis = new FileInputStream(file);
			FileChannel fc = fis.getChannel();
			buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			fc.close();
			fis.close();
		} else {
			buffer = BufferUtils.createByteBuffer(bufferSize);
			InputStream source = url.openStream();
			if (source == null)
				throw new FileNotFoundException(resource);
			try {
				byte[] buf = new byte[8192];
				while (true) {
					int bytes = source.read(buf, 0, buf.length);
					if (bytes == -1)
						break;
					if (buffer.remaining() < bytes)
						buffer = resizeBuffer(buffer, Math.max(buffer.capacity() * 2, buffer.capacity() - buffer.remaining() + bytes));
					buffer.put(buf, 0, bytes);
				}
				buffer.flip();
			} finally {
				source.close();
			}
		}
		return buffer;
	}

}
