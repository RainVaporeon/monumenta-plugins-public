package com.playmonumenta.plugins.utils;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;

public class ItemUtils {
	public static final Set<Material> armors = EnumSet.of(
			Material.LEATHER_BOOTS,
			Material.LEATHER_CHESTPLATE,
			Material.LEATHER_HELMET,
			Material.LEATHER_LEGGINGS,

			Material.CHAINMAIL_BOOTS,
			Material.CHAINMAIL_CHESTPLATE,
			Material.CHAINMAIL_HELMET,
			Material.CHAINMAIL_LEGGINGS,

			Material.GOLDEN_BOOTS,
			Material.GOLDEN_CHESTPLATE,
			Material.GOLDEN_HELMET,
			Material.GOLDEN_LEGGINGS,

			Material.IRON_BOOTS,
			Material.IRON_CHESTPLATE,
			Material.IRON_HELMET,
			Material.IRON_LEGGINGS,

			Material.DIAMOND_BOOTS,
			Material.DIAMOND_CHESTPLATE,
			Material.DIAMOND_HELMET,
			Material.DIAMOND_LEGGINGS
		);

	// List of materials that trees can replace when they grow
	public static final Set<Material> allowedTreeReplaceMaterials = EnumSet.of(
			Material.AIR,
			Material.OAK_LEAVES,
			Material.SPRUCE_LEAVES,
			Material.BIRCH_LEAVES,
			Material.JUNGLE_LEAVES,
			Material.ACACIA_LEAVES,
			Material.DARK_OAK_LEAVES,
			Material.OAK_SAPLING,
			Material.ACACIA_SAPLING,
			Material.BIRCH_SAPLING,
			Material.DARK_OAK_SAPLING,
			Material.JUNGLE_SAPLING,
			Material.SPRUCE_SAPLING,
			Material.VINE
		);

	/**
	 * Items drop if they have lore that does not contain $$$
	 */
	public static float getItemDropChance(ItemStack item) {
		if (item != null && (item.hasItemMeta() && item.getItemMeta().hasLore()) && !InventoryUtils.testForItemWithLore(item, "$$")) {
			return 1.0f;
		} else {
			return -200.0f;
		}
	}

	public static ItemStack createTippedArrows(PotionType type, int amount, PotionData data) {
		ItemStack stack = new ItemStack(Material.TIPPED_ARROW, amount);

		PotionMeta meta = (PotionMeta)stack.getItemMeta();
		meta.setBasePotionData(data);
		stack.setItemMeta(meta);

		return stack;
	}

	public static ItemStack createStackedPotions(PotionEffectType type, int amount, int duration, int amplifier, String name) {
		ItemStack stack = new ItemStack(Material.SPLASH_POTION, amount);

		PotionMeta meta = (PotionMeta)stack.getItemMeta();

		meta.setDisplayName("�r" + name);
		meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
		meta.setColor(type.getColor());
		stack.setItemMeta(meta);

		return stack;
	}

	public static void addPotionEffect(ItemStack potion, PotionInfo info) {
		PotionMeta meta = (PotionMeta)potion.getItemMeta();
		meta.addCustomEffect(new PotionEffect(info.type, info.duration, info.amplifier, false, true), false);
		potion.setItemMeta(meta);
	}

	public static void setPotionMeta(ItemStack potion, String name, Color color) {
		PotionMeta meta = (PotionMeta)potion.getItemMeta();
		meta.setDisplayName("�r" + name);
		meta.setColor(color);
		potion.setItemMeta(meta);
	}

	public static PotionMeta getPotionMeta(ItemStack potion) {
		Material type = potion.getType();
		if (type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION) {
			PotionMeta meta = (PotionMeta)potion.getItemMeta();
			return meta;
		}

		return null;
	}

	public static boolean isArmorItem(Material item) {
		return armors.contains(item);
	}

	public static boolean isAllowedTreeReplace(Material item) {
		return allowedTreeReplaceMaterials.contains(item);
	}
}
