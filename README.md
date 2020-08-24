# Patronage
### A city building game with an unknown patron. 

You manage a tribe in an unknown world, created by an unknown patron, diety or architect. They will try to guide your tribe to their idea of what life should be, but your ideas might not be the same





IP of Megan Ridout. 

## Helper classes

### Client
This is the main class to use when using this library. 

### ShaderProgram and ShaderProgramSystem2 (names likely to change) 
THe ShaderProgram is the storage class (with all the sub classes written inside it) 
The ShaderProgramSystem2 is the system that manages and works with the ShaderProgram class

### ShaderPrograms (note the S ) 
This is a wrapper class for the ShaderProgramSystem2, holding premade shader programs and set up for them. 


### GLContext
A wrapper class for the OpenGL context that manages the generation of (most) "names"/ID's and stores them all for easy deletion. 

### FrameTimeManager
A helper class that allows to choose a desired frame rate or frame time, ms per frame or frames per second. 

### FrameTimings
A helper class that encapsulates the delta between frames, calculated once per frame. Holds the total and delta time, in both nanos and second, and the current FPS. 

### NkContextSingleton
A wrapper class for the Nuklear library. 

### MirroredWindowCallbacks
