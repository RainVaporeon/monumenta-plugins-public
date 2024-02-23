package com.playmonumenta.plugins.adapters;

import com.google.gson.JsonObject;
import java.util.Set;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public interface VersionAdapter {

	void removeAllMetadata(Plugin plugin);

	void resetPlayerIdleTimer(Player player);

	void customDamageEntity(@Nullable LivingEntity damager, LivingEntity damagee, double amount, boolean blockable, @Nullable String killedUsingMsg);

	<T extends Entity> T duplicateEntity(T entity);

	/**
	 * Gets an entity by its {@link Entity#getEntityId() id} (i.e. not by its {@link Entity#getUniqueId() UUID}).
	 */
	@Nullable Entity getEntityById(World world, int entityId);

	/**
	 * Gets the actual direction of an entity instead of the direction of its head.
	 * This is particularly useful for players as this gives the direction a player is actually looking
	 * instead of one slightly in the past as the head is lagging behind the actual direction.
	 */
	Vector getActualDirection(Entity entity);

	int getAttackCooldown(LivingEntity entity);

	void setAttackCooldown(LivingEntity entity, int newCooldown);

	/**
	 * Forces the given living entity to stop using its active item, e.g. lowers a raised shield or charges a crossbow (if it has been changing for long enough).
	 *
	 * @param clearActiveItem If false, will keep the item in use. Useful only for crossbows to not cause them to shoot immediately after this method is called.
	 */
	void releaseActiveItem(LivingEntity entity, boolean clearActiveItem);

	void stunShield(Player player, int ticks);

	void cancelStrafe(Mob mob);

	/**
	 * Spawns an entity that will not be present in the world
	 *
	 * @param type  Entity type to spawn - not all types may work!
	 * @param world Any world (required for the constructor, and used for activation range and possibly some more things)
	 * @return Newly spawned entity
	 * @throws IllegalArgumentException if the provided entity type cannot be spawned
	 */
	Entity spawnWorldlessEntity(EntityType type, World world);

	/**
	 * Prevents the given parrot from moving onto a player's shoulders.
	 * This is not persistent and needs to be re-applied whenever the parrot is loaded again.
	 */
	void disablePerching(Parrot parrot);

	/**
	 * Make entity agro players
	 *
	 * @param entity       The entity who should agro players
	 * @param action       Damage action when this entity hit a player
	 */
	void setAggressive(Creature entity, DamageAction action);

	/**
	 * Make entity agro players but now with range
	 *
	 * @param entity       The entity who should agro players
	 * @param action       Damage action when this entity hit a player
	 * @param attackRange  The range of the attack
	 */
	void setAggressive(Creature entity, DamageAction action, double attackRange);

	/**
	 * Make this entity lose all desire to attack any Entity and make this only attack entities accepted by the predicate
	 *
	 * @param entity        The entity
	 * @param action        The damage action that will cat when this entity hit someone
	 * @param predicate     Predicate used for check which entity attack and which not
	 * @param attackRange   Attack range of this entity
	 */
	void setFriendly(Creature entity, DamageAction action, Predicate<LivingEntity> predicate, double attackRange);

	void setHuntingCompanion(Creature entity, DamageAction action, double attackRange);

	interface DamageAction {
		void damage(LivingEntity entity);
	}

	/**
	 * Changes the melee attack range of the given entity.
	 * <b>Note that this overrides any custom melee attack</b>, so care should be taken to only apply it to mobs with a basic melee attack.
	 * This needs to be verified by looking at the pathfinder goals of the entity.
	 * This is not persistent and needs to be re-applied whenever the entity is loaded again.
	 *
	 * @param entity      The entity whose attack range should be changed
	 * @param attackRange Attack range of the entity (calculated from feet to feet)
	 */
	void setAttackRange(Creature entity, double attackRange);

	/**
	 * Returns the NMS class representing a ResourceKey (for use in packet handlers). A resource key is a pair of identifiers,
	 * with the first one representing the type of resource, and the second one identifying the resource,
	 * e.g. the resource key "minecraft:dimension_type / minecraft:overworld" represents the vanilla overworld dimension type.
	 */
	Class<?> getResourceKeyClass();

	/**
	 * Creates a ResourceKey of type "minecraft:dimension_type" with the provided namespace and key as identifier.
	 */
	Object createDimensionTypeResourceKey(String namespace, String key);

	@Nullable World getWorldByResourceKey(Object currentWorldKey);

	/**
	 * Runs a command as the console, without logging output to server logs.
	 */
	void runConsoleCommandSilently(String command);

	/**
	 * Checks if the given bounding box collides with any blocks or "hard-colliding" entities (e.g. boats, shulkers).
	 */
	boolean hasCollision(World world, BoundingBox aabb);

	/**
	 * Checks if the given bounding box collides with any blocks.
	 */
	boolean hasCollisionWithBlocks(World world, BoundingBox aabb, boolean loadChunks);

	/**
	 * Checks if the given bounding box collides with any blocks that match the given predicate.
	 */
	boolean hasCollisionWithBlocks(World world, BoundingBox aabb, boolean loadChunks, Predicate<Material> checkedTypes);

	/**
	 * Gets all blocks colliding with the given bounding box.
	 */
	Set<Block> getCollidingBlocks(World world, BoundingBox aabb, boolean loadChunks);

	/**
	 * Performs all desired AI changes for the given newly spawned or loaded mob.
	 * Note that this is called inside an {@link com.destroystokyo.paper.event.entity.EntityAddToWorldEvent}, so must not use the world or other entities.
	 * If any extra data is needed, delay the code by 1 tick.
	 */
	void mobAIChanges(Mob entity);

	Object toVanillaChatComponent(Component component);

	/**
	 * @return Whether the given item stacks refer to the same vanilla stack. Can e.g. be used to test if an item is still in an inventory, and not just an exact copy.
	 */
	boolean isSameItem(@Nullable ItemStack item1, @Nullable ItemStack item2);

	void forceDismountVehicle(Entity entity);

	/**
	 * Gets the projectile that will be used if the player tries to shoot with the given projectile weapon.
	 */
	ItemStack getUsedProjectile(Player player, ItemStack weapon);

	/**
	 * Gets the display name of an item, without any additional styling, square brackets, or hover text.
	 */
	Component getDisplayName(ItemStack item);

	/**
	 * Moves an entity as if it moved on its own. Checks collision, updates fall distance, does fall damage, etc.
	 */
	void moveEntity(Entity entity, Vector movement);

	/**
	 * Moves an entity to another location in the same world. This directly updates the entity's position and does not call a teleport event.
	 * Unlike {@link Entity#teleport(Location)}, this can be used to move an entity that has passengers.
	 */
	void setEntityLocation(Entity entity, Vector target, float yaw, float pitch);

	JsonObject getScoreHolderScoresAsJson(String scoreHolder, Scoreboard scoreboard);

	void resetScoreHolderScores(String scoreHolder, Scoreboard scoreboard);

	void disableRangedAttackGoal(LivingEntity entity);

}
