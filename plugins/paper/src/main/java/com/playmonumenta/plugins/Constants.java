package com.playmonumenta.plugins;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.function.Function;
import net.kyori.adventure.text.KeybindComponent.KeybindLike;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;


public class Constants {
	public static final int TICKS_PER_SECOND = 20;
	public static final int HALF_TICKS_PER_SECOND = (int)(TICKS_PER_SECOND / 2.0);
	public static final int QUARTER_TICKS_PER_SECOND = (int)(HALF_TICKS_PER_SECOND / 2.0);
	public static final int TICKS_PER_MINUTE = TICKS_PER_SECOND * 60;

	public static final int TWO_MINUTES = TICKS_PER_MINUTE * 2;
	public static final int THIRTY_SECONDS = TICKS_PER_SECOND * 30;

	public static final int FIVE_MINUTES = TICKS_PER_MINUTE * 5;
	public static final int TEN_MINUTES = TICKS_PER_MINUTE * 10;
	public static final int THIRTY_MINUTES = TICKS_PER_MINUTE * 30;
	public static final int ONE_HOUR = TICKS_PER_MINUTE * 60;
	public static final int THREE_HOURS = TICKS_PER_MINUTE * 180;

	public static final String ANTI_SPEED_MODIFIER = "AntiSpeedModifier";

	public static final PotionEffect CAPITAL_SPEED_EFFECT = new PotionEffect(PotionEffectType.SPEED, 2 * TICKS_PER_SECOND - 1, 1, true, false);
	public static final PotionEffect CITY_SATURATION_EFFECT = new PotionEffect(PotionEffectType.SATURATION, 2 * TICKS_PER_SECOND - 1, 1, true, false);
	public static final PotionEffect CITY_RESISTANCE_EFFECT = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2 * TICKS_PER_SECOND - 1, 4, true, false);
	public static final PotionEffect CITY_JUMP_MASK_EFFECT = new PotionEffect(PotionEffectType.JUMP, 2 * TICKS_PER_SECOND - 1, 1000, true, false);
	public static final PotionEffect CITY_SPEED_MASK_EFFECT = new PotionEffect(PotionEffectType.SPEED, 2 * TICKS_PER_SECOND - 1, 1000, true, false);

	//  USED FOR DEBUGGING PURPOSES WHEN COMPILING A JAR FOR OTHERS. (sadly it'll still contain the code in the jar, however it won't run)
	public static final boolean CLASSES_ENABLED = true;
	public static final boolean SPECIALIZATIONS_ENABLED = true;
	public static final boolean TRACKING_MANAGER_ENABLED = true;
	public static final boolean POTION_MANAGER_ENABLED = true;
	public static final boolean COMMANDS_SERVER_ENABLED = true;

	// Metadata keys
	public static final String SPAWNER_COUNT_METAKEY = "MonumentaSpawnCount";
	public static final String PLAYER_DAMAGE_NONCE_METAKEY = "MonumentaPlayerDamageNonce";
	public static final String PLAYER_BOW_SHOT_METAKEY = "MonumentaPlayerBowShot";
	public static final String TREE_METAKEY = "MonumentaStructureGrowEvent";
	public static final String ENTITY_DAMAGE_NONCE_METAKEY = "MonumentaEntityDamageNonce";
	public static final String ENTITY_COMBUST_NONCE_METAKEY = "MonumentaEntityCombustNonce";
	public static final String ENTITY_SLOWED_NONCE_METAKEY = "MonumentaEntitySlowedNonce";
	public static final String ANVIL_CONFIRMATION_METAKEY = "MonumentaAnvilConfirmation";
	public static final String PLAYER_SNEAKING_TASK_METAKEY = "MonumentaPlayerSneaking";

	public static final String SCOREBOARD_DEATH_MESSAGE = "DeathMessage";

	// The max distance for spells that detected nearby damaged allies/enemies.
	public static final double ABILITY_ENTITY_DAMAGE_BY_ENTITY_RADIUS = 15;

	// NMS ItemBow:
	// entityarrow.a(entityhuman, entityhuman.pitch, entityhuman.yaw, 0.0F, f * 3.0F, 1.0F);
	public static final int PLAYER_BOW_INITIAL_SPEED = 3;
	// NMS ItemCrossbow:
	// return itemstack.getItem() == Items.CROSSBOW && a(itemstack, Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
	public static final double PLAYER_CROSSBOW_ARROW_INITIAL_SPEED = 3.15;
	public static final double PLAYER_CROSSBOW_ROCKET_INITIAL_SPEED = 1.6;
	// NMS EntitySkeletonAbstract:
	// entityarrow.shoot(d0, d1 + d3 * 0.20000000298023224D, d2, 1.6F, (float)(14 - this.world.getDifficulty().a() * 4));
	public static final double SKELETON_SHOOT_INITIAL_SPEED = 1.6;
	// NMS EntityPillager:
	// this.a(this, entityliving, iprojectile, f, 1.6F);
	public static final double PILLAGER_SHOOT_INITIAL_SPEED = 1.6;
	// NMS EntityPiglin:
	// this.a(this, var0, var2, var3, 1.6F);
	public static final double PIGLIN_SHOOT_INITIAL_SPEED = 1.6;

	// + Discord channel
	// + In-game rank
	// + Castle head
	// + White particles
	// + Stat tracking
	// + Patreon Parakeet
	public static final int PATREON_TIER_1 = 5;

	// + 1 daily buff
	// + Greed particles
	// + Plot Borders
	// + Emojis
	public static final int PATREON_TIER_2 = 10;

	// 1 → 2 daily buffs
	// + Red and Purple particles
	// + Free vanity
	public static final int PATREON_TIER_3 = 20;


	static {
		Materials.WEARABLE.addAll(Materials.ARMOR);
	}

	public static class Objectives {
		// PartialParticle
		public static final String PP_OWN_PASSIVE = "PPOwnPassive";
		public static final String PP_OWN_BUFF = "PPOwnBuff";
		public static final String PP_OWN_ACTIVE = "PPOwnActive";
		public static final String PP_OWN_EMOJI = "PPOwnEmoji";
		public static final String PP_OTHER_PASSIVE = "PPOtherPassive";
		public static final String PP_OTHER_BUFF = "PPOtherBuff";
		public static final String PP_OTHER_ACTIVE = "PPOtherActive";
		public static final String PP_OTHER_EMOJI = "PPOtherEmoji";
		public static final String PP_ENEMY_BUFF = "PPEnemyBuff";
		public static final String PP_ENEMY = "PPEnemy";
		public static final String PP_BOSS = "PPBoss";

		// PlayerData
		public static final String PATREON_DOLLARS = "Patreon";

		// plots
		public static final String OWN_PLOT = "Plot";
		public static final String CURRENT_PLOT = "CurrentPlot";
	}

	public static class Tags {
		// PlayerData
		public static final String NO_SELF_PARTICLES = "noSelfParticles";
		public static final String NO_TRANSPOSING = "NoTransposing";
		public static final String FORMATION = "Formation";
		public static final String ENTROPIC = "Entropic";
		public static final String REMOVE_ON_UNLOAD = "REMOVE_ON_UNLOAD";
	}

	// Note blocks
	public static class NotePitches {
		public static final float FS0 = calculatePitch(0);
		public static final float G1 = calculatePitch(1);
		public static final float GS2 = calculatePitch(2);
		public static final float A3 = calculatePitch(3);
		public static final float AS4 = calculatePitch(4);
		public static final float B5 = calculatePitch(5);
		public static final float C6 = calculatePitch(6);
		public static final float CS7 = calculatePitch(7);
		public static final float D8 = calculatePitch(8);
		public static final float DS9 = calculatePitch(9);
		public static final float E10 = calculatePitch(10);
		public static final float F11 = calculatePitch(11);
		public static final float FS12 = calculatePitch(12);
		public static final float G13 = calculatePitch(13);
		public static final float GS14 = calculatePitch(14);
		public static final float A15 = calculatePitch(15);
		public static final float AS16 = calculatePitch(16);
		public static final float B17 = calculatePitch(17);
		public static final float C18 = calculatePitch(18);
		public static final float CS19 = calculatePitch(19);
		public static final float D20 = calculatePitch(20);
		public static final float DS21 = calculatePitch(21);
		public static final float E22 = calculatePitch(22);
		public static final float F23 = calculatePitch(23);
		public static final float FS24 = calculatePitch(24);

		public static float calculatePitch(int clicks) {
			return 0.5f * (float)Math.pow(2, (clicks / 12d));
		}
	}

	// Updated note
	public enum Note {
		FS3(0, "F♯₃"),
		GB3(0, "G♭₃"),
		G3(1, "G₃"),
		GS3(2, "G♯₃"),
		AB3(2, "A♭₃"),
		A3(3, "A₃"),
		AS3(4, "A♯₃"),
		BB3(4, "B♭₃"),
		B3(5, "B₃"),
		C4(6, "C₄"),
		CS4(7, "C♯₄"),
		DB4(7, "D♭₄"),
		D4(8, "D₄"),
		DS4(9, "D♯₄"),
		EB4(9, "E♭₄"),
		E4(10, "E₄"),
		F4(11, "F₄"),
		FS4(12, "F♯₄"),
		GB4(12, "G♭₄"),
		G4(13, "G₄"),
		GS4(14, "G♯₄"),
		AB4(14, "A♭₄"),
		A4(15, "A₄"),
		AS4(16, "A♯₄"),
		BB4(16, "B♭₄"),
		B4(17, "B₄"),
		C5(18, "C₅"),
		CS5(19, "C♯₅"),
		DB5(19, "D♭₅"),
		D5(20, "D₅"),
		DS5(21, "D♯₅"),
		EB5(21, "E♭₅"),
		E5(22, "E₅"),
		F5(23, "F₅"),
		FS5(24, "F♯₅"),
		GB5(24, "G♭₅");

		public final int mClicks;
		public final String mName;
		public final float mPitch;
		public final double mNoteParticleValue;

		Note(int clicks, String name) {
			mClicks = clicks;
			mName = name;
			mNoteParticleValue = clicks / 24.0;
			mPitch = (float)Math.pow(2.0F, (clicks - 12) / 12.0F);
		}
	}

	public static class Colors {
		public static final TextColor GREENISH_BLUE = TextColor.color(85, 255, 170);
		public static final TextColor GREENISH_BLUE_DARK = TextColor.color(76, 230, 153);
	}

	public enum Keybind implements KeybindLike {
		ATTACK("key.attack"),
		USE("key.use"),
		FORWARD("key.forward"),
		LEFT("key.left"),
		BACK("key.back"),
		RIGHT("key.right"),
		JUMP("key.jump"),
		SNEAK("key.sneak"),
		SPRINT("key.sprint"),
		DROP("key.drop"),
		INVENTORY("key.inventory"),
		CHAT("key.chat"),
		PLAYER_LIST("key.playerlist"),
		PICK_ITEM("key.pickItem"),
		COMMAND("key.command"),
		SOCIAL_INTERACTIONS("key.socialInteractions"),
		SCREENSHOT("key.screenshot"),
		TOGGLE_PERSPECTIVE("key.togglePerspective"),
		SMOOTH_CAMERA("key.smoothCamera"),
		FULLSCREEN("key.fullscreen"),
		SPECTATOR_OUTLINES("key.spectatorOutlines"),
		SWAP_OFFHAND("key.swapOffhand"),
		SAVE_TOOLBAR_ACTIVATOR("key.saveToolbarActivator"),
		LOAD_TOOLBAR_ACTIVATOR("key.loadToolbarActivator"),
		ADVANCEMENTS("key.advancements"),
		HOTBAR_1("key.hotbar.1"),
		HOTBAR_2("key.hotbar.2"),
		HOTBAR_3("key.hotbar.3"),
		HOTBAR_4("key.hotbar.4"),
		HOTBAR_5("key.hotbar.5"),
		HOTBAR_6("key.hotbar.6"),
		HOTBAR_7("key.hotbar.7"),
		HOTBAR_8("key.hotbar.8"),
		HOTBAR_9("key.hotbar.9"),
		OPTIFINE_ZOOM("of.key.zoom");

		private final String mValue;

		Keybind(String value) {
			mValue = value;
		}

		// 0-indexed to match inventories; index 0 is hotbar slot 1
		public static Keybind hotbar(int index) {
			if (index < 0 || index > 8) {
				throw new RuntimeException("Invalid hotbar keybind index " + index);
			}

			Keybind[] values = Keybind.values();
			return values[index + HOTBAR_1.ordinal()];
		}

		@Override
		public @NotNull String asKeybind() {
			return mValue;
		}
	}

	public static class Materials {
		public static final EnumSet<Material> ARMOR = EnumSet.of(
			Material.LEATHER_HELMET,
			Material.LEATHER_CHESTPLATE,
			Material.LEATHER_LEGGINGS,
			Material.LEATHER_BOOTS,

			Material.GOLDEN_HELMET,
			Material.GOLDEN_CHESTPLATE,
			Material.GOLDEN_LEGGINGS,
			Material.GOLDEN_BOOTS,

			Material.CHAINMAIL_HELMET,
			Material.CHAINMAIL_CHESTPLATE,
			Material.CHAINMAIL_LEGGINGS,
			Material.CHAINMAIL_BOOTS,

			Material.IRON_HELMET,
			Material.IRON_CHESTPLATE,
			Material.IRON_LEGGINGS,
			Material.IRON_BOOTS,

			Material.DIAMOND_HELMET,
			Material.DIAMOND_CHESTPLATE,
			Material.DIAMOND_LEGGINGS,
			Material.DIAMOND_BOOTS,

			Material.NETHERITE_HELMET,
			Material.NETHERITE_CHESTPLATE,
			Material.NETHERITE_LEGGINGS,
			Material.NETHERITE_BOOTS,

			Material.TURTLE_HELMET,
			Material.ELYTRA
		);

		// ARMOR is added in static {}
		public static final EnumSet<Material> WEARABLE = EnumSet.of(
			Material.PLAYER_HEAD,
			Material.CREEPER_HEAD,
			Material.DRAGON_HEAD,
			Material.SKELETON_SKULL,
			Material.WITHER_SKELETON_SKULL,
			Material.ZOMBIE_HEAD,

			Material.CARVED_PUMPKIN
		);

		public static final EnumSet<Material> SWORDS = EnumSet.of(
			Material.WOODEN_SWORD,
			Material.STONE_SWORD,
			Material.GOLDEN_SWORD,
			Material.IRON_SWORD,
			Material.DIAMOND_SWORD,
			Material.NETHERITE_SWORD
		);
		public static final EnumSet<Material> BOWS = EnumSet.of(
			Material.BOW,
			Material.CROSSBOW
		);
		public static final EnumSet<Material> HOES = EnumSet.of(
			Material.WOODEN_HOE,
			Material.STONE_HOE,
			Material.GOLDEN_HOE,
			Material.IRON_HOE,
			Material.DIAMOND_HOE,
			Material.NETHERITE_HOE
		);
		public static final EnumSet<Material> PICKAXES = EnumSet.of(
			Material.WOODEN_PICKAXE,
			Material.STONE_PICKAXE,
			Material.GOLDEN_PICKAXE,
			Material.IRON_PICKAXE,
			Material.DIAMOND_PICKAXE,
			Material.NETHERITE_PICKAXE
		);
		public static final EnumSet<Material> AXES = EnumSet.of(
			Material.WOODEN_AXE,
			Material.STONE_AXE,
			Material.GOLDEN_AXE,
			Material.IRON_AXE,
			Material.DIAMOND_AXE,
			Material.NETHERITE_AXE
		);
		public static final EnumSet<Material> SHOVELS = EnumSet.of(
			Material.WOODEN_SHOVEL,
			Material.STONE_SHOVEL,
			Material.GOLDEN_SHOVEL,
			Material.IRON_SHOVEL,
			Material.NETHERITE_SHOVEL
		);

		public static final EnumSet<Material> POTIONS = EnumSet.of(
			Material.POTION,
			Material.SPLASH_POTION,
			Material.LINGERING_POTION
		);
		public static final EnumSet<Material> ARROWS = EnumSet.of(
			Material.ARROW,
			Material.SPECTRAL_ARROW,
			Material.TIPPED_ARROW
		);
		public static final EnumSet<Material> FISH = EnumSet.of(
			Material.COD,
			Material.PUFFERFISH,
			Material.SALMON,
			Material.TROPICAL_FISH
		);
	}

	public enum SupportedPersistentDataType {
		BYTE(PersistentDataType.BYTE, Byte::parseByte),
		SHORT(PersistentDataType.SHORT, Short::parseShort),
		INTEGER(PersistentDataType.INTEGER, Integer::parseInt),
		LONG(PersistentDataType.LONG, Long::parseLong),
		FLOAT(PersistentDataType.FLOAT, Float::parseFloat),
		DOUBLE(PersistentDataType.DOUBLE, Double::parseDouble),
		STRING(PersistentDataType.STRING, s -> s),
		;

		public final PersistentDataType<?, ?> mPersistentDataType;
		public final Function<String, Object> mParser;

		SupportedPersistentDataType(PersistentDataType<?, ?> persistentDataType, Function<String, Object> parser) {
			this.mPersistentDataType = persistentDataType;
			this.mParser = parser;
		}

		public static String[] getLowerCaseNames() {
			return Arrays.stream(values()).map(e -> e.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
		}
	}
}
