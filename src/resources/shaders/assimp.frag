#version 460 core

/*
**ambient in first column
**diffuse in second column
**specular in third column
*/
uniform mat3 materialsMats[{maxmats}];
uniform vec3 uLightColour;
uniform vec3 uLightStrength;

struct Light {
	vec3 colour;
	vec3 position;
	vec3 attenuation;
	float enabled;
};
layout (std140) uniform LightBlock{
	Light lights[{maxlights}];
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
in vec3 vLightDirection[{maxlights}];
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
	
	
	//float distance = length(vLightDirection[i]);
	///float strFactor = uLightStrength.x + (uLightStrength.y * distance) + (uLightStrength.z * distance * distance);
	
	
	vec3 finalDiffuse = vec3(0);
	for(int i = 0;i < {maxlights};i++){
		if(lights[i].enabled > 0.5){
			float nDot1 = dot(vNormal, normalize(vLightDirection[i]));
			float brightness = max(0.0, nDot1);
			vec3 matDiffuseColour = (diffuseStrength * brightness * mat_diffuse);
			vec3 lightDiffuse = brightness * lights[i].colour;
			finalDiffuse  =  finalDiffuse + (matDiffuseColour + lightDiffuse);
		}
	}
	
	//outColour = vec4(lightDiffuse, 1) + (vec4(matAmbientColour, 1) );
	outColour = vec4(finalDiffuse, 1);
	
	
}