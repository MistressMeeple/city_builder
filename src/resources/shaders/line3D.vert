#version 460 core

layout(location = 1) in highp vec3 position;
layout(location = 2) in lowp vec4 colour;
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
