package com.meeple.backend.game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.joml.Matrix4f;
import org.joml.Vector2i;

import com.meeple.backend.ShaderPrograms;
import com.meeple.backend.noise.NoiseStack;
import com.meeple.shared.CollectionSuppliers;
import com.meeple.shared.frame.FrameUtils;
import com.meeple.shared.frame.FrameUtils.GridMovement;
import com.meeple.shared.frame.OGL.ShaderProgram.Mesh;

public class Island {

	public static enum GroundSubType {
		Ground_Low(), Ground_High(), Water_Shallow(), Water_Deep();
	}

	final TreeMap<Float, GroundSubType> map = new TreeMap<>();

	public GroundSubType fromValue(float sample) {
		//			System.out.println(sample);
		GroundSubType entry0 = map.get(sample);

		Entry<Float, GroundSubType> entry1 = map.higherEntry(sample);
		Entry<Float, GroundSubType> entry2 = map.floorEntry(sample);
		Entry<Float, GroundSubType> entry3 = map.ceilingEntry(sample);
		if (entry0 == null) {
			if (entry1 != null)
				entry0 = entry1.getValue();
			if (entry2 != null)
				entry0 = entry2.getValue();

			if (entry3 != null)
				entry0 = entry3.getValue();

		}
		if (entry0 == null) {
			System.out.println(sample);
		}
		return entry0;

	}

	//	float seaLevel = 0.75f;
	static float hillStart = 0.2f;
	//	float shallowDepth = 0.05f;

	public Island() {
		/*
				float hills = seaLevel + hillStart;
				float shallows = seaLevel - shallowDepth;
				map.put(2f, GroundSubType.Ground_High);
				map.put(hills, GroundSubType.Ground_Low);
				map.put(seaLevel, GroundSubType.Water_Shallow);
				map.put(shallows, GroundSubType.Water_Deep);*/
	}

	Map<Vector2i, Tile> tiles = new CollectionSuppliers.MapSupplier<Vector2i, Tile>().get();

	private class Tile {

	}

	public void init(float seaLevel, float shallowDepth, float hillHeight) {

		float hills = seaLevel + hillHeight;
		float shallows = seaLevel - shallowDepth;
		map.put(2f, GroundSubType.Ground_High);
		map.put(hills, GroundSubType.Ground_Low);
		map.put(seaLevel, GroundSubType.Water_Shallow);
		map.put(shallows, GroundSubType.Water_Deep);
	}

	private void loop(Map<GroundSubType, Mesh> gridMeshes, NoiseStack stack, int gridSize, float islandRotation, float cutoff, float maxRad) {

	}

	public void setupArrayMesh(Map<GroundSubType, Mesh> gridMeshes, NoiseStack stack, float gridSize, float islandRotation, float cutoff, float maxRad) {

		Map<GroundSubType, List<Number>> lists = new HashMap<>();
		Map<GroundSubType, Integer> renderCounts = new HashMap<>();
		//NOTE reusable transformation matrix
		Matrix4f mat = new Matrix4f();
		int startX = 0, startY = 0;

		float x = 0, y = 0; //leave alone
		GridMovement move = GridMovement.RIGHT;
		boolean flip = true;//leave alone

		//row check count
		int rowCheckCount = 4;
		boolean[] row_checks = new boolean[rowCheckCount * 2];

		int count = 250;

		for (int range = 0; range < count * 2;) {
			boolean check = true;
			for (int i = 0; i < range / 2f; i++) {
				float nx = startX + (x);
				float ny = startY + (y);

				float sx = (nx * gridSize) * 2500f;
				float sy = (ny * gridSize) * 2500f;
				float sample = stack.sample(sx, sy);

				mat.identity();

				GroundSubType subType = fromValue(sample);
				List<Number> list = lists.get(subType);

				if (list == null) {
					list = new CollectionSuppliers.ListSupplier<Number>().get();
					lists.put(subType, list);
				}
				float x1 = nx * count;
				float y1 = ny * count;
				float c1 = 3.5f / gridSize;
				float x2 = x1 / c1;
				float y2 = y1 / c1;
				float x3 = (nx * 20f);
				float y3 = (ny * 20f);

				float tileX = x3;
				float tileY = y3;

				mat.setTranslation(tileX, tileY, 0.75f);
				mat.scale((gridSize / 2f + 1) / 10f);

				FrameUtils.appendToList(list, mat);
				renderCounts.put(subType, renderCounts.getOrDefault(subType, 0) + 1);

				check = check && subType == GroundSubType.Water_Deep;

				switch (move) {
					case RIGHT://right
						x += 2f / count;
						break;
					case DOWN://down
						y += 2f / count;
						break;
					case LEFT: //left
						x -= 2f / count;
						break;
					case UP: //up
						y -= 2f / count;
						break;
				}
			}

			//NOTE controls. 
			{
				range += 1;
				flip = !flip;
				move = move.next();
			}
			//NOTE early escape control. 
			{
				boolean[] temp = Arrays.copyOf(row_checks, row_checks.length - 1);
				boolean earlyEscape = check;

				row_checks[0] = check;

				for (int i = row_checks.length - 1; i > 0; i--) {
					int r = i;
					int t = i - 1;
					row_checks[r] = temp[t];
					earlyEscape &= row_checks[r];
				}

				if (earlyEscape && false) {
					break;
				}
			}
		}
		for (GroundSubType gsb : GroundSubType.values()) {

			List<Number> list = lists.getOrDefault(gsb, new CollectionSuppliers.ListSupplier<Number>().get());
			Integer renderCount = renderCounts.getOrDefault(gsb, 0);
			Mesh gridMesh = gridMeshes.get(gsb);

			gridMesh
				.renderCount(renderCount)
				.getAttribute(ShaderPrograms.transformAtt.name)
				.data(list).update.set(true);
		}
	}

	public void genMesh() {
		
	}
}
