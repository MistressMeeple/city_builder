#version 460 core
//-highp for vertex positions,
//-mediump for texture coordinates,
//-lowp for colors
//---------vertex data
layout(location = 1) in highp vec3 position;
//---------vertex/model colour
layout(location = 2) in lowp vec4 colour;
//---------vertex/model offset
layout(location = 3) in highp vec3 offset;

out lowp vec4 passColour;

uniform highp mat4 projectionMatrix;
uniform highp mat4 viewMatrix;
uniform highp mat4 vpMatrix;

void main(void){
	vec4 worldPos = vec4(position + offset	, 1);
	vec4 pos =  vpMatrix * worldPos;
	gl_Position =  pos;
	passColour = colour;
}
