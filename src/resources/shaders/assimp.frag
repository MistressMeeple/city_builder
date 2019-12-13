#version 460 core

/*
**ambient in first column
**diffuse in second column
**specular in third column
*/
uniform mat3 materialsMats[{maxmats}];
uniform vec3 uLightColour;
uniform vec3 uLightStrength;

layout (std140) uniform LightBlock{
	mat4 light[{maxlights}];
};

struct Material {
	vec4 baseColour;
	float baseScale;
	vec3 reflectTint;
	float reflectStrenght;
};
layout (std140) uniform MaterialBlock{
	Material materials[{maxmats}];
};

in vec3 vPosition;
in vec3 vNormal;
in vec3 vLightDirection;
flat in int vMaterialIndex;

out lowp vec4 outColour;

void main() {
	int index = int(vMaterialIndex);
	mat3 mat = materialsMats[index];
	
	vec3 mat_ambient = mat[0].xyz;
    float ambientStrength = mat[2].x;
	vec3 mat_diffuse = mat[1].xyz;
    float diffuseStrength = mat[2].y;
    float lightingScale = mat[2].z;
	
    vec3 matAmbientColour = mat_ambient;
	
	
	float distance = length(vLightDirection);
	float strFactor = uLightStrength.x + (uLightStrength.y * distance) + (uLightStrength.z * distance * distance);
	
	
	vec3 finalDiffuse = vec3(0);
	float nDot1 = dot(vNormal, normalize(vLightDirection));
	float brightness = max(0.0, nDot1);
	vec3 matDiffuseColour = (diffuseStrength * brightness * mat_diffuse);
	vec3 lightDiffuse = brightness * uLightColour;
	finalDiffuse  =  lightingScale * (matDiffuseColour + lightDiffuse);
	
	outColour = vec4(lightDiffuse, 1) + (vec4(matAmbientColour, 1) / strFactor);
	
	
}