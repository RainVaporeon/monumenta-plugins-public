package com.playmonumenta.plugins.depths.bosses.spells.hedera;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellIvyGarden extends Spell {
	private static final int DAMAGE = 40;
	private static final int RADIUS = 5;
	private static final int DURATION = 80;
	private static final String SLOWNESS_SRC = "IvyGardenSlowness";
	private static final int SLOWNESS_DURATION = 100; // 5 seconds

	private final Plugin mPlugin;
	private final int mRadius;
	private final int mTime;
	public int mCooldownTicks;
	private final Map<Location, LivingEntity> mPlants;

	public SpellIvyGarden(Plugin plugin, int cooldown, Map<Location, LivingEntity> plants) {
		mPlugin = plugin;
		mRadius = RADIUS;
		mTime = DURATION;
		mCooldownTicks = cooldown;
		mPlants = plants;
	}

	@Override
	public boolean canRun() {
		return mPlants.values().size() > 0;
	}

	@Override
	public void run() {

		for (LivingEntity le : mPlants.values()) {
			if (le != null && !le.isDead()) {
				runForce(mPlugin, le, mRadius, mTime, mCooldownTicks, false, false, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1,
						(Location loc) -> new PartialParticle(Particle.SMOKE_LARGE, loc, 1, ((double) mRadius) / 2, ((double) mRadius) / 2, ((double) mRadius) / 2, 0.05).spawnAsEntityActive(le),
						(Location loc) -> new PartialParticle(Particle.CRIT_MAGIC, loc, 1, 0.25, 0.25, 0.25, 0.1).spawnAsEntityActive(le),
						(Location loc) -> {
							World world = loc.getWorld();
							world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1.5f, 0.65f);
							world.playSound(loc, Sound.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 1f, 0.5f);
							world.playSound(loc, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.HOSTILE, 1f, 0.8f);
							new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 0.5, 0), 100, 0.5, 0, 0.5, 0.8f).spawnAsEntityActive(le);
						},
						(Location loc) -> {
							new PartialParticle(Particle.SMOKE_LARGE, loc, 1, 0.1, 0.1, 0.1, 0.3).spawnAsEntityActive(le);
							new PartialParticle(Particle.SMOKE_NORMAL, loc, 2, 0.25, 0.25, 0.25, 0.1).spawnAsEntityActive(le);
						},
						(Location loc) -> {
							for (Player player : PlayerUtils.playersInRange(le.getLocation(), mRadius, true)) {

								double distance = player.getLocation().distance(loc);
								if (distance < mRadius / 3.0) {
									com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
										new PercentSpeed(SLOWNESS_DURATION, -0.5, SLOWNESS_SRC));
									MovementUtils.knockAway(le, player, 3.0f, false);
								} else if (distance < (mRadius * 2.0) / 3.0) {
									com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
										new PercentSpeed(SLOWNESS_DURATION, -0.3, SLOWNESS_SRC));
									MovementUtils.knockAway(le, player, 2.1f, false);
								} else if (distance < mRadius) {
									com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
										new PercentSpeed(SLOWNESS_DURATION, -0.1, SLOWNESS_SRC));
									MovementUtils.knockAway(le, player, 1.2f, false);
								}
								BossUtils.blockableDamage(le, player, DamageType.MAGIC, DAMAGE, "Ivy Garden", le.getLocation());

								new PartialParticle(Particle.VILLAGER_ANGRY, player.getLocation().clone().add(0, 1, 0), 4, 0.25, 0.5, 0.25, 0).spawnAsEntityActive(le);
							}
						});
				//Resistance
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(le, PercentDamageReceived.GENERIC_NAME,
					new PercentDamageReceived(100, -0.6));
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	public void runForce(Plugin plugin, LivingEntity launcher, int radius, int duration, int cooldown, boolean canMoveWhileCasting, boolean needLineOfSight,
	                     Sound chargeSound, float soundVolume, int soundDensity, Consumer<Location> chargeAuraAction, Consumer<Location> chargeCircleAction,
	                     Consumer<Location> outburstAction, Consumer<Location> circleOutburstAction, Consumer<Location> dealDamageAction) {
		if (needLineOfSight) {
			// Don't cast if no player in sight, e.g. should not initiate cast through a wall
			boolean hasLineOfSight = false;
			for (Player player : PlayerUtils.playersInRange(launcher.getLocation(), mRadius * 4, true)) {
				if (LocationUtils.hasLineOfSight(launcher, player)) {
					hasLineOfSight = true;
					break;
				}
			}
			if (!hasLineOfSight) {
				return;
			}
		}

		if (!canMoveWhileCasting) {
			EntityUtils.selfRoot(launcher, duration);
		}

		new BukkitRunnable() {
			float mTicks = 0;
			double mCurrentRadius = mRadius;
			final World mWorld = launcher.getWorld();

			@Override
			public void run() {
				Location loc = launcher.getLocation();

				if (EntityUtils.shouldCancelSpells(launcher)) {
					launcher.setAI(true);
					if (!canMoveWhileCasting) {
						EntityUtils.cancelSelfRoot(launcher);
					}
					this.cancel();
					return;
				}
				mTicks++;
				chargeAuraAction.accept(loc.clone().add(0, 1, 0));
				if (mTicks <= (duration - 5) && mTicks % soundDensity == 0) {
					mWorld.playSound(launcher.getLocation(), chargeSound, SoundCategory.HOSTILE, soundVolume, 0.25f + (mTicks / 100));
				}
				for (double i = 0; i < 360; i += 30) {
					double radian1 = Math.toRadians(i);
					loc.add(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
					chargeCircleAction.accept(loc);
					loc.subtract(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
				}
				mCurrentRadius -= (mRadius / ((double) duration));
				if (mCurrentRadius <= 0) {
					this.cancel();
					dealDamageAction.accept(loc);
					outburstAction.accept(loc);

					new BukkitRunnable() {
						final Location mLoc = launcher.getLocation();
						double mBurstRadius = 0;

						@Override
						public void run() {
							for (int j = 0; j < 2; j++) {
								mBurstRadius += 1.5;
								for (double i = 0; i < 360; i += 15) {
									double radian1 = Math.toRadians(i);
									mLoc.add(FastUtils.cos(radian1) * mBurstRadius, 0, FastUtils.sin(radian1) * mBurstRadius);
									circleOutburstAction.accept(mLoc);
									mLoc.subtract(FastUtils.cos(radian1) * mBurstRadius, 0, FastUtils.sin(radian1) * mBurstRadius);
								}
							}
							if (mBurstRadius >= mRadius) {
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}
}
