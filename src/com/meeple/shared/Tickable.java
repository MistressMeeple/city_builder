package com.meeple.shared;

import java.util.function.BiFunction;

import com.meeple.shared.frame.OGL.GLContext;

public interface Tickable extends BiFunction<GLContext, Delta, Boolean> {

}
