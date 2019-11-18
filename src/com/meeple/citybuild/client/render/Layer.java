package com.meeple.citybuild.client.render;

public class Layer extends Renderable {
	public static void main(String[] args) {
		Layer window = new Layer();
		window.name = "window";
		Layer screenb = new Layer();
		screenb.name = "screen b";
		Layer screenc = new Layer();
		screenc.name = "screen c";
		screenc.colour.w = 0.5f;
		screenb.colour.w = 0.5f;
		screenb.setParent(window);
		screenb.setChild(screenc);
		screenb.renderTree();

	}

	public Layer() {

	}

	@Override
	public void render() {
		System.out.println("layer " + name + " render");
	}
}
