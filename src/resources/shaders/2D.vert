#version 460 core

//-highp for vertex positions,
//-mediump for texture coordinates,
//-lowp for colors
layout(location = 1) in highp vec2 offset;//model position
layout(location = 2) in lowp vec4 colour;//vertex colour
layout(location = 3) in lowp float rotation;//model rotation

out lowp vec4 inColour;
out highp float rotationOut;


void main()
{
	inColour = colour;
	rotationOut = rotation;
    gl_Position = vec4(offset.x,offset.y, 0, 1.0);
}