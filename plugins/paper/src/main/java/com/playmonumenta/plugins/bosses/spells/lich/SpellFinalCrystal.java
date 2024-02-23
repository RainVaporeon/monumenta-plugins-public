package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellFinalCrystal extends Spell {
	private final Plugin mPlugin;
	private double mT = 20 * 1;
	private final int mSoloCooldown = 20 * 20;
	private double mCooldown;
	private final double mMaxFactor = 2.0;
	private final Location mCenter;
	private final double mRange;
	private final LivingEntity mBoss;
	private final List<Location> mCrystalLoc;
	private final Collection<EnderCrystal> mCrystal = new ArrayList<>();
	private static boolean mTriggered = false;
	private boolean mRecast = false;
	private final String mShieldCrystal = "DeathCrystal";
	private boolean mTrigger = false;
	private boolean mBombActive = false;
	private List<Player> mPlayers = new ArrayList<>();
	private final PartialParticle mSoul;
	private final PartialParticle mExpH;

	public SpellFinalCrystal(Plugin plugin, LivingEntity boss, Location loc, double range, List<Location> crystalLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = range;
		mCrystalLoc = crystalLoc;
		mSoul = new PartialParticle(Particle.SOUL, mBoss.getLocation(), 8, 3, 0.15, 3, 0);
		mExpH = new PartialParticle(Particle.EXPLOSION_HUGE, mBoss.getLocation(), 1, 0, 0, 0, 0.1).minimumCount(1);
	}

	@Override
	public void run() {
		//update player count every 5 seconds
		if (!mTrigger) {
			mPlayers = Lich.playersInRange(mCenter, mRange, true);
			mTrigger = true;
			new BukkitRunnable() {

				@Override
				public void run() {
					mTrigger = false;
				}

			}.runTaskLater(mPlugin, 20 * 5);
		}
		//cooldown
		double cooldownFactor = Math.min(mMaxFactor, (Math.log10(mPlayers.size()) + 1) / 1.25);
		mCooldown = mSoloCooldown / cooldownFactor;
		mT -= 5;
		if (mT <= 0 && !mBombActive) {
			mT += mCooldown;
			holyChestModified();
		}
	}

	private void holyChestModified() {
		World world = mBoss.getWorld();
		BossBar bar = BossBar.bossBar(Component.empty(), 1, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS, Set.of(BossBar.Flag.PLAY_BOSS_MUSIC));
		//get all active crystals
		for (Location l : mCrystalLoc) {
			mCrystal.addAll(l.getNearbyEntitiesByType(EnderCrystal.class, 3));
		}

		//recast function
		if (mCrystal.isEmpty()) {
			mRecast = true;
			List<Player> players = Lich.playersInRange(mCenter, mRange, true);
			double count = Math.min(8, Math.max(2, Math.sqrt(players.size())));
			Lich.spawnCrystal(mCrystalLoc, count, mShieldCrystal);

			for (Location l : mCrystalLoc) {
				mCrystal.addAll(l.getNearbyEntitiesByType(EnderCrystal.class, 3));
			}
		}

		//time limit 6s to break all active crystals
		BukkitRunnable runA = new BukkitRunnable() {
			double mT;
			final int mCount = mCrystal.size();

			@Override
			public void run() {
				//glowy crystal to tell players to break
				if (mT == 0) {
					ScoreboardUtils.modifyTeamColor("crystal", NamedTextColor.WHITE);
					for (EnderCrystal e : mCrystal) {
						e.setGlowing(true);
						e.setBeamTarget(mBoss.getLocation().add(0, 1.5, 0));
						ScoreboardUtils.addEntityToTeam(e, "crystal");
					}
				}

				//exit function
				mCrystal.removeIf(en -> !en.isValid());
				int remainingCrystals = mCrystal.size();
				if (remainingCrystals == 0 || Lich.bossDead()) {
					world.playSound(mBoss.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 4.0f, 0.5f);
					this.cancel();
					return;
				}
				//warning 1
				if (mT == 20 * 2) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 3.0f, 0.75f);
					ScoreboardUtils.modifyTeamColor("crystal", NamedTextColor.YELLOW);
				}
				//warning 2
				if (mT == 20 * 4) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 3.0f, 0.75f);
					ScoreboardUtils.modifyTeamColor("crystal", NamedTextColor.RED);
				}
				//execute order 66
				if (mT >= 20 * 6) {
					attack();
					if (!mRecast) {
						mTriggered = true;
					}
					this.cancel();
					return;
				}
				mT++;
				//boss bar stuff
				float progress = Math.min(remainingCrystals * 1.0f / mCount, 1);
				bar.name(Component.text(remainingCrystals + " Death Crystals Remaining!", NamedTextColor.YELLOW));
				bar.progress(progress);
				if (progress <= 0.34) {
					bar.color(BossBar.Color.RED);
				} else if (progress <= 0.67) {
					bar.color(BossBar.Color.YELLOW);
				}
				for (Player player : mBoss.getWorld().getPlayers()) {
					if (player.getLocation().distance(mBoss.getLocation()) < mRange) {
						player.showBossBar(bar);
					} else {
						player.hideBossBar(bar);
					}
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				BossUtils.hideBossBar(bar, world);
			}
		};
		runA.runTaskTimer(mPlugin, 20 * 1, 1);
		mActiveRunnables.add(runA);
	}

	private void attack() {
		mBombActive = true;
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10.0f, 0.5f);
		//kill ghast shield
		for (Location loc : mCrystalLoc) {
			List<LivingEntity> enList = EntityUtils.getNearbyMobs(loc, 3);
			enList.removeIf(e -> e.getType() == EntityType.ENDER_CRYSTAL);
			for (LivingEntity en : enList) {
				en.setHealth(0);
			}
		}
		//kill end crystals + summon horse bombs
		for (EnderCrystal e : mCrystal) {
			Location spawnLoc = e.getLocation();
			e.remove();
			mExpH.location(e.getLocation()).spawnAsBoss();
			world.playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2f, 1f);
			for (int i = 0; i < 5; i++) {
				//Velocity randomized of the frosted ice as a falling block
				FallingBlock block = world.spawnFallingBlock(spawnLoc, Bukkit.createBlockData(Material.CRYING_OBSIDIAN));
				block.setDropItem(false);
				EntityUtils.disableBlockPlacement(block);
				block.setVelocity(new Vector(FastUtils.randomDoubleInRange(-0.55, 0.55), FastUtils.randomDoubleInRange(0.25, 1), FastUtils.randomDoubleInRange(-0.55, 0.55)));

				PPCircle indicator = new PPCircle(Particle.SOUL_FIRE_FLAME, block.getLocation(), 4).count(12).delta(0.2, 0, 0.2);

				BukkitRunnable runB = new BukkitRunnable() {

					@Override
					public void run() {
						// horseman bomb toss copy and paste
						if (block.isOnGround() || !block.isValid()) {
							block.remove();
							//limit particle count for ability stacking
							world.playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2, 0.85f);
							Location loc = block.getLocation();

							BukkitRunnable runC = new BukkitRunnable() {
								int mTicks = 0;
								final Location mLoc = loc.clone().add(0, 0.2, 0);

								@Override
								public void run() {
									mTicks += 5;
									mSoul.location(mLoc).spawnAsBoss();
									indicator.location(mLoc).spawnAsBoss();

									if (mTicks % 10 == 0) {
										for (Player player : Lich.playersInRange(block.getLocation(), 4, true)) {
											if (mCenter.distance(player.getLocation()) < mRange) {
												/* Fire aura can not be blocked */
												BossUtils.bossDamagePercent(mBoss, player, 0.15, "Souldrain Pool");
												AbilityUtils.increaseDamageDealtPlayer(player, 20 * 8, -0.3, "Lich");
											}
										}
									}

									if (Lich.bossDead() || mTicks >= 20 * 30 + 30) {
										this.cancel();
										mBombActive = false;
									}
								}

							};
							runC.runTaskTimer(mPlugin, 10, 5);
							mActiveRunnables.add(runC);
							this.cancel();
						}
					}

				};
				runB.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(runB);
			}
		}
	}

	public static boolean getTriggered() {
		return mTriggered;
	}

	public static void setTriggered(boolean trigger) {
		mTriggered = trigger;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
