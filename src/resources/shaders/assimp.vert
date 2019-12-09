#version 460 core

layout(location = 1) in  vec4 vertex;
layout(location = 2) in vec3 normal;
//instanced
layout(location = 3) in float materialIndex;
layout(location = 4) in mat4 modelMatrix;
 in mat3 normalMatrix;


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
//for specular
/*
out vec3 viewDirection;
out vec3 reflectDirection;
*/
out int vMaterialIndex;

void main() {
	vMaterialIndex = int(materialIndex);
    vec4 position = modelMatrix * vertex;
    gl_Position =  vpMatrix * position;
    vPosition = position.xyz;
    vNormal = normalize((modelMatrix * vec4(normal,1)).xyz);
	lightDirection = normalize(uLightPosition - vPosition);
	
	/*
	vec3 campos = (inverse(viewMatrix) * vec4(0,0,0,1)).xyz - position.xyz;
	viewDirection = normalize(campos - vPosition);
	reflectDirection = reflect(-lightDirection, vNormal);
	*/
}