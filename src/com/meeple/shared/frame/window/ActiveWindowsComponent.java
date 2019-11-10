package com.meeple.shared.frame.window;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActiveWindowsComponent {
	public List<Window> windows = Collections.synchronizedList(new ArrayList<>());

}
