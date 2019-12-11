#version 460 core

/*
**ambient in first column
**diffuse in second column
**specular in third column
*/
uniform mat3 materials[{maxmats}];
uniform vec3 uLightColour;

in vec3 vPosition;
in vec3 vNormal;
in vec3 lightDirection;
in vec3 col;

flat in int vMaterialIndex;

out lowp vec4 outColour;

void main() {
	int index = int(vMaterialIndex);
	mat3 mat = materials[index];
	
	vec3 mat_ambient = mat[0].xyz;
    float ambientStrength = mat[2].x;
	vec3 mat_diffuse = mat[1].xyz;
    float diffuseStrength = mat[2].y;
    float shininess = mat[2].z;
	float lightingScale = 1f;
    vec3 matAmbientColour = ambientStrength * mat_ambient;
	
	vec3 finalDiffuse = vec3(0);
	float nDot1 = dot(vNormal,lightDirection);
	float brightness = max(0.0, nDot1);
	vec3 matDiffuseColour = (diffuseStrength * brightness * mat_diffuse);
	vec3 lightDiffuse = (brightness * uLightColour);
	finalDiffuse  =  matDiffuseColour + lightDiffuse;
	
	
	vec3 matCombined = matAmbientColour + finalDiffuse;	
	outColour = vec4(matCombined,1);
}