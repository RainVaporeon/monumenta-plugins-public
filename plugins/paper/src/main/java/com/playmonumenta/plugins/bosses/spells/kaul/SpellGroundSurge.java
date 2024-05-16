package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.BaseMovementSpeedModifyEffect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellGroundSurge extends Spell {
	private static final String SPELL_NAME = "Ground Surge";
	private static final String SLOWNESS_TAG = "GroundSurgeSlowness";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mRange;

	private final ChargeUpManager mChargeUp;

	public SpellGroundSurge(Plugin plugin, LivingEntity boss, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;

		mChargeUp = Kaul.defaultChargeUp(mBoss, (int) (20 * 2.5), SPELL_NAME);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.clearEffects(mBoss, BaseMovementSpeedModifyEffect.GENERIC_NAME);
		com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(mBoss, BaseMovementSpeedModifyEffect.GENERIC_NAME,
			new BaseMovementSpeedModifyEffect((int) (20 * 2.75), -0.3));
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, true);
		players.removeIf(p -> p.getLocation().getY() >= 61);
		BukkitRunnable runnable = new BukkitRunnable() {
			float mPitch = 0;

			@Override
			public void run() {

				Location loc = mBoss.getLocation();
				mPitch += 0.025f;
				if (mChargeUp.getTime() % 2 == 0) {
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 3, mPitch);
				}
				new PartialParticle(Particle.BLOCK_DUST, loc, 8, 0.4, 0.1, 0.4, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.SMOKE_LARGE, loc, 2, 0.25, 0.1, 0.25, 0.25).spawnAsEntityActive(mBoss);

				if (mChargeUp.nextTick()) {
					this.cancel();
					mChargeUp.reset();
					if (players.isEmpty()) {
						return;
					}

					final int targets;
					switch (players.size()) {
						case 1 -> targets = 1;
						case 2 -> targets = 2;
						case 3 -> targets = 3;
						case 4, 5, 6 -> targets = 4;
						case 7, 8, 9, 10 -> targets = 5;
						case 11, 12, 13, 14, 15 -> targets = 6;
						case 16, 17, 18, 19, 20 -> targets = 7;
						default -> targets = 8;
					}

					List<Player> toHit = new ArrayList<>();
					while (toHit.size() < targets) {
						Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
						if (!toHit.contains(player)) {
							toHit.add(player);
						}
					}

					Location nloc = mBoss.getLocation().add(0, 0.5, 0);
					for (Player target : toHit) {
						Vector dir = LocationUtils.getDirectionTo(target.getLocation(), nloc).setY(0).normalize().multiply(1.1);

						BukkitRunnable runnable = new BukkitRunnable() {
							int mInnerTicks = 0;
							BoundingBox mBox = BoundingBox.of(nloc, 0.65, 0.65, 0.65);

							@Override
							public void run() {
								mInnerTicks++;
								mBox.shift(dir);
								Location bLoc = mBox.getCenter().toLocation(world);
								if (bLoc.getBlock().getType().isSolid()) {
									bLoc.add(0, 1, 0);
									if (bLoc.getBlock().getType().isSolid()) {
										this.cancel();
										bLoc.subtract(0, 1, 0);
									}
								}

								if (!bLoc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
									bLoc.subtract(0, 1, 0);
									if (!bLoc.getBlock().getType().isSolid()) {
										bLoc.subtract(0, 1, 0);
										if (!bLoc.getBlock().getType().isSolid()) {
											this.cancel();
										}
									}
								}
								bLoc.add(0, 0.5, 0);

								if (mInnerTicks >= 45) {
									this.cancel();
								}

								world.playSound(bLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 0.75f, 1);
								new PartialParticle(Particle.BLOCK_DUST, bLoc, 20, 0.5, 0.5, 0.5, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.FLAME, bLoc, 15, 0.5, 0.5, 0.5, 0.075).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.LAVA, bLoc, 2, 0.5, 0.5, 0.5, 0.25).spawnAsEntityActive(mBoss);
								for (Player player : players) {
									if (player.getBoundingBox().overlaps(mBox)) {
										this.cancel();
										DamageUtils.damage(mBoss, player, DamageType.BLAST, 24, null, false, true, SPELL_NAME);
										com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player,
											SLOWNESS_TAG, new PercentSpeed(20 * 20, -0.5, SLOWNESS_TAG));
										MovementUtils.knockAway(mBoss.getLocation(), player, 0.3f, 1f);
										new PartialParticle(Particle.SMOKE_LARGE, bLoc, 20, 0, 0, 0, 0.2).spawnAsEntityActive(mBoss);
										new PartialParticle(Particle.FLAME, bLoc, 75, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
										world.playSound(bLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1);

										// Send surges to all other players now.
										if (players.size() <= 1) {
											break;
										}
										// Find a random other player that is not equal to the current target
										Player rPlayer = target;
										while (rPlayer.getUniqueId().equals(target.getUniqueId())) {
											rPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
										}
										Player tPlayer = rPlayer;
										BukkitRunnable runnable = new BukkitRunnable() {
											Player mTPlayer = tPlayer;
											BoundingBox mBox = BoundingBox.of(bLoc, 0.4, 0.4, 0.4);
											int mTicks = 0;
											int mHits = 0;
											List<UUID> mHit = new ArrayList<>();

											@Override
											public void run() {
												mTicks++;
												Location innerBoxLoc = mBox.getCenter().toLocation(world);
												Vector dir = LocationUtils.getDirectionTo(mTPlayer.getLocation(), innerBoxLoc).setY(0).normalize();
												mBox.shift(dir.clone().multiply(0.7));
												if (innerBoxLoc.getBlock().getType().isSolid()) {
													innerBoxLoc.add(0, 1, 0);
													if (innerBoxLoc.getBlock().getType().isSolid()) {
														this.cancel();
														innerBoxLoc.subtract(0, 1, 0);
													}
												}
												if (!innerBoxLoc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
													innerBoxLoc.subtract(0, 1, 0);
													if (!innerBoxLoc.getBlock().getType().isSolid()) {
														innerBoxLoc.subtract(0, 1, 0);
														if (!innerBoxLoc.getBlock().getType().isSolid()) {
															this.cancel();
														}
													}
												}
												innerBoxLoc.add(0, 1, 0);
												world.playSound(innerBoxLoc, Sound.BLOCK_STONE_BREAK, SoundCategory.HOSTILE, 0f, 1);
												//Have particles with collision show only for the player who's targeted.
												//This is to prevent lag from the numerous other surges that have these same
												//Particles
												new PartialParticle(Particle.BLOCK_DUST, innerBoxLoc, 8, 0.2, 0.2, 0.2, 0.25, Material.COARSE_DIRT.createBlockData()).spawnAsEntityActive(mBoss);
												new PartialParticle(Particle.FLAME, innerBoxLoc, 6, 0.2, 0.2, 0.2, 0.075).spawnAsEntityActive(mBoss);
												new PartialParticle(Particle.LAVA, innerBoxLoc, 1, 0.2, 0.2, 0.2, 0.25).spawnAsEntityActive(mBoss);
												for (Player surgePlayer : players) {
													if (surgePlayer.getBoundingBox().overlaps(mBox)
														    && !surgePlayer.getUniqueId().equals(player.getUniqueId())
														    && !mHit.contains(surgePlayer.getUniqueId())) {
														mHit.add(surgePlayer.getUniqueId());
														DamageUtils.damage(mBoss, surgePlayer, DamageType.BLAST, 40, null, false, true, SPELL_NAME);
														com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(player,
															SLOWNESS_TAG, new PercentSpeed(20 * 20, -0.5, SLOWNESS_TAG));
														MovementUtils.knockAway(loc, player, 0.3f, 1f);
														new PartialParticle(Particle.SMOKE_LARGE, innerBoxLoc, 10, 0, 0, 0, 0.2).spawnAsEntityActive(mBoss);
														new PartialParticle(Particle.FLAME, innerBoxLoc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(mBoss);
														world.playSound(innerBoxLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 1.25f);
														mHits++;
														mTicks = 0;
														if (mHits < players.size() && mHits <= 2) {
															int attempts = 0;
															mTPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
															while (mHit.contains(mTPlayer.getUniqueId())) {
																//A rare case can occur where the loop has gone through all of the possible
																//players, but they have been hit. Add an attempt integer to make sure that
																//it does not cause an infinite loop.
																if (attempts < 5) {
																	mTPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
																	attempts++;
																} else {
																	this.cancel();
																	break;
																}
															}
														} else {
															this.cancel();
														}
													}
												}
												if (mTicks >= 20 * 1.25) {
													this.cancel();
												}
											}

										};
										runnable.runTaskTimer(mPlugin, 0, 1);
										mActiveRunnables.add(runnable);
										break;
									}
								}
							}

						};
						runnable.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(runnable);
					}
				}
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public boolean canRun() {
		if (mBoss instanceof Mob mob) {
			LivingEntity target = mob.getTarget();
			return target instanceof Player;
		}
		return false;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 10;
	}

}
