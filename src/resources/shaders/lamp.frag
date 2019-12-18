#version 460 core

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
flat in int vMaterialIndex;

out lowp vec4 outColour;

void main() {
	int index = int(vMaterialIndex);
	Material material = materials[index];
	
	vec3 baseColour = material.baseColour.rgb;
	float colStr = material.baseScale;
	float alpha = material.baseColour.a;
	
    vec3 matAmbientColour =  colStr * baseColour;
	
	outColour = vec4(matAmbientColour,alpha);
}