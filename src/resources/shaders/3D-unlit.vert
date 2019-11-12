#version 460 core
//-highp for vertex positions,
//-mediump for texture coordinates,
//-lowp for colors
//---------vertex data
layout(location = 1) in highp vec3 position;
//---------vertex normal
layout (location = 2) in highp vec3 normal;
//---------vertex/model colour
layout(location = 3) in lowp vec4 colour;
//---------model transformation
layout(location = 4) in highp mat4 modelMatrix;
//4 5 6 7 used for transform since it is 4x4 

//---------colour to fragment shader
out lowp vec4 passColour;


uniform highp mat4 projectionMatrix;
uniform highp mat4 viewMatrix;

uniform highp mat4 vpMatrix;


void main(void){
	vec4 worldPos = modelMatrix * vec4(position,1);
	vec4 pos = vpMatrix * worldPos;
	
	gl_Position =  pos;
	
	passColour = colour;	
}
