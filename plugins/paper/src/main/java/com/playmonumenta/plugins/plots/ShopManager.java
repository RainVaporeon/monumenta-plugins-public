package com.playmonumenta.plugins.plots;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.GuildAccessLevel;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import com.playmonumenta.scriptedquests.quests.QuestNpc;
import com.playmonumenta.scriptedquests.utils.DateUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Lockable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootTables;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

public class ShopManager implements Listener {
	private static final Material SHOP_EMPTY_MAT = Material.BRICKS;
	private static final Material SHOP_PURCHASED_MAT = Material.STONE_BRICKS;
	private static final int MAX_SHOP_WIDTH = 20;
	private static final int SHOP_HEIGHT = 13;
	private static final int SHOP_DEPTH = 15;
	private static final int GUILD_SHOP_WIDTH = 10;
	private static final String NPC_NAME = "SHOP NPC";
	private static final String GUILD_NPC_NAME = "GUILD SHOP NPC";
	private static final String DISABLE_LOCKING_TAG = "DisableLocking";
	private static final String LOCK_PREFIX = "* Soulbound to ";
	private static final String LOCK_SUFFIX = " *";
	private static final String UPKEEP_SCOREBOARD = "DLeft";

	// handles cancelled damage events because we're only interested in the left click interaction (and the event is always cancelled on shop shulkers anyway)
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		Entity damagee = event.getEntity();
		Entity damager = event.getDamager();

		Component customName = damagee.customName();
		if (damager instanceof Player player && damagee instanceof Shulker
				&& customName != null && MessagingUtils.plainText(customName).endsWith("Shop")
				&& ZoneUtils.hasZoneProperty(damager, ZoneProperty.SHOPS_POSSIBLE)) {

			final Shop shop;
			try {
				shop = Shop.fromShopEntity(damagee);
			} catch (WrapperCommandSyntaxException ex) {
				// Not a shop after all?
				Plugin.getInstance().getLogger().warning("Tried to damage a shulker that seemed like a shop but wasn't: " + ex.getMessage());
				return;
			}

			com.playmonumenta.scriptedquests.Plugin sq = com.playmonumenta.scriptedquests.Plugin.getInstance();

			String npcName = shop.isGuildShop() ? GUILD_NPC_NAME : NPC_NAME;
			List<QuestNpc> npc = sq.mNpcManager.getInteractNPC(npcName, EntityType.SHULKER);
			if (npc != null) {
				/*
				 * This is definitely a quest NPC, even if the player might not be able to interact with it
				 * Cancel all damage done to it
				 */
				event.setCancelled(true);

				/* Only trigger quest interactions via melee attack */
				if (event.getCause().equals(DamageCause.ENTITY_ATTACK)) {
					sq.mNpcManager.interactEvent(new QuestContext(sq, player, damagee, false, null, player.getInventory().getItemInMainHand()),
						npcName, EntityType.SHULKER, npc, false);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityCombustByBlockEvent(EntityCombustByBlockEvent event) {
		cancelIfNpc(event.getEntity(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityCombustByEntityEvent(EntityCombustByEntityEvent event) {
		cancelIfNpc(event.getEntity(), event);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		if (event.getCause() != DamageCause.CUSTOM && event.getCause() != DamageCause.VOID) {
			cancelIfNpc(event.getEntity(), event);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityPotionEffectEvent(EntityPotionEffectEvent event) {
		PotionEffect potionEffect = event.getNewEffect();
		if (event.getAction().equals(EntityPotionEffectEvent.Action.ADDED)
			&& potionEffect != null
			&& !potionEffect.getType().equals(PotionEffectType.HEAL)) {
			cancelIfNpc(event.getEntity(), event);
		}
	}

	private void cancelIfNpc(Entity damagee, Cancellable event) {
		Component customName = damagee.customName();
		if (damagee instanceof Shulker
			&& customName != null && MessagingUtils.plainText(customName).endsWith("Shop")
			&& ZoneUtils.hasZoneProperty(damagee, ZoneProperty.SHOPS_POSSIBLE)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		/* Prevent players from placing bricks */
		if (event.getBlockPlaced().getType().equals(Material.BRICKS)
			&& !event.getPlayer().getGameMode().equals(GameMode.CREATIVE)
			&& ZoneUtils.hasZoneProperty(event.getBlockPlaced().getLocation(), ZoneProperty.SHOPS_POSSIBLE)) {
			event.setCancelled(true);
			return;
		}

		if (isDisallowed(event.getPlayer(), event.getBlockPlaced())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		if (isDisallowed(event.getPlayer(), event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		if (!isAllowed(event.getBlock().getLocation(), event.getBlock())) {
			event.setCancelled(true);
		}
	}

	private boolean isAllowed(Location loc, Block block) {
		if (loc != null && block != null && ZoneUtils.hasZoneProperty(loc, ZoneProperty.SHOPS_POSSIBLE)) {
			loc.setY(10);
			return loc.getBlock().getType().equals(Material.SPONGE);
		}
		return true;
	}

	private boolean isDisallowed(Player player, Block block) {
		if (player != null && block != null && !player.getGameMode().equals(GameMode.CREATIVE) && ZoneUtils.hasZoneProperty(player, ZoneProperty.SHOPS_POSSIBLE)) {
			Location loc = block.getLocation();
			int y = loc.getBlockY();
			loc.setY(10);
			Material mat = loc.getBlock().getType();
			// Sponge or (wet sponge below y = 53) allows building non-container inventories
			return !mat.equals(Material.SPONGE) && (!mat.equals(Material.WET_SPONGE) || y >= 53 || block.getState() instanceof Lockable);
		}
		// Allow otherwise - not in the zone, not a block, in creative, etc.
		return false;
	}

	private static class Shop {
		private final Location mMin;
		private final Location mMax;
		private final Shulker mEntity;
		private final String mOwnerName;
		private final @Nullable String mOwnerGuildName;
		private final UUID mOwnerUUID;
		private final Material mOriginalEntityMat;
		private final boolean mUpkeepPaid;
		private boolean mLockingEnabled;

		private Shop(Location min, Location max, Shulker entity, String ownerName, @Nullable String ownerGuildName, UUID ownerUUID, Material originalEntityMat) {
			mMin = min;
			mMax = max;
			mEntity = entity;
			mOwnerName = ownerName;
			mOwnerGuildName = ownerGuildName;
			mOwnerUUID = ownerUUID;
			mOriginalEntityMat = originalEntityMat;
			mLockingEnabled = !entity.getScoreboardTags().contains(DISABLE_LOCKING_TAG);
			mUpkeepPaid = (ScoreboardUtils.getScoreboardValue(entity, UPKEEP_SCOREBOARD).orElse(Integer.MAX_VALUE) - DateUtils.getDaysSinceEpoch()) >= 0;
		}

		protected static Shop fromShopEntity(Entity shopEntity) throws WrapperCommandSyntaxException {
			if (!(shopEntity instanceof Shulker)) {
				throw CommandAPI.failWithString("Invalid shop entity - should be shulker");
			}

			if (!ZoneUtils.hasZoneProperty(shopEntity, ZoneProperty.SHOPS_POSSIBLE)) {
				throw CommandAPI.failWithString("This command can only be used within a Shops Possible zone");
			}

			Integer x1 = null;
			Integer y1 = null;
			Integer z1 = null;
			Integer x2 = null;
			Integer y2 = null;
			Integer z2 = null;
			String ownerName = null;
			String ownerGuildName = null;
			UUID ownerUUID = null;
			Material originalEntityMat = null;
			try {
				for (String tag : shopEntity.getScoreboardTags()) {
					if (tag.startsWith("shop_x1=")) {
						x1 = Integer.parseInt(tag.substring("shop_x1=".length()));
					} else if (tag.startsWith("shop_y1=")) {
						y1 = Integer.parseInt(tag.substring("shop_y1=".length()));
					} else if (tag.startsWith("shop_z1=")) {
						z1 = Integer.parseInt(tag.substring("shop_z1=".length()));
					} else if (tag.startsWith("shop_x2=")) {
						x2 = Integer.parseInt(tag.substring("shop_x2=".length()));
					} else if (tag.startsWith("shop_y2=")) {
						y2 = Integer.parseInt(tag.substring("shop_y2=".length()));
					} else if (tag.startsWith("shop_z2=")) {
						z2 = Integer.parseInt(tag.substring("shop_z2=".length()));
					} else if (tag.startsWith("shop_ownerName=")) {
						ownerName = tag.substring("shop_ownerName=".length());
					} else if (tag.startsWith("shop_ownerGuildName=")) {
						ownerGuildName = tag.substring("shop_ownerGuildName=".length());
					} else if (tag.startsWith("shop_ownerUUID=")) {
						ownerUUID = UUID.fromString(tag.substring("shop_ownerUUID=".length()));
					} else if (tag.startsWith("shop_origMat=")) {
						originalEntityMat = Material.valueOf(tag.substring("shop_origMat=".length()));
					}
				}
			} catch (Exception ex) {
				throw CommandAPI.failWithString(ex.getMessage());
			}

			if (x1 == null || y1 == null || z1 == null || x2 == null || y2 == null || z2 == null || ownerName == null || ownerUUID == null || originalEntityMat == null) {
				throw CommandAPI.failWithString("Shop entity is missing a required tag");
			}

			return new Shop(new Location(shopEntity.getWorld(), x1, y1, z1), new Location(shopEntity.getWorld(), x2, y2, z2),
			                (Shulker) shopEntity, ownerName, ownerGuildName, ownerUUID, originalEntityMat);
		}

		/* Calls func(Location) for each shop platform block, plus a 1-block border */
		private void iterExpandedArea(Consumer<Location> func) {
			iterArea(1, func);
		}

		/* Calls func(Location) for each shop platform block */
		private void iterArea(Consumer<Location> func) {
			iterArea(0, func);
		}

		/* Calls func(Location) for each shop platform block, plus an additional border */
		private void iterArea(int extraWidth, Consumer<Location> func) {
			Location loc = mEntity.getLocation().clone();
			for (int x = mMin.getBlockX() - extraWidth; x <= mMax.getBlockX() + extraWidth; x++) {
				for (int z = mMin.getBlockZ() - extraWidth; z <= mMax.getBlockZ() + extraWidth; z++) {
					loc.setX(x);
					loc.setY(mMin.getBlockY());
					loc.setZ(z);
					func.accept(loc);
				}
			}
		}

		private void particles() {
			iterArea((Location plat) -> {
				plat.add(0, 1, 0);
				new PartialParticle(Particle.VILLAGER_HAPPY, plat, 1, 0.1, 0.1, 0.1).spawnFull();
			});
		}

		private void enableSurvival() {
			iterExpandedArea((Location plat) -> {
				/*
				 * Enable editing the walls of the shop
				 * This sets the entire area to WET_SPONGE,
				 * then the next step will set the core back to SPONGE
				 */
				plat.setY(10);
				plat.getBlock().setType(Material.WET_SPONGE);
			});

			iterArea((Location plat) -> {
				/* Enable survival mode directly under the platform */
				plat.setY(10);
				plat.getBlock().setType(Material.SPONGE);
			});
		}

		private void disableSurvival() {
			iterExpandedArea((Location plat) -> {
				plat.setY(10);
				plat.getBlock().setType(Material.RED_CONCRETE);
			});
		}

		private boolean isGuildShop() {
			return Math.abs(mMax.getBlockX() - mMin.getBlockX()) >= GUILD_SHOP_WIDTH;
		}

		private boolean isLockingDisabled() {
			return !mLockingEnabled;
		}

		private boolean isUpkeepPaid() {
			return mUpkeepPaid;
		}

		private void setLockingEnabled(boolean enabled) {
			if (enabled) {
				mEntity.getScoreboardTags().remove(DISABLE_LOCKING_TAG);
			} else {
				mEntity.getScoreboardTags().add(DISABLE_LOCKING_TAG);
			}
			mLockingEnabled = enabled;
		}

		private BoundingBox getExpandedBoundingBox() {
			return new BoundingBox(mMin.getX() - 1, mMin.getY() - SHOP_DEPTH - 1, mMin.getZ() - 1,
			                       mMax.getX() + 1, mMax.getY() + SHOP_HEIGHT + 1, mMax.getZ() + 1);
		}

		private Collection<Entity> getEntities() {
			return mMin.getWorld().getNearbyEntities(getExpandedBoundingBox());
		}
	}

	public static void registerCommands() {
		/* ******************** NEW ******************** */
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("new"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				shopNew((Player)args[1]);
			})
			.register();

		/* ******************** LOCK ******************** */
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("lock"))
			.withArguments(new EntitySelectorArgument.OneEntity("entity"))
			.executes((sender, args) -> {
				shopLock((Entity) args[1], null, true);
			})
			.register();
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("lock"))
			.withArguments(new EntitySelectorArgument.OneEntity("entity"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				shopLock((Entity) args[1], (Player) args[2], true);
			})
			.register();

		/* ******************** SEMI-LOCK (adventure mode, but unlocked containers) ******************** */
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("semilock"))
			.withArguments(new EntitySelectorArgument.OneEntity("entity"))
			.executes((sender, args) -> {
				shopLock((Entity) args[1], null, false);
			})
			.register();
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("semilock"))
			.withArguments(new EntitySelectorArgument.OneEntity("entity"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				shopLock((Entity) args[1], (Player) args[2], false);
			})
			.register();

		/* ******************** UNLOCK ******************** */
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("unlock"))
			.withArguments(new EntitySelectorArgument.OneEntity("entity"))
			.executes((sender, args) -> {
				shopUnlock((Entity) args[1], null);
			})
			.register();
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("unlock"))
			.withArguments(new EntitySelectorArgument.OneEntity("entity"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				shopUnlock((Entity)args[1], (Player)args[2]);
			})
			.register();

		/* ******************** LOCK DISABLE ******************** */
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("setlockable"))
			.withArguments(new EntitySelectorArgument.OneEntity("entity"))
			.withArguments(new BooleanArgument("lockable"))
			.executes((sender, args) -> {
				setLockable((Entity)args[1], null, (boolean)args[2]);
			})
			.register();
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("setlockable"))
			.withArguments(new EntitySelectorArgument.OneEntity("entity"))
			.withArguments(new BooleanArgument("lockable"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				setLockable((Entity)args[1], (Player)args[3], (boolean)args[2]);
			})
			.register();

		/* ******************** RESET ******************** */
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("reset"))
			.withArguments(new EntitySelectorArgument.OneEntity("entity"))
			.executes((sender, args) -> {
				shopReset((Entity)args[1], null);
			})
			.register();
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("reset"))
			.withArguments(new EntitySelectorArgument.OneEntity("entity"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				shopReset((Entity)args[1], (Player)args[2]);
			})
			.register();

		/* ******************* UTILITIES *************************** */
		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("utilities"))
			.withArguments(new MultiLiteralArgument("unlocknearbybarrels"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.withArguments(new IntegerArgument("radius"))
			.executes((sender, args) -> {
				unlockMarketBarrels((Player) args[0], (Player) args[1], (int) args[2]);
			})
			.register();

		new CommandAPICommand("monumentashop")
			.withPermission(CommandPermission.fromString("monumenta.shop"))
			.withArguments(new MultiLiteralArgument("utilities"))
			.withArguments(new MultiLiteralArgument("createunlockkey"))
			.withArguments(new StringArgument("player name"))
			.executes((sender, args) -> {
				createBarrelKey((Player) args[0], (String) args[1]);
			})
			.register();

	}

	private static void shopNew(Player player) throws WrapperCommandSyntaxException {
		/* First find the block of the correct type under where the player is standing */
		Location startLoc = null;
		Location pLoc = player.getLocation();
		for (int i = 0; i < 4; i++) {
			if (pLoc.getBlock().getType().equals(SHOP_EMPTY_MAT)) {
				startLoc = pLoc;
				break;
			}
			pLoc.subtract(0, 1, 0);
		}
		if (startLoc == null) {
			throw CommandAPI.failWithString("Could not find material " + SHOP_EMPTY_MAT + " under the player");
		}

		if (!ZoneUtils.hasZoneProperty(player, ZoneProperty.SHOPS_POSSIBLE)) {
			throw CommandAPI.failWithString("This command can only be used within a Shops Possible zone");
		}

		/* Find the dimensions of the platform */
		int tmpMinX = -1;
		int tmpMinZ = -1;
		int tmpMaxX = -1;
		int tmpMaxZ = -1;
		Location testLoc = startLoc.clone();
		for (int i = 0; i < MAX_SHOP_WIDTH; i++) {
			if (testLoc.getBlock().getType().equals(SHOP_EMPTY_MAT)) {
				tmpMinX = testLoc.getBlockX();
			} else {
				break;
			}
			testLoc.subtract(1, 0, 0);
		}
		testLoc = startLoc.clone();
		for (int i = 0; i < MAX_SHOP_WIDTH; i++) {
			if (testLoc.getBlock().getType().equals(SHOP_EMPTY_MAT)) {
				tmpMaxX = testLoc.getBlockX();
			} else {
				break;
			}
			testLoc.add(1, 0, 0);
		}
		testLoc = startLoc.clone();
		for (int i = 0; i < MAX_SHOP_WIDTH; i++) {
			if (testLoc.getBlock().getType().equals(SHOP_EMPTY_MAT)) {
				tmpMinZ = testLoc.getBlockZ();
			} else {
				break;
			}
			testLoc.subtract(0, 0, 1);
		}
		testLoc = startLoc.clone();
		for (int i = 0; i < MAX_SHOP_WIDTH; i++) {
			if (testLoc.getBlock().getType().equals(SHOP_EMPTY_MAT)) {
				tmpMaxZ = testLoc.getBlockZ();
			} else {
				break;
			}
			testLoc.add(0, 0, 1);
		}
		final int minX = tmpMinX;
		final int minZ = tmpMinZ;
		final int maxX = tmpMaxX;
		final int maxZ = tmpMaxZ;
		final int platY = startLoc.getBlockY();

		boolean isGuildShop = Math.abs(maxX - minX) >= GUILD_SHOP_WIDTH;
		final String guildName;
		if (isGuildShop) {
			guildName = LuckPermsIntegration.getUnlockedGuildName(LuckPermsIntegration.getGuild(player));
			if (guildName == null) {
				throw CommandAPI.failWithString("You must be in a guild to purchase a guild shop");
			}
		} else {
			guildName = null;
		}

		/* Figure out which side the entity should go on */
		double minXD = Math.abs(player.getLocation().getX() - minX);
		double minZD = Math.abs(player.getLocation().getZ() - minZ);
		double maxXD = Math.abs(player.getLocation().getX() - maxX);
		double maxZD = Math.abs(player.getLocation().getZ() - maxZ);

		Location entityLoc = startLoc.clone();
		if (minXD <= minZD && minXD <= maxXD && minXD <= maxZD) {
			entityLoc.setX(minX - 1);
			entityLoc.setZ((maxZ + minZ) >> 1);
		} else if (minZD <= minXD && minZD <= maxXD && minZD <= maxZD) {
			entityLoc.setZ(minZ - 1);
			entityLoc.setX((maxX + minX) >> 1);
		} else if (maxXD <= minXD && maxXD <= minZD && maxXD <= maxZD) {
			entityLoc.setX(maxX + 1);
			entityLoc.setZ((maxZ + minZ) >> 1);
		} else {
			entityLoc.setZ(maxZ + 1);
			entityLoc.setX((maxX + minX) >> 1);
		}
		entityLoc.add(0.5, 0, 0.5);

		Material originalEntityMat = entityLoc.getBlock().getType();
		entityLoc.getBlock().setType(Material.AIR);
		Shulker shopEntity = entityLoc.getWorld().spawn(entityLoc, Shulker.class, SpawnReason.CUSTOM, (preloadEntity) -> {
			preloadEntity.setAI(false);
			preloadEntity.setGravity(false);
			preloadEntity.setSilent(true);
			preloadEntity.setColor(DyeColor.MAGENTA);
			preloadEntity.setLootTable(LootTables.EMPTY.getLootTable());
			preloadEntity.setPersistent(true);

			if (isGuildShop) {
				preloadEntity.customName(Component.text(guildName + "'s Guild Shop", NamedTextColor.GREEN));
			} else {
				preloadEntity.customName(Component.text(player.getName() + "'s Shop", NamedTextColor.AQUA));
			}
			Set<String> tags = preloadEntity.getScoreboardTags();
			tags.add("shop_x1=" + minX);
			tags.add("shop_y1=" + platY);
			tags.add("shop_z1=" + minZ);
			tags.add("shop_x2=" + maxX);
			tags.add("shop_y2=" + platY);
			tags.add("shop_z2=" + maxZ);
			tags.add("shop_ownerName=" + player.getName());
			tags.add("shop_ownerUUID=" + player.getUniqueId());
			tags.add("shop_origMat=" + originalEntityMat);
			tags.add("shop_shulker");
			tags.add("SkillImmune");
			tags.add(AbilityUtils.IGNORE_TAG);
			if (isGuildShop) {
				tags.add("guild_shop");
				tags.add("shop_ownerGuildName=" + guildName);
			} else {
				tags.add("player_shop");
			}
		});

		Shop shop = Shop.fromShopEntity(shopEntity);

		shopEntity.getWorld().playSound(shopEntity.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1.0f, 1.3f);

		shop.particles();
		shop.enableSurvival();

		shop.iterArea((Location plat) -> {
			/* Replace platform area */
			plat.getBlock().setType(SHOP_PURCHASED_MAT);

			/* Add a ceiling */
			plat.add(0, SHOP_HEIGHT + 1, 0);
			plat.getBlock().setType(Material.BARRIER);
		});

		shop.iterExpandedArea((Location plat) -> {
			/* Add a cellar floor */
			plat.subtract(0, SHOP_DEPTH + 1, 0);
			plat.getBlock().setType(Material.BEDROCK);
		});
	}

	public static void setLockable(Entity shopEntity, @Nullable Player player, boolean lockable) throws WrapperCommandSyntaxException {
		if (!ZoneUtils.hasZoneProperty(shopEntity, ZoneProperty.SHOPS_POSSIBLE)) {
			throw CommandAPI.failWithString("This command can only be used within a Shops Possible zone");
		}

		Shop shop = Shop.fromShopEntity(shopEntity);

		/* Make sure the player (if any) is allowed to change the lock */
		checkAllowedToChangeLock(shop, player);

		shop.setLockingEnabled(lockable);

		if (player != null) {
			player.sendMessage(Component.text("Locking/unlocking your shop is now " + (lockable ? "enabled" : "disabled"), NamedTextColor.WHITE));
		}

		shopEntity.getWorld().playSound(shopEntity.getLocation(), Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1.0f, 1.1f);
		shop.particles();
	}

	public static void shopLock(Entity shopEntity, @Nullable Player player, boolean fullLock) throws WrapperCommandSyntaxException {
		if (!ZoneUtils.hasZoneProperty(shopEntity, ZoneProperty.SHOPS_POSSIBLE)) {
			throw CommandAPI.failWithString("This command can only be used within a Shops Possible zone");
		}

		Shop shop = Shop.fromShopEntity(shopEntity);

		/* Make sure the player (if any) is allowed to change the lock */
		checkAllowedToChangeLock(shop, player);

		if (shop.isLockingDisabled() && shop.isUpkeepPaid()) {
			/* Note - you can not prevent a shop from being locked if it does not have its upkeep paid */
			if (player != null) {
				player.sendMessage(Component.text("Locking and unlocking this shop is disabled, change that setting first before trying to lock.", NamedTextColor.WHITE));
			}
			return;
		}

		if (player != null) {
			if (fullLock) {
				player.sendMessage(Component.text("Your shop has been locked.", NamedTextColor.WHITE));
			} else {
				player.sendMessage(Component.text("Your shop has been locked, except for barrels.", NamedTextColor.WHITE));
			}
		}

		shopEntity.getWorld().playSound(shopEntity.getLocation(), Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1.0f, 0.9f);
		shop.particles();
		shop.disableSurvival();

		if (fullLock) {
			shop.iterExpandedArea((Location plat) -> {
				/* Lock tile entities */
				plat.subtract(0, SHOP_DEPTH, 0);
				for (int y = 0; y <= SHOP_HEIGHT + SHOP_DEPTH + 2; y++) {
					BlockState state = plat.getBlock().getState();
					if (state instanceof Lockable lockable && lockable.getLock().isEmpty()) {
						lockable.setLock(LOCK_PREFIX + shop.mOwnerName + LOCK_SUFFIX);
						state.update();
					}
					plat.add(0, 1, 0);
				}
			});
		}

		/* Lock regular entities */
		for (Entity entity : shop.getEntities()) {
			if (entity instanceof ItemFrame) {
				entity.setInvulnerable(true);
			} else if (entity instanceof ArmorStand) {
				entity.setInvulnerable(true);
				((ArmorStand) entity).setDisabledSlots(EquipmentSlot.values());
			}
		}
	}

	private static void shopUnlock(Entity shopEntity, @Nullable Player player) throws WrapperCommandSyntaxException {
		if (!ZoneUtils.hasZoneProperty(shopEntity, ZoneProperty.SHOPS_POSSIBLE)) {
			throw CommandAPI.failWithString("This command can only be used within a Shops Possible zone");
		}

		Shop shop = Shop.fromShopEntity(shopEntity);

		/* Make sure the player (if any) is allowed to change the lock */
		checkAllowedToChangeLock(shop, player);

		if (player != null) {
			player.sendMessage(Component.text("Your shop has been unlocked.", NamedTextColor.WHITE));
		}

		shopEntity.getWorld().playSound(shopEntity.getLocation(), Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1.0f, 0.5f);
		shop.particles();
		shop.enableSurvival();

		shop.iterExpandedArea((Location plat) -> {
			/* Unlock tile entities */
			plat.subtract(0, SHOP_DEPTH, 0);
			for (int y = 0; y <= SHOP_HEIGHT + SHOP_DEPTH + 2; y++) {
				BlockState state = plat.getBlock().getState();
				if (state instanceof Lockable lockable) {
					((Lockable) state).getLock();
					if (lockable.getLock().startsWith(LOCK_PREFIX)) {
						lockable.setLock(null);
						state.update();
					}
				}
				plat.add(0, 1, 0);
			}
		});

		/* Unlock regular entities */
		for (Entity entity : shop.getEntities()) {
			if (entity instanceof ItemFrame) {
				entity.setInvulnerable(false);
			} else if (entity instanceof ArmorStand) {
				entity.setInvulnerable(false);
				((ArmorStand)entity).removeDisabledSlots(EquipmentSlot.values());
			}
		}
	}

	private static void shopReset(Entity shopEntity, @Nullable Player player) throws WrapperCommandSyntaxException {
		if (!ZoneUtils.hasZoneProperty(shopEntity, ZoneProperty.SHOPS_POSSIBLE)) {
			throw CommandAPI.failWithString("This command can only be used within a Shops Possible zone");
		}

		Shop shop = Shop.fromShopEntity(shopEntity);

		/* Make sure the player (if any) is allowed to change the lock */
		checkAllowedToChangeLock(shop, player);

		if (player != null) {
			player.sendMessage(Component.text("Your shop has been reset.", NamedTextColor.WHITE));
		}

		shop.particles();
		shop.disableSurvival();

		shop.iterArea((Location plat) -> {
			/* Replace platform area */
			plat.getBlock().setType(SHOP_EMPTY_MAT);

			/* Delete above platform */
			for (int y = 0; y <= SHOP_HEIGHT; y++) {
				plat.add(0, 1, 0);
				clearContainer(plat.getBlock());
				plat.getBlock().setType(Material.AIR);
			}
		});

		shop.iterExpandedArea((Location plat) -> {
			/* Delete below platform */
			plat.subtract(0, SHOP_DEPTH, 0);
			for (int y = 0; y < SHOP_DEPTH; y++) {
				clearContainer(plat.getBlock());
				plat.getBlock().setType(Material.SAND);
				plat.add(0, 1, 0);
			}
		});

		/* Remove entities */
		for (Entity entity : shop.getEntities()) {
			if (entity instanceof ItemFrame || entity instanceof Painting || entity instanceof ArmorStand) {
				entity.remove();
			}
		}

		/* Remove the entity */
		Location entityLoc = shop.mEntity.getLocation();
		shop.mEntity.remove();
		entityLoc.getBlock().setType(shop.mOriginalEntityMat);
	}

	/* Throws an exception if not allowed and sends a message to the player, otherwise returns */
	private static void checkAllowedToChangeLock(Shop shop, @Nullable Player player) throws WrapperCommandSyntaxException {
		/* This command was run without a player (i.e. via a command block) - always allow it */
		if (player == null || player.isOp() || player.getGameMode().equals(GameMode.CREATIVE)) {
			return;
		}

		/* This command was initiated by a player interacting with the NPC - check permissions */
		if (shop.isGuildShop()) {
			Group guild = LuckPermsIntegration.getGuild(player);
			if (guild == null) {
				String msg = "You must be a *manager* or *founder* of the guild '" + shop.mOwnerGuildName + "' to change settings for this shop";
				player.sendMessage(Component.text(msg, NamedTextColor.RED));
				throw CommandAPI.failWithString(msg);
			}
			String guildName = LuckPermsIntegration.getUnlockedGuildName(guild);
			if (guildName == null || !guildName.equalsIgnoreCase(shop.mOwnerGuildName)) {
				String msg = "You must be a *manager* or *founder* of the guild '" + shop.mOwnerGuildName + "' to change settings for this shop";
				player.sendMessage(Component.text(msg, NamedTextColor.RED));
				throw CommandAPI.failWithString(msg);
			}
			if (GuildAccessLevel.byGroup(guild).compareTo(GuildAccessLevel.MANAGER) > 0) {
				String msg = "You must be a *manager* or *founder* of the guild '" + shop.mOwnerGuildName + "' to change settings for this shop";
				player.sendMessage(Component.text(msg, NamedTextColor.RED));
				throw CommandAPI.failWithString(msg);
			}
		} else if (!player.getUniqueId().equals(shop.mOwnerUUID)) {
			/* Not a guild shop, and also not the owning player */
			String msg = "You must be the owner of this shop to change its settings";
			player.sendMessage(Component.text(msg, NamedTextColor.RED));
			throw CommandAPI.failWithString(msg);
		}
	}

	private static void clearContainer(Block block) {
		BlockState state = block.getState();
		if (state instanceof Container) {
			((Container)state).getInventory().clear();
			state.update();
		}
	}

	private static void unlockMarketBarrels(Player sender, Player target, int radius) {
		Location start = sender.getLocation();
		for (double x = start.getX() - radius; x <= start.getX() + radius; x++) {
			for (double y = start.getY() - radius; y <= start.getY() + radius; y++) {
				for (double z = start.getZ() - radius; z <= start.getZ() + radius; z++) {
					BlockState state = new Location(sender.getWorld(), x, y, z).getBlock().getState();
					if (state instanceof Lockable lockable) {
						lockable.getLock();
						if (lockable.getLock().equalsIgnoreCase(LOCK_PREFIX + target.getName() + LOCK_SUFFIX)) {
							lockable.setLock(null);
							state.update();
						}
					}
				}
			}
		}
	}

	private static void createBarrelKey(Player sender, String target) {
		ItemStack newKey = new ItemStack(Material.LIGHTNING_ROD, 1);
		ItemMeta keyMeta = newKey.getItemMeta();
		keyMeta.displayName(Component.text(LOCK_PREFIX + target + LOCK_SUFFIX));
		newKey.setItemMeta(keyMeta);
		InventoryUtils.giveItem(sender, newKey);
	}
}
