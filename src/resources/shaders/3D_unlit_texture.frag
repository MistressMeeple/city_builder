#version 460 core
in vec2 vTextureCoords;

out lowp vec4 outColour;

uniform sampler2D texture01;

void main(void){
	outColour = texture(texture01,vTextureCoords);
}

