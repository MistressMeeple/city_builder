package com.meeple.demo;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.meeple.backend.Client;
import com.meeple.backend.FrameTimings;

public class Demo extends Client{

	private static Logger logger = Logger.getLogger(Demo.class);

	private static String debugLayout = "[%r][%d{HH:mm:ss:SSS}][%t][%p] (%F:%L) %m%n";

	public static void main(String[] args) {

		Logger.getRootLogger().setLevel(org.apache.log4j.Level.ALL);
		Appender a = new ConsoleAppender(new PatternLayout(debugLayout));
		BasicConfigurator.configure(a);

		try (Demo d = new Demo()) {
			d.setup(1400, 800, "Barebones client demo");
			d.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void render(FrameTimings delta) {
		
	}

	@Override
	protected void setupGL() {
		logger.debug("Setup!");
		
	}
}
