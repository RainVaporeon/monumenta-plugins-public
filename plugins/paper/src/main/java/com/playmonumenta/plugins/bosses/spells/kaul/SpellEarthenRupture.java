package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * Earthen Rupture: After charging for 2 seconds, the Elemental will cause a large rupture that
spans out 6 blocks, knocking back all players, dealing 18 damage, and applying Slowness II for 10 seconds.
 */
public class SpellEarthenRupture extends Spell {
	private static final String SPELL_NAME = "Earthen Rupture";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;

	private final ChargeUpManager mChargeUp;

	public SpellEarthenRupture(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mChargeUp = Kaul.defaultChargeUp(mBoss, 45, SPELL_NAME);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mBoss.removePotionEffect(PotionEffectType.SLOW);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 50, 1));
		BukkitRunnable runnable = new BukkitRunnable() {

			@Override
			public void run() {

				Location loc = mBoss.getLocation();
				if (mChargeUp.getTime() % 2 == 0) {
					world.playSound(loc, Sound.BLOCK_GRAVEL_HIT, SoundCategory.HOSTILE, 2, 0.9f);
				}

				new PartialParticle(Particle.BLOCK_DUST, loc, 8, 0.4, 0.1, 0.4, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 2, 0.25, 0.1, 0.25, 0.25).spawnAsEntityActive(mBoss);
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.reset();
					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.5f, 0.9f);
					new PartialParticle(Particle.BLOCK_DUST, loc, 250, 3, 0.1, 3, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.LAVA, loc, 100, 3, 0.1, 3, 0.25).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 75, 3, 0.1, 3, 0.25).spawnAsEntityActive(mBoss);
					for (Player player : PlayerUtils.playersInRange(loc, 6, true)) {
						DamageUtils.damage(mBoss, player, DamageType.BLAST, 20, null, false, true, SPELL_NAME);
						MovementUtils.knockAway(loc, player, 0.50f, 1.5f);
						com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player, "EarthenRuptureSlowness",
							new PercentSpeed(20 * 10, -0.5, "EarthenRuptureSlowness"));
					}
				}
			}

			@Override
			public synchronized void cancel() {
				mActiveRunnables.remove(this);
				super.cancel();
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 15;
	}

}
