#version 460 core

uniform vec3 uLightPosition;
uniform vec3 uViewPosition;

/*
**ambient in first column
**diffuse in second column
**specular in third column
*/
uniform mat3 materials[{maxmats}];

in vec3 vPosition;
in vec3 vNormal;
in flat int vMaterialIndex;
out lowp vec4 outColour;

void main() {

	int index = int(vMaterialIndex);
	mat3 mat = materials[index];
	
	vec3 mat_ambient = mat[0].xyz;
    float ambientStrength = 0.5;
	vec3 mat_diffuse = mat[1].xyz;
    float diffuseStrength = 0.5;
	vec3 mat_specular = mat[2].xyz;
    float specularStrength = 0.5;
    float shininess = 4.0;
	
    vec3 ambientColor = ambientStrength * mat_ambient;
    vec3 normal = normalize(vNormal);
    vec3 lightDirection = normalize(uLightPosition - vPosition);
    vec3 diffuseColor = diffuseStrength * max(0.0, dot(normal, lightDirection)) * mat_diffuse;
    vec3 viewDirection = normalize(uViewPosition - vPosition);
    vec3 reflectDirection = reflect(-lightDirection, normal);
    vec3 specularColor = specularStrength * pow(max(dot(viewDirection, reflectDirection), 0.0), shininess) * mat_specular;
	
	outColour = vec4(ambientColor + diffuseColor + specularColor,1);
	
	
}