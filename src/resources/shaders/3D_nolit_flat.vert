#version 460 core


layout (std140) uniform Matrices
{
	mat4 projectionMatrix;
	mat4 viewMatrix;
	mat4 vpMatrix;
};


layout(location = 1) in vec4 vertex;
layout(location = 2) in vec4 colour;
layout(location = 3) in mat4  modelMatrix;

out vec4 vColour;

void main() {
	vColour = colour;
    vec4 position = modelMatrix * vertex;
    gl_Position = vpMatrix * position;
}