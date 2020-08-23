#version 460 core
in vec2 vTextureCoords;

out lowp vec4 outColour;
uniform float alphaDiscardThreshold;
uniform sampler2D texture1;

void main(void){
	vec4 sampled = texture(texture1,vTextureCoords);
	if(sampled.a < alphaDiscardThreshold){
		discard;
	}
	outColour = sampled;
}

