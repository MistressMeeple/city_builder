#version 460 core

//-highp for vertex positions,
//-mediump for texture coordinates,
//-lowp for colors
in lowp vec4 passColour;
in highp vec3 surfaceNormal;
in highp vec3 toLightVector; 
in highp vec4 passNormal;
out lowp vec4 outColour;

uniform lowp vec3 lightColour;
uniform lowp vec3 lightStrength;
uniform lowp float ambientLight;

void main(void){
	vec3 normalizedSurfaceNormal = normalize(surfaceNormal);
	vec3 normalizedToLightVector= normalize(toLightVector);
	float lightDot = dot(normalizedSurfaceNormal,normalizedToLightVector);
	float brightness = max(lightDot, ambientLight);
	vec4 diffuse = vec4(brightness * lightColour,1.0);
	
	outColour = diffuse * passColour;
	
	
}