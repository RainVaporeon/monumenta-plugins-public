package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.aspects.BowAspect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.WeakHashMap;

public class Earthquake extends DepthsAbility {
	public static final String ABILITY_NAME = "Earthquake";
	public static final int COOLDOWN = 16 * 20;
	public static final int[] DAMAGE = {20, 25, 30, 35, 40, 50};
	public static final int[] SILENCE_DURATION = {80, 90, 100, 110, 120, 140};
	public static final int EARTHQUAKE_TIME = 20;
	public static final int RADIUS = 4;
	public static final double KNOCKBACK = 0.8;
	public static final int MAX_TICKS = 4 * 20;
	public static final String EARTHQUAKE_ARROW_METADATA = "EarthquakeArrow";

	private WeakHashMap<AbstractArrow, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;

	public Earthquake(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.COARSE_DIRT;
		mTree = DepthsTree.EARTHBOUND;
		mInfo.mLinkedSpell = ClassAbility.EARTHQUAKE;
		mInfo.mCooldown = COOLDOWN;
		mInfo.mIgnoreCooldown = true;
		mPlayerItemStatsMap = new WeakHashMap<>();
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof AbstractArrow arrow && mPlayerItemStatsMap.containsKey(arrow)) {
			quake(arrow, enemy.getLocation());
		}
		return false; // prevents multiple calls by removing the arrow (from the world and the player stats map)
	}

	private void quake(AbstractArrow arrow, Location loc) {
		World world = mPlayer.getWorld();

		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(arrow);
		if (playerItemStats != null) {
			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					if (mTicks >= EARTHQUAKE_TIME) {
						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RADIUS)) {
							if (!DepthsUtils.isPlant(mob)) {
								knockup(mob);
							}

							DamageUtils.damage(mob, mPlayer, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.mLinkedSpell, playerItemStats), DAMAGE[mRarity - 1], false, true, null);

							if (!EntityUtils.isBoss(mob)) {
								EntityUtils.applySilence(mPlugin, SILENCE_DURATION[mRarity - 1], mob);
							}
						}

						for (Player player : PlayerUtils.playersInRange(loc, RADIUS, true)) {
							knockup(player);
						}

						world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 30, RADIUS / 2, 0.1, RADIUS / 2, 0.1);
						world.spawnParticle(Particle.LAVA, loc, 20, RADIUS / 2, 0.3, RADIUS / 2, 0.1);
						world.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 3, 1.0f);
						world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 1, 1.0f);
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.75f, 1.0f);
						this.cancel();
					} else {
						world.spawnParticle(Particle.BLOCK_CRACK, loc, 30, RADIUS / 2, 0.25, RADIUS / 2, 0.1, Bukkit.createBlockData(Material.PODZOL));
						world.spawnParticle(Particle.BLOCK_CRACK, loc, 30, RADIUS / 2, 0.25, RADIUS / 2, 0.1, Bukkit.createBlockData(Material.GRANITE));
						world.spawnParticle(Particle.BLOCK_CRACK, loc, 30, RADIUS / 2, 0.25, RADIUS / 2, 0.1, Bukkit.createBlockData(Material.IRON_ORE));
						world.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 2, 1.0f);
						world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.75f, 0.5f);
					}

					mTicks += 5;
				}
			}.runTaskTimer(mPlugin, 0, 5);
		}

		arrow.remove();
	}

	private void knockup(LivingEntity le) {
		le.setVelocity(le.getVelocity().add(new Vector(0.0, KNOCKBACK, 0.0)));
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
			return true;
		}

		if (mPlayer.isSneaking()) {
			mInfo.mCooldown = (int) (COOLDOWN * BowAspect.getCooldownReduction(mPlayer));
			putOnCooldown();
			World world = mPlayer.getWorld();
			world.playSound(mPlayer.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 2, 1.0f);

			arrow.setPierceLevel(0);
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
			arrow.setVelocity(mPlayer.getLocation().getDirection().multiply(2.0));

			mPlayerItemStatsMap.put(arrow, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));

			mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.LAVA);

			new BukkitRunnable() {
				int mT = 0;
				@Override
				public void run() {
					if (mT > COOLDOWN || !mPlayerItemStatsMap.containsKey(arrow)) {
						arrow.remove();

						this.cancel();
					}

					if (arrow.getVelocity().length() < .05 || arrow.isOnGround()) {
						quake(arrow, arrow.getLocation());

						this.cancel();
					}
					mT++;
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}

		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Shooting a bow or trident while sneaking causes an earthquake " + EARTHQUAKE_TIME / 20 + " second after impact. The earthquake deals " + DepthsUtils.getRarityColor(rarity) + DAMAGE[rarity - 1] + ChatColor.WHITE + " magic damage to mobs in a " + RADIUS + " block radius, silencing for " + DepthsUtils.getRarityColor(rarity) + (float)SILENCE_DURATION[rarity - 1] / 20 + ChatColor.WHITE + " seconds and knocking upward. Cooldown: " + COOLDOWN / 20 + "s.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.EARTHBOUND;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SHIFT_BOW;
	}
}
