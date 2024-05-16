package com.playmonumenta.plugins.bosses.spells.oldslabsbos;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellBash extends Spell {
	private static final String SPELL_NAME = "Bash";
	private static final String SELF_SLOWNESS_SRC = "SelfBashSlowness";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;

	public SpellBash(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = mBoss.getWorld();
	}

	@Override
	public void run() {
		if (mBoss instanceof Mob mob) {
			LivingEntity target = mob.getTarget();
			if (target == null) {
				return;
			}
			com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, SELF_SLOWNESS_SRC,
				new BaseMovementSpeedModifyEffect(25, -0.3));
			mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.5f, 0.7f);
			mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 1.5f, 1.75f);
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0.175).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.CRIT, mBoss.getLocation().add(0, 1, 0), 3, 0.4, 0.5, 0.4, 0.025).spawnAsEntityActive(mBoss);
					if (mTicks >= 25) {
						this.cancel();
						Location loc = mBoss.getEyeLocation().subtract(0, 0.15, 0);
						Vector direction = LocationUtils.getDirectionTo(target.getLocation().add(0, 1.25, 0), loc);
						loc.setDirection(direction);
						mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1.5f, 0.7f);
						mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.HOSTILE, 1.5f, 1.25f);
						new PartialParticle(Particle.CLOUD, mBoss.getLocation(), 25, 0.1, 0.1, 0.1, 0.25).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 15, 0.1, 0.1, 0.1, 0.2).spawnAsEntityActive(mBoss);
						new BukkitRunnable() {
							double mDegrees = 30;

							@Override
							public void run() {
								Vector vec;
								for (double r = 1; r < 5; r += 0.5) {
									for (double degree = mDegrees; degree <= mDegrees + 60; degree += 8) {
										double radian1 = Math.toRadians(degree);
										vec = new Vector(FastUtils.cos(radian1) * r, 0.75, FastUtils.sin(radian1) * r);
										vec = VectorUtils.rotateZAxis(vec, 20);
										vec = VectorUtils.rotateXAxis(vec, loc.getPitch() - 20);
										vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

										Location l = loc.clone().add(vec);
										new PartialParticle(Particle.CRIT, l, 1, 0.1, 0.1, 0.1, 0.025).spawnAsEntityActive(mBoss);
										new PartialParticle(Particle.CRIT_MAGIC, l, 1, 0.1, 0.1, 0.1, 0.025).spawnAsEntityActive(mBoss);
									}
								}
								mDegrees += 60;
								if (mDegrees >= 150) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 0, 1);

						for (Player player : PlayerUtils.playersInRange(loc, 4, true)) {
							Vector toPlayerVector = player.getLocation().toVector().subtract(loc.toVector()).normalize();
							if (direction.dot(toPlayerVector) > 0.33f) {
								BossUtils.blockableDamage(mBoss, player, DamageType.MELEE, 6, SPELL_NAME, mBoss.getLocation());
								MovementUtils.knockAway(mBoss.getLocation(), player, 0.5f, 0.65f, false);
							}
						}
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public int cooldownTicks() {
		return 20 * 5;
	}

	@Override
	public boolean canRun() {
		if (mBoss instanceof Mob mob) {
			LivingEntity target = mob.getTarget();
			if (target != null) {
				return target.getLocation().distance(mBoss.getLocation()) < 4;
			}
		}
		return false;
	}

}
