package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.adapters.VersionAdapter;
import com.playmonumenta.plugins.player.activity.ActivityManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.Lootable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class WorldListener implements Listener {
	Plugin mPlugin;
	public static final int SPAWNER_BREAK_CHEST_CHECK_RADIUS = 16;
	public static final NamespacedKey ENTITY_SCORES_DATA_KEY = NamespacedKeyUtils.fromString("monumenta:entity_scores");
	public int mEntityScoreLastDeleteTick = Integer.MIN_VALUE;
	public Set<String> mEntityScoresDeletedThisTick = new HashSet<>();

	public WorldListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		Entity entity = event.getEntity();

		MessagingUtils.updatePlainName(entity);

		Bukkit.getScheduler().runTask(mPlugin, () -> mPlugin.mTrackingManager.addEntity(entity));
	}

	// Convenience list of offsets to get adjacent blocks
	private static final List<Vector> ADJACENT_OFFSETS = Arrays.asList(
	                                                         new Vector(0, 0, 1),
	                                                         new Vector(0, 0, -1),
	                                                         new Vector(0, 1, 0),
	                                                         new Vector(0, 1, 1),
	                                                         new Vector(0, 1, -1),
	                                                         new Vector(0, -1, 0),
	                                                         new Vector(0, -1, 1),
	                                                         new Vector(0, -1, -1),
	                                                         new Vector(1, 0, 0),
	                                                         new Vector(1, 0, 1),
	                                                         new Vector(1, 0, -1),
	                                                         new Vector(1, 1, 0),
	                                                         new Vector(1, 1, 1),
	                                                         new Vector(1, 1, -1),
	                                                         new Vector(1, -1, 0),
	                                                         new Vector(1, -1, 1),
	                                                         new Vector(1, -1, -1),
	                                                         new Vector(-1, 0, 0),
	                                                         new Vector(-1, 0, 1),
	                                                         new Vector(-1, 0, -1),
	                                                         new Vector(-1, 1, 0),
	                                                         new Vector(-1, 1, 1),
	                                                         new Vector(-1, 1, -1),
	                                                         new Vector(-1, -1, 0),
	                                                         new Vector(-1, -1, 1),
	                                                         new Vector(-1, -1, -1)
	                                                     );

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntitiesLoadEvent(EntitiesLoadEvent event) {
		if (Plugin.IS_PLAY_SERVER && !ServerProperties.getEntityScoresDisabled(event.getWorld())) {
			loadEntityScores(event.getEntities());
		}

		for (Entity entity : event.getEntities()) {
			if (entity.getScoreboardTags().contains(Constants.Tags.REMOVE_ON_UNLOAD)) {
				Bukkit.getScheduler().runTask(mPlugin, entity::remove);
				continue;
			}
			MessagingUtils.updatePlainName(entity);
		}
	}

	public void loadEntityScores(Collection<Entity> entities) {
		Gson gson = new Gson();
		for (Entity entity : entities) {
			if (entity instanceof Player) {
				continue;
			}

			String objectStr = entity.getPersistentDataContainer().getOrDefault(ENTITY_SCORES_DATA_KEY, PersistentDataType.STRING, "{}");
			JsonObject object;
			try {
				object = gson.fromJson(objectStr, JsonObject.class);
			} catch (Exception e) {
				MMLog.warning("Could not load scores from entity " + entity.getUniqueId() + " (" + entity.getLocation() + "): " + objectStr);
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);
				continue;
			}

			for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
				String objective = entry.getKey();
				int value;
				try {
					value = entry.getValue().getAsInt();
				} catch (Exception e) {
					MMLog.warning("Could not load score from entity " + entity.getUniqueId()
						+ " (" + entity.getLocation() + ") for objective " + objective + ": " + entry.getValue());
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);
					continue;
				}
				ScoreboardUtils.setScoreboardValue(entity, objective, value);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onWorldSave(WorldSaveEvent event) {
		if (!Plugin.IS_PLAY_SERVER || ServerProperties.getEntityScoresDisabled(event.getWorld())) {
			return;
		}
		Collection<Entity> entities = event.getWorld().getEntities();
		for (Entity entity : entities) {
			saveEntityScores(entity, false, "world save");
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntitiesUnloadEvent(EntitiesUnloadEvent event) {
		if (!Plugin.IS_PLAY_SERVER || ServerProperties.getEntityScoresDisabled(event.getWorld())) {
			return;
		}
		Collection<Entity> entities = event.getEntities();
		for (Entity entity : entities) {
			saveEntityScores(entity, true, "entities unload");
		}
	}

	// Doesn't trigger EntitiesUnloadEvent for remaining chunks
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onWorldUnload(WorldUnloadEvent event) {
		if (!Plugin.IS_PLAY_SERVER || ServerProperties.getEntityScoresDisabled(event.getWorld())) {
			return;
		}
		Collection<Entity> entities = event.getWorld().getEntities();
		for (Entity entity : entities) {
			saveEntityScores(entity, true, "world unload");
		}
	}

	private void saveEntityScores(Entity entity, boolean deleteFromScoreboard, String reason) {
		if (entity instanceof Player) {
			return;
		}

		Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
		String scoreHolder = entity.getUniqueId().toString();
		VersionAdapter versionAdapter = NmsUtils.getVersionAdapter();
		JsonObject entityScores = versionAdapter.getScoreHolderScoresAsJson(scoreHolder, scoreboard);

		if (deleteFromScoreboard) {
			int thisTick = Bukkit.getCurrentTick();
			if (thisTick != mEntityScoreLastDeleteTick) {
				mEntityScoresDeletedThisTick.clear();
				mEntityScoreLastDeleteTick = thisTick;
			}

			if (!mEntityScoresDeletedThisTick.contains(scoreHolder)) {
				versionAdapter.resetScoreHolderScores(scoreHolder, scoreboard);
				mEntityScoresDeletedThisTick.add(scoreHolder);
				MMLog.finer(() -> "[Entity Scores] "
					+ Bukkit.getCurrentTick()
					+ ": "
					+ scoreHolder
					+ ": Deleted scores for this entity: "
					+ entityScores.size()
					+ ", "
					+ reason);
			} else {
				MMLog.finer(() -> "[Entity Scores] "
					+ Bukkit.getCurrentTick()
					+ ": "
					+ scoreHolder
					+ ": Already deleted scores for this entity: "
					+ entityScores.size()
					+ ", "
					+ reason);
				return;
			}
		} else if (mEntityScoresDeletedThisTick.contains(scoreHolder)) {
			MMLog.finer(() -> "[Entity Scores] "
				+ Bukkit.getCurrentTick()
				+ ": "
				+ scoreHolder
				+ ": Not saving entity scores as they were deleted this tick: "
				+ entityScores.size()
				+ ", "
				+ reason);
			return;
		}

		if (entityScores.size() > 0) {
			entity.getPersistentDataContainer().set(ENTITY_SCORES_DATA_KEY, PersistentDataType.STRING, entityScores.toString());
		} else {
			entity.getPersistentDataContainer().remove(ENTITY_SCORES_DATA_KEY);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void structureGrowEvent(StructureGrowEvent event) {
		List<BlockState> blockStates = event.getBlocks();

		ListIterator<BlockState> iter = blockStates.listIterator();
		while (iter.hasNext()) {
			BlockState bs = iter.next();

			if (ItemUtils.isAllowedTreeReplace(bs.getBlock().getType())) {
				// Mark every potentially allowed block with metadata
				bs.getBlock().setMetadata(Constants.TREE_METAKEY, new FixedMetadataValue(mPlugin, true));
			} else {
				// Remove blocks that replace other materials than the allowed ones
				iter.remove();
			}
		}

		/*
		 * Create a new FIFO queue which will be used to walk the list
		 * of all elements that grew as a result of this event
		 * (those that have the above metadata)
		 *
		 * Locations that still have the metadata are added to the back of the queue
		 * as they are encountered, and items are pulled off the queue to process them.
		 *
		 * When a block is processed, each neighboring block is checked for the
		 * metadata. If it has it, it is added to the back of the queue.
		 *
		 * Once a block is done processing, it's metadata is removed.
		 *
		 * This performs effectively a breadth-first search which removes the metadata
		 * from all reachable blocks. Any remaining blocks after this operation
		 * completes should be removed as they are not reachable from the structure's
		 * origin
		 */
		Queue<Location> locList = new ArrayDeque<>();
		// Start at the first block of the structure/tree (the sapling)
		locList.add(event.getLocation());

		// Starting block is reachable - prevent it from being re-checked
		event.getLocation().getBlock().removeMetadata(Constants.TREE_METAKEY, mPlugin);

		while (!locList.isEmpty()) {
			Location loc = locList.remove();

			// Add adjacent blocks with metadata to the back of the queue
			for (Vector vec : ADJACENT_OFFSETS) {
				Location tmpLoc = loc.clone().add(vec);
				Block blk = tmpLoc.getBlock();
				if (blk.hasMetadata(Constants.TREE_METAKEY)) {
					locList.add(tmpLoc);

					// This block is reachable - prevent it from being checked again
					blk.removeMetadata(Constants.TREE_METAKEY, mPlugin);
				}
			}
		}

		/*
		 * Iterate over the list of blocks one last time and remove any that
		 * still have the metadata (they are not attached to a path that
		 * reaches the sapling where the tree grew)
		 */
		iter = blockStates.listIterator();
		while (iter.hasNext()) {
			BlockState bs = iter.next();
			Block blk = bs.getBlock();

			if (blk.hasMetadata(Constants.TREE_METAKEY)) {
				blk.removeMetadata(Constants.TREE_METAKEY, mPlugin);
				iter.remove();

				// Debug to visualize the removed blocks
				// blk.setType(Material.REDSTONE_BLOCK);
			}
		}
	}

	// Block Dispense Event
	// Cancel dispensers/droppers dropping specific items
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		Block block = event.getBlock();
		ItemStack dispensed = event.getItem();
		if (!mPlugin.mItemOverrides.blockDispenseInteraction(mPlugin, block, dispensed)) {
			event.setCancelled(true);
		}
	}

	// Block Dispense Armor Event
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void blockDispenseArmorEvent(BlockDispenseArmorEvent event) {
		if (event.getTargetEntity() instanceof Player) {
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, (Player) event.getTargetEntity(), event);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockFertilizeEvent(BlockFertilizeEvent event) {
		List<BlockState> modifiedBlocks = event.getBlocks();
		Set<BlockState> blocksToRestore = new HashSet<>(modifiedBlocks.size());
		for (BlockState modifiedBlock : modifiedBlocks) {
			if (ZoneUtils.isInPlot(modifiedBlock.getLocation())) {
				continue;
			}
			if (!BlockUtils.isWaterSource(modifiedBlock)) {
				continue;
			}
			BlockState originalBlock = modifiedBlock.getBlock().getState();
			if (BlockUtils.isWaterSource(originalBlock)) {
				continue;
			}
			blocksToRestore.add(originalBlock);
			boolean preserveBlockUnderneath = false;
			if (modifiedBlock.getType().equals(Material.KELP)) {
				preserveBlockUnderneath = true;
			} else if (modifiedBlock.getBlockData() instanceof Bisected bisected) {
				if (bisected.getHalf().equals(Bisected.Half.TOP)) {
					preserveBlockUnderneath = true;
				}
			}
			if (preserveBlockUnderneath) {
				blocksToRestore.add(originalBlock.getLocation().subtract(0.0, 1.0, 0.0).getBlock().getState());
			}
		}
		if (blocksToRestore.isEmpty()) {
			return;
		}
		// Null if dispenser was used
		@Nullable Player player = event.getPlayer();
		if (modifiedBlocks.size() == blocksToRestore.size()) {
			if (player != null) {
				player.sendActionBar(Component.text("Cannot spread waterlogged blocks to non-source blocks outside of plots.", NamedTextColor.RED));
			}
			event.setCancelled(true);
			return;
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				for (BlockState originalState : blocksToRestore) {
					originalState.update(true, false);
				}
			}
		}.runTaskLater(Plugin.getInstance(), 0);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockFormEvent(BlockFormEvent event) {
		Material blockType = event.getNewState().getType();

		if (blockType.equals(Material.SNOW) || blockType.equals(Material.ICE)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void tntPrimeEvent(TNTPrimeEvent event) {
		Location loc = event.getBlock().getLocation();
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_EXPLOSIONS)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void inventoryPickupItemEvent(InventoryPickupItemEvent event) {
		Item itemEntity = event.getItem();
		if (!itemEntity.canPlayerPickup() || itemEntity.getOwner() != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPistonExtendEvent(BlockPistonExtendEvent event) {
		Block piston = event.getBlock();
		BlockFace direction = event.getDirection();
		for (Block block : event.getBlocks()) {
			if (shouldCancelPiston(piston, direction, block)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPistonRetractEvent(BlockPistonRetractEvent event) {
		Block piston = event.getBlock();
		BlockFace direction = event.getDirection();
		for (Block block : event.getBlocks()) {
			if (shouldCancelPiston(piston, direction, block)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void spawnerBreakEvent(BlockBreakEvent event) {
		Block spawner = event.getBlock();
		if (spawner.getType() == Material.SPAWNER) {
			playerBrokeSpawner(event.getPlayer(), spawner);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		EntityType entityType = event.getEntityType();
		if (!(entityType == EntityType.CREEPER || entityType == EntityType.PRIMED_TNT)) {
			return;
		}
		for (Block b : event.blockList()) {
			Material mat = b.getType();
			Location loc = b.getLocation();
			if (mat == Material.SPAWNER) {
				Player p = EntityUtils.getNearestPlayer(loc, 100);
				if (p != null) {
					playerBrokeSpawner(p, b);
				}
			}
		}
	}

	// Prevent a cauldron's water level from decreasing when cleaning items on plot worlds
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void cauldronLevelChangeEvent(CauldronLevelChangeEvent event) {
		CauldronLevelChangeEvent.ChangeReason reason = event.getReason();
		if (event.getNewState().getBlockData() instanceof Levelled cauldron
			    && (reason == CauldronLevelChangeEvent.ChangeReason.BANNER_WASH
				        || reason == CauldronLevelChangeEvent.ChangeReason.ARMOR_WASH
				        || reason == CauldronLevelChangeEvent.ChangeReason.SHULKER_WASH)
			    && ServerProperties.getShardName().endsWith("plots")) {
			// Cannot cancel the event, as that prevents cleaning items, so increase the level by one instead.
			cauldron.setLevel(Math.min(cauldron.getLevel() + 1, cauldron.getMaximumLevel()));
			event.getNewState().setBlockData(cauldron);
		}
	}

	public void playerBrokeSpawner(Player player, Block spawner) {
		if (!Plugin.IS_PLAY_SERVER) {
			return;
		}
		ActivityManager.getManager().addActivity(player);
		List<Chunk> chunkList = LocationUtils.getSurroundingChunks(spawner, SPAWNER_BREAK_CHEST_CHECK_RADIUS);
		int chests = 0;
		for (Chunk chunk : chunkList) {
			for (BlockState interestingBlock : chunk.getTileEntities()) {
				if (ChestUtils.isUnscaledChest(interestingBlock.getBlock()) && LocationUtils.blocksAreWithinRadius(spawner, interestingBlock.getBlock(), SPAWNER_BREAK_CHEST_CHECK_RADIUS)) {
					chests++;
					List<Player> players = PlayerUtils.playersInLootScalingRange(spawner.getLocation());
					((Lootable) interestingBlock).setSeed(players.size());
					interestingBlock.update();
					MMLog.fine("SpawnerBreakLootScaling : Players in radius: " + players.size());
				}
			}
		}
		MMLog.fine("SpawnerBreakLootScaling : Set loot table seed for " + chests + " chests.");
	}

	public boolean shouldCancelPiston(Block piston, BlockFace direction, Block affectedBlock) {
		int pistonTestValue = pistonZonePropertyTest(piston.getLocation());
		if (pistonTestValue < 0) {
			return true;
		}

		Location oldLoc = affectedBlock.getLocation();
		if (pistonTestValue != pistonZonePropertyTest(oldLoc)) {
			return true;
		}

		Location newLoc = new Location(oldLoc.getWorld(),
			oldLoc.getX() + direction.getModX(),
			oldLoc.getY() + direction.getModY(),
			oldLoc.getZ() + direction.getModZ());
		return pistonTestValue != pistonZonePropertyTest(newLoc);
	}

	// Return bit flags that must all be equal for affected locations. Return -1 to disallow in any case.
	private int pistonZonePropertyTest(Location loc) {
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.OVERWORLD_BLOCK_RESET)) {
			return -1;
		}
		int testValue = 0;
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			testValue |= 1;
		}
		if (ZoneUtils.isInPlot(loc)) {
			testValue |= 2;
		}
		return testValue;
	}
}
