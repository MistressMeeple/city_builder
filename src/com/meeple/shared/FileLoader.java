package com.meeple.shared;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class FileLoader {
	public static Reader loadFile(String file) {
		Reader stream = loadInternal(file);
		if (stream == null) {
			stream = FileLoader.loadExternal(file);
		}
		return stream;
	}

	private static Reader loadInternal(String file) {
		InputStreamReader ret = null;
		InputStream stream = FileLoader.class.getResourceAsStream(file);
		if (stream == null) {
			stream = FileLoader.class.getResourceAsStream("/" + file);
		}
		try {
			ret = new InputStreamReader(stream);
		} catch (Exception e) {

		}
		return ret;
	}

	private static Reader loadExternal(String file) {
		FileReader f = null;
		try {
			f = new FileReader(new File(file));
		} catch (FileNotFoundException err) {
			err.printStackTrace();
		}
		return f;
	}

}
