package com.playmonumenta.plugins.utils;

import com.destroystokyo.paper.MaterialSetTag;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.classes.Rogue;
import com.playmonumenta.plugins.classes.Scout;
import com.playmonumenta.plugins.classes.Warlock;
import com.playmonumenta.plugins.classes.Warrior;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.player.activity.ActivityManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.structures.StructuresPlugin;
import com.playmonumenta.structures.managers.RespawningStructure;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.OptionalInt;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


public class PlayerUtils {
	public static void callAbilityCastEvent(Player player, ClassAbility spell) {
		AbilityCastEvent event = new AbilityCastEvent(player, spell);
		Bukkit.getPluginManager().callEvent(event);
	}

	public static void awardStrike(Plugin plugin, Player player, String reason) {
		int strikes = ScoreboardUtils.getScoreboardValue(player, "Strikes").orElse(0);
		strikes++;
		ScoreboardUtils.setScoreboardValue(player, "Strikes", strikes);

		Location loc = player.getLocation();
		String oobLoc = "[" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]";

		player.sendMessage(Component.text("WARNING: " + reason, NamedTextColor.RED));
		player.sendMessage(Component.text("Location: " + oobLoc, NamedTextColor.RED));
		player.sendMessage(Component.text("This is an automated message generated by breaking a game rule.", NamedTextColor.YELLOW));
		player.sendMessage(Component.text("You have been teleported to spawn and given slowness for 5 minutes.", NamedTextColor.YELLOW));
		player.sendMessage(Component.text("There is no further punishment, but please do follow the rules.", NamedTextColor.YELLOW));

		plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION,
			new PotionEffect(PotionEffectType.SLOW, 5 * 20 * 60, 3, false, true));

		player.teleport(player.getWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
	}

	public static boolean playerCountsForLootScaling(Player player) {
		return player.getGameMode() != GameMode.SPECTATOR
			       && (player.getGameMode() != GameMode.CREATIVE || !Plugin.IS_PLAY_SERVER)
			       && ActivityManager.getManager().isActive(player)
			       && !Shattered.hasMaxShatteredItemEquipped(player);
	}

	public static List<Player> playersInLootScalingRange(Location loc) {
		// In dungeons, all players in the same world (i.e. the entire dungeon) are in range
		boolean isDungeon = ScoreboardUtils.getScoreboardValue("$IsDungeon", "const").orElse(0) > 0;
		if (isDungeon) {
			return loc.getWorld().getPlayers().stream()
				       .filter(PlayerUtils::playerCountsForLootScaling)
				       .toList();
		}

		// In a POI, all players within the same POI are in range
		List<RespawningStructure> structures = StructuresPlugin.getInstance().mRespawnManager.getStructures(loc.toVector(), false)
			                                       .stream().filter(structure -> structure.isWithin(loc)).toList();
		if (!structures.isEmpty()) {
			return loc.getWorld().getPlayers().stream()
				       .filter(p -> playerCountsForLootScaling(p) && playerIsInPOI(structures, p))
				       .toList();
		}

		// Otherwise, perform no loot scaling
		return Collections.emptyList();
	}

	public static List<Player> playersInLootScalingRange(Player player, boolean excludeTarget) {
		List<Player> players = new ArrayList<>(playersInLootScalingRange(player.getLocation()));
		if (excludeTarget) {
			players.remove(player);
		}
		return players;
	}

	public static boolean playerIsInPOI(List<RespawningStructure> structures, Player player) {
		return structures.stream().anyMatch(structure -> structure.isWithin(player));
	}

	public static boolean playerIsInPOI(Location loc, Player player) {
		return playerIsInPOI(StructuresPlugin.getInstance().mRespawnManager.getStructures(loc.toVector(), true), player);
	}

	public static boolean playerIsInPOI(Player player) {
		return playerIsInPOI(player.getLocation(), player);
	}

	public static List<Player> playersInRange(Location loc, double range, boolean includeNonTargetable, boolean includeDead) {
		List<Player> players = new ArrayList<>();

		double rangeSquared = range * range;
		for (Player player : loc.getWorld().getPlayers()) {
			if (player.getLocation().distanceSquared(loc) < rangeSquared
				    && player.getGameMode() != GameMode.SPECTATOR
				    && (includeNonTargetable || !AbilityUtils.isStealthed(player))
				    && (includeDead || !Plugin.getInstance().mEffectManager.hasEffect(player, RespawnStasis.class))) {
				players.add(player);
			}
		}

		return players;
	}

	public static List<Player> playersInRange(Location loc, double range, boolean includeNonTargetable) {
		return playersInRange(loc, range, includeNonTargetable, false);
	}

	public static List<Player> otherPlayersInRange(Player player, double radius, boolean includeNonTargetable) {
		List<Player> players = playersInRange(player.getLocation(), radius, includeNonTargetable);
		players.remove(player);
		return players;
	}

	public static boolean isCursed(Plugin plugin, Player p) {
		return plugin.mEffectManager.hasEffect(p, "CurseEffect");
	}

	public static void removeCursed(Plugin plugin, Player p) {
		setCursedTicks(plugin, p, 0);
		p.removePotionEffect(PotionEffectType.BAD_OMEN);
		p.removePotionEffect(PotionEffectType.UNLUCK);
	}

	public static void setCursedTicks(Plugin plugin, Player p, int ticks) {
		NavigableSet<Effect> cursed = plugin.mEffectManager.getEffects(p, "CurseEffect");
		if (cursed != null) {
			for (Effect curse : cursed) {
				curse.setDuration(ticks);
			}
		}
	}

	public static double healPlayer(Plugin plugin, Player player, double healAmount) {
		return healPlayer(plugin, player, healAmount, null);
	}

	// Returns the change in player's health
	public static double healPlayer(Plugin plugin, Player player, double healAmount, @Nullable Player sourcePlayer) {
		if (healAmount <= 0 || player.isDead()) {
			return 0;
		}

		if ((sourcePlayer != null) && (player != sourcePlayer)) {
			double healBonus = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TRIAGE) * 0.05;
			healAmount *= 1 + healBonus;
		}

		EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, healAmount, EntityRegainHealthEvent.RegainReason.CUSTOM);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			double oldHealth = player.getHealth();
			double newHealth = Math.min(oldHealth + event.getAmount(), EntityUtils.getMaxHealth(player));
			player.setHealth(newHealth);

			// Add to activity
			if (sourcePlayer != null && player != sourcePlayer && ActivityManager.getManager().isActive(player)) {
				ActivityManager.getManager().addHealingDealt(sourcePlayer, healAmount);
			}

			return newHealth - oldHealth;
		}

		return 0;
	}

	public static Location getRightSide(Location location, double distance) {
		double angle = location.getYaw() / 57.296;
		return location.clone().subtract(new Vector(FastUtils.cos(angle), 0, FastUtils.sin(angle)).normalize().multiply(distance));
	}

	/* Audience of nearby players for sending messages */
	public static Audience nearbyPlayersAudience(Location loc, int radius) {
		return Audience.audience(loc.getNearbyPlayers(radius));
	}

	public static Audience nearbyOtherPlayersAudience(Player player, int radius) {
		return Audience.audience(otherPlayersInRange(player, radius, true));
	}

	/* Command should use @s for targeting selector */
	public static void executeCommandOnNearbyPlayers(Location loc, int radius, String command) {
		for (Player player : loc.getNearbyPlayers(radius)) {
			// getNearbyPlayers returns players in a cube, not a sphere, so we need this additional check
			if (loc.distanceSquared(player.getLocation()) > radius * radius) {
				continue;
			}
			executeCommandOnPlayer(player, command);
		}
	}

	public static void executeCommandOnPlayer(Player player, String command) {
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
			"execute as " + player.getUniqueId() + " at @s run " + command);
	}

	// How far back the player drew their bow,
	// vs what its max launch speed would be.
	// Launch velocity used to calculate is specifically for PLAYERS shooting BOWS!
	// Returns from 0.0 to 1.0, with 1.0 being full draw
	public static double calculateBowDraw(AbstractArrow arrowlike) {
		double currentSpeed = arrowlike.getVelocity().length();
		double maxLaunchSpeed = Constants.PLAYER_BOW_INITIAL_SPEED;

		return Math.min(
			1,
			currentSpeed / maxLaunchSpeed
		);
	}

	/*
	 * Whether the player meets the conditions for a crit,
	 * emulating the vanilla check in full.
	 *
	 * Ie, no critting while sprinting.
	 */
	public static boolean isCriticalAttack(Player player) {
		// NMS EntityHuman:
		// float f = (float)this.b((AttributeBase)GenericAttributes.ATTACK_DAMAGE);
		//     f1 = EnchantmentManager.a(this.getItemInMainHand(), ((EntityLiving)entity).getMonsterType());
		// float f2 = this.getAttackCooldown(0.5F);
		// f *= 0.2F + f2 * f2 * 0.8F;
		// f1 *= f2;
		// if (f > 0.0F || f1 > 0.0F) {
		//     boolean flag = f2 > 0.9F;
		//     boolean flag2 = flag && this.fallDistance > 0.0F && !this.onGround && !this.isClimbing() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && entity instanceof EntityLiving;
		//     flag2 = flag2 && !this.isSprinting();
		return (
			isFallingAttack(player)
				&& !player.isInWater()
				&& !player.isSprinting()
		);
	}

	/*
	 * Whether the player meets the conditions for a crit,
	 * emulating the vanilla check,
	 * except the not in water and not sprinting requirements.
	 *
	 * This is used because MM has historically had a non-exact crit check,
	 * that allowed things like crit-triggered abilities to trigger off non-crit
	 * melee damage while sprinting.
	 */
	public static boolean isFallingAttack(Player player) {
		return (
			player.getCooledAttackStrength(0.5f) > 0.9
				&& player.getFallDistance() > 0
				&& isFreeFalling(player)
				&& !player.hasPotionEffect(PotionEffectType.BLINDNESS)
				&& !player.isInsideVehicle()
			//TODO pass in the Entity in question to check if LivingEntity
		);
	}

	/*
	 * Whether the player is considered to be freely falling in air or liquid.
	 * They cannot be on the ground or climbing.
	 */
	public static boolean isFreeFalling(Player player) {
		if (!isOnGround(player) && player.getLocation().isChunkLoaded()) {
			Material playerFeetMaterial = player.getLocation().getBlock().getType();
			// Accounts for vines, ladders, nether vines, scaffolding etc
			return !MaterialSetTag.CLIMBABLE.isTagged(playerFeetMaterial);
		}

		return false;
	}

	public static boolean hasLineOfSight(Player player, Block block) {
		Location fromLocation = player.getEyeLocation();
		Location toLocation = block.getLocation();
		int range = (int)fromLocation.distance(toLocation) + 1;
		Vector direction = toLocation.toVector().subtract(fromLocation.toVector()).normalize();

		try {
			BlockIterator bi = new BlockIterator(fromLocation.getWorld(), fromLocation.toVector(), direction, 0, range);

			while (bi.hasNext()) {
				Block b = bi.next();

				// If block is occluding (shouldn't include transparent blocks, liquids etc.),
				// line of sight is broken, return false
				if (BlockUtils.isLosBlockingBlock(b.getType()) && b != block) {
					return false;
				}
			}
		} catch (IllegalStateException e) {
			// Thrown sometimes when chunks aren't loaded at exactly the right time
			return false;
		}

		return true;
	}

	/*
	 * Whether the player meets the conditions for a sweeping attack,
	 * emulating the vanilla check, except the on ground,
	 * movement increment limit, sword, and proximity requirements.
	 */
	public static boolean isNonFallingAttack(
		Player player,
		Entity enemy
	) {
		return (
			player.getCooledAttackStrength(0.5f) > 0.9
				&& !isCriticalAttack(player)
				&& !player.isSprinting()
			// Last check on horizontal movement increment requires an internal
			// vanilla collision adjustment Vec3D.
			// It is not simply player.getVelocity(); that is used elsewhere
		);
	}

	/*
	 * Whether the player meets the conditions for a sweeping attack,
	 * emulating the vanilla check, except the movement increment limit, sword,
	 * and proximity requirements.
	 */
	public static boolean isSweepingAttack(
		Player player,
		Entity enemy
	) {
		// NMS Entity:
		// this.z = this.A;
		// this.A = (float)((double)this.A + (double)MathHelper.sqrt(c(vec3d1)) * 0.6D);
		// public static double c(Vec3D vec3d) {
		//     return vec3d.x * vec3d.x + vec3d.z * vec3d.z;
		// }
		//
		// NMS EntityHuman:
		// if (this.isSprinting() && flag) {
		//     flag1 = true;
		// }
		// double d0 = (double)(this.A - this.z);
		// if (flag && !flag2 && !flag1 && this.onGround && d0 < (double)this.dN()) {
		//     ItemStack itemStack = this.b((EnumHand)EnumHand.MAIN_HAND);
		//     if (itemStack.getItem() instanceof ItemSword) {
		//     List<EntityLiving> list = this.world.a(EntityLiving.class, entity.getBoundingBox().grow(1.0D, 0.25D, 1.0D));
		return (
			isNonFallingAttack(player, enemy)
				&& isOnGround(player)
		);
	}

	public static boolean checkPlayer(Player player) {
		return player.isValid() && player.getGameMode() != GameMode.SPECTATOR;
	}

	/*
	 * Returns players within a bounding box of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInBox(
		Location boxCenter,
		double totalWidth,
		double totalHeight
	) {
		return boxCenter.getNearbyPlayers(
			totalWidth / 2,
			totalHeight / 2,
			PlayerUtils::checkPlayer
		);
	}

	/*
	 * Returns players within a cube of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInCube(
		Location cubeCenter,
		double sideLength
	) {
		return playersInBox(cubeCenter, sideLength, sideLength);
	}

	/*
	 * Returns players within a sphere of the specified dimensions.
	 *
	 * Measures based on feet location.
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInSphere(
		Location sphereCenter,
		double radius
	) {
		Collection<Player> spherePlayers = playersInCube(sphereCenter, radius * 2);
		double radiusSquared = radius * radius;
		spherePlayers.removeIf((Player player) -> sphereCenter.distanceSquared(player.getLocation()) > radiusSquared);

		return spherePlayers;
	}

	/*
	 * Returns players within an upright cylinder of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static Collection<Player> playersInCylinder(
		Location cylinderCenter,
		double radius,
		double totalHeight
	) {
		Collection<Player> cylinderPlayers = playersInBox(cylinderCenter, radius * 2, totalHeight);
		double centerY = cylinderCenter.getY();
		cylinderPlayers.removeIf((Player player) -> {
			Location flattenedLocation = player.getLocation();
			flattenedLocation.setY(centerY);
			return cylinderCenter.distanceSquared(flattenedLocation) > radius * radius;
		});

		return cylinderPlayers;
	}

	public static boolean isMage(Player player) {
		OptionalInt opt = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME);
		return opt.orElse(0) == Mage.CLASS_ID;
	}

	public static boolean isWarrior(Player player) {
		OptionalInt opt = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME);
		return opt.orElse(0) == Warrior.CLASS_ID;
	}

	public static boolean isCleric(Player player) {
		OptionalInt opt = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME);
		return opt.orElse(0) == Cleric.CLASS_ID;
	}

	public static boolean isRogue(Player player) {
		OptionalInt opt = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME);
		return opt.orElse(0) == Rogue.CLASS_ID;
	}

	public static boolean isAlchemist(Player player) {
		OptionalInt opt = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME);
		return opt.orElse(0) == Alchemist.CLASS_ID;
	}

	public static boolean isScout(Player player) {
		OptionalInt opt = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME);
		return opt.orElse(0) == Scout.CLASS_ID;
	}

	public static boolean isWarlock(Player player) {
		OptionalInt opt = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME);
		return opt.orElse(0) == Warlock.CLASS_ID;
	}

	public static void resetAttackCooldown(Player player) {
		NmsUtils.getVersionAdapter().setAttackCooldown(player, 0);
	}

	public static double getJumpHeight(Player player) {
		PotionEffect jump = player.getPotionEffect(PotionEffectType.JUMP);
		double jumpLevel = (jump == null ? -1 : jump.getAmplifier());
		return jumpLevel < 0 ? 1.2523 : 0.0308354 * jumpLevel * jumpLevel + 0.744631 * jumpLevel + 1.836131; // Quadratic function taken from mc wiki - thanks mojank!
	}

	/**
	 * Just to quarantine the deprecation warnings tbh
	 */
	public static boolean isOnGround(Player player) {
		return player.isOnGround();
	}
}
