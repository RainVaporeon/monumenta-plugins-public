package com.playmonumenta.plugins.bosses.spells.sealedremorse;

import com.playmonumenta.plugins.bosses.bosses.Ghalkor;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class GhalkorForwardSweep extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Ghalkor mBossClass;
	private static final String SLOWNESS_SRC = "ForwardSweepSlowness";
	private static final int SLOW_DURATION = 20 * 8;
	private static final double SLOW_POTENCY = -0.3;

	public GhalkorForwardSweep(Plugin plugin, LivingEntity boss, Ghalkor bossClass) {
		mPlugin = plugin;
		mBoss = boss;
		mBossClass = bossClass;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Vector dir = mBoss.getLocation().getDirection();
		com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
			new BaseMovementSpeedModifyEffect(20, -0.15));

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				Location loc = mBoss.getLocation();
				loc.setDirection(dir);

				if (mTicks >= 20) {
					Vector vec;
					List<BoundingBox> boxes = new ArrayList<>();

					world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 3, 0.5f);
					world.playSound(loc, Sound.ENTITY_WITHER_SKELETON_DEATH, SoundCategory.HOSTILE, 3, 0.2f);

					//Rather inefficient, but was not able to optimize without breaking it
					//Final particle show
					for (double r = 0; r < 7; r++) {
						for (double degree = 60; degree < 120; degree += 5) {
							double radian1 = Math.toRadians(degree);
							vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
							vec = VectorUtils.rotateYAxis(vec, loc.getYaw());
							Location l = loc.clone().add(vec);
							//1.5 -> 15
							BoundingBox box = BoundingBox.of(l, 0.65, 3, 0.65);
							boxes.add(box);

							new PartialParticle(Particle.SWEEP_ATTACK, l, 1, 0.1, 0.2, 0.1, 0.1).spawnAsEntityActive(mBoss);
							new PartialParticle(Particle.ITEM_CRACK, l, 1, 0.1, 0.2, 0.1, 0.1, new ItemStack(Material.BONE)).spawnAsEntityActive(mBoss);
						}
					}

					for (Player player : PlayerUtils.playersInRange(loc, 40, true)) {
						for (BoundingBox box : boxes) {
							if (player.getBoundingBox().overlaps(box)) {
								DamageUtils.damage(mBoss, player, DamageType.MELEE, 26, null, false, true, "Forward Sweep");
								com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, SLOWNESS_SRC,
									new PercentSpeed(SLOW_DURATION, SLOW_POTENCY, SLOWNESS_SRC));
							}
						}
					}

					this.cancel();
				} else {
					world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 2f);

					Vector vec;
					//Rather inefficient, but was not able to optimize without breaking it
					//The degree range is 60 degrees for 30 blocks radius
					for (double r = 0; r < 7; r++) {
						for (double degree = 60; degree < 120; degree += 5) {
							double radian1 = Math.toRadians(degree);
							vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
							vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

							//Spawns particles
							Location l = loc.clone().add(vec);
							new PartialParticle(Particle.CRIT, l, 1, 0.1, 0.2, 0.1, 0).spawnAsEntityActive(mBoss);
						}
					}
				}
				mTicks += 2;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return (int) (5 * 20 * mBossClass.mCastSpeed);
	}
}
