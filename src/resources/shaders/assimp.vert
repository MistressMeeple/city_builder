#version 460 core

layout(location = 1) in  vec4 vertex;
layout(location = 2) in vec3 normal;
//instanced
layout(location = 3) in float materialIndex;
layout(location = 4) in mat4 modelMatrix;
layout(location = 8) in mat4 normalMatrix;


layout (std140) uniform Matrices
{
	mat4 projectionMatrix;
	mat4 viewMatrix;
	mat4 vpMatrix;
};
uniform vec3 uLightPosition;

out vec3 vPosition;
out vec3 vNormal;
out vec3 lightDirection;
out int vMaterialIndex;
out vec3 col;

void main() {
	vMaterialIndex = int(materialIndex);
    vec4 position = modelMatrix * vertex;
    gl_Position =  vpMatrix * position;
    vPosition = position.xyz;
	vNormal = normalize(normalMatrix * vec4(normal,1)).xyz;
	lightDirection = normalize(uLightPosition - vPosition);
	
	
}