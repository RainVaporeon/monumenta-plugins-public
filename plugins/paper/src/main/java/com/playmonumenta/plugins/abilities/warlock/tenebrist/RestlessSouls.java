package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.bosses.bosses.abilities.RestlessSoulsBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class RestlessSouls extends Ability {
	private static final int DAMAGE_1 = 10;
	private static final int DAMAGE_2 = 15;
	private static final int SILENCE_DURATION_1 = 2 * 20;
	private static final int SILENCE_DURATION_2 = 3 * 20;
	private static final int VEX_DURATION = 15 * 20;
	private static final int VEX_CAP_1 = 3;
	private static final int VEX_CAP_2 = 5;
	private static final int DEBUFF_DURATION = 4 * 20;
	public static final String VEX_NAME = "RestlessSoul";
	private static final int TICK_INTERVAL = 1;
	private static final int DETECTION_RANGE = 24;
	private static final int RANGE = 8;
	private static final double DEBUFF_RANGE = 0.25;
	private static final double MOVESPEED = 5; // block(s) per second

	public static final String CHARM_DAMAGE = "Restless Souls Damage";
	public static final String CHARM_RADIUS = "Restless Souls Radius";
	public static final String CHARM_DURATION = "Restless Souls Duration";
	public static final String CHARM_CAP = "Restless Souls Vex Cap";
	public static final String CHARM_DEBUFF_RANGE = "Restless Souls Debuff Radius";
	public static final String CHARM_SPEED = "Restless Souls Movement Speed";

	public static final AbilityInfo<RestlessSouls> INFO =
		new AbilityInfo<>(RestlessSouls.class, "Restless Souls", RestlessSouls::new)
			.linkedSpell(ClassAbility.RESTLESS_SOULS)
			.scoreboardId("RestlessSouls")
			.shorthandName("RS")
			.descriptions(
				"Whenever an enemy dies within " + RANGE + " blocks of you, a glowing invisible invulnerable vex spawns. " +
					"The vex targets your enemies and possesses them, dealing " + DAMAGE_1 + " damage to the target and silences all mobs within " + DEBUFF_RANGE + " blocks for " + SILENCE_DURATION_1 / 20 + " seconds. " +
					"Vex count is capped at " + VEX_CAP_1 + " and each lasts for " + VEX_DURATION / 20 + " seconds. " +
					"Each vex can only possess 1 enemy. Enemies killed by the vex will not spawn additional vexes.",
				"Damage is increased to " + DAMAGE_2 + " and silence duration increased to " + SILENCE_DURATION_2 / 20 + " seconds. " +
					"Maximum vex count increased to " + VEX_CAP_2 + ". " +
					"Additionally, the possessed mob is inflicted with a level 1 debuff of the corresponding active skill that is on cooldown for " + DEBUFF_DURATION / 20 + " seconds. " +
					"Grasping Claws > 10% Slowness. Level 1 Choleric Flames > Set mobs on Fire. Level 2 Choleric Flames > Hunger. " +
					"Melancholic Lament > 10% Weaken. Withering Gaze > Wither. Haunting Shades > 10% Vulnerability.")
			.simpleDescription("Mobs that die near you spawn a vex that targets enemies, dealing damage and silencing them.")
			.displayItem(Material.VEX_SPAWN_EGG);


	private final boolean mLevel;
	private final double mDamage;
	private final int mSilenceTime;
	private final int mVexCap;
	private final double mDebuffRange;
	private final double mMoveSpeed;
	private final List<Vex> mVexList = new ArrayList<>();

	public RestlessSouls(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		boolean isLevelOne = isLevelOne();
		mLevel = isLevelOne;
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne ? DAMAGE_1 : DAMAGE_2);
		mSilenceTime = isLevelOne ? SILENCE_DURATION_1 : SILENCE_DURATION_2;
		mVexCap = (int) CharmManager.getLevel(player, CHARM_CAP) + (isLevelOne ? VEX_CAP_1 : VEX_CAP_2);
		mDebuffRange = CharmManager.calculateFlatAndPercentValue(player, CHARM_DEBUFF_RANGE, DEBUFF_RANGE);
		mMoveSpeed = CharmManager.calculateFlatAndPercentValue(player, CHARM_SPEED, MOVESPEED);
	}

	@Override
	public double entityDeathRadius() {
		return CharmManager.getRadius(mPlayer, CHARM_RADIUS, RANGE);
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		World world = mPlayer.getWorld();
		Location summonLoc = event.getEntity().getLocation();


		if (!summonLoc.isChunkLoaded()) {
			// mob is standing somewhere that's not loaded, abort
			return;
		}

		mVexList.removeIf(e -> !e.isValid() || e.isDead());

		Set<String> tags = event.getEntity().getScoreboardTags();
		if (tags.contains("TeneGhost") || tags.contains(AbilityUtils.IGNORE_TAG)) {
			return;
		}

		if (mVexList.size() < mVexCap) {
			Vex vex = (Vex) LibraryOfSoulsIntegration.summon(summonLoc.clone(), VEX_NAME);
			if (vex == null) {
				MMLog.warning("Failed to summon RestlessSoul");
				return;
			}
			mVexList.add(vex);

			RestlessSoulsBoss restlessSoulsBoss = BossUtils.getBossOfClass(vex, RestlessSoulsBoss.class);
			if (restlessSoulsBoss == null) {
				MMLog.warning("Failed to get RestlessSoulsBoss");
				return;
			}
			ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
			restlessSoulsBoss.spawn(mPlayer, mDamage, mDebuffRange, mSilenceTime, DEBUFF_DURATION, mLevel, playerItemStats);

			PartialParticle particle1 = new PartialParticle(Particle.SOUL, vex.getLocation().add(0, 0.25, 0), 1, 0.2, 0.2, 0.2, 0.01).spawnAsPlayerActive(mPlayer);
			PartialParticle particle2 = new PartialParticle(Particle.SOUL_FIRE_FLAME, vex.getLocation().add(0, 0.25, 0), 1, 0.2, 0.2, 0.2, 0.01).spawnAsPlayerActive(mPlayer);

			int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, VEX_DURATION);

			new BukkitRunnable() {
				int mTicksElapsed = 0;
				@Nullable LivingEntity mTarget;
				final Vex mBoss = Objects.requireNonNull(vex);
				double mRadian = FastUtils.randomDoubleInRange(0, Math.PI);

				@Override
				public void run() {
					Location loc = mBoss.getLocation().add(0, 0.25, 0);
					particle1.location(loc).spawnAsPlayerActive(mPlayer);
					particle2.location(loc).spawnAsPlayerActive(mPlayer);

					boolean isOutOfTime = mTicksElapsed >= duration;
					if (isOutOfTime || !mBoss.isValid()) {
						if (isOutOfTime && mBoss.isValid()) {
							Location vexLoc = mBoss.getLocation();

							world.playSound(vexLoc, Sound.ENTITY_VEX_DEATH, SoundCategory.PLAYERS, 1.5f, 1.0f);
							new PartialParticle(Particle.SOUL, vexLoc, 20, 0.2, 0.2, 0.2).spawnAsPlayerActive(mPlayer);
						}
						mVexList.remove(mBoss);
						mBoss.remove();

						this.cancel();
						return;
					}

					// re-aggro
					mTarget = mBoss.getTarget();
					if (mTarget == null || mTarget.isDead() || mTarget.getHealth() <= 0) {
						Location pLoc = mPlayer.getLocation();
						List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(pLoc, DETECTION_RANGE, mBoss);
						if (!nearbyMobs.isEmpty()) {
							nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
							nearbyMobs.removeIf(mob -> DamageUtils.isImmuneToDamage(mob, DamageEvent.DamageType.MAGIC));
							// check mob count again after removal of vexes
							if (nearbyMobs.size() > 0) {
								Collections.shuffle(nearbyMobs);
								LivingEntity randomMob = nearbyMobs.get(0);
								if (randomMob != null) {
									mBoss.setTarget(randomMob);
									mTarget = randomMob;
									world.playSound(mBoss.getLocation(), Sound.ENTITY_VEX_AMBIENT, SoundCategory.PLAYERS, 1.5f, 1.0f);

									Vector dir = LocationUtils.getHalfHeightLocation(randomMob).subtract(mBoss.getLocation()).toVector().normalize();
									new PPLine(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), LocationUtils.getEntityCenter(randomMob))
										.countPerMeter(1.5)
										.directionalMode(true)
										.delta(dir.getX(), dir.getY(), dir.getZ())
										.extra(0.12)
										.includeEnd(false)
										.spawnAsPlayerActive(mPlayer);
									new PartialParticle(Particle.SOUL, randomMob.getLocation().add(0, randomMob.getEyeHeight() + 1, 0), 8).extra(0.02).spawnAsPlayerActive(mPlayer);
								}
							}
						}
					}

					// haunted move boss method
					// movement
					Location vexLoc = mBoss.getLocation();
					if (mTarget != null && !mTarget.isDead()) {
						mBoss.setCharging(true);
						Vector direction = LocationUtils.getDirectionTo(LocationUtils.getEntityCenter(mTarget), vexLoc);
						//0.2x distance for vertical movement for flying mobs
						double yDiff = (mTarget.getLocation().getY() - mBoss.getLocation().getY()) * 0.2;
						if (yDiff > direction.getY()) {
							direction.setY(yDiff);
						}
						vexLoc.setDirection(direction);
						// set speed
						vexLoc.add(direction.multiply(mMoveSpeed * TICK_INTERVAL / 20));
						// attack
						if (mBoss.getBoundingBox().overlaps(mTarget.getBoundingBox())) {
							mVexList.remove(mBoss);
							mBoss.attack(mTarget);
						}
					} else {
						mBoss.setCharging(false);
					}
					// bobbing
					mBoss.teleport(vexLoc.clone().add(0, FastMath.sin(mRadian) * 0.05, 0));
					mRadian += Math.PI / 20D; // Finishes a sin bob in (20 * 2) ticks
					mTicksElapsed += TICK_INTERVAL;
				}
			}.runTaskTimer(mPlugin, 0, TICK_INTERVAL);
		}
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (mVexList != null) {
			mVexList.removeIf(e -> !e.isValid() || e.isDead());
			if (mVexList.size() > 0) {
				for (Vex v : mVexList) {
					v.remove();
				}
				mVexList.clear();
			}
		}
	}
}
