#version 460 core

layout(location = 1) in  vec4 vertex;
layout(location = 2) in vec3 normal;
layout(location = 3) in int materialIndex;
//instanced
uniform mat4 modelMatrix;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 vpMatrix;

uniform mat3 normalMatrix;

out vec3 vPosition;
out vec3 vNormal;
out int vMaterialIndex;

void main() {
    vec4 position = modelMatrix * vertex;
    gl_Position = vpMatrix * position;
    vPosition = position.xyz;
    vNormal = normalMatrix * normal;
	vMaterialIndex = materialIndex ;
	
}