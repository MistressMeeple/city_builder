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
	mat4 projectionMatrix;
	mat4 viewMatrix;
	mat4 vpMatrix;
	vec3 viewPosition;
};


layout(location = 1) in highp vec4 vertex;
layout(location = 2) in highp vec3 normal;
//instanced
layout(location = 3) in lowp vec4 colour;
layout(location = 4) in highp mat4 meshMatrix;
layout(location = 8) in highp mat4 normalMatrix;


out vec3 vPosition;
out vec3 vNormal;
out vec3 vLightDirection[{maxlights}];
out vec4 vColour;

void main() {
	vColour = colour;
    vec4 position = meshMatrix * vertex;
    gl_Position =  vpMatrix * position;
    vPosition = position.xyz;
	vNormal = (normalMatrix * vec4(normal,1)).xyz;
	for(int i = 0; i < {maxlights}; i++){		
		vLightDirection[i] = lights[i].position.xyz - position.xyz;
	}
}