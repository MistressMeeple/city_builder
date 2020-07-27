package com.meeple.backend.entity;

public class LivingEntity extends EntityBase {
	private float currentHealth = 1f;
	private float maxHealth = 10f;
	private float armour;

	public float armour() {
		return armour;
	}

	public float currentHealth() {
		return currentHealth;
	}

	public float maxHealth() {
		return maxHealth;
	}

	public void armour(float armour) {
		this.armour = armour;
	}

	public void currentHealth(float currentHealth) {
		this.currentHealth = currentHealth;
	}

	public void maxHealth(float maxHealth) {
		this.maxHealth = maxHealth;
	}

	/**
	 * Damages this entity. returns true if still "alive"
	 * ({@link #currentHealth}>{@link #maxHealth});
	 * 
	 * @param damage for entity to take
	 * @return true if ({@link #currentHealth}>{@link #maxHealth}), false otherwise
	 */
	public boolean takeDamage(float damage) {
		currentHealth -= damage;
		if (currentHealth < maxHealth) {
			return false;
		}
		return true;
	}

	/**
	 * Tests whether or not this entity will "die" if it takes this damage.
	 * 
	 * @param damage for the entity to take
	 * @return true if ({@link #currentHealth}-damage>{@link #maxHealth}), false
	 *         otherwise
	 */
	public boolean wouldDie(float damage) {
		return currentHealth - damage < maxHealth;
	}

}
