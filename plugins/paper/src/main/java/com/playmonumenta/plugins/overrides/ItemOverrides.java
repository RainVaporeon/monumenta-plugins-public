package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import de.tr7zw.nbtapi.NBTItem;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemOverrides {
	/*
	 * Exceptions for materials that players in survival can place,
	 * or use to perform right-click interactions like
	 * tilling/stripping/making paths, even when the item has lore.
	 */
	public static final EnumSet<Material> EXCEPTION_LORED_MATERIALS = EnumSet.of(
		Material.ANVIL,
		Material.CHEST,
		Material.FLINT_AND_STEEL,
		Material.ENDER_CHEST,

		Material.WHITE_BANNER,
		Material.ORANGE_BANNER,
		Material.MAGENTA_BANNER,
		Material.LIGHT_BLUE_BANNER,
		Material.YELLOW_BANNER,
		Material.LIME_BANNER,
		Material.PINK_BANNER,
		Material.GRAY_BANNER,
		Material.LIGHT_GRAY_BANNER,
		Material.CYAN_BANNER,
		Material.PURPLE_BANNER,
		Material.BLUE_BANNER,
		Material.BROWN_BANNER,
		Material.GREEN_BANNER,
		Material.RED_BANNER,
		Material.BLACK_BANNER,

		Material.WHITE_WOOL,
		Material.ORANGE_WOOL,
		Material.MAGENTA_WOOL,
		Material.LIGHT_BLUE_WOOL,
		Material.YELLOW_WOOL,
		Material.LIME_WOOL,
		Material.PINK_WOOL,
		Material.GRAY_WOOL,
		Material.LIGHT_GRAY_WOOL,
		Material.CYAN_WOOL,
		Material.PURPLE_WOOL,
		Material.BLUE_WOOL,
		Material.BROWN_WOOL,
		Material.GREEN_WOOL,
		Material.RED_WOOL,
		Material.BLACK_WOOL,

		Material.ACACIA_SIGN,
		Material.ACACIA_WALL_SIGN,
		Material.BIRCH_SIGN,
		Material.BIRCH_WALL_SIGN,
		Material.CRIMSON_SIGN,
		Material.CRIMSON_WALL_SIGN,
		Material.DARK_OAK_SIGN,
		Material.DARK_OAK_WALL_SIGN,
		Material.JUNGLE_SIGN,
		Material.JUNGLE_WALL_SIGN,
		Material.OAK_SIGN,
		Material.OAK_WALL_SIGN,
		Material.SPRUCE_SIGN,
		Material.SPRUCE_WALL_SIGN,
		Material.WARPED_SIGN,
		Material.WARPED_WALL_SIGN,
		Material.MANGROVE_SIGN,
		Material.MANGROVE_WALL_SIGN,
		Material.CHERRY_SIGN,
		Material.CHERRY_WALL_SIGN
	);
	Map<Material, BaseOverride> mItems = new EnumMap<Material, BaseOverride>(Material.class);

	public ItemOverrides() {
		registerOverrides();
	}

	public void registerOverrides() {
		BaseOverride monsterEggOverride = new MonsterEggOverride();
		mItems.put(Material.ALLAY_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.AXOLOTL_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.BAT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.BEE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.BLAZE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.CAT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.CAVE_SPIDER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.CHICKEN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.COD_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.COW_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.CREEPER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.DOLPHIN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.DONKEY_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.DROWNED_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ELDER_GUARDIAN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ENDERMAN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ENDERMITE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.EVOKER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.FROG_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.FOX_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.GHAST_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.GLOW_SQUID_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.GOAT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.GUARDIAN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.HOGLIN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.HORSE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.HUSK_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.LLAMA_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.MAGMA_CUBE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.MOOSHROOM_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.MULE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.OCELOT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PANDA_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PARROT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PHANTOM_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PIG_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PIGLIN_BRUTE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PIGLIN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PILLAGER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.POLAR_BEAR_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.PUFFERFISH_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.RABBIT_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.RAVAGER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SALMON_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SHEEP_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SHULKER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SILVERFISH_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SKELETON_HORSE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SKELETON_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SLIME_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SPIDER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.SQUID_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.STRAY_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.STRIDER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.TADPOLE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.TRADER_LLAMA_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.TROPICAL_FISH_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.TURTLE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.VEX_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.VILLAGER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.VINDICATOR_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WANDERING_TRADER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WARDEN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WITCH_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WITHER_SKELETON_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.WOLF_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOGLIN_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIE_HORSE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIE_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIE_VILLAGER_SPAWN_EGG, monsterEggOverride);
		mItems.put(Material.ZOMBIFIED_PIGLIN_SPAWN_EGG, monsterEggOverride);

		BaseOverride bedOverride = new BedOverride();
		mItems.put(Material.WHITE_BED, bedOverride);
		mItems.put(Material.ORANGE_BED, bedOverride);
		mItems.put(Material.MAGENTA_BED, bedOverride);
		mItems.put(Material.LIGHT_BLUE_BED, bedOverride);
		mItems.put(Material.YELLOW_BED, bedOverride);
		mItems.put(Material.LIME_BED, bedOverride);
		mItems.put(Material.PINK_BED, bedOverride);
		mItems.put(Material.GRAY_BED, bedOverride);
		mItems.put(Material.LIGHT_GRAY_BED, bedOverride);
		mItems.put(Material.CYAN_BED, bedOverride);
		mItems.put(Material.PURPLE_BED, bedOverride);
		mItems.put(Material.BLUE_BED, bedOverride);
		mItems.put(Material.BROWN_BED, bedOverride);
		mItems.put(Material.GREEN_BED, bedOverride);
		mItems.put(Material.RED_BED, bedOverride);
		mItems.put(Material.BLACK_BED, bedOverride);

		RespawnAnchorOverride respawnAnchorOverride = new RespawnAnchorOverride();
		mItems.put(Material.RESPAWN_ANCHOR, respawnAnchorOverride);

		BaseOverride boatOverride = new BoatOverride();
		mItems.put(Material.OAK_BOAT, boatOverride);
		mItems.put(Material.OAK_CHEST_BOAT, boatOverride);
		mItems.put(Material.ACACIA_BOAT, boatOverride);
		mItems.put(Material.ACACIA_CHEST_BOAT, boatOverride);
		mItems.put(Material.BIRCH_BOAT, boatOverride);
		mItems.put(Material.BIRCH_CHEST_BOAT, boatOverride);
		mItems.put(Material.DARK_OAK_BOAT, boatOverride);
		mItems.put(Material.DARK_OAK_CHEST_BOAT, boatOverride);
		mItems.put(Material.JUNGLE_BOAT, boatOverride);
		mItems.put(Material.JUNGLE_CHEST_BOAT, boatOverride);
		mItems.put(Material.SPRUCE_BOAT, boatOverride);
		mItems.put(Material.SPRUCE_CHEST_BOAT, boatOverride);
		mItems.put(Material.CHERRY_BOAT, boatOverride);
		mItems.put(Material.CHERRY_CHEST_BOAT, boatOverride);
		mItems.put(Material.MANGROVE_BOAT, boatOverride);
		mItems.put(Material.MANGROVE_CHEST_BOAT, boatOverride);


		BaseOverride minecartOverride = new MinecartOverride();
		mItems.put(Material.MINECART, minecartOverride);
		mItems.put(Material.CHEST_MINECART, minecartOverride);
		mItems.put(Material.COMMAND_BLOCK_MINECART, minecartOverride);
		mItems.put(Material.FURNACE_MINECART, minecartOverride);
		mItems.put(Material.HOPPER_MINECART, minecartOverride);
		mItems.put(Material.TNT_MINECART, minecartOverride);

		BaseOverride kelpOverride = new KelpOverride();
		mItems.put(Material.KELP, kelpOverride);
		mItems.put(Material.SEAGRASS, kelpOverride);

		mItems.put(Material.COMPOSTER, new ComposterOverride());
		mItems.put(Material.CONDUIT, new ConduitOverride());
		mItems.put(Material.GRINDSTONE, new GrindstoneOverride());
		mItems.put(Material.FILLED_MAP, new MapOverride());
		mItems.put(Material.PUMPKIN_PIE, new PumpkinPieOverride());

		BaseOverride bucketOverride = new BucketOverride();
		mItems.put(Material.BUCKET, bucketOverride);
		mItems.put(Material.WATER_BUCKET, bucketOverride);
		mItems.put(Material.LAVA_BUCKET, bucketOverride);
		mItems.put(Material.POWDER_SNOW_BUCKET, bucketOverride);
		mItems.put(Material.COD_BUCKET, bucketOverride);
		mItems.put(Material.MILK_BUCKET, bucketOverride);
		mItems.put(Material.PUFFERFISH_BUCKET, bucketOverride);
		mItems.put(Material.SALMON_BUCKET, bucketOverride);
		mItems.put(Material.TROPICAL_FISH_BUCKET, bucketOverride);
		mItems.put(Material.AXOLOTL_BUCKET, bucketOverride);
		mItems.put(Material.TADPOLE_BUCKET, bucketOverride);

		BaseOverride chestOverride = new ChestOverride();
		mItems.put(Material.CHEST, chestOverride);
		mItems.put(Material.TRAPPED_CHEST, chestOverride);

		mItems.put(Material.YELLOW_STAINED_GLASS, new YellowTesseractOverride());
		mItems.put(Material.LIME_STAINED_GLASS, new LimeTesseractOverride());
		mItems.put(Material.ICE, new FestiveTesseractOverride());

		mItems.put(InflationOverride.itemMaterial, new InflationOverride());

		BaseOverride anvilOverride = new AnvilOverride();
		mItems.put(Material.ANVIL, anvilOverride);
		mItems.put(Material.CHIPPED_ANVIL, anvilOverride);
		mItems.put(Material.DAMAGED_ANVIL, anvilOverride);

		mItems.put(Material.FISHING_ROD, new FishingRodOverride());
		mItems.put(Material.ENCHANTING_TABLE, new EnchantmentTableOverride());
		mItems.put(Material.ENDER_CHEST, new EnderChestOverride());
		mItems.put(Material.FARMLAND, new FarmlandOverride());
		mItems.put(Material.PACKED_ICE, new PackedIceOverride());
		mItems.put(Material.FIREWORK_ROCKET, new FireworkOverride());
		mItems.put(Material.HOPPER, new HopperOverride());
		mItems.put(Material.MAGMA_BLOCK, new MagmaOverride());
		mItems.put(Material.BEACON, new BeaconOverride());
		mItems.put(Material.TRIDENT, new TridentOverride());
		mItems.put(Material.BONE, new BoneOverride());
		mItems.put(Material.COOKED_SALMON, new FishOverride());
		mItems.put(Material.COOKED_COD, new FishOverride());
		mItems.put(Material.COD, new FishOverride());
		mItems.put(Material.SALMON, new FishOverride());
		mItems.put(Material.SHEARS, new ShearsOverride());
		mItems.put(Material.FLINT_AND_STEEL, new FlintAndSteelOverride());
		mItems.put(Material.PUFFERFISH, new PufferfishOverride());
		mItems.put(Material.END_CRYSTAL, new EndCrystalOverride());
		mItems.put(Material.QUARTZ, new QuartzOverride());

		BaseOverride sculkOverride = new SculkOverride();
		mItems.put(Material.SCULK, sculkOverride);
		mItems.put(Material.SCULK_CATALYST, sculkOverride);
		mItems.put(Material.SCULK_SENSOR, sculkOverride);
		mItems.put(Material.SCULK_SHRIEKER, sculkOverride);

		BaseOverride signOverride = new SignOverride();
		mItems.put(Material.ACACIA_SIGN, signOverride);
		mItems.put(Material.ACACIA_WALL_SIGN, signOverride);
		mItems.put(Material.BIRCH_SIGN, signOverride);
		mItems.put(Material.BIRCH_WALL_SIGN, signOverride);
		mItems.put(Material.DARK_OAK_SIGN, signOverride);
		mItems.put(Material.DARK_OAK_WALL_SIGN, signOverride);
		mItems.put(Material.JUNGLE_SIGN, signOverride);
		mItems.put(Material.JUNGLE_WALL_SIGN, signOverride);
		mItems.put(Material.OAK_SIGN, signOverride);
		mItems.put(Material.OAK_WALL_SIGN, signOverride);
		mItems.put(Material.SPRUCE_SIGN, signOverride);
		mItems.put(Material.SPRUCE_WALL_SIGN, signOverride);
		mItems.put(Material.WARPED_SIGN, signOverride);
		mItems.put(Material.WARPED_WALL_SIGN, signOverride);
		mItems.put(Material.CRIMSON_SIGN, signOverride);
		mItems.put(Material.CRIMSON_WALL_SIGN, signOverride);
		mItems.put(Material.MANGROVE_SIGN, signOverride);
		mItems.put(Material.MANGROVE_WALL_SIGN, signOverride);
		mItems.put(Material.CHERRY_SIGN, signOverride);
		mItems.put(Material.CHERRY_WALL_SIGN, signOverride);

		BaseOverride trapdoorOverride = new TrapdoorOverride();
		mItems.put(Material.ACACIA_TRAPDOOR, trapdoorOverride);
		mItems.put(Material.BIRCH_TRAPDOOR, trapdoorOverride);
		mItems.put(Material.CRIMSON_TRAPDOOR, trapdoorOverride);
		mItems.put(Material.DARK_OAK_TRAPDOOR, trapdoorOverride);
		mItems.put(Material.JUNGLE_TRAPDOOR, trapdoorOverride);
		mItems.put(Material.OAK_TRAPDOOR, trapdoorOverride);
		mItems.put(Material.SPRUCE_TRAPDOOR, trapdoorOverride);
		mItems.put(Material.WARPED_TRAPDOOR, trapdoorOverride);
		mItems.put(Material.MANGROVE_TRAPDOOR, trapdoorOverride);
		mItems.put(Material.CHERRY_TRAPDOOR, trapdoorOverride);

		BaseOverride berryBushOverride = new BerryBushOverride();
		mItems.put(Material.SWEET_BERRY_BUSH, berryBushOverride);
		mItems.put(Material.CAVE_VINES, berryBushOverride);
		mItems.put(Material.CAVE_VINES_PLANT, berryBushOverride);

		BaseOverride doorOverride = new DoorOverride();
		mItems.put(Material.ACACIA_DOOR, doorOverride);
		mItems.put(Material.BIRCH_DOOR, doorOverride);
		mItems.put(Material.CRIMSON_DOOR, doorOverride);
		mItems.put(Material.DARK_OAK_DOOR, doorOverride);
		mItems.put(Material.JUNGLE_DOOR, doorOverride);
		mItems.put(Material.OAK_DOOR, doorOverride);
		mItems.put(Material.SPRUCE_DOOR, doorOverride);
		mItems.put(Material.WARPED_DOOR, doorOverride);
		mItems.put(Material.MANGROVE_DOOR, doorOverride);
		mItems.put(Material.CHERRY_DOOR, doorOverride);

		BaseOverride unbreakableOverride = new UnbreakableOnBedrockOverride();
		mItems.put(Material.SPAWNER, unbreakableOverride);

		BaseOverride flowerOverride = new FlowerPotOverride();
		mItems.put(Material.FLOWER_POT, flowerOverride);
		mItems.put(Material.POTTED_DANDELION, flowerOverride);
		mItems.put(Material.POTTED_POPPY, flowerOverride);
		mItems.put(Material.POTTED_BLUE_ORCHID, flowerOverride);
		mItems.put(Material.POTTED_ALLIUM, flowerOverride);
		mItems.put(Material.POTTED_AZURE_BLUET, flowerOverride);
		mItems.put(Material.POTTED_RED_TULIP, flowerOverride);
		mItems.put(Material.POTTED_ORANGE_TULIP, flowerOverride);
		mItems.put(Material.POTTED_WHITE_TULIP, flowerOverride);
		mItems.put(Material.POTTED_PINK_TULIP, flowerOverride);
		mItems.put(Material.POTTED_OXEYE_DAISY, flowerOverride);
		mItems.put(Material.POTTED_CORNFLOWER, flowerOverride);
		mItems.put(Material.POTTED_LILY_OF_THE_VALLEY, flowerOverride);
		mItems.put(Material.POTTED_WITHER_ROSE, flowerOverride);
		mItems.put(Material.POTTED_OAK_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_SPRUCE_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_BIRCH_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_JUNGLE_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_ACACIA_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_DARK_OAK_SAPLING, flowerOverride);
		mItems.put(Material.POTTED_AZALEA_BUSH, flowerOverride);
		mItems.put(Material.POTTED_FLOWERING_AZALEA_BUSH, flowerOverride);
		mItems.put(Material.POTTED_RED_MUSHROOM, flowerOverride);
		mItems.put(Material.POTTED_BROWN_MUSHROOM, flowerOverride);
		mItems.put(Material.POTTED_FERN, flowerOverride);
		mItems.put(Material.POTTED_DEAD_BUSH, flowerOverride);
		mItems.put(Material.POTTED_CACTUS, flowerOverride);
		mItems.put(Material.POTTED_BAMBOO, flowerOverride);
		mItems.put(Material.POTTED_CRIMSON_FUNGUS, flowerOverride);
		mItems.put(Material.POTTED_CRIMSON_ROOTS, flowerOverride);
		mItems.put(Material.POTTED_WARPED_FUNGUS, flowerOverride);
		mItems.put(Material.POTTED_WARPED_ROOTS, flowerOverride);

		BaseOverride shulkerBoxOverride = new ShulkerBoxOverride();
		mItems.put(Material.SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.WHITE_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.ORANGE_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.MAGENTA_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.LIGHT_BLUE_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.YELLOW_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.LIME_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.PINK_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.GRAY_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.LIGHT_GRAY_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.CYAN_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.PURPLE_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.BLUE_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.BROWN_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.GREEN_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.RED_SHULKER_BOX, shulkerBoxOverride);
		mItems.put(Material.BLACK_SHULKER_BOX, shulkerBoxOverride);

		BaseOverride noAdventurePlaceOverride = new NoAdventureModePlacementOverride();
		mItems.put(Material.LILY_PAD, noAdventurePlaceOverride);

		BaseOverride horseFoodOverride = new HorseFoodOverride();
		mItems.put(Material.SUGAR, horseFoodOverride);
		mItems.put(Material.WHEAT, horseFoodOverride);
		mItems.put(Material.APPLE, horseFoodOverride);
		mItems.put(Material.GOLDEN_CARROT, horseFoodOverride);
		mItems.put(Material.HAY_BLOCK, horseFoodOverride);
		// GOLDEN_APPLE + ENCHANTED_GOLDEN_APPLE are in GoldenAppleOverride, which manually calls this override

		BaseOverride goldenAppleOverride = new GoldenAppleOverride();
		mItems.put(Material.GOLDEN_APPLE, goldenAppleOverride);
		mItems.put(Material.ENCHANTED_GOLDEN_APPLE, goldenAppleOverride);

		BaseOverride lodestoneOverride = new LodestoneOverride();
		mItems.put(Material.LODESTONE, lodestoneOverride);

		BaseOverride boneMealOverride = new BoneMealOverride();
		mItems.put(Material.BONE_MEAL, boneMealOverride);

		BaseOverride glassBottleOverride = new GlassBottleOverride();
		mItems.put(Material.GLASS_BOTTLE, glassBottleOverride);

		//Just needs the base cake since you cant put candles on cakes that already have them
		BaseOverride candleCakeOverride = new CandleCakeOverride();
		mItems.put(Material.CAKE, candleCakeOverride);

		BaseOverride alchPotionOverride = new AlchPotionOverride();
		mItems.put(Material.SPLASH_POTION, alchPotionOverride);

		BaseOverride goatHornOverride = new GoatHornOverride();
		mItems.put(Material.GOAT_HORN, goatHornOverride);

		BaseOverride snowballOverride = new SnowballOverride();
		mItems.put(Material.SNOWBALL, snowballOverride);
	}

	public void rightClickInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, @Nullable Block block, PlayerInteractEvent event) {
		Material itemType = (item != null) ? item.getType() : Material.AIR;
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		BaseOverride itemOverride = mItems.get(itemType);
		BaseOverride blockOverride = mItems.get(blockType);

		if (item != null && itemOverride != null && event.useItemInHand() != Event.Result.DENY) {
			if (!itemOverride.rightClickItemInteraction(plugin, player, action, item, block)) {
				event.setUseItemInHand(Event.Result.DENY);
			}
		}

		if (block != null && blockOverride != null && event.useInteractedBlock() != Event.Result.DENY) {
			if (!blockOverride.rightClickBlockInteraction(plugin, player, action, item, block, event)) {
				event.setUseInteractedBlock(Event.Result.DENY);
			}
		}
	}

	public void leftClickInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item,
									 @Nullable Block block, PlayerInteractEvent event) {
		Material itemType = (item != null) ? item.getType() : Material.AIR;
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		BaseOverride itemOverride = mItems.get(itemType);
		BaseOverride blockOverride = mItems.get(blockType);

		if (item != null && itemOverride != null && event.useItemInHand() != Event.Result.DENY) {
			if (!itemOverride.leftClickItemInteraction(plugin, player, action, item, block)) {
				event.setUseItemInHand(Event.Result.DENY);
			}
		}

		if (block != null && blockOverride != null && event.useInteractedBlock() != Event.Result.DENY) {
			if (!blockOverride.leftClickBlockInteraction(plugin, player, action, item, block)) {
				event.setUseInteractedBlock(Event.Result.DENY);
			}
		}
	}

	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity,
											   ItemStack itemInHand) {
		Material itemType = (itemInHand != null) ? itemInHand.getType() : Material.AIR;
		BaseOverride override = mItems.get(itemType);

		return (override == null) || override.rightClickEntityInteraction(plugin, player, clickedEntity, itemInHand);
	}

	public boolean inventoryClickInteraction(Plugin plugin, Player player, InventoryClickEvent event) {
		ItemStack cursorItem = event.getCursor();
		if ((event.getClick() != ClickType.RIGHT && event.getClick() != ClickType.LEFT)
			|| (cursorItem != null && cursorItem.getType() != Material.AIR)) {
			return true;
		}
		ItemStack item = event.getCurrentItem();
		if (item == null) {
			return true;
		}
		BaseOverride override = mItems.get(item.getType());
		return override == null || override.inventoryClickInteraction(plugin, player, item, event);
	}

	// Generalised inventoryClickEvent for overrides that don't necessarily care if there's an item on the cursor
	public boolean inventoryClickEvent(Plugin plugin, Player player, InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (item == null) {
			return true;
		}
		BaseOverride override = mItems.get(item.getType());
		return override == null || override.inventoryClickEvent(plugin, player, item, event);
	}

	// Returns true is event swaphands is to be cancelled, this cancels player abilities too (like Shield Wall).
	public boolean swapHandsInteraction(Plugin plugin, Player player) {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (item == null) {
			return true;
		}
		BaseOverride override = mItems.get(item.getType());
		return override != null && override.swapHandsInteraction(plugin, player, item);
	}

	// Returns eventCancelled = true if disallowed, otherwise false
	@SuppressWarnings({"unused"})
	private boolean safezoneDisallowsBlockChange(Plugin plugin, Player player, Block block) {
		boolean eventCancelled = false;

		// Prevent players from breaking blocks in safezones from outside of them
		if (!eventCancelled && player.getGameMode() != GameMode.CREATIVE) {
			if (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.ADVENTURE_MODE) &&
				!ZoneUtils.hasZoneProperty(player.getLocation(), ZoneProperty.ADVENTURE_MODE)) {
				// Allow breaking if the player would be in survival mode at that spot
				if (!ZoneUtils.isInPlot(block.getLocation())) {
					eventCancelled = true;
				}
			}
		}

		return eventCancelled;
	}

	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item,
										 BlockPlaceEvent event) {
		boolean eventCancelled = false;

		//  If it's not a certain lore item go ahead and run the normal override place interaction.
		BaseOverride override = mItems.get(item.getType());
		if (override != null) {
			eventCancelled |= !override.blockPlaceInteraction(plugin, player, item, event);
		}

		//  Don't allow placing of certain items with Lore.
		if (item.hasItemMeta()
			&& item.getItemMeta().hasLore()
			&& player.getGameMode() != GameMode.CREATIVE
			&& !EXCEPTION_LORED_MATERIALS.contains(item.getType())
			&& !ItemUtils.isNullOrAir(item)
			&& !Objects.equals(new NBTItem(item).getByte("Placeable"), (byte) 1)) {
			eventCancelled = true;
		}

		// Don't allow placing blocks on top of barriers for plots
		if (event.getBlockPlaced().getLocation().getBlockY() > 0 && !player.getGameMode().equals(GameMode.CREATIVE)) {
			Material belowMat = event.getBlockPlaced().getRelative(BlockFace.DOWN).getType();
			if (belowMat.equals(Material.BARRIER)) {
				eventCancelled = true;
			}

			// Don't allow players to place rail on bedrock because of a dumb mojang bug
			Material blockPlacedMat = event.getBlockPlaced().getType();
			if (belowMat.equals(Material.BEDROCK) &&
				(blockPlacedMat.equals(Material.RAIL) ||
					blockPlacedMat.equals(Material.POWERED_RAIL) ||
					blockPlacedMat.equals(Material.DETECTOR_RAIL))) {
				eventCancelled = true;
			}
		}

		// NOTE: This is disabled because while functionally nicer, players don't like it
		//eventCancelled |= safezoneDisallowsBlockChange(plugin, player, event.getBlockPlaced());

		return !eventCancelled;
	}

	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block, BlockBreakEvent event) {
		boolean eventCancelled = false;

		BaseOverride override = mItems.get(block.getType());
		if (override != null) {
			eventCancelled = !override.blockBreakInteraction(plugin, player, block, event);
		}

		// Don't allow blocks to break if they're on the server's list of unbreakable blocks
		if (!eventCancelled && player.getGameMode() != GameMode.CREATIVE &&
			ServerProperties.getUnbreakableBlocks().contains(block.getType())) {
			eventCancelled = true;
		}

		// NOTE: This is disabled because, while functionally nicer, players don't like it
		//eventCancelled |= safezoneDisallowsBlockChange(plugin, player, block);

		return !eventCancelled;
	}

	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		BaseOverride override = mItems.get(block.getType());
		if (override != null) {
			return override.blockExplodeInteraction(plugin, block);
		}

		return true;
	}

	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		BaseOverride override = mItems.get(dispensed.getType());
		if (override != null) {
			return override.blockDispenseInteraction(plugin, block, dispensed);
		}

		return true;
	}

	public boolean blockChangeInteraction(Plugin plugin, Block block) {
		BaseOverride override = mItems.get(block.getType());
		if (override != null) {
			return override.blockChangeInteraction(plugin, block);
		}
		return true;
	}

	public boolean playerItemConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		BaseOverride override = mItems.get(event.getItem().getType());
		if (override != null) {
			return override.playerItemConsume(plugin, player, event);
		}
		return true;
	}

	public boolean playerRiptide(Plugin plugin, Player player, PlayerRiptideEvent event) {
		BaseOverride override = mItems.get(event.getItem().getType());
		if (override != null) {
			return override.playerRiptide(plugin, player, event);
		}
		return true;
	}
}
