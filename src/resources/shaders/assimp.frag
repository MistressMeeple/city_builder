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
//for specular
/*
in vec3 viewDirection;
in vec3 reflectDirection;
*/
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
    vec3 matAmbientColour = ambientStrength * mat_ambient;
	
	vec3 finalDiffuse = vec3(0);
	float nDot1 = dot(vNormal,lightDirection);
	float brightness = max(0.0, nDot1);
	vec3 matDiffuseColour = (diffuseStrength * brightness * mat_diffuse);
	vec3 lightDiffuse = (brightness * uLightColour);
	finalDiffuse  =  matDiffuseColour + lightDiffuse;
	
	/*
	float specularFactor = dot(reflectDirection,viewDirection);
	specularFactor = max(0, specularFactor);
	float dampedFactor = pow(specularFactor,0.5);
	vec3 specularColour = dampedFactor * uLightColour;
	*/
	
	vec3 matCombined = matAmbientColour + finalDiffuse;//+ specularColour;
	
	outColour = vec4(matCombined,1);
	
}