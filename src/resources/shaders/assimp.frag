#version 460 core

/*
**ambient in first column
**diffuse in second column
**specular in third column
*/

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
	vec3 reflectTint;
	float baseScale;
	float reflectStrength;
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
	Material material = materials[index];
	
	vec3 baseColour = material.baseColour.rgb;
	float colStr = material.baseScale;
	vec3 reflectTint = material.reflectTint;
	float refStr = material.reflectStrength;
	float alpha = material.baseColour.a;
	
    vec3 matAmbientColour = colStr * baseColour;
	
	//float distance = length(vLightDirection[i]);
	///float strFactor = uLightStrength.x + (uLightStrength.y * distance) + (uLightStrength.z * distance * distance);
	
	
	vec3 runningDiffuse = vec3(0);
	vec3 runningLightSource  = vec3(0);
	
	
	for(int i = 0;i < {maxlights};i++){
		if(lights[i].enabled > 0.5){
			float nDot1 = dot(vNormal, normalize(vLightDirection[i]));
			float brightness = max(0.0, nDot1);
			vec3 matDiffuseColour = (refStr * brightness * reflectTint);
			vec3 lightDiffuse = brightness * lights[i].colour.rgb;
			runningDiffuse  =  runningDiffuse + (+ lightDiffuse);
		
		}
	}
	runningDiffuse = runningDiffuse;
	//outColour = vec4(lightDiffuse, 1) + (vec4(matAmbientColour, 1) );
	outColour = vec4(runningDiffuse, 1) ;//+ vec4(matAmbientColour,alpha);
	
		
}