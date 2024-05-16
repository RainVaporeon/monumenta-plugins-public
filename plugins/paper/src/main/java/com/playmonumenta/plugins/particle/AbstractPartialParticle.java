package com.playmonumenta.plugins.particle;

import com.destroystokyo.paper.ParticleBuilder;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class AbstractPartialParticle<SelfT extends AbstractPartialParticle<SelfT>> {

	private static final int PARTICLE_SPAWN_DISTANCE_SQUARED = 50 * 50;

	private static long mSpawnedParticles;

	public Particle mParticle;
	public Location mLocation;
	public int mCount = 1;
	public double mDeltaX = 0;
	public double mDeltaY = 0;
	public double mDeltaZ = 0;
	public double mExtra = 0;
	public @Nullable Object mData = null;

	/*
	 * Set to true to use mDelta values to move particles specifically in that
	 * relative direction.
	 * Set to false to use mDeltas for normal location randomisation.
	 */
	public boolean mDirectionalMode = false;

	/*
	 * Set to non-0 to randomly vary individual particles' mExtra values,
	 * by +- mExtraVariance.
	 */
	public double mExtraVariance = 0;

	public int mMinimumCount = 1;
	public double mMaximumMultiplier = 2;

	public double mDistanceFalloff = 0;

	/*
	 * Whether to randomise between negative mDelta or 0, and 0 or mDelta,
	 * for each axis, for individual particles' mDelta values.
	 */
	public boolean mVaryPositiveX = false;
	public boolean mVaryPositiveY = false;
	public boolean mVaryPositiveZ = false;
	public boolean mVaryNegativeX = false;
	public boolean mVaryNegativeY = false;
	public boolean mVaryNegativeZ = false;

	private @Nullable Predicate<Player> mPlayerCondition;

	private double mSkipBelowMultiplier = 0;

	public AbstractPartialParticle(Particle particle, Location location) {
		mParticle = particle;
		mLocation = location.clone();
	}

	/*
	 * Returns a good value to use for the X and Z deltas,
	 * if you want most particles to start within the specified entity's width.
	 */
	public static double getWidthDelta(Entity entity) {
		return getWidthDelta(entity.getWidth());
	}

	/*
	 * Returns a good value to use for the X and Z deltas,
	 * if you want most particles to start within the specified width.
	 */
	public static double getWidthDelta(double entityWidth) {
		return getDelta(entityWidth);
	}

	/*
	 * Returns a good value to use for the Y delta,
	 * if you want most particles to start within the specified entity's height.
	 */
	public static double getHeightDelta(Entity entity) {
		return getHeightDelta(entity.getHeight());
	}

	/*
	 * Returns a good value to use for the Y delta,
	 * if you want most particles to start within the specified height.
	 */
	public static double getHeightDelta(double entityHeight) {
		return getDelta(entityHeight);
	}

	/*
	 * Returns a good value to use for a delta,
	 * if you want most particles to start within the specified length.
	 */
	public static double getDelta(double length) {
		// When spawning normally, particles randomly vary their location
		// following Gaussian distribution, up to delta * 8 away,
		// but mostly within the closer half.
		return length / 4;
	}

	public SelfT deltaVariance(boolean deltaVariance) {
		return deltaVariance(deltaVariance, deltaVariance, deltaVariance);
	}

	public SelfT deltaVariance(boolean varyX, boolean varyY, boolean varyZ) {
		mVaryPositiveX = varyX;
		mVaryNegativeX = varyX;
		mVaryPositiveY = varyY;
		mVaryNegativeY = varyY;
		mVaryPositiveZ = varyZ;
		mVaryNegativeZ = varyZ;
		return getSelf();
	}

	public SelfT deltaVariance(boolean varyPositiveX, boolean varyNegativeX, boolean varyPositiveY, boolean varyNegativeY, boolean varyPositiveZ, boolean varyNegativeZ) {
		mVaryPositiveX = varyPositiveX;
		mVaryNegativeX = varyNegativeX;
		mVaryPositiveY = varyPositiveY;
		mVaryNegativeY = varyNegativeY;
		mVaryPositiveZ = varyPositiveZ;
		mVaryNegativeZ = varyNegativeZ;
		return getSelf();
	}

	/*
	 * Whether extra variance has been enabled.
	 */
	public boolean isExtraVaried() {
		return mExtraVariance != 0;
	}

	public boolean isDeltaVaried() {
		return mVaryNegativeX || mVaryPositiveX
			       || mVaryNegativeY || mVaryPositiveY
			       || mVaryNegativeZ || mVaryPositiveZ;
	}

	// methods for builder-style usage of PartialParticle

	public SelfT particle(Particle particle) {
		mParticle = particle;
		return getSelf();
	}

	public SelfT location(Location location) {
		mLocation = location;
		return getSelf();
	}

	public SelfT count(int count) {
		mCount = count;
		return getSelf();
	}

	public SelfT delta(double delta) {
		mDeltaX = delta;
		mDeltaY = delta;
		mDeltaZ = delta;
		return getSelf();
	}

	public SelfT delta(double deltaX, double deltaY, double deltaZ) {
		mDeltaX = deltaX;
		mDeltaY = deltaY;
		mDeltaZ = deltaZ;
		return getSelf();
	}

	public SelfT data(@Nullable Object data) {
		mData = data;
		return getSelf();
	}

	public SelfT directionalMode(boolean directionalMode) {
		mDirectionalMode = directionalMode;
		return getSelf();
	}

	public SelfT extra(double extra) {
		mExtra = extra;
		return getSelf();
	}

	public SelfT extraVariance(double extraVariance) {
		mExtraVariance = extraVariance;
		return getSelf();
	}

	public SelfT extraRange(double extraMin, double extraMax) {
		mExtra = (extraMin + extraMax) / 2;
		mExtraVariance = (extraMax - extraMin) / 2;
		return getSelf();
	}

	public SelfT minimumCount(int minimumCount) {
		mMinimumCount = minimumCount;
		return getSelf();
	}

	public SelfT maximumMultiplier(double maximumMultiplier) {
		mMaximumMultiplier = maximumMultiplier;
		return getSelf();
	}

	public SelfT conditional(Predicate<Player> playerCondition) {
		mPlayerCondition = playerCondition;
		return getSelf();
	}

	/**
	 * Reduces number of particles with distance, up to no particles at the given distance
	 */
	public SelfT distanceFalloff(double distanceFalloff) {
		mDistanceFalloff = distanceFalloff;
		return getSelf();
	}

	/**
	 * Sets a multiplier below which this particle won't be shown at all to the player.
	 */
	public SelfT skipBelowMultiplier(double skipBelowMultiplier) {
		mSkipBelowMultiplier = skipBelowMultiplier;
		return getSelf();
	}

	// spawn methods

	public SelfT spawnAsPlayerActive(Player sourcePlayer) {
		return spawnAsPlayer(sourcePlayer, ParticleCategory.OWN_ACTIVE, ParticleCategory.OTHER_ACTIVE);
	}

	public SelfT spawnAsOtherPlayerActive() {
		return spawnForPlayers(ParticleCategory.OTHER_ACTIVE);
	}

	public SelfT spawnAsPlayerPassive(Player sourcePlayer) {
		return spawnAsPlayer(sourcePlayer, ParticleCategory.OWN_PASSIVE, ParticleCategory.OTHER_PASSIVE);
	}

	public SelfT spawnAsOtherPlayerPassive() {
		return spawnForPlayers(ParticleCategory.OTHER_PASSIVE);
	}

	public SelfT spawnAsPlayerBuff(Player sourcePlayer) {
		return spawnAsPlayer(sourcePlayer, ParticleCategory.OWN_BUFF, ParticleCategory.OTHER_BUFF);
	}

	public SelfT spawnAsPlayerEmoji(Player sourcePlayer) {
		return spawnAsPlayer(sourcePlayer, ParticleCategory.OWN_EMOJI, ParticleCategory.OTHER_EMOJI);
	}

	/*
	 * Spawns particles for each nearby player,
	 * based on individual particle multiplier settings.
	 *
	 * Specify a sourcePlayer so we know which multiplier to use on each player;
	 * the OWN_ multipliers are used on the player causing the particles,
	 * while the OTHER_ multipliers are used for other players seeing his/her
	 * particles.
	 *
	 * Specify isPassive as false for active particles
	 * (eg Mana Lance ability, Spark enchant),
	 * or as true for passive particles (eg Gilded enchant).
	 */
	public SelfT spawnAsPlayer(Player sourcePlayer, ParticleCategory ownCategory, ParticleCategory othersCategory) {
		prepareSpawn();
		return forEachNearbyPlayer(
			(Player player) -> {
				spawnForPlayerInternal(player, player == sourcePlayer ? ownCategory : othersCategory);
			}
		);
	}

	/**
	 * Calls {@link #spawnAsPlayerActive(Player)} for a player entity, and {@link #spawnAsEnemy()} otherwise.
	 */
	public void spawnAsEntityActive(Entity entity) {
		if (entity instanceof Player) {
			spawnAsPlayerActive((Player) entity);
		} else if (entity.getScoreboardTags().contains("Boss")) {
			spawnAsBoss();
		} else {
			spawnAsEnemy();
		}
	}

	/**
	 * Calls {@link #spawnAsPlayerBuff(Player)} for a player entity, and {@link #spawnAsEnemyBuff()} otherwise.
	 */
	public void spawnAsEntityBuff(Entity entity) {
		if (entity instanceof Player) {
			spawnAsPlayerBuff((Player) entity);
		} else {
			// no boss check here - buffs are often applied by players and can interfere with particles from boss abilities
			spawnAsEnemyBuff();
		}
	}

	/**
	 * Spawns particles for each nearby player,
	 * based on individual enemy particle multiplier settings.
	 */
	public SelfT spawnAsEnemy() {
		return spawnForPlayers(ParticleCategory.ENEMY);
	}

	public SelfT spawnAsEnemyBuff() {
		return spawnForPlayers(ParticleCategory.ENEMY_BUFF);
	}

	/**
	 * Spawns particles for each nearby player,
	 * based on individual boss particle multiplier settings.
	 */
	public SelfT spawnAsBoss() {
		return spawnForPlayers(ParticleCategory.BOSS);
	}

	/*
	 * Spawns particles for each nearby player,
	 * with no partial multiplier applied
	 * (always spawns the full mCount amount).
	 */
	public SelfT spawnFull() {
		return spawnForPlayers(ParticleCategory.FULL);
	}

	/*
	 * Called once per nearby player,
	 * with packaged up values for you to use them to spawn particles in your
	 * desired pattern.
	 *
	 * This is likely the method you wish to override when subclassing.
	 * You have the chance to apply custom logic and then call
	 * spawnUsingSettings() with different packagedValues,
	 * as many times as needed.
	 */
	protected void doSpawn(ParticleBuilder packagedValues) {
		spawnUsingSettings(packagedValues);
	}

	/*
	 * Spawns the specified packagedValues normally,
	 * or if directional mode and/or delta/extra variance are enabled,
	 * applies them to a clone of the specified packagedValues,
	 * looping internally as needed.
	 */
	protected void spawnUsingSettings(ParticleBuilder packagedValues) {
		if (mDistanceFalloff != 0) {
			double distance = Objects.requireNonNull(packagedValues.location()).distance(Objects.requireNonNull(packagedValues.receivers()).get(0).getLocation());
			if (distance > mDistanceFalloff) {
				return;
			}
			int count = FastUtils.roundRandomly(packagedValues.count() * (1 - distance / mDistanceFalloff));
			if (count == 0) {
				return;
			}
			packagedValues.count(count);
		}
		mSpawnedParticles += packagedValues.count();
		if (!(mDirectionalMode || isDeltaVaried() || isExtraVaried())) {
			packagedValues.spawn();
		} else {
			ParticleBuilder variedClone = new ParticleBuilder(packagedValues.particle());
			variedClone.location(Objects.requireNonNull(packagedValues.location()));
			variedClone.extra(packagedValues.extra());
			variedClone.data(packagedValues.data());
			variedClone.receivers(packagedValues.receivers());

			int loops = packagedValues.count();
			if (mDirectionalMode) {
				// If directional mode, need to spawn one by one.
				// We set count to 0 to use MC's directional movement exception
				variedClone.count(0);
			} else {
				// Otherwise, if want to vary delta or extra,
				// also need to loop to spawn 1 each time
				variedClone.count(1);
			}

			for (int i = 0; i < loops; i++) {
				double variedDeltaX = packagedValues.offsetX();
				double variedDeltaY = packagedValues.offsetY();
				double variedDeltaZ = packagedValues.offsetZ();
				if (mVaryNegativeX || mVaryPositiveX) {
					variedDeltaX = FastUtils.randomDoubleInRange(
						mVaryNegativeX ? -variedDeltaX : 0,
						mVaryPositiveX ? variedDeltaX : 0
					);
				}
				if (mVaryNegativeY || mVaryPositiveY) {
					variedDeltaY = FastUtils.randomDoubleInRange(
						mVaryNegativeY ? -variedDeltaY : 0,
						mVaryPositiveY ? variedDeltaY : 0
					);
				}
				if (mVaryNegativeZ || mVaryPositiveZ) {
					variedDeltaZ = FastUtils.randomDoubleInRange(
						mVaryNegativeZ ? -variedDeltaZ : 0,
						mVaryPositiveZ ? variedDeltaZ : 0
					);
				}
				variedClone.offset(variedDeltaX, variedDeltaY, variedDeltaZ);

				if (isExtraVaried()) {
					variedClone.extra(packagedValues.extra() + FastUtils.randomDoubleInRange(-mExtraVariance, mExtraVariance));
				}

				variedClone.spawn();
			}
		}
	}

	public SelfT spawnForPlayers(ParticleCategory source) {
		prepareSpawn();
		return forEachNearbyPlayer(
			(Player player) -> spawnForPlayerInternal(player, source)
		);
	}

	public SelfT spawnForPlayers(ParticleCategory source, Collection<Player> players) {
		prepareSpawn();
		players.forEach(player -> {
			spawnForPlayerInternal(player, source);
		});
		return getSelf();
	}

	public SelfT spawnForPlayer(ParticleCategory source, Player player) {
		prepareSpawn();
		spawnForPlayerInternal(player, source);
		return getSelf();
	}

	protected void prepareSpawn() {
	}

	private SelfT forEachNearbyPlayer(Consumer<Player> playerAction) {
		for (Player player : mLocation.getWorld().getPlayers()) {
			if (player.getLocation().distanceSquared(mLocation) < PARTICLE_SPAWN_DISTANCE_SQUARED
				    && (mPlayerCondition == null || mPlayerCondition.test(player))) {
				playerAction.accept(player);
			}
		}
		return getSelf();
	}

	private void spawnForPlayerInternal(Player player, ParticleCategory source) {
		double particleMultiplier = Math.min(PlayerData.getParticleMultiplier(player, source), mMaximumMultiplier);
		if (particleMultiplier <= mSkipBelowMultiplier) {
			return;
		}
		int partialCount = getPartialCount(particleMultiplier, player, source);
		if (partialCount <= 0) {
			return;
		}

		ParticleBuilder packagedValues = new ParticleBuilder(mParticle);
		packagedValues.location(mLocation);
		packagedValues.count(partialCount);
		packagedValues.offset(mDeltaX, mDeltaY, mDeltaZ);
		packagedValues.extra(mExtra);
		packagedValues.data(mData);
		packagedValues.force(true);

		packagedValues.receivers(player);

		doSpawn(packagedValues);
	}

	protected int getPartialCount(double multiplier, Player player, ParticleCategory source) {
		double multipliedCount = Math.max(mMinimumCount, mCount * multiplier);
		if (multipliedCount >= 1) {
			return (int) Math.ceil(multipliedCount);
		} else {
			// If we don't want minimum multiplier (don't assume ceil 1 particle),
			// and count is a double under 1,
			// we randomise whether to see that 1 particle
			if (FastUtils.RANDOM.nextDouble() < multipliedCount) {
				return 1;
			} else {
				// partialCount of 0
				return 0;
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected SelfT getSelf() {
		return (SelfT) this;
	}

	/**
	 * Returns the total number of particles spawned by any partial particle since the server started.
	 */
	public static long getSpawnedParticles() {
		return mSpawnedParticles;
	}

}
