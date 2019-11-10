package com.meeple.citybuild;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.joml.Vector2f;
import org.joml.Vector2i;

import com.meeple.citybuild.LevelData.Chunk.Tile;


public class Buildings {

	public static BuildingTemplate stockpileTemplate = new BuildingTemplate() {

		@Override
		public BuildingType getType() {
			return BuildingType.Storage;
		}

		@Override
		public int hardPeopleLimitLower() {
			return 0;
		}

		@Override
		public int hardPoepleLimitUpper() {
			return 50;
		}

		@Override
		public int softPeopleLimitLower() {
			return 25;
		}

		@Override
		public int softPeopleLimitUpper() {
			return 0;
		}

		@Override
		public int hardAreaLimitLower() {
			return 0;
		}

		@Override
		public int hardAreaLimitUpper() {
			return 32;
		}

		@Override
		public int softAreaLimitLower() {
			return 4;
		}

		@Override
		public int softAreaLimitUpper() {
			return 16;
		}

	};

	class Person {
		Vector2f personalAreaLimits = new Vector2f();
		Vector2f personalAreaPreference = new Vector2f();
		Vector2i coHabbitantLimits = new Vector2i(1, 50);
		Vector2i coHabbitantPreference = new Vector2i(4, 7);
		EnumSet<BuildingType> buildingAccessRequired;
		EnumSet<BuildingType> buildingAccessPreference;

	}

	enum ItemQuality {
		Low, Medium, High;
	}

	enum ItemType {
		Clothing, Food, Drink;
	}

	class Item {
		ItemQuality quality = ItemQuality.Low;
		ItemType type;
		String name;
		/**
		 * This marks if an item is a luxury item, does not count towards needed items but provides happiness bonus. 
		 */
		boolean isLuxury = false;
	}

	enum RawMaterial {
		Fiber, Wood, Stone, Metal
	}

	enum RawNutritian {
		Meat, Fruit, Vegtable, Fungi
	}

	enum BuildingType {
		Social, Exersize, Nourishment, Housing, Work, Education, ResourceGathering, ResourceRefining, Storage;
	}

	interface BuildingTemplate extends BuildingLimits {
		public BuildingType getType();
	}

	interface BuildingLimits {
		/**
		 * This denotes the absolute min people capacity of a building.
		 * @return the hard min people-capacity value 
		 */
		public int hardPeopleLimitLower();

		/**
		 * This denotes the absolute max people capacity of a building.
		 * @return the hard max people-capacity value 
		 */
		public int hardPoepleLimitUpper();

		/**
		 * This is the initial people-capacity lower limit of the building when created. <br> 
		 * This can be reduced but cannot go past the {@link #hardPeopleLimitLower()}
		 * @return the soft min people-capacity value 
		 */
		public int softPeopleLimitLower();

		/**
		 * This is the initial people-capacity upper limit of the building when created. <br> 
		 * This can be increased but cannot go past the {@link #hardPeopleLimitUpper()}
		 * @return the soft max people-capacity value
		 */
		public int softPeopleLimitUpper();

		/**
		 * This denotes the absolute min area of a building.
		 * @return the min area value 
		 */
		public int hardAreaLimitLower();

		/**
		 * This denotes the absolute max area of a building.
		 * @return the max area value 
		 */
		public int hardAreaLimitUpper();

		/**
		 * This is the initial area limits of the building when created. <br> 
		 * This can be reduced but cannot go past the {@link #hardAreaLimitLower()}
		 * @return the soft min area value 
		 */
		public int softAreaLimitLower();

		/**
		 * This is the initial area limits of the building when created. <br> 
		 * This can be increased but cannot go past the {@link #hardAreaLimitUpper()}
		 * @return the soft max area value 
		 */
		public int softAreaLimitUpper();
	}

	class BuildingInstance implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -2650642460711317618L;
		public int currentPeople = 0;
		//dont save as these are soft references
		transient public Set<Tile> tiles = new HashSet<>();

	}

}
