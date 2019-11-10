#version 460 core

//-highp for vertex positions,
//-mediump for texture coordinates,
//-lowp for colors

in lowp vec4 passColour;
out lowp vec4 outColour;

void main(void){
	outColour = passColour;	
}