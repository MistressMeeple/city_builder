#version 460 core

layout (std140) uniform MatrixBlock
{
  mat4 projectionMatrix;
};

layout(location = 1) in highp vec2 vertex;
layout(location = 2) in lowp vec4 colour;
layout(location = 3) in lowp float zIndex;
layout(location = 4) in highp mat4 meshTransform;

out lowp vec4 passColour;

void main(void){
	passColour = colour;
	vec4 worldPos = meshTransform * vec4(vertex, zIndex, 1);
	vec4 pos =   projectionMatrix * worldPos;
	gl_Position =  pos;
}
