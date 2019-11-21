package com.meeple.shared;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

public class ClientOptionSystem {
	private static Logger logger = Logger.getLogger(ClientOptionSystem.class);
	private static final File file = new File("options.txt");
	private static final String[] strSplits = { ":", "=" };

	public enum Delimeter {
		Int("i", Integer.class, "I\\s*" + strSplits[0] + "\\s*\\w*\\s*" + strSplits[1] + "\\s*\\d*", "%d"),
		Bool("b", Boolean.class, "B\\s*" + strSplits[0] + "\\s*\\w*\\s*" + strSplits[1] + "\\s*(true|false)", "%b"),
		Str("s", String.class, "S\\s*" + strSplits[0] + "\\s*\\w*\\s*" + strSplits[1] + "\\s*('|\")[\\w\\s]*('|\")", "%s");
		String initial;
		Class<?> clazz;
		String lineReadFormat;
		String lineWriteFormat;

		private Delimeter(String initial, Class<?> clazz, String lineRead, String lineWrite) {
			this.initial = initial;
			this.lineReadFormat = lineRead;
			this.lineWriteFormat = initial.toUpperCase() + strSplits[0] + "%s" + strSplits[1] + lineWrite + "\r\n";
			this.clazz = clazz;
		}

		public static Delimeter get(String s) {
			for (Delimeter d : Delimeter.values()) {
				if (s.equalsIgnoreCase(d.initial)) {
					return d;
				}
			}
			return null;
		}

		public static Delimeter get(Object o) {
			for (Delimeter d : Delimeter.values()) {
				if (o.getClass().isAssignableFrom(d.clazz)) {
					return d;
				}
			}
			return null;
		}

	}

	public void readSettingsFile(ClientOptions options) {
		logger.trace("Loading client options");
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

			String line;
			int lineIndex = 0;
			while ((line = reader.readLine()) != null) {
				String trim = line.trim();
				String[] split = trim.split("(:|=)");

				Delimeter delim = Delimeter.get(split[0].toLowerCase());
				if (split.length != 3 || delim == null) {
					System.out.println("unable to parse line: " + lineIndex + " '" + line + "'");
				} else {
					if (trim.matches(delim.lineReadFormat)) {
						Map<String, Object> map = options.options.get(delim);
						switch (delim) {
							case Bool:
								map.put(split[1].trim(), Boolean.parseBoolean(split[2].trim()));
								break;
							case Int:
								map.put(split[1].trim(), Integer.parseInt(split[2].trim()));
								break;
							case Str:
								String[] s = split[2].trim().split("(\":|')");
								map.put(split[1].trim(), s[s.length - 1]);
								break;
							default:
								break;

						}
					}
				}
				lineIndex += 1;
			}
		} catch (FileNotFoundException err) {
			logger.warn("Client options file not found, now creating the file with default contents");
			writeSettingsFile(options);
		} catch (IOException err) {
			logger.warn("IO exception when reading client settings", err);
		}

	}

	public void writeSettingsFile(ClientOptions options) {

		logger.trace("Saving client options");
		try {
			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();
		} catch (Exception e) {

		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			Set<Entry<Delimeter, Map<String, Object>>> set = options.options.entrySet();
			synchronized (set) {
				for (Iterator<Entry<Delimeter, Map<String, Object>>> iterator = set.iterator(); iterator.hasNext();) {
					Entry<Delimeter, Map<String, Object>> entry = iterator.next();
					Delimeter delim = entry.getKey();
					Map<String, Object> map = entry.getValue();

					Set<Entry<String, Object>> set3 = map.entrySet();
					synchronized (map) {
						for (Iterator<Entry<String, Object>> iterator2 = set3.iterator(); iterator2.hasNext();) {
							Entry<String, Object> entry2 = iterator2.next();
							String key = entry2.getKey();
							Object value = entry2.getValue();
							writer.write(String.format(delim.lineWriteFormat, key, value));

						}
					}

				}
			}
		} catch (IOException err) {
			logger.warn("failed to save client options", err);
		}

	}
}
