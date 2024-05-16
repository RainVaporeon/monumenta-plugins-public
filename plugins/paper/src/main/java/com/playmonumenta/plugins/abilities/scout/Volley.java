package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

public class Volley extends Ability {

	private static final int VOLLEY_COOLDOWN = 15 * 20;
	private static final int VOLLEY_1_ARROW_COUNT = 7;
	private static final int VOLLEY_2_ARROW_COUNT = 11;
	private static final double VOLLEY_1_DAMAGE_MULTIPLIER = 1.3;
	private static final double VOLLEY_2_DAMAGE_MULTIPLIER = 1.5;
	private static final double ENHANCEMENT_BLEED_POTENCY = 0.1;
	private static final int ENHANCEMENT_BLEED_DURATION = 4 * 20;
	public Set<Projectile> mVolley;
	private final Map<LivingEntity, Integer> mVolleyHitMap;

	public static final String CHARM_COOLDOWN = "Volley Cooldown";
	public static final String CHARM_ARROWS = "Volley Arrows";
	public static final String CHARM_DAMAGE = "Volley Damage";
	public static final String CHARM_BLEED_AMPLIFIER = "Volley Bleed Amplifier";
	public static final String CHARM_BLEED_DURATION = "Volley Bleed Duration";
	public static final String CHARM_PIERCING = "Volley Piercing";

	public static final AbilityInfo<Volley> INFO =
		new AbilityInfo<>(Volley.class, "Volley", Volley::new)
			.linkedSpell(ClassAbility.VOLLEY)
			.scoreboardId("Volley")
			.shorthandName("Vly")
			.descriptions(
				String.format("When you shoot a projectile while sneaking, you shoot a volley consisting of %d projectiles instead. " +
					              "Only one arrow is consumed, and each projectile deals %d%% bonus damage. Cooldown: 15s.", VOLLEY_1_ARROW_COUNT, (int) ((VOLLEY_1_DAMAGE_MULTIPLIER - 1) * 100)),
				String.format("Increases the number of projectiles to %d and enhances the bonus damage to %d%%.", VOLLEY_2_ARROW_COUNT, (int) ((VOLLEY_2_DAMAGE_MULTIPLIER - 1) * 100)),
				String.format("Volley now fires in a 360 degree arc. The projectiles inflict %d%% Bleed for %ds.", (int) (ENHANCEMENT_BLEED_POTENCY * 100), ENHANCEMENT_BLEED_DURATION / 20))
			.simpleDescription("Fire a volley of projectiles in front of you.")
			.cooldown(VOLLEY_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.ARROW)
			.priorityAmount(900); // cancels damage events of volley arrows, so needs to run before other abilities

	private final int mArrows;
	private final double mMultiplier;

	public Volley(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mArrows = (isLevelOne() ? VOLLEY_1_ARROW_COUNT : VOLLEY_2_ARROW_COUNT) + (int) CharmManager.getLevel(mPlayer, CHARM_ARROWS);
		mMultiplier = isLevelOne() ? VOLLEY_1_DAMAGE_MULTIPLIER : VOLLEY_2_DAMAGE_MULTIPLIER;

		mVolley = new HashSet<>();
		mVolleyHitMap = new HashMap<>();
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (!mPlayer.isSneaking()
			    || isOnCooldown()
			    || !EntityUtils.isAbilityTriggeringProjectile(projectile, false)) {
			return true;
		}

		// Start the cooldown first so we don't cause an infinite loop of Volleys
		putOnCooldown();
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 0.75f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 1f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1, 1.33f);
		// Garbage Collector at home
		mVolley.clear();
		mVolleyHitMap.clear();
		float arrowSpeed = (float) projectile.getVelocity().length();
		// Give time for other skills to set data
		new BukkitRunnable() {
			@Override
			public void run() {
				// Store PotionData from the original arrow only if it is weakness or slowness
				PotionData tArrowData = null;

				if (projectile instanceof Arrow regularArrow) {
					if (regularArrow.hasCustomEffects()) {
						tArrowData = regularArrow.getBasePotionData();
						if (tArrowData.getType() != PotionType.SLOWNESS && tArrowData.getType() != PotionType.WEAKNESS) {
							// This arrow isn't weakness or slowness - don't store the potion data
							tArrowData = null;
						}
					}
				}

				List<Projectile> projectiles;
				if (!isEnhanced()) {
					projectiles = EntityUtils.spawnVolley(mPlayer, mArrows, arrowSpeed, 5, projectile.getClass());
				} else {
					projectiles = EntityUtils.spawnVolley(mPlayer, mArrows * 5, arrowSpeed, 360.0 / (mArrows * 5), projectile.getClass());
				}

				int piercing = (projectile instanceof AbstractArrow) ? (int) CharmManager.getLevel(mPlayer, CHARM_PIERCING) : 0;

				for (Projectile proj : projectiles) {

					mVolley.add(proj);

					if (proj instanceof AbstractArrow arrow) {
						arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);

						arrow.setCritical(projectile instanceof AbstractArrow projectileArrow && projectileArrow.isCritical());
						arrow.setPierceLevel(piercing);

						// If the base arrow's potion data is still stored, apply it to the new arrows
						if (tArrowData != null) {
							((Arrow) proj).setBasePotionData(tArrowData);
						}
					} else if (proj instanceof ThrowableProjectile throwable && projectile instanceof ThrowableProjectile oldThrowable) {
						ItemUtils.setSnowballItem(throwable, oldThrowable.getItem());
					}

					mPlugin.mProjectileEffectTimers.addEntity(proj, Particle.SMOKE_NORMAL);

					ProjectileLaunchEvent event = new ProjectileLaunchEvent(proj);
					Bukkit.getPluginManager().callEvent(event);
				}

				// We can't just use arrow.remove() because that cancels the event and refunds the arrow
				Location jankWorkAround = mPlayer.getLocation();
				jankWorkAround.setY(-15);
				projectile.teleport(jankWorkAround);
			}
		}.runTaskLater(mPlugin, 0);

		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity proj = event.getDamager();
		if (event.getType() == DamageType.PROJECTILE && mVolley.contains(proj)) {
			if (notBeenHit(enemy)) {
				double damage = event.getDamage() * mMultiplier * (1 + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE));
				event.setDamage(damage);
				if (isEnhanced()) {
					EntityUtils.applyBleed(mPlugin, ENHANCEMENT_BLEED_DURATION, ENHANCEMENT_BLEED_POTENCY, enemy);
				}
			} else {
				// Only let one Volley arrow hit a given mob
				event.setCancelled(true);
			}
		}
		return false; // only changes event damage
	}

	private boolean notBeenHit(LivingEntity enemy) {
		// Basically the same logic as with MetadataUtils.happenedThisTick but with a hashmap in its stead
		if (mVolleyHitMap.get(enemy) != null && mVolleyHitMap.get(enemy) == enemy.getTicksLived()) {
			return false;
		}
		mVolleyHitMap.put(enemy, enemy.getTicksLived());
		return true;
	}

}
