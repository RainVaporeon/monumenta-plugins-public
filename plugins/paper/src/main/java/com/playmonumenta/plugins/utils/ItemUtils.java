package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Constants.Materials;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enchantments.Multitool;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.ItemType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTList;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;


public class ItemUtils {
	private static final String PLAIN_KEY = "plain";
	private static final String DISPLAY_KEY = "display";
	private static final String LORE_KEY = "Lore";
	private static final String NAME_KEY = "Name";
	private static final Pattern NON_PLAIN_REGEX = Pattern.compile("[^ -~]");

	private static final String plainNamePath = PLAIN_KEY + "." + DISPLAY_KEY + "." + NAME_KEY;
		// resolveOrDefault doesn't support NBTCompoundList or StringList
	private static final String plainDisplayPath = PLAIN_KEY + "." + DISPLAY_KEY;

	// List of materials that trees can't replace when they grow
	public static final Set<Material> notAllowedTreeReplace = EnumSet.of(
		// Basically #minecraft:wither_immune + chests, barrels, shulker boxes, spawners
		Material.BARRIER,
		Material.BEDROCK,
		Material.END_PORTAL,
		Material.END_PORTAL_FRAME,
		Material.END_GATEWAY,
		Material.COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.STRUCTURE_BLOCK,
		Material.JIGSAW,
		Material.MOVING_PISTON,
		Material.CHEST,
		Material.BARREL,
		Material.SPAWNER,
		Material.SHULKER_BOX,
		Material.WHITE_SHULKER_BOX,
		Material.ORANGE_SHULKER_BOX,
		Material.MAGENTA_SHULKER_BOX,
		Material.LIGHT_BLUE_SHULKER_BOX,
		Material.YELLOW_SHULKER_BOX,
		Material.LIME_SHULKER_BOX,
		Material.PINK_SHULKER_BOX,
		Material.GRAY_SHULKER_BOX,
		Material.LIGHT_GRAY_SHULKER_BOX,
		Material.CYAN_SHULKER_BOX,
		Material.PURPLE_SHULKER_BOX,
		Material.BLUE_SHULKER_BOX,
		Material.BROWN_SHULKER_BOX,
		Material.GREEN_SHULKER_BOX,
		Material.RED_SHULKER_BOX,
		Material.BLACK_SHULKER_BOX
	);

	// List of blocks that can be interacted with using right click and generally perform some functionality
	public static final Set<Material> interactableBlocks = EnumSet.of(
		Material.ACACIA_BUTTON,
		Material.ACACIA_DOOR,
		Material.ACACIA_FENCE_GATE,
		Material.ACACIA_TRAPDOOR,
		Material.ANVIL,
		Material.CHIPPED_ANVIL,
		Material.DAMAGED_ANVIL,
		Material.CHERRY_BUTTON,
		Material.CHERRY_DOOR,
		Material.CHERRY_FENCE_GATE,
		Material.CHERRY_TRAPDOOR,
		Material.BEACON,
		Material.BIRCH_BUTTON,
		Material.BIRCH_DOOR,
		Material.BIRCH_FENCE_GATE,
		Material.BIRCH_TRAPDOOR,
		Material.BLACK_BED,
		Material.BLACK_SHULKER_BOX,
		Material.BLUE_BED,
		Material.BLUE_SHULKER_BOX,
		Material.BREWING_STAND,
		Material.BROWN_BED,
		Material.BROWN_SHULKER_BOX,
		Material.CAKE,
		Material.CHAIN_COMMAND_BLOCK,
		Material.CHEST,
		Material.COMMAND_BLOCK,
		Material.COMPARATOR,
		Material.CRAFTING_TABLE,
		Material.CYAN_BED,
		Material.CYAN_SHULKER_BOX,
		Material.DARK_OAK_BUTTON,
		Material.DARK_OAK_DOOR,
		Material.DARK_OAK_FENCE_GATE,
		Material.DARK_OAK_TRAPDOOR,
		Material.DISPENSER,
		Material.DRAGON_EGG,
		Material.DROPPER,
		Material.ENCHANTING_TABLE,
		Material.ENDER_CHEST,
		Material.FLOWER_POT,
		Material.FURNACE,
		Material.GRAY_BED,
		Material.GRAY_SHULKER_BOX,
		Material.GREEN_BED,
		Material.GREEN_SHULKER_BOX,
		Material.HOPPER,
		Material.JUKEBOX,
		Material.JUNGLE_BUTTON,
		Material.JUNGLE_DOOR,
		Material.JUNGLE_FENCE_GATE,
		Material.JUNGLE_TRAPDOOR,
		Material.LEVER,
		Material.LIGHT_BLUE_BED,
		Material.LIGHT_BLUE_SHULKER_BOX,
		Material.LIGHT_GRAY_BED,
		Material.LIGHT_GRAY_SHULKER_BOX,
		Material.LIME_BED,
		Material.LIME_SHULKER_BOX,
		Material.MAGENTA_BED,
		Material.MAGENTA_SHULKER_BOX,
		Material.MANGROVE_BUTTON,
		Material.MANGROVE_DOOR,
		Material.MANGROVE_FENCE_GATE,
		Material.MANGROVE_TRAPDOOR,
		Material.NOTE_BLOCK,
		Material.OAK_BUTTON,
		Material.OAK_DOOR,
		Material.OAK_FENCE_GATE,
		Material.OAK_TRAPDOOR,
		Material.ORANGE_BED,
		Material.ORANGE_SHULKER_BOX,
		Material.PINK_BED,
		Material.PINK_SHULKER_BOX,
		Material.POTTED_ACACIA_SAPLING,
		Material.POTTED_ALLIUM,
		Material.POTTED_AZURE_BLUET,
		Material.POTTED_BIRCH_SAPLING,
		Material.POTTED_BLUE_ORCHID,
		Material.POTTED_BROWN_MUSHROOM,
		Material.POTTED_CACTUS,
		Material.POTTED_DANDELION,
		Material.POTTED_DARK_OAK_SAPLING,
		Material.POTTED_DEAD_BUSH,
		Material.POTTED_FERN,
		Material.POTTED_JUNGLE_SAPLING,
		Material.POTTED_OAK_SAPLING,
		Material.POTTED_ORANGE_TULIP,
		Material.POTTED_OXEYE_DAISY,
		Material.POTTED_PINK_TULIP,
		Material.POTTED_POPPY,
		Material.POTTED_RED_MUSHROOM,
		Material.POTTED_RED_TULIP,
		Material.POTTED_SPRUCE_SAPLING,
		Material.POTTED_WHITE_TULIP,
		Material.PUMPKIN,
		Material.PURPLE_BED,
		Material.PURPLE_SHULKER_BOX,
		Material.RED_BED,
		Material.RED_SHULKER_BOX,
		Material.REPEATER,
		Material.REPEATING_COMMAND_BLOCK,
		Material.SHULKER_BOX,
		Material.SPRUCE_BUTTON,
		Material.SPRUCE_DOOR,
		Material.SPRUCE_FENCE_GATE,
		Material.SPRUCE_TRAPDOOR,
		Material.STONE_BUTTON,
		Material.STRUCTURE_BLOCK,
		Material.TNT,
		Material.TRAPPED_CHEST,
		Material.WHITE_BED,
		Material.WHITE_SHULKER_BOX,
		Material.YELLOW_BED,
		Material.YELLOW_SHULKER_BOX,
		Material.LOOM,
		Material.BARREL,
		Material.SMOKER,
		Material.BLAST_FURNACE,
		Material.CARTOGRAPHY_TABLE,
		Material.FLETCHING_TABLE,
		Material.GRINDSTONE,
		Material.SMITHING_TABLE,
		Material.STONECUTTER,
		Material.BELL,
		Material.COMPOSTER,
		Material.WARPED_DOOR,
		Material.WARPED_TRAPDOOR,
		Material.WARPED_FENCE_GATE,
		Material.CRIMSON_DOOR,
		Material.CRIMSON_TRAPDOOR,
		Material.CRIMSON_FENCE_GATE
	);

	public static final Set<Material> CARPETS = EnumSet.of(
		Material.WHITE_CARPET,
		Material.ORANGE_CARPET,
		Material.MAGENTA_CARPET,
		Material.LIGHT_BLUE_CARPET,
		Material.YELLOW_CARPET,
		Material.LIME_CARPET,
		Material.PINK_CARPET,
		Material.GRAY_CARPET,
		Material.LIGHT_GRAY_CARPET,
		Material.CYAN_CARPET,
		Material.PURPLE_CARPET,
		Material.BLUE_CARPET,
		Material.BROWN_CARPET,
		Material.GREEN_CARPET,
		Material.RED_CARPET,
		Material.BLACK_CARPET,
		Material.MOSS_CARPET
	);

	public static final Set<Material> BUTTONS = EnumSet.of(
		Material.ACACIA_BUTTON,
		Material.CRIMSON_BUTTON,
		Material.BIRCH_BUTTON,
		Material.STONE_BUTTON,
		Material.WARPED_BUTTON,
		Material.DARK_OAK_BUTTON,
		Material.JUNGLE_BUTTON,
		Material.POLISHED_BLACKSTONE_BUTTON,
		Material.OAK_BUTTON,
		Material.SPRUCE_BUTTON,
		Material.CHERRY_BUTTON,
		Material.MANGROVE_BUTTON
	);

	public static final Set<Material> shulkerBoxes = EnumSet.of(
		Material.SHULKER_BOX,
		Material.WHITE_SHULKER_BOX,
		Material.ORANGE_SHULKER_BOX,
		Material.MAGENTA_SHULKER_BOX,
		Material.LIGHT_BLUE_SHULKER_BOX,
		Material.YELLOW_SHULKER_BOX,
		Material.LIME_SHULKER_BOX,
		Material.PINK_SHULKER_BOX,
		Material.GRAY_SHULKER_BOX,
		Material.LIGHT_GRAY_SHULKER_BOX,
		Material.CYAN_SHULKER_BOX,
		Material.PURPLE_SHULKER_BOX,
		Material.BLUE_SHULKER_BOX,
		Material.BROWN_SHULKER_BOX,
		Material.GREEN_SHULKER_BOX,
		Material.RED_SHULKER_BOX,
		Material.BLACK_SHULKER_BOX
	);

	public static final Set<Material> ranged = EnumSet.of(
		Material.BOW,
		Material.CROSSBOW,
		Material.TRIDENT,
		Material.ENDER_PEARL,
		Material.EGG,
		Material.SNOWBALL
	);

	public static final Set<Material> dyes = EnumSet.of(
		Material.RED_DYE,
		Material.GREEN_DYE,
		Material.PURPLE_DYE,
		Material.CYAN_DYE,
		Material.LIGHT_GRAY_DYE,
		Material.GRAY_DYE,
		Material.PINK_DYE,
		Material.LIME_DYE,
		Material.YELLOW_DYE,
		Material.BLUE_DYE,
		Material.LIGHT_BLUE_DYE,
		Material.MAGENTA_DYE,
		Material.ORANGE_DYE,
		Material.BROWN_DYE,
		Material.BLACK_DYE,
		Material.WHITE_DYE
	);

	// Exclude tridents, which get checked manually because Riptide tridents are not shootable
	public static final Set<Material> SHOOTABLES = EnumSet.of(
		Material.BOW,
		Material.CROSSBOW,
		Material.SNOWBALL,
		Material.EGG,
		Material.ENDER_PEARL,
		Material.FIREWORK_ROCKET,
		Material.FISHING_ROD,
		Material.SPLASH_POTION,
		Material.LINGERING_POTION,
		Material.EXPERIENCE_BOTTLE
	);

	public static final Set<Material> GOOD_OCCLUDERS = EnumSet.of(
		Material.RED_STAINED_GLASS,
		Material.GREEN_STAINED_GLASS,
		Material.PURPLE_STAINED_GLASS,
		Material.CYAN_STAINED_GLASS,
		Material.LIGHT_GRAY_STAINED_GLASS,
		Material.GRAY_STAINED_GLASS,
		Material.PINK_STAINED_GLASS,
		Material.LIME_STAINED_GLASS,
		Material.YELLOW_STAINED_GLASS,
		Material.BLUE_STAINED_GLASS,
		Material.LIGHT_BLUE_STAINED_GLASS,
		Material.MAGENTA_STAINED_GLASS,
		Material.ORANGE_STAINED_GLASS,
		Material.BROWN_STAINED_GLASS,
		Material.BLACK_STAINED_GLASS,
		Material.WHITE_STAINED_GLASS,
		Material.GLASS,
		Material.TINTED_GLASS,
		Material.SPRUCE_LEAVES,
		Material.OAK_LEAVES,
		Material.DARK_OAK_LEAVES,
		Material.JUNGLE_LEAVES,
		Material.BIRCH_LEAVES,
		Material.ACACIA_LEAVES,
		Material.AZALEA_LEAVES,
		Material.FLOWERING_AZALEA_LEAVES,
		Material.MANGROVE_LEAVES,
		Material.CHERRY_LEAVES,
		Material.SEA_LANTERN,
		Material.GLOWSTONE
	);

	public static final Set<Material> STRIPPABLES = EnumSet.of(
		Material.BIRCH_LOG,
		Material.SPRUCE_LOG,
		Material.JUNGLE_LOG,
		Material.DARK_OAK_LOG,
		Material.ACACIA_LOG,
		Material.OAK_LOG,
		Material.MANGROVE_LOG,
		Material.CHERRY_LOG,
		Material.CRIMSON_HYPHAE,
		Material.CRIMSON_STEM,
		Material.BIRCH_WOOD,
		Material.SPRUCE_WOOD,
		Material.JUNGLE_WOOD,
		Material.DARK_OAK_WOOD,
		Material.ACACIA_WOOD,
		Material.OAK_WOOD,
		Material.MANGROVE_WOOD,
		Material.CHERRY_WOOD,
		Material.WARPED_STEM,
		Material.WARPED_HYPHAE
	);

	public static final Set<Material> SIGNS = EnumSet.of(
		Material.ACACIA_SIGN,
		Material.ACACIA_WALL_SIGN,
		Material.BIRCH_SIGN,
		Material.BIRCH_WALL_SIGN,
		Material.CHERRY_SIGN,
		Material.CHERRY_WALL_SIGN,
		Material.CRIMSON_SIGN,
		Material.CRIMSON_WALL_SIGN,
		Material.DARK_OAK_SIGN,
		Material.DARK_OAK_WALL_SIGN,
		Material.JUNGLE_SIGN,
		Material.JUNGLE_WALL_SIGN,
		Material.MANGROVE_SIGN,
		Material.MANGROVE_WALL_SIGN,
		Material.OAK_SIGN,
		Material.OAK_WALL_SIGN,
		Material.SPRUCE_SIGN,
		Material.SPRUCE_WALL_SIGN,
		Material.WARPED_SIGN,
		Material.WARPED_WALL_SIGN
	);

	public static final Set<Material> CANDLES = EnumSet.of(
		Material.CANDLE,
		Material.WHITE_CANDLE,
		Material.LIGHT_GRAY_CANDLE,
		Material.GRAY_CANDLE,
		Material.BLACK_CANDLE,
		Material.BROWN_CANDLE,
		Material.RED_CANDLE,
		Material.ORANGE_CANDLE,
		Material.YELLOW_CANDLE,
		Material.LIME_CANDLE,
		Material.GREEN_CANDLE,
		Material.LIGHT_BLUE_CANDLE,
		Material.BLUE_CANDLE,
		Material.CYAN_CANDLE,
		Material.PURPLE_CANDLE,
		Material.MAGENTA_CANDLE,
		Material.PINK_CANDLE
	);

	public static final Set<Material> FLOWER_POTS = EnumSet.of(
		Material.FLOWER_POT,
		Material.POTTED_OAK_SAPLING, Material.POTTED_SPRUCE_SAPLING, Material.POTTED_BIRCH_SAPLING, Material.POTTED_JUNGLE_SAPLING,
		Material.POTTED_ACACIA_SAPLING, Material.POTTED_DARK_OAK_SAPLING, Material.POTTED_FERN, Material.POTTED_DANDELION,
		Material.POTTED_POPPY, Material.POTTED_BLUE_ORCHID, Material.POTTED_ALLIUM, Material.POTTED_AZURE_BLUET,
		Material.POTTED_RED_TULIP, Material.POTTED_ORANGE_TULIP, Material.POTTED_WHITE_TULIP, Material.POTTED_PINK_TULIP,
		Material.POTTED_OXEYE_DAISY, Material.POTTED_CORNFLOWER, Material.POTTED_LILY_OF_THE_VALLEY, Material.POTTED_WITHER_ROSE,
		Material.POTTED_RED_MUSHROOM, Material.POTTED_BROWN_MUSHROOM, Material.POTTED_DEAD_BUSH, Material.POTTED_CACTUS,
		Material.POTTED_BAMBOO, Material.POTTED_CRIMSON_FUNGUS, Material.POTTED_WARPED_FUNGUS, Material.POTTED_CRIMSON_ROOTS,
		Material.POTTED_WARPED_ROOTS, Material.POTTED_AZALEA_BUSH, Material.POTTED_FLOWERING_AZALEA_BUSH, Material.POTTED_MANGROVE_PROPAGULE,
		Material.POTTED_CHERRY_SAPLING
	);

	// list of blocks that are supposedly used as limits to player movements
	public static final Set<Material> noPassthrough = EnumSet.of(
		Material.BARRIER,
		Material.BEDROCK
	);

	private static final EnumSet<Material> DEFAULT_ATTRIBUTE_MATERIALS = EnumSet.of(Material.TRIDENT); // tridents also count since they have melee attack damage

	private static final Map<Material, Material> BANNER_TO_FLOOR_BANNER = new HashMap<>();
	private static final Map<Material, Material> BANNER_TO_WALL_BANNER = new HashMap<>();

	static {
		DEFAULT_ATTRIBUTE_MATERIALS.addAll(Materials.ARMOR);
		DEFAULT_ATTRIBUTE_MATERIALS.addAll(Materials.AXES);
		DEFAULT_ATTRIBUTE_MATERIALS.addAll(Materials.BOWS); // technically doesn't apply to bows since it's melee attack damage
		DEFAULT_ATTRIBUTE_MATERIALS.addAll(Materials.HOES);
		DEFAULT_ATTRIBUTE_MATERIALS.addAll(Materials.PICKAXES);
		DEFAULT_ATTRIBUTE_MATERIALS.addAll(Materials.SHOVELS);
		DEFAULT_ATTRIBUTE_MATERIALS.addAll(Materials.SWORDS);
		// ! for future minecraft versions: add more items with base vanilla attributes

		registerBannerType(Material.WHITE_BANNER, Material.WHITE_WALL_BANNER);
		registerBannerType(Material.ORANGE_BANNER, Material.ORANGE_WALL_BANNER);
		registerBannerType(Material.MAGENTA_BANNER, Material.MAGENTA_WALL_BANNER);
		registerBannerType(Material.LIGHT_BLUE_BANNER, Material.LIGHT_BLUE_WALL_BANNER);
		registerBannerType(Material.YELLOW_BANNER, Material.YELLOW_WALL_BANNER);
		registerBannerType(Material.LIME_BANNER, Material.LIME_WALL_BANNER);
		registerBannerType(Material.PINK_BANNER, Material.PINK_WALL_BANNER);
		registerBannerType(Material.GRAY_BANNER, Material.GRAY_WALL_BANNER);
		registerBannerType(Material.LIGHT_GRAY_BANNER, Material.LIGHT_GRAY_WALL_BANNER);
		registerBannerType(Material.CYAN_BANNER, Material.CYAN_WALL_BANNER);
		registerBannerType(Material.PURPLE_BANNER, Material.PURPLE_WALL_BANNER);
		registerBannerType(Material.BLUE_BANNER, Material.BLUE_WALL_BANNER);
		registerBannerType(Material.BROWN_BANNER, Material.BROWN_WALL_BANNER);
		registerBannerType(Material.GREEN_BANNER, Material.GREEN_WALL_BANNER);
		registerBannerType(Material.RED_BANNER, Material.RED_WALL_BANNER);
		registerBannerType(Material.BLACK_BANNER, Material.BLACK_WALL_BANNER);
	}

	private static void registerBannerType(Material floorBanner, Material wallBanner) {
		BANNER_TO_FLOOR_BANNER.put(floorBanner, floorBanner);
		BANNER_TO_FLOOR_BANNER.put(wallBanner, floorBanner);
		BANNER_TO_WALL_BANNER.put(wallBanner, wallBanner);
		BANNER_TO_WALL_BANNER.put(floorBanner, wallBanner);
	}

	// Return the quest ID string, which is assumed to start with "#Q", or null
	public static @Nullable String getItemQuestId(@Nullable ItemStack item) {
		if (item == null) {
			return null;
		}
		List<String> loreEntries = getPlainLoreIfExists(item);
		if (loreEntries == null) {
			return null;
		}
		for (String loreEntry : loreEntries) {
			if (loreEntry.startsWith("#Q")) {
				return loreEntry;
			}
		}
		return null;
	}

	public static boolean isQuestItem(@Nullable ItemStack item) {
		return InventoryUtils.testForItemWithLore(item, "Quest Item");
	}

	@Contract("null -> true")
	public static boolean isNullOrAir(@Nullable ItemStack itemStack) {
		return itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0;
	}

	// Returns the costs (in tier 2 currency (CXP/CCS/etc.)) for each region to reforge a list of items.
	// There is no global "MONUMENTA" currency. The calling function will determine what to do with this cost.
	public static Map<Region, Integer> getReforgeCosts(Collection<ItemStack> items) {
		Map<Region, Integer> costs = new HashMap<>();
		costs.put(Region.VALLEY, 0);
		costs.put(Region.ISLES, 0);
		costs.put(Region.RING, 0);
		costs.put(Region.SHULKER_BOX, 0);
		for (ItemStack item : items) {
			Region type = ItemStatUtils.getRegion(item);
			costs.computeIfPresent(type, (k, v) -> v += getReforgeCost(item));
		}

		return costs;
	}

	// Returns the cost (in tier 2 currency (CXP/CCS/etc.)) to reforge an item.
	public static Integer getReforgeCost(ItemStack item) {
		switch (ItemStatUtils.getTier(item)) {
			case IV:
				return item.getAmount() * 1;
			case V:
				return item.getAmount() * 4;
			case UNCOMMON:
			case UNIQUE:
				return item.getAmount() * 16;
			case EVENT:
				return item.getAmount() * 32;
			case RARE:
			case PATRON:
				return item.getAmount() * 48;
			case ARTIFACT:
			case OBFUSCATED:
				return item.getAmount() * 64;
			case SHULKER_BOX:
				return item.getAmount() * 64 * 2;
			case EPIC:
				return item.getAmount() * 64 * 3;
			default:
				return 0;
		}
	}

	public static EquipmentSlot getEquipmentSlot(ItemStack item) {
		if (item == null) {
			return EquipmentSlot.HAND;
		}
		return getEquipmentSlot(item.getType());
	}

	public static EquipmentSlot getEquipmentSlot(Material material) {
		if (material == null) {
			return EquipmentSlot.HAND;
		}
		switch (material) {
			case LEATHER_HELMET:
			case CHAINMAIL_HELMET:
			case IRON_HELMET:
			case GOLDEN_HELMET:
			case DIAMOND_HELMET:
			case NETHERITE_HELMET:
			case TURTLE_HELMET:
			case CARVED_PUMPKIN:
			case CREEPER_HEAD:
			case SKELETON_SKULL:
			case WITHER_SKELETON_SKULL:
			case ZOMBIE_HEAD:
			case PLAYER_HEAD:
				return EquipmentSlot.HEAD;
			case LEATHER_CHESTPLATE:
			case CHAINMAIL_CHESTPLATE:
			case IRON_CHESTPLATE:
			case GOLDEN_CHESTPLATE:
			case DIAMOND_CHESTPLATE:
			case NETHERITE_CHESTPLATE:
				return EquipmentSlot.CHEST;
			case LEATHER_LEGGINGS:
			case CHAINMAIL_LEGGINGS:
			case IRON_LEGGINGS:
			case GOLDEN_LEGGINGS:
			case DIAMOND_LEGGINGS:
			case NETHERITE_LEGGINGS:
				return EquipmentSlot.LEGS;
			case LEATHER_BOOTS:
			case CHAINMAIL_BOOTS:
			case IRON_BOOTS:
			case GOLDEN_BOOTS:
			case DIAMOND_BOOTS:
			case NETHERITE_BOOTS:
				return EquipmentSlot.FEET;
			case SHIELD:
				return EquipmentSlot.OFF_HAND;
			default:
				return EquipmentSlot.HAND;
		}
	}

	//Gets the armor equip sound based off of the armor type
	public static Sound getArmorEquipSound(Material mat) {
		switch (mat) {
			case CHAINMAIL_HELMET:
			case CHAINMAIL_CHESTPLATE:
			case CHAINMAIL_LEGGINGS:
			case CHAINMAIL_BOOTS:
				return Sound.ITEM_ARMOR_EQUIP_CHAIN;
			case DIAMOND_HELMET:
			case DIAMOND_CHESTPLATE:
			case DIAMOND_LEGGINGS:
			case DIAMOND_BOOTS:
				return Sound.ITEM_ARMOR_EQUIP_DIAMOND;
			case ELYTRA:
				return Sound.ITEM_ARMOR_EQUIP_ELYTRA;
			default:
				return Sound.ITEM_ARMOR_EQUIP_GENERIC;
			case GOLDEN_HELMET:
			case GOLDEN_CHESTPLATE:
			case GOLDEN_LEGGINGS:
			case GOLDEN_BOOTS:
				return Sound.ITEM_ARMOR_EQUIP_GOLD;
			case IRON_HELMET:
			case IRON_CHESTPLATE:
			case IRON_LEGGINGS:
			case IRON_BOOTS:
				return Sound.ITEM_ARMOR_EQUIP_IRON;
			case LEATHER_HELMET:
			case LEATHER_CHESTPLATE:
			case LEATHER_LEGGINGS:
			case LEATHER_BOOTS:
				return Sound.ITEM_ARMOR_EQUIP_LEATHER;
			case TURTLE_HELMET:
				return Sound.ITEM_ARMOR_EQUIP_TURTLE;
		}
	}

	/**
	 * Items drop if they have lore that does not contain $$ and they are a Quest Item or Material
	 */
	public static float getItemDropChance(@Nullable ItemStack item) {
		Tier tier = ItemStatUtils.getTier(item);
		if (item != null && (item.hasItemMeta() && item.getItemMeta().hasLore()) && !InventoryUtils.testForItemWithLore(item, "$$") && (isQuestItem(item) || ItemStatUtils.isMaterial(item) || (tier != null && tier != Tier.CURRENCY))) {
			return 100.0f;
		} else {
			return -200.0f;
		}
	}

	public static boolean doDropItemAfterSpawnerLimit(@Nullable ItemStack item) {
		return getItemDropChance(item) > 0;
	}

	public static @Nullable String getBookTitle(@Nullable ItemStack book) {
		if (book == null) {
			return null;
		}
		ItemMeta itemMeta = book.getItemMeta();
		if (itemMeta == null || !(itemMeta instanceof BookMeta)) {
			return null;
		}
		return ((BookMeta) itemMeta).getTitle();
	}

	public static @Nullable String getBookAuthor(@Nullable ItemStack book) {
		if (book == null) {
			return null;
		}
		ItemMeta itemMeta = book.getItemMeta();
		if (itemMeta instanceof BookMeta bookMeta) {
			return bookMeta.getAuthor();
		}
		return null;
	}

	public static @Nullable LootTable getLootTable(@Nullable ItemStack itemStack) {
		if (itemStack == null) {
			return null;
		}
		ItemMeta meta = itemStack.getItemMeta();
		if (meta == null) {
			return null;
		}
		if (meta instanceof Lootable lootable) {
			return lootable.getLootTable();
		}
		return null;
	}

	public static void addPotionEffect(ItemStack potion, PotionInfo info) {
		PotionMeta meta = (PotionMeta) potion.getItemMeta();
		meta.addCustomEffect(new PotionEffect(info.mType, info.mDuration, info.mAmplifier, false, true), false);
		potion.setItemMeta(meta);
	}

	public static boolean isShootableItem(ItemStack item, boolean excludeRiptide) {
		Material mat = item.getType();
		if (mat == Material.TRIDENT) {
			return !(excludeRiptide && item.containsEnchantment(Enchantment.RIPTIDE));
		} else {
			return SHOOTABLES.contains(mat);
		}
	}

	public static boolean isShootableItem(ItemStack item) {
		return isShootableItem(item, true);
	}

	public static boolean isAllowedTreeReplace(@Nullable Material mat) {
		return mat == null || !notAllowedTreeReplace.contains(mat);
	}

	public static boolean isShulkerBox(@Nullable Material mat) {
		return mat != null && shulkerBoxes.contains(mat);
	}

	//Returns true if the item material is something a player can launch an AbstractArrow/Projectile from
	public static boolean isRanged(@Nullable Material mat) {
		return mat != null && ranged.contains(mat);
	}

	//Returns true if the item material is a dye
	public static boolean isDye(@Nullable Material mat) {
		return mat != null && dyes.contains(mat);
	}

	public static boolean isStrippable(@Nullable Material mat) {
		return mat != null && STRIPPABLES.contains(mat);
	}

	public static boolean isSign(@Nullable Material mat) {
		return mat != null && SIGNS.contains(mat);
	}

	public static boolean isWool(@Nullable Material mat) {
		if (mat == null) {
			return false;
		}
		return switch (mat) {
			case WHITE_WOOL, BLACK_WOOL, BLUE_WOOL, BROWN_WOOL, CYAN_WOOL, GRAY_WOOL, GREEN_WOOL,
				     LIGHT_BLUE_WOOL, LIGHT_GRAY_WOOL, LIME_WOOL, MAGENTA_WOOL, ORANGE_WOOL,
				     PINK_WOOL, PURPLE_WOOL, RED_WOOL, YELLOW_WOOL -> true;
			default -> false;
		};
	}

	public static float getDamagePercent(@Nullable ItemStack item) {
		if (item == null) {
			return 0.0f;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return 0.0f;
		}
		if (!(meta instanceof Damageable damageable)) {
			return 0.0f;
		} else {
			return 1.0f * damageable.getDamage() / item.getType().getMaxDurability();
		}
	}

	public static void damageItem(ItemStack item, int damage, boolean canBreak) {
		ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;
		if (meta instanceof Damageable dMeta && !meta.isUnbreakable()) {
			// This item can be damaged - remove some durability from it
			short maxDurability = item.getType().getMaxDurability();
			int currentDamage = dMeta.getDamage();
			int newDamage = currentDamage + damage;
			if (canBreak && newDamage > maxDurability - 1) {
				Shattered.shatter(item, Shattered.DURABILITY_SHATTER);
			} else {
				dMeta.setDamage(Math.min(maxDurability - 1, newDamage));
				item.setItemMeta(meta);
			}
		}
	}

	public static void damageItemPercent(ItemStack item, double damagePercent, boolean canBreak) {
		ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : null;
		if (meta instanceof Damageable dMeta && !meta.isUnbreakable()) {
			// This item can be damaged - remove some durability from it
			short maxDurability = item.getType().getMaxDurability();
			int currentDamage = dMeta.getDamage();
			int newDamage = (int) (currentDamage + (maxDurability * damagePercent) / 100);
			if (canBreak && newDamage > maxDurability - 1) {
				Shattered.shatter(item, Shattered.DURABILITY_SHATTER);
			} else {
				dMeta.setDamage(Math.min(maxDurability - 1, newDamage));
				item.setItemMeta(meta);
			}
		}
	}

	public static void damageShield(Player player, int damage) {
		PlayerInventory inv = player.getInventory();
		ItemStack mainHand = inv.getItemInMainHand();
		if (mainHand.getType().equals(Material.SHIELD)) {
			damageItem(mainHand, damage / (mainHand.getEnchantmentLevel(Enchantment.DURABILITY) + 1), true);
		} else {
			ItemStack offHand = inv.getItemInMainHand();
			if (offHand.getType().equals(Material.SHIELD)) {
				damageItem(offHand, damage / (offHand.getEnchantmentLevel(Enchantment.DURABILITY) + 1), true);
			}
		}
	}

	public static void damageItemWithUnbreaking(Plugin plugin, Player player, ItemStack item, int damage, boolean canBreak) {
		//Damages item by chance based on unbreaking level (always damages for no unbreaking)
		//Chance to do damage (Unbreaking 0 = 1 or 100%, Unbreaking 1 = 1/2 or 50%, etc.)
		//Colossal is also an extra 50% chance to not lose durability on top of unbreaking
		//Need to add all enchantments to do with durability here
		double chance = 1.0 / ((item.getEnchantmentLevel(Enchantment.DURABILITY) + 1) * (plugin.mItemStatManager.getInfusionLevel(player, InfusionType.COLOSSAL) + 1));
		double rand = Math.random();
		if (rand < chance) {
			damageItem(item, damage, canBreak);
		}
	}

	public static String[] getBukkitMaterialStringArray() {
		ArrayList<String> strList = new ArrayList<>();
		for (Material m : Material.values()) {
			if (m.isItem()) {
				strList.add(m.toString().toLowerCase(Locale.getDefault()));
			}
		}
		return strList.toArray(new String[0]);
	}

	public static void setPlainTag(ItemStack itemStack) {
		setPlainName(itemStack);
		setPlainLore(itemStack);
	}

	public static Component getRawDisplayName(@Nullable ItemStack itemStack) {
		if (ItemUtils.isNullOrAir(itemStack)) {
			return Component.empty();
		}
		// other method is ambigious between Function/Consumer
		// see: https://github.com/tr7zw/Item-NBT-API/blob/3bb4d7b4493c74911f82ac9226c6be7150629875/item-nbt-api/src/main/java/de/tr7zw/changeme/nbtapi/NBT.java#L54-L74
		return NBT.get(itemStack, nbt -> {
			return getRawDisplayName(nbt);
		});
	}

	// faster than using .getItemMeta.get
	public static Component getRawDisplayName(ReadableNBT nbt) {
		ReadableNBT display = nbt.getCompound(DISPLAY_KEY);
		if (display == null) {
			return Component.empty();
		}
		String json = display.getString(NAME_KEY);
		if (json == null || json.isEmpty()) {
			return Component.empty();
		}
		Component displayNameComponent = MessagingUtils.parseComponent(json);
		return displayNameComponent;
	}

	public static String getRawDisplayNameAsString(ReadableNBT nbt) {
		Component displayName = getRawDisplayName(nbt);
		return toPlainTagText(displayName);
	}

	public static String getRawDisplayNameAsString(ItemStack itemStack) {
		Component displayName = getRawDisplayName(itemStack);
		return toPlainTagText(displayName);
	}

	public static String getPlainName(@Nullable ItemStack itemStack) {
		if (ItemUtils.isNullOrAir(itemStack)) {
			return "";
		}
		String plainName = getPlainNameIfExists(itemStack);
		if (plainName.isBlank()) {
			plainName = getRawDisplayNameAsString(itemStack);
		}
		return plainName;
	}

	public static String getPlainNameIfExists(@Nullable ItemStack itemStack) {
		if (ItemUtils.isNullOrAir(itemStack)) {
			return "";
		}
		return NBT.get(itemStack, nbt -> {
			return getPlainNameIfExists(nbt);
		});
	}

	public static String getPlainNameIfExists(ReadableNBT nbt) {
		return nbt.resolveOrDefault(plainNamePath, "");
	}

	public static String getPlainNameOrDefault(@Nullable ItemStack itemStack) {
		if (ItemUtils.isNullOrAir(itemStack)) {
			return "";
		}
		String name = getPlainNameIfExists(itemStack);
		if (name != null && !name.isEmpty()) {
			return name;
		}
		return MessagingUtils.plainText(GlobalTranslator.render(Component.translatable(itemStack.translationKey()), Locale.ENGLISH));
	}

	public static boolean hasPlainName(@Nullable ItemStack itemStack) {
		if (ItemUtils.isNullOrAir(itemStack)) {
			return false;
		}
		return NBT.get(itemStack, nbt -> {
			ReadableNBT display = nbt.resolveCompound(plainDisplayPath);
			if (display == null) {
				return false;
			}
			return display.hasTag(NAME_KEY);
		});
	}

	public static void setPlainName(ItemStack itemStack) {
		String itemName = null;
		if (itemStack.hasItemMeta()) {
			ItemMeta itemMeta = itemStack.getItemMeta();
			if (itemMeta.hasDisplayName()) {
				itemName = toPlainTagText(itemMeta.displayName());
			}
		}
		setPlainName(itemStack, itemName);
	}

	public static void setPlainName(@Nullable ItemStack itemStack, @Nullable String plainName) {
		if (ItemUtils.isNullOrAir(itemStack)) {
			return;
		}

		NBT.modify(itemStack, nbt -> {
			if (plainName != null) {
				// addComponent effectively runs:
				// if (key exists) { return tag(key) } else { return new tag(key) }
				nbt.getOrCreateCompound(PLAIN_KEY).getOrCreateCompound(DISPLAY_KEY).setString(NAME_KEY, plainName);
			} else {
				ReadWriteNBT plain = nbt.getCompound(PLAIN_KEY);
				if (plain != null) {
					ReadWriteNBT display = plain.getCompound(DISPLAY_KEY);
					if (display != null) {
						display.removeKey(NAME_KEY);
						if (display.getKeys().size() == 0) {
							plain.removeKey(DISPLAY_KEY);
							if (plain.getKeys().size() == 0) {
								nbt.removeKey(PLAIN_KEY);
							}
						}
					}
				}
			}
		});
	}

	public static List<String> getPlainLore(@Nullable ItemStack itemStack) {
		if (ItemUtils.isNullOrAir(itemStack)) {
			return new ArrayList<>(0);
		}
		List<String> plainLore = getPlainLoreIfExists(itemStack);
		if (plainLore.isEmpty() && itemStack.hasItemMeta()) {
			ItemMeta itemMeta = itemStack.getItemMeta();
			if (itemMeta.hasLore()) {
				List<Component> lore = itemMeta.lore();
				if (lore == null) {
					return plainLore;
				}
				for (Component loreLine : lore) {
					plainLore.add(toPlainTagText(loreLine));
				}
			}
		}
		return plainLore;
	}

	public static List<String> getPlainLoreIfExists(@Nullable ItemStack itemStack) {
		if (ItemUtils.isNullOrAir(itemStack)) {
			return new ArrayList<>(0);
		}
		return NBT.get(itemStack, nbt -> {
			ReadableNBT display = nbt.resolveCompound(plainDisplayPath);
			if (display == null) {
				return new ArrayList<>(0);
			}
			ReadableNBTList<String> lore = display.getStringList(LORE_KEY);
			if (lore == null || lore.isEmpty()) {
				return new ArrayList<>(0);
			}
			return lore.toListCopy();
		});
	}

	public static boolean hasPlainLore(@Nullable ItemStack itemStack) {
		if (itemStack == null || itemStack.getType().isAir()) {
			return false;
		}
		return NBT.get(itemStack, nbt -> {
			ReadableNBT display = nbt.resolveCompound(plainDisplayPath);
			if (display == null) {
				return false;
			}
			return display.hasTag(LORE_KEY);
		});
	}

	public static void setPlainComponentLore(ReadWriteNBT nbt, List<Component> lore) {
		List<String> plainLore = new ArrayList<>(lore.size());
		for (Component loreLine : lore) {
			plainLore.add(toPlainTagText(loreLine));
		}
		setPlainLore(nbt, plainLore);
	}

	public static void setPlainLore(ItemStack itemStack) {
		List<String> plainLore = null;
		if (itemStack.hasItemMeta()) {
			ItemMeta itemMeta = itemStack.getItemMeta();
			if (itemMeta.hasLore()) {
				List<Component> lore = itemMeta.lore();
				if (lore != null) {
					plainLore = new ArrayList<>();
					for (Component loreLine : lore) {
						plainLore.add(toPlainTagText(loreLine));
					}
				}
			}
		}
		setPlainLore(itemStack, plainLore);
	}

	public static void setPlainLore(ItemStack itemStack, @Nullable List<String> plainLore) {
		NBT.modify(itemStack, nbt -> {
			setPlainLore(nbt, plainLore);
		});
	}

	public static void setPlainLore(ReadWriteNBT nbt, @Nullable List<String> plainLore) {
		if (plainLore != null && plainLore.size() > 0) {
			// addComponent effectively runs:
			// if (key exists) { return tag(key) } else { return new tag(key) }
			ReadWriteNBTList<String> loreList = nbt.getOrCreateCompound(PLAIN_KEY).getOrCreateCompound(DISPLAY_KEY).getStringList(LORE_KEY);
			if (plainLore.size() == loreList.size()) {
				for (int i = 0; i < plainLore.size(); i++) {
					String oldLine = loreList.get(i);
					String newLine = plainLore.get(i);
					if (!oldLine.equals(newLine)) {
						loreList.set(i, newLine);
					}
				}
			} else {
				loreList.clear();
				loreList.addAll(plainLore);
			}
		} else {
			ReadWriteNBT plain = nbt.getCompound(PLAIN_KEY);
			if (plain != null) {
				ReadWriteNBT display = plain.getCompound(DISPLAY_KEY);
				if (display != null) {
					display.removeKey(LORE_KEY);
					if (display.getKeys().size() == 0) {
						plain.removeKey(DISPLAY_KEY);
						if (plain.getKeys().size() == 0) {
							nbt.removeKey(PLAIN_KEY);
						}
					}
				}
			}
		}
	}

	public static void setDisplayLore(ItemStack item, List<Component> lore) {
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}
		NBT.modify(item, nbt -> {
			setDisplayLore(nbt, lore);
		});
	}

	public static void setDisplayLore(ReadWriteNBT nbt, List<Component> lore) {
		ReadWriteNBT display = nbt.getOrCreateCompound(DISPLAY_KEY);
		ReadWriteNBTList<String> internalLore = display.getStringList(LORE_KEY);
		internalLore.clear();
		if (lore.isEmpty()) {
			ItemUpdateHelper.cleanEmptyTags(nbt, DISPLAY_KEY);
			return;
		}
		for (Component line : lore) {
			String newLine = MessagingUtils.serializeComponent(line);
			internalLore.add(newLine);
		}
	}

	public static List<Component> getDisplayLore(ItemStack item) {
		if (ItemUtils.isNullOrAir(item)) {
			return new ArrayList<>(0);
		}
		return NBT.get(item, nbt -> {
			return getDisplayLore(nbt);
		});
	}

	public static List<Component> getDisplayLore(ReadableNBT nbt) {
		ReadableNBT display = nbt.getCompound("display");
		List<Component> lore = new ArrayList<>();
		if (display == null) {
			return lore;
		}
		ReadableNBTList<String> internalLore = display.getStringList(LORE_KEY);
		if (internalLore == null) {
			return lore;
		}
		for (String line : internalLore) {
			lore.add(MessagingUtils.fromGson(line));
		}
		return lore;
	}

	public static String toPlainTagText(String formattedText) {
		return toPlainTagText(Component.text(formattedText));
	}

	public static String toPlainTagText(Component formattedText) {
		if (formattedText.equals(Component.empty())) {
			return "";
		}
		String plainText = MessagingUtils.plainText(formattedText);
		return NON_PLAIN_REGEX.matcher(plainText).replaceAll("").trim();
	}

	public static boolean isArmor(@Nullable ItemStack itemStack) {
		if (itemStack != null) {
			return Materials.ARMOR.contains(itemStack.getType());
		} else {
			return false;
		}
	}

	public static boolean isWearable(@Nullable ItemStack itemStack) {
		if (itemStack != null) {
			return Materials.WEARABLE.contains(itemStack.getType());
		} else {
			return false;
		}
	}

	public static boolean isArmorOrWearable(@Nullable ItemStack itemStack) {
		return isWearable(itemStack) || isArmor(itemStack);
	}

	public static boolean isSword(@Nullable ItemStack itemStack) {
		if (itemStack != null) {
			return Materials.SWORDS.contains(itemStack.getType());
		} else {
			return false;
		}
	}

	public static boolean isSomeBow(@Nullable ItemStack itemStack) {
		if (itemStack != null) {
			return Materials.BOWS.contains(itemStack.getType());
		} else {
			return false;
		}
	}

	//Does not include riptide tridents
	public static boolean isBowOrTrident(@Nullable ItemStack itemStack) {
		if (itemStack != null) {
			return Materials.BOWS.contains(itemStack.getType()) || (itemStack.getType() == Material.TRIDENT && ItemStatUtils.getEnchantmentLevel(itemStack, EnchantmentType.RIPTIDE) == 0);
		} else {
			return false;
		}
	}

	public static boolean isProjectileWeapon(@Nullable ItemStack itemStack) {
		if (itemStack != null) {
			return isBowOrTrident(itemStack)
				       || (itemStack.getType() == Material.SNOWBALL
					           && ItemStatUtils.getAttributeAmount(itemStack, AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) > 0);
		} else {
			return false;
		}
	}

	/*
	 * Does not count shattered hoes.
	 */
	public static boolean isHoe(@Nullable ItemStack itemStack) {
		if (itemStack != null) {
			return Materials.HOES.contains(itemStack.getType());
		} else {
			return false;
		}
	}

	public static boolean isWand(@Nullable ItemStack itemStack) {
		return ItemStatUtils.getEnchantmentLevel(itemStack, EnchantmentType.MAGIC_WAND) > 0;
	}

	public static boolean isAlchemistItem(@Nullable ItemStack itemStack) {
		return ItemStatUtils.getEnchantmentLevel(itemStack, EnchantmentType.ALCHEMICAL_ALEMBIC) > 0;
	}

	public static boolean isPickaxe(@Nullable ItemStack itemStack) {
		if (itemStack != null) {
			return Materials.PICKAXES.contains(itemStack.getType());
		} else {
			return false;
		}
	}

	public static boolean isAxe(@Nullable ItemStack itemStack) {
		return itemStack != null && Materials.AXES.contains(itemStack.getType());
	}

	public static boolean isShovel(@Nullable ItemStack itemStack) {
		return itemStack != null && Materials.SHOVELS.contains(itemStack.getType());
	}

	public static boolean isSomePotion(@Nullable ItemStack itemStack) {
		return itemStack != null && isSomePotion(itemStack.getType());
	}

	public static boolean isSomePotion(Material material) {
		return Materials.POTIONS.contains(material);
	}

	public static boolean isArrow(@Nullable ItemStack itemStack) {
		return itemStack != null && Materials.ARROWS.contains(itemStack.getType());
	}

	/**
	 * Checks if the item would have default/vanilla attributes applied to it
	 * Used to apply a placeholder empty attribute tag if necessary
	 * Source: https://minecraft.fandom.com/wiki/Attribute#Vanilla_modifiers
	 */
	public static boolean hasDefaultAttributes(@Nullable ItemStack itemStack) {
		return itemStack != null && DEFAULT_ATTRIBUTE_MATERIALS.contains(itemStack.getType());
	}

	public static int getMiningSpeed(@Nullable ItemStack itemStack) {
		if (itemStack == null) {
			return 1;
		}
		int baseSpeed = switch (itemStack.getType()) {
			case WOODEN_PICKAXE, WOODEN_AXE, WOODEN_SHOVEL -> 2;
			case STONE_PICKAXE, STONE_AXE, STONE_SHOVEL -> 4;
			case IRON_PICKAXE, IRON_AXE, IRON_SHOVEL -> 6;
			case DIAMOND_PICKAXE, DIAMOND_AXE, DIAMOND_SHOVEL -> 8;
			case NETHERITE_PICKAXE, NETHERITE_AXE, NETHERITE_SHOVEL -> 9;
			case GOLDEN_PICKAXE, GOLDEN_AXE, GOLDEN_SHOVEL -> 12;
			default -> 1;
		};
		if (baseSpeed == 1) {
			return 1;
		}
		int efficiency = ItemStatUtils.getEnchantmentLevel(itemStack, EnchantmentType.EFFICIENCY);
		return baseSpeed + (efficiency > 0 ? efficiency * efficiency + 1 : 0);
	}

	public static boolean isBanner(@Nullable ItemStack itemStack) {
		if (itemStack == null) {
			return false;
		}
		return switch (itemStack.getType()) {
			case BLACK_BANNER, BLACK_WALL_BANNER, BLUE_BANNER, BLUE_WALL_BANNER, BROWN_BANNER, BROWN_WALL_BANNER, CYAN_BANNER, CYAN_WALL_BANNER,
				     GRAY_BANNER, GRAY_WALL_BANNER, GREEN_BANNER, GREEN_WALL_BANNER, LIGHT_BLUE_BANNER, LIGHT_BLUE_WALL_BANNER,
				     LIGHT_GRAY_BANNER, LIGHT_GRAY_WALL_BANNER, LIME_BANNER, LIME_WALL_BANNER, MAGENTA_BANNER, MAGENTA_WALL_BANNER,
				     ORANGE_BANNER, ORANGE_WALL_BANNER, PINK_BANNER, PINK_WALL_BANNER, PURPLE_BANNER, PURPLE_WALL_BANNER,
				     RED_BANNER, RED_WALL_BANNER, WHITE_BANNER, WHITE_WALL_BANNER, YELLOW_BANNER, YELLOW_WALL_BANNER ->
				true;
			default -> false;
		};
	}

	public static @Nullable Material toFloorBanner(Material banner) {
		return BANNER_TO_FLOOR_BANNER.get(banner);
	}

	public static @Nullable Material toWallBanner(Material banner) {
		return BANNER_TO_WALL_BANNER.get(banner);
	}

	public static boolean hasLore(ItemStack item) {
		return item.hasItemMeta() && item.getItemMeta().hasLore();
	}

	public static boolean isInteresting(ItemStack item) {
		return ServerProperties.getAlwaysPickupMats().contains(item.getType())
			       || (hasLore(item) && ItemStatUtils.getTier(item) != Tier.ZERO)
			       || (item.hasItemMeta() && item.getItemMeta().hasDisplayName() && ServerProperties.getNamedPickupMats().contains(item.getType()));
	}

	/**
	 * Properly clones an item stack. The default clone method does not completely clone all NBT, which can lead to issues.
	 *
	 * @param itemStack The item stack to be cloned, may be null
	 * @return A completely new item stack that is a clone of the original item stack
	 */
	@Contract("!null -> !null")
	public static @Nullable ItemStack clone(@Nullable ItemStack itemStack) {
		if (itemStack == null) {
			return null;
		}

		// NBT doesn't support arbitrary item stacks amounts, so we need to handle this ourselves
		int amount = itemStack.getAmount();
		if (amount < 0 || amount > 64) {
			try {
				itemStack.setAmount(1);
				ItemStack clone = NBT.itemStackFromNBT(NBT.itemStackToNBT(itemStack));
				if (clone == null) {
					return null;
				}
				clone.setAmount(amount);
				return clone;
			} finally {
				itemStack.setAmount(amount);
			}
		}

		return NBT.itemStackFromNBT(NBT.itemStackToNBT(itemStack));
	}

	/**
	 * Parses an item stack from an NBT Mojangson string (format {"id": "...", "count": x, "tag": {...}})
	 */
	public static ItemStack parseItemStack(String nbtMojangson) {
		return NBT.itemStackFromNBT(NBT.parseNBT(nbtMojangson));
	}

	public static String serializeItemStack(ItemStack itemStack) {
		return NBT.itemStackToNBT(itemStack).toString();
	}

	/**
	 * Gets the entity type of a spawn egg by material, which is the default entity the egg will spawn unless overridden in the NBT tags.
	 * Thus, use the more accurate {@link #getSpawnEggType(ItemStack) item stack check} instead if possible.
	 */
	public static EntityType getSpawnEggType(Material material) {
		return switch (material) {
			case ALLAY_SPAWN_EGG -> EntityType.ALLAY;
			case AXOLOTL_SPAWN_EGG -> EntityType.AXOLOTL;
			case BAT_SPAWN_EGG -> EntityType.BAT;
			case BEE_SPAWN_EGG -> EntityType.BEE;
			case BLAZE_SPAWN_EGG -> EntityType.BLAZE;
			case CAT_SPAWN_EGG -> EntityType.CAT;
			case CAVE_SPIDER_SPAWN_EGG -> EntityType.CAVE_SPIDER;
			case CHICKEN_SPAWN_EGG -> EntityType.CHICKEN;
			case COD_SPAWN_EGG -> EntityType.COD;
			case COW_SPAWN_EGG -> EntityType.COW;
			case CREEPER_SPAWN_EGG -> EntityType.CREEPER;
			case DOLPHIN_SPAWN_EGG -> EntityType.DOLPHIN;
			case DONKEY_SPAWN_EGG -> EntityType.DONKEY;
			case DROWNED_SPAWN_EGG -> EntityType.DROWNED;
			case ELDER_GUARDIAN_SPAWN_EGG -> EntityType.ELDER_GUARDIAN;
			case ENDERMAN_SPAWN_EGG -> EntityType.ENDERMAN;
			case ENDERMITE_SPAWN_EGG -> EntityType.ENDERMITE;
			case EVOKER_SPAWN_EGG -> EntityType.EVOKER;
			case FOX_SPAWN_EGG -> EntityType.FOX;
			case FROG_SPAWN_EGG -> EntityType.FROG;
			case GHAST_SPAWN_EGG -> EntityType.GHAST;
			case GLOW_SQUID_SPAWN_EGG -> EntityType.GLOW_SQUID;
			case GOAT_SPAWN_EGG -> EntityType.GOAT;
			case GUARDIAN_SPAWN_EGG -> EntityType.GUARDIAN;
			case HOGLIN_SPAWN_EGG -> EntityType.HOGLIN;
			case HORSE_SPAWN_EGG -> EntityType.HORSE;
			case HUSK_SPAWN_EGG -> EntityType.HUSK;
			case LLAMA_SPAWN_EGG -> EntityType.LLAMA;
			case MAGMA_CUBE_SPAWN_EGG -> EntityType.MAGMA_CUBE;
			case MOOSHROOM_SPAWN_EGG -> EntityType.MUSHROOM_COW;
			case MULE_SPAWN_EGG -> EntityType.MULE;
			case OCELOT_SPAWN_EGG -> EntityType.OCELOT;
			case PANDA_SPAWN_EGG -> EntityType.PANDA;
			case PARROT_SPAWN_EGG -> EntityType.PARROT;
			case PHANTOM_SPAWN_EGG -> EntityType.PHANTOM;
			case PIG_SPAWN_EGG -> EntityType.PIG;
			case PIGLIN_SPAWN_EGG -> EntityType.PIGLIN;
			case PIGLIN_BRUTE_SPAWN_EGG -> EntityType.PIGLIN_BRUTE;
			case PILLAGER_SPAWN_EGG -> EntityType.PILLAGER;
			case POLAR_BEAR_SPAWN_EGG -> EntityType.POLAR_BEAR;
			case PUFFERFISH_SPAWN_EGG -> EntityType.PUFFERFISH;
			case RABBIT_SPAWN_EGG -> EntityType.RABBIT;
			case RAVAGER_SPAWN_EGG -> EntityType.RAVAGER;
			case SALMON_SPAWN_EGG -> EntityType.SALMON;
			case SHEEP_SPAWN_EGG -> EntityType.SHEEP;
			case SHULKER_SPAWN_EGG -> EntityType.SHULKER;
			case SILVERFISH_SPAWN_EGG -> EntityType.SILVERFISH;
			case SKELETON_SPAWN_EGG -> EntityType.SKELETON;
			case SKELETON_HORSE_SPAWN_EGG -> EntityType.SKELETON_HORSE;
			case SLIME_SPAWN_EGG -> EntityType.SLIME;
			case SPIDER_SPAWN_EGG -> EntityType.SPIDER;
			case SQUID_SPAWN_EGG -> EntityType.SQUID;
			case STRAY_SPAWN_EGG -> EntityType.STRAY;
			case STRIDER_SPAWN_EGG -> EntityType.STRIDER;
			case TADPOLE_SPAWN_EGG -> EntityType.TADPOLE;
			case TRADER_LLAMA_SPAWN_EGG -> EntityType.TRADER_LLAMA;
			case TROPICAL_FISH_SPAWN_EGG -> EntityType.TROPICAL_FISH;
			case TURTLE_SPAWN_EGG -> EntityType.TURTLE;
			case VEX_SPAWN_EGG -> EntityType.VEX;
			case VILLAGER_SPAWN_EGG -> EntityType.VILLAGER;
			case VINDICATOR_SPAWN_EGG -> EntityType.VINDICATOR;
			case WANDERING_TRADER_SPAWN_EGG -> EntityType.WANDERING_TRADER;
			case WARDEN_SPAWN_EGG -> EntityType.WARDEN;
			case WITCH_SPAWN_EGG -> EntityType.WITCH;
			case WITHER_SKELETON_SPAWN_EGG -> EntityType.WITHER_SKELETON;
			case WOLF_SPAWN_EGG -> EntityType.WOLF;
			case ZOGLIN_SPAWN_EGG -> EntityType.ZOGLIN;
			case ZOMBIE_SPAWN_EGG -> EntityType.ZOMBIE;
			case ZOMBIE_HORSE_SPAWN_EGG -> EntityType.ZOMBIE_HORSE;
			case ZOMBIE_VILLAGER_SPAWN_EGG -> EntityType.ZOMBIE_VILLAGER;
			case ZOMBIFIED_PIGLIN_SPAWN_EGG -> EntityType.ZOMBIFIED_PIGLIN;
			default -> EntityType.UNKNOWN;
		};
	}

	/**
	 * Gets the entity type of a spawn egg. THis is more accurate than the {@link #getSpawnEggType(Material) material-only check},
	 * as a spawn egg can spawn mobs of a different type if the NBT tag {@code EntityTag.id} is set.
	 */
	public static EntityType getSpawnEggType(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return EntityType.UNKNOWN;
		}

		return NBT.get(item, nbt -> {
			ReadableNBT entityTag = nbt.getCompound("EntityTag");
			if (entityTag == null) {
				return getSpawnEggType(item.getType());
			}
			String id = entityTag.getString("id");
			if (id == null || id.isEmpty()) {
				return getSpawnEggType(item.getType());
			}
			NamespacedKey key = NamespacedKey.fromString(id);
			if (key == null) {
				return getSpawnEggType(item.getType());
			}
			for (EntityType entityType : EntityType.values()) {
				if (key.equals(entityType.getKey())) {
					return entityType;
				}
			}
			return getSpawnEggType(item.getType());
		});
	}

	public static ItemType getItemType(ItemStack item) {
		return ItemType.of(item);
	}

	public static String getGiveCommand(ItemStack item) {
		if (item == null) {
			return "/tellraw @s {\"text\":\"Item is null\"}";
		}

		if (item.getType() == Material.AIR) {
			return "/tellraw @s {\"text\":\"Item is air\"}";
		}

		if (item.getAmount() <= 0) {
			return "/tellraw @s {\"text\":\"Item has invalid count\"}";
		}

		String itemId = item.getType().getKey().asString();

		return NBT.get(item, nbt -> {
			return "/give @s " + itemId + nbt.toString() + " " + item.getAmount();
		});
	}

	/**
	 * Gets the plain display name of the given item, or the default name of the material if no custom name is set.
	 *
	 * @see #getDisplayName(ItemStack)
	 */
	public static Component getPlainNameComponent(ItemStack item) {
		if (hasPlainName(item)) {
			return Component.text(getPlainNameIfExists(item));
		} else {
			return Component.translatable(item.getType().translationKey());
		}
	}

	public static Component getPlainNameComponentWithHover(ItemStack item) {
		ItemStack clone = item.clone();
		if (clone.getItemMeta() instanceof BookMeta book) {
			book.pages(Collections.emptyList());
			clone.setItemMeta(book);
		} else if (clone.getItemMeta() instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof ShulkerBox shulker) {
			shulker.getInventory().clear();
			blockStateMeta.setBlockState(shulker);
			clone.setItemMeta(blockStateMeta);
		}
		return getPlainNameComponent(item).hoverEvent(clone.asHoverEvent());
	}

	/**
	 * Gets the (styled) display name of the given item, or the default name of the item if no custom name is set.
	 *
	 * @see #getPlainNameComponent(ItemStack)
	 */
	public static Component getDisplayName(ItemStack item) {
		return NmsUtils.getVersionAdapter().getDisplayName(item);
	}

	public static ItemStack createBanner(Material material, org.bukkit.block.banner.Pattern... patterns) {
		return createBanner(material, null, patterns);
	}

	public static ItemStack createBanner(Material material, @Nullable List<Component> lore, org.bukkit.block.banner.Pattern... patterns) {
		ItemStack banner = new ItemStack(material);
		ItemMeta itemMeta = banner.getItemMeta();
		if (itemMeta instanceof BannerMeta bannerMeta) {
			for (org.bukkit.block.banner.Pattern pattern : patterns) {
				bannerMeta.addPattern(pattern);
			}
			// Change lore:
			bannerMeta.lore(lore);
			// Hide patterns:
			bannerMeta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS); // banner patterns are actually the same 'data' as potion effects, lmao
			// Finalize:
			banner.setItemMeta(bannerMeta);
		}
		return banner;
	}

	public static void setName(ItemStack item, TextComponent name) {
		ItemMeta meta = item.getItemMeta();
		meta.displayName(name);
		item.setItemMeta(meta);
	}

	public static void setLore(ItemStack item, List<Component> lore) {
		ItemMeta meta = item.getItemMeta();
		meta.lore(lore);
		item.setItemMeta(meta);
	}

	/**
	 * Identifies a Monumenta item, ignoring Masterwork level.
	 * Items are usually identified via type + plain name, but a few items can change base type
	 * (e.g. dyeing shulker or due to Multitool) and are still considered the same item.
	 * Whether this is taken into account or not depends on the value of the 'normalized' parameter various methods take.
	 */
	public static final class ItemIdentifier {
		public final Material mType;
		public final @Nullable String mName;

		public ItemIdentifier(@Nullable Material type, @Nullable String name) {
			mType = type == null ? Material.AIR : type;
			mName = name != null && name.isEmpty() ? null : name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ItemIdentifier that = (ItemIdentifier) o;
			return mType == that.mType && Objects.equals(mName, that.mName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(mType, mName);
		}

		public boolean isIdentifierFor(ItemStack item, boolean normalized) {
			if (normalized) {
				ItemIdentifier otherId = getIdentifier(item, true);
				return equals(otherId) || normalized(() -> ItemStatUtils.hasEnchantment(item, EnchantmentType.MULTITOOL)).equals(otherId);
			} else {
				return equals(getIdentifier(item, false));
			}
		}

		public ItemIdentifier normalized(BooleanSupplier isMultitool) {
			if (isShulkerBox(mType)) {
				return new ItemIdentifier(Material.SHULKER_BOX, mName);
			}
			if (Multitool.isValidMultitoolMaterial(mType) && isMultitool.getAsBoolean()) {
				return new ItemIdentifier(Multitool.getBaseMaterial(mType), mName);
			}
			return this;
		}

		public Component getDisplayName() {
			if (mName != null) {
				return Component.text(mName).decoration(TextDecoration.ITALIC, false);
			}
			return Component.translatable(mType.translationKey());
		}
	}

	public static ItemIdentifier getIdentifier(@Nullable ItemStack item, boolean normalized) {
		if (item != null) {
			ItemIdentifier id = new ItemIdentifier(item.getType(), getPlainNameIfExists(item));
			if (normalized) {
				return id.normalized(() -> ItemStatUtils.hasEnchantment(item, EnchantmentType.MULTITOOL));
			}
			return id;
		} else {
			return new ItemIdentifier(null, null);
		}
	}

	public static ItemStack modifyMeta(ItemStack item, Consumer<ItemMeta> metaFunction) {
		ItemMeta itemMeta = item.getItemMeta();
		if (itemMeta != null) {
			metaFunction.accept(itemMeta);
			item.setItemMeta(itemMeta);
		}
		return item;
	}

	public static boolean equalIgnoringDurability(@Nullable ItemStack item1, @Nullable ItemStack item2) {
		if (item1 == null) {
			return item2 == null;
		}
		if (item2 == null || item1.getType() != item2.getType()) {
			return false;
		}
		if (!(item2.getItemMeta() instanceof Damageable damageable2)) {
			return item1.equals(item2);
		}

		ItemStack clone1 = clone(item1);
		if (clone1.getItemMeta() instanceof Damageable damageable1) {
			damageable1.setDamage(damageable2.getDamage());
			clone1.setItemMeta(damageable1);
		}
		return clone1.equals(item2);
	}

	public static boolean isPortableEnder(@Nullable ItemStack item) {
		if (item == null || !isShulkerBox(item.getType())) {
			return false;
		}
		String name = getPlainNameIfExists(item);
		return "Remnant of the Rose".equals(name) || "Bottomless Present".equals(name);
	}

	public static boolean hasPortableEnder(Player player) {
		return Arrays.stream(player.getInventory().getContents()).anyMatch(ItemUtils::isPortableEnder);
	}

	public static boolean hasPortableEnderOrIsNearEnderChest(Player player) {
		return hasPortableEnder(player) || LocationUtils.hasNearbyBlock(player.getLocation(), 5, Material.ENDER_CHEST);
	}

	public static @Nullable String getContainerLock(ReadableNBT nbt) {
		ReadableNBT blockEntityTag = nbt.getCompound("BlockEntityTag");
		if (blockEntityTag == null) {
			return null;
		}

		String lock = blockEntityTag.getString("Lock");
		if (lock == null || lock.isEmpty()) {
			return null;
		}

		return lock;
	}

	public static @Nullable ReadableNBTList<String> getPages(ItemStack itemStack) {
		ReadWriteNBT nbtTags = NBT.itemStackToNBT(itemStack).getCompound("tag");

		if (nbtTags == null) {
			return null;
		}

		ReadableNBTList<String> pages = nbtTags.getStringList("pages");
		if (pages == null) {
			return null;
		}

		return pages;
	}

	public static @Nullable List<Component> getSignContents(ItemStack item) {
		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (!meta.hasDisplayName()) {
				if (ItemUtils.isSign(item.getType()) && meta instanceof BlockStateMeta blockStateMeta) {
					BlockState blockState = blockStateMeta.getBlockState();
					if (blockState instanceof Sign signItem) {
						return signItem.lines();
					}
				}
			}
		}
		return null;
	}

	public static @Nullable ReadableNBTList<ReadWriteNBT> getContainerItems(ItemStack itemStack) {
		ReadWriteNBT nbtTags = NBT.itemStackToNBT(itemStack).getCompound("tag");
		if (nbtTags == null) {
			return null;
		}
		return getContainerItems(nbtTags);
	}

	public static @Nullable ReadableNBTList<ReadWriteNBT> getContainerItems(ReadableNBT nbt) {
		ReadableNBT blockEntityTag = nbt.getCompound("BlockEntityTag");
		if (blockEntityTag == null) {
			return null;
		}

		ReadableNBTList<ReadWriteNBT> itemStacks = blockEntityTag.getCompoundList("Items");
		return itemStacks;
	}

	public static void addPlainLoreLine(ItemStack item, String line) {
		List<String> lore = getPlainLore(item);
		lore.add(line);
		setPlainLore(item, lore);
	}

	public static void setSnowballItem(ThrowableProjectile snowball, ItemStack oldItem) {
		ItemStack newItem = oldItem.clone();
		setPlainTag(newItem);
		addPlainLoreLine(newItem, "ThrownSnowball");
		snowball.setItem(newItem);
	}

	public static void setMapId(MapMeta meta, int id) {
		meta.setMapView(Bukkit.getMap(id));
	}

	public static List<Component> getNonNullLoreCopy(ItemStack item) {
		List<Component> lore = item.lore();
		if (lore == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(lore);
	}

}
