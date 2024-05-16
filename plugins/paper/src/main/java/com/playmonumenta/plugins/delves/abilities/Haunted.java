package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class Haunted {

	public static final String DESCRIPTION = "Your regrets haunt you.";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("A looming figure haunts you relentlessly,"),
			Component.text("only moving when you do.")
		};
	}

	public static final double MAX_SPEED = 0.5;
	public static final double DAMAGE = 0.4; //percentage
	public static final double RANGE = 50;
	public static final double VERTICAL_SPEED_DEBUFF = 3; // This makes Looming Consequence move slower vertically
	public static final double PLAYER_VERTICAL_CHANGE_DEBUFF = 2; // This makes the player's vertical movement factor less into Looming Consequence movement

	private static void followPlayer(Player p, ArmorStand armorStand) {
		Vector playerYDivider = new Vector(1, PLAYER_VERTICAL_CHANGE_DEBUFF, 1);
		Vector hauntedYDivider = new Vector(1, VERTICAL_SPEED_DEBUFF, 1);
		new BukkitRunnable() {
			Location mPLoc = p.getLocation();
			double mRadian = 0;
			double mSpeed = MAX_SPEED;

			int mHitTimer = 0;

			int mRangeCD = 0;
			@Override
			public void run() {
				if (!p.isOnline() || !armorStand.isValid()) {
					this.cancel();

					// We "hide" the armor stand by removing its equipment and glowing, and use its location to spawn a new shade later
					armorStand.getEquipment().clear();
					armorStand.setGlowing(false);
					return;
				} else if (p.isDead()) {
					return;
				}

				if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) {
					return;
				}

				Location sLoc = armorStand.getLocation();
				double distance = armorStand.getLocation().distance(p.getLocation());

				if (distance > RANGE) {
					armorStand.remove();
					this.cancel();
					summonBeast(p, p.getLocation().add(p.getLocation().getDirection().multiply(-10)));
					return;
				} else if (distance > RANGE / 2) {
					mSpeed = MAX_SPEED * 2;
				} else {
					mSpeed = MAX_SPEED;
				}

				if (EntityUtils.isInWater(p)) {
					mSpeed *= .8;
				}

				// Change the direction of the stand and its head
				Vector direction = LocationUtils.getDirectionTo(p.getLocation(), sLoc);
				sLoc.setDirection(direction);
				armorStand.setHeadPose(new EulerAngle(Math.toRadians(sLoc.getPitch()), 0, 0));
				armorStand.teleport(sLoc.clone().add(0, (FastMath.sin(mRadian) * 0.35)/10, 0));
				mRadian += Math.PI / 20D; // Finishes a sin bob in (20 * 2) ticks

				// Hit detection
				if (mHitTimer <= 0 && distance < 1) {
					p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, SoundCategory.HOSTILE, 1f, 2f);
					p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 1f, 0.5f);

					Location loc = p.getLocation().add(0, 1, 0);
					BossUtils.bossDamagePercent(armorStand, p, DAMAGE);
					if (p.isDead()) {
						p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_GHAST_HURT, SoundCategory.HOSTILE, 1f, 0.5f);
						p.getWorld().playSound(armorStand.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 1f, 0.65f);

						new PartialParticle(Particle.SOUL, loc, 70, 0, 0, 0, 0.15)
							.spawnAsPlayerActive(p);
						new PartialParticle(Particle.SMOKE_LARGE, loc, 40, 0, 0, 0, 0.185)
							.spawnAsPlayerActive(p);
					} else {
						new PartialParticle(Particle.SMOKE_LARGE, loc, 15, 0, 0, 0, 0.125)
							.spawnAsPlayerActive(p);
					}
					mHitTimer = 10;
				}
				mHitTimer--;

				// Visuals
				new PartialParticle(Particle.SMOKE_LARGE, armorStand.getLocation().add(0, 1, 0), 1, 0.3, 0.4, 0.3, 0)
					.spawnAsEntityActive(armorStand);
				new PartialParticle(Particle.SOUL, armorStand.getLocation().add(0, 1, 0), 1, 0.3, 0.4, 0.3, 0.025)
					.spawnAsEntityActive(armorStand);

				//Sounds
				distance = sLoc.distance(p.getLocation());
				if (distance <= 16 && mRangeCD <= 0) {
					p.playSound(armorStand.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.HOSTILE, 1.75f, 0f);
					mRangeCD = 70;
				}
				mRangeCD--;

				double pMovedTick = mPLoc.clone().subtract(p.getLocation()).toVector() // Delta between last location and current location
					.divide(playerYDivider).length(); // Decrease movement based on vertical player movement
				if (pMovedTick < 0.005) {
					return;
				} else if (pMovedTick > 1) {
					pMovedTick = 1;
				}

				armorStand.teleport(sLoc.add(direction.multiply(mSpeed*pMovedTick).divide(hauntedYDivider)));
				mPLoc = p.getLocation();
			}
		}.runTaskTimer(Plugin.getInstance(), 0L, 1L);
	}

	private static void summonBeast(Player p, Location loc) {
		String phantomName = DelvesManager.PHANTOM_NAME;
		for (Entity nearbyEntity : p.getLocation().getNearbyEntities(100, 100, 100)) {
			if (nearbyEntity instanceof ArmorStand && nearbyEntity.getScoreboardTags().contains(phantomName + p.getUniqueId())) {
				return;
			}
		}
		ArmorStand armorStand = Objects.requireNonNull((ArmorStand) LibraryOfSoulsIntegration.summon(loc, "LoomingConsequence"));
		armorStand.addScoreboardTag(phantomName + p.getUniqueId());
		followPlayer(p, armorStand);
	}

	public static void moveBackwards(Player player, int multiplier) {
		String phantomName = DelvesManager.PHANTOM_NAME;
		for (Entity nearbyEntity : player.getLocation().getNearbyEntities(20, 20, 20)) {
			if (nearbyEntity instanceof ArmorStand && nearbyEntity.getScoreboardTags().contains(phantomName + player.getUniqueId())) {
				Vector direction = nearbyEntity.getLocation().getDirection();
				direction.multiply(-multiplier);
				nearbyEntity.teleport(nearbyEntity.getLocation().add(direction));
			}
		}
	}

	public static void applyModifiers(Player p) {
		String phantomName = DelvesManager.PHANTOM_NAME;
		if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		Location loc = p.getLocation().add(p.getLocation().getDirection().multiply(-10));
		ArmorStand armorStand = null;
		List<Entity> nearbyEntities = (List<Entity>) p.getWorld().getNearbyEntities(p.getLocation(), 100, 100, 100);
		for (Entity nearbyEntity : nearbyEntities) {
			if (nearbyEntity instanceof ArmorStand stand && nearbyEntity.getScoreboardTags().contains(phantomName + p.getUniqueId())) {

				// We found the old "hidden" armor stand, so now we use its location to spawn a fresh Shade, and remove the old one.
				Location standLoc = stand.getLocation();
				armorStand = Objects.requireNonNull((ArmorStand) LibraryOfSoulsIntegration.summon(standLoc, "LoomingConsequence"));
				armorStand.addScoreboardTag(phantomName + p.getUniqueId());

				stand.remove();
			}
		}
		if (armorStand == null) {
			BukkitScheduler scheduler = Bukkit.getScheduler();
			scheduler.runTaskLater(Plugin.getInstance(), () -> {
				summonBeast(p, loc);
			}, 100L);
		} else {
			followPlayer(p, armorStand);
		}
	}

}
