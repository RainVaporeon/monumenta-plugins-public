package com.playmonumenta.plugins.bosses.spells.sealedremorse;

import com.playmonumenta.plugins.bosses.bosses.BeastOfTheBlackFlame;
import com.playmonumenta.plugins.bosses.bosses.Ghalkor;
import com.playmonumenta.plugins.bosses.spells.SpellBaseCharge;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

public class BlackflameCharge extends SpellBaseCharge {
	private static final int DAMAGE = 25;
	private static final int GROUND_DAMAGE = 22;
	private static final int FIRE_DURATION = 20 * 4;

	private final BeastOfTheBlackFlame mBossClass;

	public BlackflameCharge(Plugin plugin, LivingEntity boss, BeastOfTheBlackFlame bossClass) {
		super(plugin, boss, 20, 20 * 6, 10, false,
			// Warning sound/particles at boss location and slow boss
			(LivingEntity player) -> {
				new PartialParticle(Particle.VILLAGER_ANGRY, boss.getLocation(), 50, 2, 2, 2, 0).spawnAsEntityActive(boss);
				com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(boss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
					new BaseMovementSpeedModifyEffect(40, -0.75));
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 1.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 1.15f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_VINDICATOR_AMBIENT, SoundCategory.HOSTILE, 1f, 0.85f);
			},
			// Warning particles
			(Location loc) -> new PartialParticle(Particle.CRIT, loc, 2, 0.65, 0.65, 0.65, 0).spawnAsEntityActive(boss),
			// Charge attack sound/particles at boss location
			(LivingEntity player) -> {
				new PartialParticle(Particle.SMOKE_NORMAL, boss.getLocation(), 125, 0.4, 0.4, 0.4, 0.25).spawnAsEntityActive(boss);
				new PartialParticle(Particle.CLOUD, boss.getLocation(), 45, 0.15, 0.4, 0.15, 0.15).spawnAsEntityActive(boss);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 0.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1f, 0.9f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 1.4f);
			},
			// Attack hit a player
			(LivingEntity player) -> {
				new PartialParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 5, 0.4,
					0.4, 0.4, 0.4, Material.REDSTONE_BLOCK.createBlockData()).spawnAsEntityActive(boss);
				new PartialParticle(Particle.BLOCK_CRACK, player.getLocation().add(0, 1, 0), 12, 0.4,
					0.4, 0.4, 0.4, Material.REDSTONE_WIRE.createBlockData()).spawnAsEntityActive(boss);
				BossUtils.blockableDamage(boss, player, DamageType.MAGIC, DAMAGE, "Blackflame Charge", boss.getLocation());
			},
			// Attack particles
			(Location loc) -> {
				new PartialParticle(Particle.SWEEP_ATTACK, loc, 4, 0.5, 0.5, 0.5, 0.075).spawnAsEntityActive(boss);
				new PartialParticle(Particle.CRIT, loc, 8, 0.5, 0.5, 0.5, 0.75).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SMOKE_NORMAL, loc, 12, 0.5, 0.5, 0.5, 0.2).spawnAsEntityActive(boss);

				List<Player> players = PlayerUtils.playersInRange(boss.getLocation(), Ghalkor.detectionRange, true);
				World world = boss.getWorld();

				//Damaging trail left behind
				new BukkitRunnable() {
					private int mT = 0;
					private final BoundingBox mHitbox = new BoundingBox().shift(loc).expand(1);
					private final Location mParticleLoc = loc.clone().subtract(0, 1, 0);

					@Override
					public void run() {
						if (mT >= 20 * 5 || boss.isDead() || !boss.isValid()) {
							this.cancel();
						}

						if (mT % 20 == 0) {
							world.playSound(mParticleLoc, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 0.025f, 0f);
							new PartialParticle(Particle.SQUID_INK, mParticleLoc, 1, 0.6, 0.2, 0.6, 0.02).spawnAsEntityActive(boss);
						}
						new PartialParticle(Particle.SMOKE_LARGE, mParticleLoc, 1, 0.6, 0.2, 0.6, 0).spawnAsEntityActive(boss);
						new PartialParticle(Particle.ASH, mParticleLoc, 1, 0.6, 0.4, 0.6, 0).spawnAsEntityActive(boss);

						for (Player player : players) {
							if (mHitbox.overlaps(player.getBoundingBox())) {
								world.playSound(mParticleLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 0.5f, 0f);
								EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), FIRE_DURATION, player, boss);
								DamageUtils.damage(boss, player, DamageType.MAGIC, GROUND_DAMAGE, null, false, true, "Blackflame Charge");
							}
						}

						mT += 10;
					}
				}.runTaskTimer(plugin, 0, 10);
			},
			// Ending particles on boss
			() -> {
				new PartialParticle(Particle.SMOKE_NORMAL, boss.getLocation(), 125, 0.4, 0.4, 0.4, 0.25).spawnAsEntityActive(boss);
				new PartialParticle(Particle.CLOUD, boss.getLocation(), 45, 0.15, 0.4, 0.15, 0.15).spawnAsEntityActive(boss);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 0.1f, 1.5f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 1f, 0.75f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1f, 0.9f);
				boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1f, 1.4f);
			});
		mBossClass = bossClass;
	}

	@Override
	public int cooldownTicks() {
		return (int) (6 * 20 * mBossClass.mCastSpeed);
	}

}
