package com.meeple.shared.frame.window;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Platform;

import com.meeple.shared.frame.window.hints.ClientAPI;
import com.meeple.shared.frame.window.hints.ContextCreationAPI;
import com.meeple.shared.frame.window.hints.ContextReleaseBehavior;
import com.meeple.shared.frame.window.hints.ContextRobustness;
import com.meeple.shared.frame.window.hints.HasID;
import com.meeple.shared.frame.window.hints.OpenGLProfile;

public class WindowHints {
	private static Logger logger = Logger.getLogger(WindowHints.class);
	public static boolean debug = true;

	public static WindowHints defaultWindowHints = new WindowHints()
		.setResizable(true)
		.setVisible(true)
		.setContextVersionMajor(3)
		.setContextVersionMinor(3)
		.setOpenGLProfile(OpenGLProfile.OPENGL_CORE_PROFILE)
		.setOpenGLDebugContext(true)
		.setOpenGLForwardCompat(Platform.get() == Platform.MACOSX);

	private Boolean resizable;
	private Boolean visible;
	private Boolean decorated;
	private Boolean focused;
	private Boolean autoIconify;
	private Boolean floating;
	private Boolean maximised;
	private Boolean centerCursor;
	private Boolean transparentFramebuffer;
	private Boolean focusOnShow;
	private Boolean scaleToMonitor;
	private Boolean stereo;
	private Boolean srgbCapable;
	private Boolean doublebuffer;
	private Boolean contextNoError;
	private Boolean openGLForwardCompat;
	private Boolean openGLDebugContext;
	private Boolean cocoaRetinaFramebuffer;
	private Boolean cocoaGraphicsSwitching;

	private Integer auxBuffers;
	private Integer samples;
	private Integer contextVersionMajor;//1 Any valid major version number of the chosen client API 
	private Integer contextVersionMinor;//0 Any valid minor version number of the chosen client API 

	//dont care-able
	private Integer redBits;
	private Integer greenBits;
	private Integer blueBits;
	private Integer alphaBits;
	private Integer depthBits;
	private Integer stencilBits;
	private Integer accumRedBits;
	private Integer accumBlueBits;
	private Integer accumGreenBits;
	private Integer accumAlphaBits;
	private Integer refreshRate;

	private String cocoaFrameName;
	private String x11ClassName;
	private String x11InstanceName;

	private ClientAPI clientAPI;
	private ContextCreationAPI contextCreationApi;
	private ContextRobustness contextRobustness;
	private ContextReleaseBehavior contextReleaseBehavior;
	private OpenGLProfile openGLProfile;

	public WindowHints() {

	}

	/**
	 * Sets all the hints to the ones provided 
	 * @param other
	 */
	public void copyFrom(WindowHints other, boolean includeNulls) {

		if (other.resizable != null || includeNulls) {
			this.resizable = other.resizable;
		}
		if (other.visible != null || includeNulls) {
			this.visible = other.visible;
		}
		if (other.decorated != null || includeNulls) {
			this.decorated = other.decorated;
		}
		if (other.focused != null || includeNulls) {
			this.focused = other.focused;
		}
		if (other.autoIconify != null || includeNulls) {
			this.autoIconify = other.autoIconify;
		}
		if (other.floating != null || includeNulls) {
			this.floating = other.floating;
		}
		if (other.maximised != null || includeNulls) {
			this.maximised = other.maximised;
		}
		if (other.centerCursor != null || includeNulls) {
			this.centerCursor = other.centerCursor;
		}
		if (other.transparentFramebuffer != null || includeNulls) {
			this.transparentFramebuffer = other.transparentFramebuffer;
		}
		if (other.focusOnShow != null || includeNulls) {
			this.focusOnShow = other.focusOnShow;
		}
		if (other.scaleToMonitor != null || includeNulls) {
			this.scaleToMonitor = other.scaleToMonitor;
		}
		if (other.stereo != null || includeNulls) {
			this.stereo = other.stereo;
		}
		if (other.srgbCapable != null || includeNulls) {
			this.srgbCapable = other.srgbCapable;
		}
		if (other.doublebuffer != null || includeNulls) {
			this.doublebuffer = other.doublebuffer;
		}
		if (other.contextNoError != null || includeNulls) {
			this.contextNoError = other.contextNoError;
		}
		if (other.openGLForwardCompat != null || includeNulls) {
			this.openGLForwardCompat = other.openGLForwardCompat;
		}
		if (other.openGLDebugContext != null || includeNulls) {
			this.openGLDebugContext = other.openGLDebugContext;
		}
		if (other.cocoaRetinaFramebuffer != null || includeNulls) {
			this.cocoaRetinaFramebuffer = other.cocoaRetinaFramebuffer;
		}
		if (other.cocoaGraphicsSwitching != null || includeNulls) {
			this.cocoaGraphicsSwitching = other.cocoaGraphicsSwitching;
		}

		if (other.auxBuffers != null || includeNulls) {
			this.auxBuffers = other.auxBuffers;
		}
		if (other.samples != null || includeNulls) {
			this.samples = other.samples;
		}
		if (other.contextVersionMajor != null || includeNulls) { //1 Any valid major version number of the chosen client API 
			this.contextVersionMajor = other.contextVersionMajor;
		}
		if (other.contextVersionMinor != null || includeNulls) { //0 Any valid minor version number of the chosen client API
			this.contextVersionMinor = other.contextVersionMinor;
		}

		//dont care-able
		if (other.redBits != null || includeNulls) {
			this.redBits = other.redBits;
		}
		if (other.greenBits != null || includeNulls) {
			this.greenBits = other.greenBits;
		}
		if (other.blueBits != null || includeNulls) {
			this.blueBits = other.blueBits;
		}
		if (other.alphaBits != null || includeNulls) {
			this.alphaBits = other.alphaBits;
		}
		if (other.depthBits != null || includeNulls) {
			this.depthBits = other.depthBits;
		}
		if (other.stencilBits != null || includeNulls) {
			this.stencilBits = other.stencilBits;
		}
		if (other.accumRedBits != null || includeNulls) {
			this.accumRedBits = other.accumRedBits;
		}
		if (other.accumBlueBits != null || includeNulls) {
			this.accumBlueBits = other.accumBlueBits;
		}
		if (other.accumGreenBits != null || includeNulls) {
			this.accumGreenBits = other.accumGreenBits;
		}
		if (other.accumAlphaBits != null || includeNulls) {
			this.accumAlphaBits = other.accumAlphaBits;
		}
		if (other.refreshRate != null || includeNulls) {
			this.refreshRate = other.refreshRate;
		}

		if (other.cocoaFrameName != null || includeNulls) {
			this.cocoaFrameName = other.cocoaFrameName;
		}
		if (other.x11ClassName != null || includeNulls) {
			this.x11ClassName = other.x11ClassName;
		}
		if (other.x11InstanceName != null || includeNulls) {

			this.x11InstanceName = other.x11InstanceName;
		}
		if (other.clientAPI != null || includeNulls) {
			this.clientAPI = other.clientAPI;
		}
		if (other.contextCreationApi != null || includeNulls) {
			this.contextCreationApi = other.contextCreationApi;
		}
		if (other.contextRobustness != null || includeNulls) {
			this.contextRobustness = other.contextRobustness;
		}
		if (other.contextReleaseBehavior != null || includeNulls) {
			this.contextReleaseBehavior = other.contextReleaseBehavior;
		}
		if (other.openGLProfile != null || includeNulls) {
			this.openGLProfile = other.openGLProfile;
		}
	}

	public void process() {
		logger.trace("Initialising hints");
		String format = "\t%20s: %-15s";
		GLFW.glfwDefaultWindowHints();

		if (debug && focused != null) {
			logger.trace(String.format(format, "focused", focused));
		}
		setBooleanHint(GLFW.GLFW_FOCUSED, focused);

		if (debug && resizable != null) {
			logger.trace(String.format(format, "resizable", resizable));
		}
		setBooleanHint(GLFW.GLFW_RESIZABLE, resizable);

		if (debug && visible != null) {
			logger.trace(String.format(format, "visble", visible));
		}
		setBooleanHint(GLFW.GLFW_VISIBLE, visible);

		if (debug && decorated != null) {
			logger.trace(String.format(format, "decorated", decorated));
		}
		setBooleanHint(GLFW.GLFW_DECORATED, decorated);

		if (debug && autoIconify != null) {
			logger.trace(String.format(format, "auto-iconify", autoIconify));
		}
		setBooleanHint(GLFW.GLFW_AUTO_ICONIFY, autoIconify);

		if (debug && floating != null) {
			logger.trace(String.format(format, "floating", floating));
		}
		setBooleanHint(GLFW.GLFW_FLOATING, floating);

		if (debug && maximised != null) {
			logger.trace(String.format(format, "maximised", maximised));
		}
		setBooleanHint(GLFW.GLFW_MAXIMIZED, maximised);

		if (debug && centerCursor != null) {
			logger.trace(String.format(format, "centerCursor", centerCursor));
		}
		setBooleanHint(GLFW.GLFW_CENTER_CURSOR, centerCursor);

		if (debug && transparentFramebuffer != null) {
			logger.trace(String.format(format, "transparentFramebuffer", transparentFramebuffer));
		}
		setBooleanHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, transparentFramebuffer);

		if (debug && focusOnShow != null) {
			logger.trace(String.format(format, "focusOnShow", focusOnShow));
		}
		setBooleanHint(GLFW.GLFW_FOCUS_ON_SHOW, focusOnShow);

		if (debug && clientAPI != null) {
			logger.trace(String.format(format, "clientAPI", clientAPI));
		}
		setIntHint(GLFW.GLFW_CLIENT_API, clientAPI);

		if (debug && contextVersionMajor != null) {
			logger.trace(String.format(format, "contextVersionMajor", contextVersionMajor));
		}
		setIntHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, contextVersionMajor);

		if (debug && contextVersionMinor != null) {
			logger.trace(String.format(format, "contextVersionMinor", contextVersionMinor));
		}
		setIntHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, contextVersionMinor);

		if (debug && contextRobustness != null) {
			logger.trace(String.format(format, "contextRobustness", contextRobustness));
		}
		setIntHint(GLFW.GLFW_CONTEXT_ROBUSTNESS, contextRobustness);

		if (debug && openGLForwardCompat != null) {
			logger.trace(String.format(format, "openGLForwardCompat", openGLForwardCompat));
		}
		setBooleanHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, openGLForwardCompat);

		if (debug && openGLDebugContext != null) {
			logger.trace(String.format(format, "openGLDebugContext", openGLDebugContext));
		}
		setBooleanHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, openGLDebugContext);

		if (debug && openGLProfile != null) {
			logger.trace(String.format(format, "openGLProfile", openGLProfile));
		}
		setIntHint(GLFW.GLFW_OPENGL_PROFILE, openGLProfile);

		if (debug && contextReleaseBehavior != null) {
			logger.trace(String.format(format, "contextReleaseBehavior", contextReleaseBehavior));
		}
		setIntHint(GLFW.GLFW_CONTEXT_RELEASE_BEHAVIOR, contextReleaseBehavior);

		if (debug && contextNoError != null) {
			logger.trace(String.format(format, "contextNoError", contextNoError));
		}
		setBooleanHint(GLFW.GLFW_CONTEXT_NO_ERROR, contextNoError);

		if (debug && contextCreationApi != null) {
			logger.trace(String.format(format, "contextCreationApi", contextCreationApi));
		}
		setIntHint(GLFW.GLFW_CONTEXT_CREATION_API, contextCreationApi);

		if (debug && scaleToMonitor != null) {
			logger.trace(String.format(format, "scaleToMonitor", scaleToMonitor));
		}
		setBooleanHint(GLFW.GLFW_SCALE_TO_MONITOR, scaleToMonitor);

		if (debug && redBits != null) {
			logger.trace(String.format(format, "redBits", redBits));
		}
		setDontCareIntHint(GLFW.GLFW_RED_BITS, redBits);

		if (debug && greenBits != null) {
			logger.trace(String.format(format, "greenBits", greenBits));
		}
		setDontCareIntHint(GLFW.GLFW_GREEN_BITS, greenBits);

		if (debug && blueBits != null) {
			logger.trace(String.format(format, "blueBits", blueBits));
		}
		setDontCareIntHint(GLFW.GLFW_BLUE_BITS, blueBits);

		if (debug && alphaBits != null) {
			logger.trace(String.format(format, "alphaBits", alphaBits));
		}
		setDontCareIntHint(GLFW.GLFW_ALPHA_BITS, alphaBits);

		if (debug && depthBits != null) {
			logger.trace(String.format(format, "depthBits", depthBits));
		}
		setDontCareIntHint(GLFW.GLFW_DEPTH_BITS, depthBits);

		if (debug && stencilBits != null) {
			logger.trace(String.format(format, "stencilBits", stencilBits));
		}
		setDontCareIntHint(GLFW.GLFW_STENCIL_BITS, stencilBits);

		if (debug && accumRedBits != null) {
			logger.trace(String.format(format, "accumRedBits", accumRedBits));
		}
		setDontCareIntHint(GLFW.GLFW_ACCUM_RED_BITS, accumRedBits);

		if (debug && accumGreenBits != null) {
			logger.trace(String.format(format, "accumGreenBits", accumGreenBits));
		}
		setDontCareIntHint(GLFW.GLFW_ACCUM_GREEN_BITS, accumGreenBits);

		if (debug && accumBlueBits != null) {
			logger.trace(String.format(format, "accumBlueBits", accumBlueBits));
		}
		setDontCareIntHint(GLFW.GLFW_ACCUM_BLUE_BITS, accumBlueBits);

		if (debug && accumAlphaBits != null) {
			logger.trace(String.format(format, "accumAlphaBits", accumAlphaBits));
		}
		setDontCareIntHint(GLFW.GLFW_ACCUM_ALPHA_BITS, accumAlphaBits);

		if (debug && auxBuffers != null) {
			logger.trace(String.format(format, "auxBuffers", auxBuffers));
		}
		setIntHint(GLFW.GLFW_AUX_BUFFERS, auxBuffers);

		if (debug && stereo != null) {
			logger.trace(String.format(format, "stereo", stereo));
		}
		setBooleanHint(GLFW.GLFW_STEREO, stereo);

		if (debug && samples != null) {
			logger.trace(String.format(format, "samples", samples));
		}
		setIntHint(GLFW.GLFW_SAMPLES, samples);

		if (debug && srgbCapable != null) {
			logger.trace(String.format(format, "srgbCapable", srgbCapable));
		}
		setBooleanHint(GLFW.GLFW_SRGB_CAPABLE, srgbCapable);

		if (debug && refreshRate != null) {
			logger.trace(String.format(format, "refreshRate", refreshRate));
		}
		setDontCareIntHint(GLFW.GLFW_REFRESH_RATE, refreshRate);

		if (debug && doublebuffer != null) {
			logger.trace(String.format(format, "doublebuffer", doublebuffer));
		}
		setBooleanHint(GLFW.GLFW_DOUBLEBUFFER, doublebuffer);

		if (debug && cocoaRetinaFramebuffer != null) {
			logger.trace(String.format(format, "cocoaRetinaFramebuffer", cocoaRetinaFramebuffer));
		}
		setBooleanHint(GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER, cocoaRetinaFramebuffer);

		if (debug && cocoaGraphicsSwitching != null) {
			logger.trace(String.format(format, "cocoaGraphicsSwitching", cocoaGraphicsSwitching));
		}
		setBooleanHint(GLFW.GLFW_COCOA_GRAPHICS_SWITCHING, cocoaGraphicsSwitching);

		if (debug && cocoaFrameName != null) {
			logger.trace(String.format(format, "cocoaFrameName", cocoaFrameName));
		}
		setStringHint(GLFW.GLFW_COCOA_FRAME_NAME, cocoaFrameName);

		if (debug && x11ClassName != null) {
			logger.trace(String.format(format, "x11ClassName", x11ClassName));
		}
		setStringHint(GLFW.GLFW_X11_CLASS_NAME, x11ClassName);

		if (debug && x11InstanceName != null) {
			logger.trace(String.format(format, "x11InstanceName", x11InstanceName));
		}
		setStringHint(GLFW.GLFW_X11_INSTANCE_NAME, x11InstanceName);

	}

	public static WindowHints getDefaultWindowHint() {
		return defaultWindowHints;
	}

	public Boolean getResizable() {
		return resizable;
	}

	public Boolean getVisible() {
		return visible;
	}

	public Boolean getDecorated() {
		return decorated;
	}

	public Boolean getFocused() {
		return focused;
	}

	public Boolean getAutoIconify() {
		return autoIconify;
	}

	public Boolean getFloating() {
		return floating;
	}

	public Boolean getMaximised() {
		return maximised;
	}

	public Boolean getCenterCursor() {
		return centerCursor;
	}

	public Boolean getTransparentFramebuffer() {
		return transparentFramebuffer;
	}

	public Boolean getFocusOnShow() {
		return focusOnShow;
	}

	public Boolean getScaleToMonitor() {
		return scaleToMonitor;
	}

	public Boolean getStereo() {
		return stereo;
	}

	public Boolean getSrgbCapable() {
		return srgbCapable;
	}

	public Boolean getDoublebuffer() {
		return doublebuffer;
	}

	public Boolean getContextNoError() {
		return contextNoError;
	}

	public Boolean getOpenGLForwardCompat() {
		return openGLForwardCompat;
	}

	public Boolean getOpenGLDebugContext() {
		return openGLDebugContext;
	}

	public Boolean getCocoaRetinaFramebuffer() {
		return cocoaRetinaFramebuffer;
	}

	public Boolean getCocoaGraphicsSwitching() {
		return cocoaGraphicsSwitching;
	}

	public Integer getAuxBuffers() {
		return auxBuffers;
	}

	public Integer getSamples() {
		return samples;
	}

	public Integer getContextVersionMajor() {
		return contextVersionMajor;
	}

	public Integer getContextVersionMinor() {
		return contextVersionMinor;
	}

	public Integer getRedBits() {
		return redBits;
	}

	public Integer getGreenBits() {
		return greenBits;
	}

	public Integer getBlueBits() {
		return blueBits;
	}

	public Integer getAlphaBits() {
		return alphaBits;
	}

	public Integer getDepthBits() {
		return depthBits;
	}

	public Integer getStencilBits() {
		return stencilBits;
	}

	public Integer getAccumRedBits() {
		return accumRedBits;
	}

	public Integer getAccumBlueBits() {
		return accumBlueBits;
	}

	public Integer getAccumGreenBits() {
		return accumGreenBits;
	}

	public Integer getAccumAlphaBits() {
		return accumAlphaBits;
	}

	public Integer getRefreshRate() {
		return refreshRate;
	}

	public String getCocoaFrameName() {
		return cocoaFrameName;
	}

	public String getX11ClassName() {
		return x11ClassName;
	}

	public String getX11InstanceName() {
		return x11InstanceName;
	}

	public ClientAPI getClientAPI() {
		return clientAPI;
	}

	public ContextCreationAPI getContextCreationApi() {
		return contextCreationApi;
	}

	public ContextRobustness getContextRobustness() {
		return contextRobustness;
	}

	public ContextReleaseBehavior getContextReleaseBehavior() {
		return contextReleaseBehavior;
	}

	public OpenGLProfile getOpenGLProfile() {
		return openGLProfile;
	}

	/**
	 * @see GLFW#GLFW_RESIZABLE
	 * @return
	 */
	public WindowHints setResizable(Boolean resizable) {
		this.resizable = resizable;
		return this;
	}

	/**
	 * @see GLFW#GLFW_VISIBLE
	 * @param visible
	 * @return
	 */
	public WindowHints setVisible(Boolean visible) {
		this.visible = visible;
		return this;
	}

	/**
	 * 
	 * @see GLFW#GLFW_DECORATED
	 * @param decorated
	 * @return
	 */
	public WindowHints setDecorated(Boolean decorated) {
		this.decorated = decorated;
		return this;
	}

	/**
	 * @see GLFW#GLFW_FOCUSED
	 */
	public WindowHints setFocused(Boolean focused) {
		this.focused = focused;
		return this;
	}

	/**
	 * @see GLFW#GLFW_AUTO_ICONIFY
	 */
	public WindowHints setAutoIconify(Boolean autoIconify) {
		this.autoIconify = autoIconify;
		return this;
	}

	/**
	 * @see GLFW#GLFW_FLOATING
	 */
	public WindowHints setFloating(Boolean floating) {
		this.floating = floating;
		return this;
	}

	/**
	 * @see GLFW#GLFW_MAXIMIZED
	 */
	public WindowHints setMaximised(Boolean maximised) {
		this.maximised = maximised;
		return this;
	}

	/**
	 * @see GLFW#GLFW_CENTER_CURSOR
	 */
	public WindowHints setCenterCursor(Boolean centerCursor) {
		this.centerCursor = centerCursor;
		return this;
	}

	/**
	 * @see GLFW#GLFW_TRANSPARENT_FRAMEBUFFER
	 */
	public WindowHints setTransparentFramebuffer(Boolean transparentFramebuffer) {
		this.transparentFramebuffer = transparentFramebuffer;
		return this;
	}

	/**
	 * @see GLFW#GLFW_FOCUS_ON_SHOW
	 */
	public WindowHints setFocusOnShow(Boolean focusOnShow) {
		this.focusOnShow = focusOnShow;
		return this;
	}

	/**
	 * @see GLFW#GLFW_SCALE_TO_MONITOR
	 */
	public WindowHints setScaleToMonitor(Boolean scaleToMonitor) {
		this.scaleToMonitor = scaleToMonitor;
		return this;
	}

	/**
	 * @see GLFW#GLFW_STEREO
	 */
	public WindowHints setStereo(Boolean stereo) {
		this.stereo = stereo;
		return this;
	}

	/**
	 * @see GLFW#GLFW_SRGB_CAPABLE
	 */
	public WindowHints setSrgbCapable(Boolean srgbCapable) {
		this.srgbCapable = srgbCapable;
		return this;
	}

	/**
	 * @see GLFW#GLFW_DOUBLEBUFFER
	 */
	public WindowHints setDoublebuffer(Boolean doublebuffer) {
		this.doublebuffer = doublebuffer;
		return this;
	}

	/**
	 * @see GLFW#GLFW_CONTEXT_NO_ERROR
	 */
	public WindowHints setContextNoError(Boolean contextNoError) {
		this.contextNoError = contextNoError;
		return this;
	}

	/**
	 * @see GLFW#GLFW_OPENGL_FORWARD_COMPAT
	 */
	public WindowHints setOpenGLForwardCompat(Boolean openGLForwardCompat) {
		this.openGLForwardCompat = openGLForwardCompat;
		return this;
	}

	/**
	 * @see GLFW#GLFW_OPENGL_DEBUG_CONTEXT
	 */
	public WindowHints setOpenGLDebugContext(Boolean openGLDebugContext) {
		this.openGLDebugContext = openGLDebugContext;
		return this;
	}

	/**
	 * @see GLFW#GLFW_COCOA_RETINA_FRAMEBUFFER
	 */
	public WindowHints setCocoaRetinaFramebuffer(Boolean cocoaRetinaFramebuffer) {
		this.cocoaRetinaFramebuffer = cocoaRetinaFramebuffer;
		return this;
	}

	/**
	 * @see GLFW#GLFW_COCOA_GRAPHICS_SWITCHING
	 */
	public WindowHints setCocoaGraphicsSwitching(Boolean cocoaGraphicsSwitching) {
		this.cocoaGraphicsSwitching = cocoaGraphicsSwitching;
		return this;
	}

	/**
	 * @see GLFW#GLFW_AUX_BUFFERS
	 */
	public WindowHints setAuxBuffers(Integer auxBuffers) {
		this.auxBuffers = auxBuffers;
		return this;
	}

	/**
	 * @see GLFW#GLFW_SAMPLES
	 */
	public WindowHints setSamples(Integer samples) {
		this.samples = samples;
		return this;
	}

	/**
	 * @see GLFW#GLFW_CONTEXT_VERSION_MAJOR
	 */
	public WindowHints setContextVersionMajor(Integer contextVersionMajor) {
		this.contextVersionMajor = contextVersionMajor;
		return this;
	}

	/**
	 * @see GLFW#GLFW_CONTEXT_VERSION_MINOR
	 */
	public WindowHints setContextVersionMinor(Integer contextVersionMinor) {
		this.contextVersionMinor = contextVersionMinor;
		return this;
	}

	/**
	 * @see GLFW#GLFW_RED_BITS
	 */

	public WindowHints setRedBits(Integer redBits) {
		this.redBits = redBits;
		return this;
	}

	/**
	 * @see GLFW#GLFW_GREEN_BITS
	 */
	public WindowHints setGreenBits(Integer greenBits) {
		this.greenBits = greenBits;
		return this;
	}

	/**
	 * @see GLFW#GLFW_BLUE_BITS
	 */
	public WindowHints setBlueBits(Integer blueBits) {
		this.blueBits = blueBits;
		return this;
	}

	/**
	 * @see GLFW#GLFW_ALPHA_BITS
	 */
	public WindowHints setAlphaBits(Integer alphaBits) {
		this.alphaBits = alphaBits;
		return this;
	}

	/**
	 * @see GLFW#GLFW_DEPTH_BITS
	 */
	public WindowHints setDepthBits(Integer depthBits) {
		this.depthBits = depthBits;
		return this;
	}

	/**
	 * @see GLFW#GLFW_STENCIL_BITS
	 */
	public WindowHints setStencilBits(Integer stencilBits) {
		this.stencilBits = stencilBits;
		return this;
	}

	/**
	 * @see GLFW#GLFW_ACCUM_RED_BITS
	 */

	public WindowHints setAccumRedBits(Integer accumRedBits) {
		this.accumRedBits = accumRedBits;
		return this;
	}

	/**
	 * @see GLFW#GLFW_ACCUM_BLUE_BITS
	 */

	public WindowHints setAccumBlueBits(Integer accumBlueBits) {
		this.accumBlueBits = accumBlueBits;
		return this;
	}

	/**
	 * @see GLFW#GLFW_ACCUM_GREEN_BITS
	 */

	public WindowHints setAccumGreenBits(Integer accumGreenBits) {
		this.accumGreenBits = accumGreenBits;
		return this;
	}

	/**
	 * @see GLFW#GLFW_ACCUM_ALPHA_BITS
	 */
	public WindowHints setAccumAlphaBits(Integer accumAlphaBits) {
		this.accumAlphaBits = accumAlphaBits;
		return this;
	}

	/**
	 * @see GLFW#GLFW_REFRESH_RATE
	 */
	public WindowHints setRefreshRate(Integer refreshRate) {
		this.refreshRate = refreshRate;
		return this;
	}

	/**
	 * @see GLFW#GLFW_COCOA_FRAME_NAME
	 */
	public WindowHints setCocoaFrameName(String cocoaFrameName) {
		this.cocoaFrameName = cocoaFrameName;
		return this;
	}

	/**
	 * @see GLFW#GLFW_X11_CLASS_NAME
	 */
	public WindowHints setX11ClassName(String x11ClassName) {
		this.x11ClassName = x11ClassName;
		return this;
	}

	/**
	 * @see GLFW#GLFW_X11_INSTANCE_NAME
	 */
	public WindowHints setX11InstanceName(String x11InstanceName) {
		this.x11InstanceName = x11InstanceName;
		return this;
	}

	/**
	 * @see GLFW#GLFW_CONTEXT_CREATION_API
	 */
	public WindowHints setContextCreationApi(ContextCreationAPI contextCreationApi) {
		this.contextCreationApi = contextCreationApi;
		return this;
	}

	/**
	 * @see GLFW#GLFW_CONTEXT_ROBUSTNESS
	 */
	public WindowHints setContextRobustness(ContextRobustness contextRobustness) {
		this.contextRobustness = contextRobustness;
		return this;
	}

	/**
	 * @see GLFW#GLFW_CLIENT_API
	 */
	public WindowHints setClientAPI(ClientAPI value) {
		clientAPI = value;
		return this;
	}

	/**
	 * @see GLFW#GLFW_CONTEXT_RELEASE_BEHAVIOR
	 */
	public WindowHints setContextReleaseBehavior(ContextReleaseBehavior value) {
		contextReleaseBehavior = value;
		return this;
	}

	/**
	 * @see GLFW#GLFW_OPENGL_PROFILE
	 */
	public WindowHints setOpenGLProfile(OpenGLProfile value) {
		openGLProfile = value;
		return this;
	}

	private static void setBooleanHint(int GLFWHint, Boolean value) {
		if (value != null) {

			if (value) {
				GLFW.glfwWindowHint(GLFWHint, GLFW.GLFW_TRUE);
			} else {
				GLFW.glfwWindowHint(GLFWHint, GLFW.GLFW_FALSE);
			}
		}
	}

	private static void setIntHint(int GLFWHint, Integer value) {
		if (value != null) {

			if (value >= 0 && value <= Integer.MAX_VALUE) {
				GLFW.glfwWindowHint(GLFWHint, value);
			} else {
				logger.trace("Value wasnt supported by hint " + GLFWHint);
			}
		}
	}

	private static void setIntHint(int GLFWHint, HasID<Integer> value) {
		if (value != null) {
			int val = value.getID();

			if (val >= 0 && val <= Integer.MAX_VALUE) {
				GLFW.glfwWindowHint(GLFWHint, val);
			} else {
				logger.trace("Value wasnt supported by hint " + GLFWHint);
			}
		}
	}

	private static void setDontCareIntHint(int GLFWHint, Integer value) {
		if (value != null) {

			if (value == GLFW.GLFW_DONT_CARE || (value >= 0 && value <= Integer.MAX_VALUE)) {
				GLFW.glfwWindowHint(GLFWHint, value);
			} else {
				logger.trace("Value wasnt supported by hint " + GLFWHint);
			}
		}
	}

	private static void setStringHint(int GLFWHint, String value) {
		if (value != null) {

			ByteBuffer buff = BufferUtils.createByteBuffer(value.length());
			buff.put(value.getBytes());
			GLFW.glfwWindowHintString(GLFWHint, buff);
		}
	}

}
