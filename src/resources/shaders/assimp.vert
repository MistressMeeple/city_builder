#version 460 core

layout(location = 1) in  vec4 vertex;
layout(location = 2) in vec3 normal;
layout(location = 3) in float materialIndex;
//instanced
layout(location = 4) in mat4 modelMatrix;
layout(location = 8) in mat3 normalMatrix;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 vpMatrix;


out vec3 vPosition;
out vec3 vNormal;
out int vMaterialIndex;

void main() {
    vec4 position = modelMatrix * vertex;
    gl_Position = vpMatrix * position;
    vPosition = position.xyz;
    vNormal = normalMatrix * normal;
	vMaterialIndex = int(materialIndex );
	
}