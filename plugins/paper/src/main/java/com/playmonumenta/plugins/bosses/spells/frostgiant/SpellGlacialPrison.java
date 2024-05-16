package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/*
 Glacial Prison - Traps ⅓ players in ice for 3 seconds, after those 3
 seconds the prison explodes dealing 20 damage and giving mining fatigue
 3 for 10 seconds and weakness 2 for 10 seconds.
 */
public class SpellGlacialPrison extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mRange;
	private final Location mStartLoc;

	private boolean mCooldown = false;

	public SpellGlacialPrison(Plugin plugin, LivingEntity boss, double range, Location start) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mStartLoc = start;
	}

	@Override
	public void run() {
		FrostGiant.freezeGolems(mBoss);
		FrostGiant.delayHailstormDamage();
		//Glacial Prison can not be cast within 60 seconds of the previous cast of it
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 60);

		//Plays warning sound and chooses 1/3 of players to target randomly
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 3, 0.5f);
		List<Player> players = PlayerUtils.playersInRange(mStartLoc, mRange, true);
		List<Player> targets = new ArrayList<Player>();
		if (players.size() >= 2) {
			int cap = (players.size() + 1) / 2;
			for (int i = 0; i < cap; i++) {
				Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
				if (!targets.contains(player)) {
					targets.add(player);
				} else {
					cap++;
				}
			}
		} else {
			targets = players;
		}

		for (Player player : targets) {
			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					mT++;
					new PartialParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1, 0), 2, 0.4, 0.4, 0.4, 0.05).spawnAsEntityActive(mBoss);

					if (mBoss.isDead() || !mBoss.isValid()) {
						this.cancel();
					}

					if (mT >= 40) {
						this.cancel();
						//Blocks
						new PartialParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1, 0), 30, 0.4, 0.4, 0.4, 0.2).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.EXPLOSION_NORMAL, player.getLocation().add(0, 1, 0), 35, 0.4, 0.4, 0.4, 0.25).spawnAsEntityActive(mBoss);
						world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 0.5f);
						world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 0.75f);

						//Center the player first
						Vector dir = player.getLocation().getDirection();
						Location l = player.getLocation().getBlock().getLocation().add(0.5, 0.15, 0.5).setDirection(dir);
						while (l.getY() - mStartLoc.getY() >= 3) {
							l.add(0, -1, 0);
						}
						player.teleport(l);

						Location center = player.getLocation();
						Location[] locs = new Location[] {
							//First Layer
							center.clone().add(1, 0, 0),
							center.clone().add(-1, 0, 0),
							center.clone().add(0, 0, 1),
							center.clone().add(0, 0, -1),

							//Second Layer
							center.clone().add(1, 1, 0),
							center.clone().add(-1, 1, 0),
							center.clone().add(0, 1, 1),
							center.clone().add(0, 1, -1),

							//Top & Bottom
							center.clone().add(0, 2, 0),
							center.clone().add(0, -1, 0)
						};

						List<Block> changedBlocks = new ArrayList<>();
						int prisonDuration = 20 * 8;
						for (Location loc : locs) {
							Material mat = Material.BLUE_ICE;
							if (Math.abs(loc.getY() - (center.getY() + 1)) < 0.1) {
								// Ice at eye level to see through
								mat = Material.ICE;
							} else if (loc.getY() < center.getY()) {
								// A sea lantern below the player's feet
								mat = Material.SEA_LANTERN;
							}
							if (TemporaryBlockChangeManager.INSTANCE.changeBlock(loc.getBlock(), mat, prisonDuration - 1)) {
								changedBlocks.add(loc.getBlock());
							}
						}

						player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, prisonDuration / 2, 1));

						//Only lasts 4 seconds, needs to be done more than once
						FrostGiant.delayHailstormDamage();

						new BukkitRunnable() {
							int mTicks = 0;
							float mPitch = 0;

							@Override
							public void run() {
								mTicks++;
								mPitch += 0.02f;

								Location middle = center.clone().add(0, 1, 0);

								if (mBoss.isDead() || !mBoss.isValid()) {
									this.cancel();
									TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(changedBlocks, Material.BLUE_ICE);
									TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(changedBlocks, Material.ICE);
									TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(changedBlocks, Material.SEA_LANTERN);
									return;
								}

								new PartialParticle(Particle.FIREWORKS_SPARK, middle, 3, 1, 1, 1, 0).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.CLOUD, middle, 2, 1, 1, 1, 0).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.DAMAGE_INDICATOR, middle, 1, 0.5, -0.25, 0.5, 0.005).spawnAsEntityActive(mBoss);

								if (mTicks % 10 == 0) {
									world.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.HOSTILE, 1, mPitch);
								}

								if (mTicks == 20 * 2) {
									FrostGiant.delayHailstormDamage();
								}

								//If player did not escape within 4 seconds, damage by 80% of health and remove the ice prison
								if (mTicks >= prisonDuration / 2) {
									FrostGiant.unfreezeGolems(mBoss);
									this.cancel();
									new PartialParticle(Particle.FIREWORKS_SPARK, middle, 50, 1, 1, 1, 0.35).spawnAsEntityActive(mBoss);
									new PartialParticle(Particle.CLOUD, middle, 75, 1, 1, 1, 0.25).spawnAsEntityActive(mBoss);
									world.playSound(center, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1, 0.75f);
									world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0.75f);
									if (player.getLocation().distance(center) <= 2) {
										BossUtils.bossDamagePercent(mBoss, player, 0.8, "Glacial Prison");
									}
								}
							}
						}.runTaskTimer(mPlugin, 0, 2);
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	@Override
	public int cooldownTicks() {
		return 10 * 20;
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

}
