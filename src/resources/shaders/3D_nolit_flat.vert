#version 460 core


layout (std140) uniform Matrices
{
	mat4 projectionMatrix;
	mat4 viewMatrix;
	mat4 vpMatrix;
};


layout(location = 1) in vec3 vertex;
layout(location = 2) in vec4 colour;


out vec4 vColour;

void main() {
	vColour = colour;
    vec4 position = vpMatrix * vec4(vertex,1);
    gl_Position =   position;
}