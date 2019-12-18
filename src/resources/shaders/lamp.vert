#version 460 core

struct Light {
	vec3 colour;
	vec3 position;
	vec3 attenuation;
	float enabled;
};
layout (std140) uniform LightBlock{
	Light lights[{maxlights}];
};

layout (std140) uniform Matrices
{
	mat4 viewMatrix;
	mat4 projectionMatrix;
	mat4 vpMatrix;
};


layout(location = 1) in  vec4 vertex;
//instanced
layout(location = 3) in float materialIndex;
layout(location = 4) in mat4 modelMatrix;

out vec3 vPosition;
out int vMaterialIndex;

void main() {
	vMaterialIndex = int(materialIndex);
    vec4 position = modelMatrix * vertex;
    gl_Position =  vpMatrix * position;
    vPosition = position.xyz;
}