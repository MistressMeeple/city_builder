#version 460 core
//-highp for vertex positions,
//-mediump for texture coordinates,
//-lowp for colors
//---------vertex data
layout(location = 1) in highp vec2 position;
//---------vertex/model colour
layout(location = 2) in lowp vec4 colour;
//---------mesh rotation
layout(location = 3) in lowp float zIndex;
//---------mesh offset
layout(location = 4) in highp vec2 offset;

out lowp vec4 passColour;

uniform highp mat4 projectionMatrix;
uniform highp mat4 viewMatrix;
uniform highp mat4 vpMatrix;
void main(void){

    //mat2 rot =  mat2(cos(rotation),- sin(rotation), sin(rotation), cos(rotation));
	
	vec4 worldPos = vec4(position + offset, zIndex, 1);
	vec4 pos =   projectionMatrix * worldPos;
	gl_Position =  pos;
	passColour = colour;
}
