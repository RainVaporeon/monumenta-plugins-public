package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * Casts Volley every 16 seconds in a cone in front of it dealing 32 damage to enemies hit.
 */

public class SpellVolley extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final PartialParticle mSpark;

	public SpellVolley(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mSpark = new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0);
	}

	@Override
	public void run() {
		World w = mBoss.getWorld();
		w.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.HOSTILE, 3, 1);
		w.playSound(mBoss.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.HOSTILE, 3, 1);
		w.playSound(mBoss.getLocation(), Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.HOSTILE, 3, 1);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20, 0, false));
		BukkitRunnable runA = new BukkitRunnable() {

			@Override
			public void run() {
				w.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 3, 1);
				w.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.HOSTILE, 3, 1);
				List<Projectile> projectiles = EntityUtils.spawnVolley(mBoss, 10, 2, 5.0, Arrow.class);
				for (Projectile projectile : projectiles) {
					AbstractArrow proj = (AbstractArrow) projectile;

					proj.setPickupStatus(PickupStatus.CREATIVE_ONLY);
					proj.setDamage(20);

					BukkitRunnable runB = new BukkitRunnable() {

						@Override
						public void run() {
							// spawn particle
							mSpark.location(proj.getLocation()).spawnAsEnemy();

							if (proj.isInBlock() || !proj.isValid()) {
								this.cancel();
							}
						}

					};
					runB.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runB);
				}
			}

		};
		runA.runTaskLater(mPlugin, 20);
		mActiveRunnables.add(runA);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 16;
	}

}
