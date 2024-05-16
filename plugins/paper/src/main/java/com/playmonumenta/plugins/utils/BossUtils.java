package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.scriptedquests.managers.SongManager;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class BossUtils {

	/**
	 * Returns boss health scaling coefficient.
	 * Multiply base health to get final health.
	 * Inverse the coefficient to get damage reduction.
	 * Formula: coefficient = X ^ ((playerCount - 1) ^ Y)
	 * @param playerCount Player amount for health scaling
	 * @param x Base exponent, increase X to increase health scaling. range = [0 < X < 1]
	 * @param y Player count exponent, decrease Y to increase health scaling. range = [0 < Y < 1]
	 */
	public static double healthScalingCoef(int playerCount, double x, double y) {
		if (playerCount < 1) {
			return 1;
		}
		double scalingCoef = 0;
		// calculates smallest scaling increase first. largest scaling increase last.
		while (playerCount > 0) {
			double iterCoef = Math.pow(x, Math.pow(playerCount - 1, y));
			scalingCoef += iterCoef;
			playerCount--;
		}
		return scalingCoef;
	}

	public static boolean bossDamageBlocked(Player player, @Nullable Location location) {
		/*
		 * Attacks can only be blocked if:
		 * - They have a source location
		 * - Shield is not on cooldown
		 * - The player is facing towards the damage
		 */
		if (player.isBlocking() && location != null && player.getCooldown(Material.SHIELD) <= 0) {
			Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
			Vector toSourceVector = location.toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
			return playerDir.dot(toSourceVector) > 0.33;
		}

		return false;
	}

	public static boolean blockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double damage) {
		Location location = null;
		if (damager != null) {
			location = damager.getLocation();
		}
		return blockableDamage(damager, damagee, type, damage, null, location);
	}

	public static boolean blockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double damage, @Nullable Location location) {
		return blockableDamage(damager, damagee, type, damage, null, location);
	}

	public static boolean blockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double damage, @Nullable String cause, @Nullable Location location) {
		int stunTicks = (int) (20 * damage / 2.5);

		// adjust stun time based on region
		if (damagee instanceof Player player) {
			// every 2.5 damage = 1s of stun time in R1, 3.5 damage per 1s in R2, and 4 damage per 1s in R3.
			double stunRatio = ServerProperties.getClassSpecializationsEnabled(player) ? (ServerProperties.getAbilityEnhancementsEnabled(player) ? 4.0 : 3.5) : 2.5;
			stunTicks = (int) (20 * damage / stunRatio);
		}
		return blockableDamage(damager, damagee, type, damage, cause, location, stunTicks);
	}

	public static boolean blockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double damage, @Nullable String cause, @Nullable Location location, int stunTicks) {
		// One shield durability damage for every 5 points of damage
		return blockableDamage(damager, damagee, type, damage, false, true, cause, location, stunTicks, (int) (damage / 5));
	}

	//Returns whether the attack was blocked or otherwise completely negated (true = not blocked)
	public static boolean blockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double damage, boolean bypassIFrames, boolean causeKnockback, @Nullable String cause, @Nullable Location location, int stunTicks, int durability) {
		if (DamageUtils.isImmuneToDamage(damagee, type)) {
			return false;
		}

		if (damagee instanceof Player player && bossDamageBlocked(player, location)) {
			if (stunTicks > 0) {
				NmsUtils.getVersionAdapter().stunShield(player, stunTicks);
				damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
			}
			ItemUtils.damageShield(player, durability);
			return false;
		} else {
			DamageUtils.damage(damager, damagee, new DamageEvent.Metadata(type, null, null, cause), damage, bypassIFrames, causeKnockback, false);
			if (damagee instanceof Player player && Shielding.doesShieldingApply(player, damager) && damage > 0 && stunTicks > 0) {
				Shielding.disable(player);
			}
			return true;
		}
	}

	public static boolean bossDamagePercent(@Nullable LivingEntity boss, LivingEntity target, double percentHealth) {
		return bossDamagePercent(boss, target, percentHealth, null, false, null, true);
	}

	public static boolean bossDamagePercent(@Nullable LivingEntity boss, LivingEntity target, double percentHealth, @Nullable Location location) {
		return bossDamagePercent(boss, target, percentHealth, location, false, null, true);
	}

	public static boolean bossDamagePercent(@Nullable LivingEntity boss, LivingEntity target, double percentHealth, @Nullable Location location, boolean raw) {
		return bossDamagePercent(boss, target, percentHealth, location, raw, null, true);
	}

	public static boolean bossDamagePercent(@Nullable LivingEntity boss, LivingEntity target, double percentHealth, @Nullable String cause) {
		return bossDamagePercent(boss, target, percentHealth, null, false, cause, true);
	}

	public static boolean bossDamagePercent(@Nullable LivingEntity boss, LivingEntity target, double percentHealth, @Nullable Location location, @Nullable String cause) {
		return bossDamagePercent(boss, target, percentHealth, location, false, cause, true);
	}

	public static boolean bossDamagePercent(@Nullable LivingEntity boss, LivingEntity target, double percentHealth, @Nullable String cause, boolean knockback) {
		return bossDamagePercent(boss, target, percentHealth, null, false, cause, knockback);
	}

	public static boolean bossDamagePercent(@Nullable LivingEntity boss, LivingEntity target, double percentHealth, @Nullable Location location, boolean raw, @Nullable String cause) {
		return bossDamagePercent(boss, target, percentHealth, location, raw, cause, true);
	}

	/*
	 * Returns whether or not the player survived (true) or was killed (false)
	 */
	public static boolean bossDamagePercent(@Nullable LivingEntity boss, LivingEntity target, double percentHealth, @Nullable Location location, boolean raw, @Nullable String cause, boolean knockback) {
		if (percentHealth <= 0) {
			return true;
		}

		if (DamageUtils.isImmuneToDamage(target, null)) {
			return true;
		}

		double toTake = raw ? percentHealth : EntityUtils.getMaxHealth(target) * percentHealth;

		if (target instanceof Player player && bossDamageBlocked(player, location)) {
			/*
			 * One second of cooldown for every 2 points of damage
			 * Since this is % based, compute cooldown based on "Normal" health
			 */
			if (raw) {
				if (toTake > 1) {
					NmsUtils.getVersionAdapter().stunShield(player, (int) Math.ceil(toTake * 0.5));
				}
				target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				ItemUtils.damageShield(player, (int) Math.ceil(toTake / 2.5));
			} else {
				NmsUtils.getVersionAdapter().stunShield(player, (int) (20 * percentHealth * 20));
				target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				ItemUtils.damageShield(player, (int)(percentHealth * 20 / 2.5));
			}
		} else {
			double absorp = AbsorptionUtils.getAbsorption(target);
			double adjustedHealth = (target.getHealth() + absorp) - toTake;

			if (adjustedHealth <= 0) {
				// Kill the player, but allow totems to trigger
				target.setNoDamageTicks(0);
				DamageUtils.damage(boss, target, new DamageEvent.Metadata(DamageType.OTHER, null, null, cause), toTake, false, knockback, false);
				return false;
			} else {
				double originalDamage = toTake;
				if (absorp > 0) {
					if (absorp - toTake > 0) {
						AbsorptionUtils.setAbsorption(target, (float) (absorp - toTake), -1);
						toTake = 0;
					} else {
						AbsorptionUtils.setAbsorption(target, 0f, -1);
						toTake -= absorp;
					}
				}
				if (toTake > 0) {
					if (target.getHealth() - toTake > EntityUtils.getMaxHealth(target)) {
						target.setHealth(EntityUtils.getMaxHealth(target));
					} else {
						target.setHealth(Math.max(target.getHealth() - toTake, 1));
					}
				}

				// deal a small amount of damage for abilities to trigger and the hurt effect to play

				double lastDamage = target.getLastDamage();
				int noDamageTicks = target.getNoDamageTicks();
				target.setNoDamageTicks(0);

				DamageUtils.damage(boss, target, new DamageEvent.Metadata(DamageType.OTHER, null, null, cause), 0.001, false, knockback, false);

				if (noDamageTicks <= target.getMaximumNoDamageTicks() / 2f) {
					// had iframes: increase iframes by dealt damage, but keep length
					target.setNoDamageTicks(noDamageTicks);
					target.setLastDamage(lastDamage + originalDamage);
				} else {
					// had no iframes: add iframes for the damage dealt
					target.setNoDamageTicks(target.getMaximumNoDamageTicks());
					target.setLastDamage(originalDamage);
				}
			}

			if (target instanceof Player player && Shielding.doesShieldingApply(player, boss) && percentHealth > 0) {
				Shielding.disable(player);
			}
		}

		return true;
	}

	public static int getPlayersInRangeForHealthScaling(Entity entity, double radius) {
		return getPlayersInRangeForHealthScaling(entity.getLocation(), radius);
	}

	public static int getPlayersInRangeForHealthScaling(Location loc, double radius) {
		return PlayerUtils.playersInRange(loc, radius, true).size();
	}

	public static int getPlayersInRangeForHealthScaling(Location loc, double radius, double ceiling) {
		return (int) PlayerUtils.playersInRange(loc, radius, true).stream()
			.filter(p -> p.getLocation().getY() - loc.getY() <= ceiling)
			.count();
	}

	/**
	 * Adds modifiers to the existing map from the string
	 *
	 * @param s   a string like "[damage=10, cooldown=60, detectionrange=80, singletarget=true....]
	 * @param map the map where the values ​​will be added
	 */
	public static void addModifiersFromString(Map<String, String> map, String s) {
		if (!s.startsWith("[")) {
			return;
		}
		s = s.substring(1);

		if (s.endsWith("]")) {
			s = s.substring(0, s.length() - 1);
		}
		String[] toMap;
		int lastSplitIndex = 0;
		int squareBrackets = 0;
		int roundBrackets = 0;
		int quoteCount = 0;
		char charAtI;

		for (int i = 0; i < s.length(); i++) {
			charAtI = s.charAt(i);
			switch (charAtI) {
				case '[':
					squareBrackets++;
					break;
				case ']':
					squareBrackets--;
					break;
				case '(':
					roundBrackets++;
					break;
				case ')':
					roundBrackets--;
					break;
				case '"':
					quoteCount = (quoteCount + 1) % 2;
					break;
				default:
			}

			if (squareBrackets == 0 && roundBrackets == 0 && quoteCount == 0 && charAtI == ',') {
				toMap = s.substring(lastSplitIndex, i).split("=");
				if (toMap.length == 2) {
					map.put(toMap[0].replace(" ", "").toLowerCase(Locale.getDefault()), toMap[1]);
				} else {
					Plugin.getInstance().getLogger().warning("Fail to load: " + s.substring(lastSplitIndex, i) + ". Illegal declaration");
				}
				lastSplitIndex = i + 1;
			}
		}

		if (squareBrackets == 0 && roundBrackets == 0 && quoteCount == 0 && lastSplitIndex != s.length()) {
			toMap = s.substring(lastSplitIndex, s.length()).split("=");
			if (toMap.length == 2) {
				map.put(toMap[0].replace(" ", "").toLowerCase(Locale.getDefault()), toMap[1]);
			} else {
				Plugin.getInstance().getLogger().warning("Fail to load: [" + String.join(",", toMap) + "]. Illegal declaration");
			}
		} else {
			Plugin.getInstance().getLogger().warning("Fail too many brackets/quote inside: " + s);
		}
	}

	public static String translateFieldNameToTag(String fieldName) {
		return fieldName.toLowerCase(Locale.getDefault()).replaceAll("[^a-z0-9]", "");
	}

	public static boolean checkParametersStringProperty(String tag) throws Exception {
		int roundBrackets = 0;
		int squareBrackets = 0;
		boolean doubleQuote = false;

		for (int i = 0; i < tag.length(); i++) {
			char c = tag.charAt(i);
			switch (c) {
				case '[' -> {
					squareBrackets = doubleQuote ? squareBrackets : squareBrackets + 1;
				}
				case ']' -> {
					squareBrackets = doubleQuote ? squareBrackets : squareBrackets - 1;
				}
				case '(' -> {
					roundBrackets = doubleQuote ? roundBrackets : roundBrackets + 1;
				}
				case ')' -> {
					roundBrackets = doubleQuote ? roundBrackets : roundBrackets - 1;
				}
				case '"' -> {
					doubleQuote = !doubleQuote;
				}
				default -> {
				}
			}
		}

		if (roundBrackets != 0) {
			throw new Exception("too many round brackets () " + roundBrackets);
		}

		if (squareBrackets != 0) {
			throw new Exception("too many square brackets [] " + squareBrackets);
		}

		if (doubleQuote) {
			throw new Exception("too many double_Quote \" ");
		}

		return true;

	}

	@SuppressWarnings("unchecked")
	public static @Nullable <T extends BossAbilityGroup> T getBossOfClass(Entity entity, Class<T> cls) {
		BossManager bossManager = BossManager.getInstance();
		if (bossManager != null) {
			List<BossAbilityGroup> abilities = BossManager.getInstance().getAbilities(entity);
			for (BossAbilityGroup ability : abilities) {
				if (ability.getClass() == cls) {
					return (T) ability;
				}
			}
		}
		return null;
	}

	public static int getBlueTimeOfDay(Entity entity) {
		int timeOfDay = 0;
		if (ScoreboardUtils.getScoreboardValue("$IsDungeon", "const").orElse(0) == 1) {
			long time = entity.getWorld().getTime();
			timeOfDay = (int) Math.floor(time / 6000.0);

			// Pretty sure Time ranges from 0 to 23999, but just in case...
			if (timeOfDay > 3) {
				timeOfDay = 3;
			}
		}
		return timeOfDay;
	}

	public static void hideBossBar(BossBar bar, World world) {
		for (Player player : world.getPlayers()) {
			player.hideBossBar(bar);
		}
	}

	public static void endBossFightEffects(List<Player> players) {
		endBossFightEffects(null, players, 10 * 20, false, false);
	}

	public static void endBossFightEffects(@Nullable LivingEntity boss, List<Player> players) {
		endBossFightEffects(boss, players, 10 * 20, false, false);
	}

	public static void endBossFightEffects(@Nullable LivingEntity boss, List<Player> players, int winEffectDuration) {
		endBossFightEffects(boss, players, winEffectDuration, false, false);
	}

	/**
	 * Handles generic end of boss fight tasks for stateful bosses.
	 * This should only be called by the overridden death method for each boss that uses it.
	 * @param boss Boss to apply effects to. Defaults to null
	 * @param players List of Players to apply effects to
	 * @param winEffectDuration Duration of win effects to apply to players in ticks. Defaults to 10 seconds
	 * @param keepBossAlive If true, make boss invulnerable, remove its AI, remove gravity, etc. Defaults to false
	 * @param removeGlowing If true, remove glowing effect from boss. Defaults to false
	 */
	public static void endBossFightEffects(@Nullable LivingEntity boss, List<Player> players, int winEffectDuration, boolean keepBossAlive, boolean removeGlowing) {
		final String BOSS_WIN_RESISTANCE = "BossWinResistance";
		final String BOSS_WIN_REGENERATION = "BossWinRegeneration";
		final int REGENERATION_INTERVAL = 12;

		if (boss != null) {
			if (keepBossAlive) {
				boss.setHealth(1);
				boss.setInvulnerable(true);
				boss.setAI(false);
				boss.setGravity(false);
				boss.setPersistent(true);
				boss.setSilent(true);
				Plugin.getInstance().mEffectManager.clearEffects(boss, PercentDamageReceived.GENERIC_NAME);
				Plugin.getInstance().mEffectManager.addEffect(boss, PercentDamageReceived.GENERIC_NAME,
					new PercentDamageReceived(20 * 60, -1.0));
			}

			if (removeGlowing) {
				boss.removePotionEffect(PotionEffectType.GLOWING);
				boss.setGlowing(false);
			}
		}

		for (Player player : players) {
			Plugin.getInstance().mEffectManager.addEffect(player, BOSS_WIN_RESISTANCE,
				new PercentDamageReceived(winEffectDuration, -1.0));
			Plugin.getInstance().mEffectManager.addEffect(player, BOSS_WIN_REGENERATION,
				new CustomRegeneration(winEffectDuration, 1, REGENERATION_INTERVAL, null, Plugin.getInstance()));
			SongManager.stopSong(player, true);
		}
	}
}
