package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

public class AbilityUtils {

	private static final String ARROW_BASE_DAMAGE_METAKEY = "ArrowBaseDamageFromAbilities"; // For Quickdraw
	private static final String ARROW_BONUS_DAMAGE_METAKEY = "ArrowBonusDamageFromAbilities"; // For Bow Mastery and Sharpshooter
	private static final String ARROW_VELOCITY_DAMAGE_MULTIPLIER_METAKEY = "ArrowVelocityDamageMultiplier"; // Multiplier based on arrow speed
	private static final String ARROW_FINAL_DAMAGE_MULTIPLIER_METAKEY = "ArrowFinalDamageMultiplier"; // For Volley and Pinning Shot
	private static final String ARROW_REFUNDED_METAKEY = "ArrowRefunded";
	private static final String POTION_REFUNDED_METAKEY = "PotionRefunded";
	// This value obtained from testing; in reality, a fully charged shot outputs an arrow with a velocity between 2.95 and 3.05
	private static final float ARROW_MAX_VELOCITY = 2.9f;

	private static final Map<UUID, Integer> INVISIBLE_PLAYERS = new HashMap<UUID, Integer>();
	private static BukkitRunnable invisTracker = null;

	private static void startInvisTracker(Plugin plugin) {
		invisTracker = new BukkitRunnable() {
			@Override
			public void run() {
				if (INVISIBLE_PLAYERS.isEmpty()) {
					this.cancel();
				} else {
					for (Entry<UUID, Integer> entry : INVISIBLE_PLAYERS.entrySet()) {
						Player player = Bukkit.getPlayer(entry.getKey());
						ItemStack item = player.getInventory().getItemInMainHand();
						if (entry.getValue() <= 0) {
							removeStealth(plugin, player, false);
						} else if (!InventoryUtils.isAxeItem(item) && !InventoryUtils.isSwordItem(item) && !InventoryUtils.isScytheItem(item)) {
							removeStealth(plugin, player, true);
						} else {
							player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, player.getLocation().clone().add(0, 0.5, 0), 1, 0.35, 0.25, 0.35, 0.05f);
							INVISIBLE_PLAYERS.put(player.getUniqueId(), entry.getValue() - 1);
						}
					}
				}
			}
		};
		invisTracker.runTaskTimer(plugin, 0, 1);
	}

	public static boolean isStealthed(Player player) {
		return INVISIBLE_PLAYERS.containsKey(player.getUniqueId());
	}

	public static void removeStealth(Plugin plugin, Player player, boolean inflictPenalty) {
		Location loc = player.getLocation();
		World world = player.getWorld();

		world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.1f);
		world.spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.5f);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_HURT, 0.6f, 0.5f);

		plugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.INVISIBILITY);

		INVISIBLE_PLAYERS.remove(player.getUniqueId());

		if (inflictPenalty) {
			plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SLOW_DIGGING, 20 * 3, 1));
		}
	}

	public static void applyStealth(Plugin plugin, Player player, int duration) {
		Location loc = player.getLocation();
		World world = player.getWorld();

		world.spawnParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.1f);
		world.spawnParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.5f);
		world.playSound(loc, Sound.ENTITY_SNOW_GOLEM_DEATH, 1f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, 0.5f, 2f);

		if (!isStealthed(player)) {
			plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0));
			INVISIBLE_PLAYERS.put(player.getUniqueId(), duration);
		} else {
			int currentDuration = INVISIBLE_PLAYERS.get(player.getUniqueId());
			plugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.INVISIBILITY);
			plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INVISIBILITY, duration + currentDuration, 0));
			INVISIBLE_PLAYERS.put(player.getUniqueId(), duration + currentDuration);
		}

		if (invisTracker == null || invisTracker.isCancelled()) {
			startInvisTracker(plugin);
		}

		for (LivingEntity entity : EntityUtils.getNearbyMobs(player.getLocation(), 64)) {
			if (entity instanceof Mob) {
				Mob mob = (Mob) entity;
				if (mob.getTarget() != null && mob.getTarget().getUniqueId().equals(player.getUniqueId())) {
					mob.setTarget(null);
				}
			}
		}
	}

	public static double getArrowFinalDamageMultiplier(Arrow arrow) {
		if (arrow.hasMetadata(ARROW_FINAL_DAMAGE_MULTIPLIER_METAKEY)) {
			return arrow.getMetadata(ARROW_FINAL_DAMAGE_MULTIPLIER_METAKEY).get(0).asDouble();
		}
		return 1;
	}

	public static void multiplyArrowFinalDamageMultiplier(Plugin plugin, Arrow arrow, double multiplier) {
		arrow.setMetadata(ARROW_FINAL_DAMAGE_MULTIPLIER_METAKEY, new FixedMetadataValue(plugin, getArrowFinalDamageMultiplier(arrow) * multiplier));
	}

	public static double getArrowVelocityDamageMultiplier(Arrow arrow) {
		if (arrow.hasMetadata(ARROW_VELOCITY_DAMAGE_MULTIPLIER_METAKEY)) {
			return arrow.getMetadata(ARROW_VELOCITY_DAMAGE_MULTIPLIER_METAKEY).get(0).asDouble();
		}
		return 1;
	}

	public static void setArrowVelocityDamageMultiplier(Plugin plugin, Arrow arrow) {
		arrow.setMetadata(ARROW_VELOCITY_DAMAGE_MULTIPLIER_METAKEY, new FixedMetadataValue(plugin, Math.min(1, arrow.getVelocity().length() / ARROW_MAX_VELOCITY)));
	}

	public static double getArrowBonusDamage(Arrow arrow) {
		if (arrow.hasMetadata(ARROW_BONUS_DAMAGE_METAKEY)) {
			return arrow.getMetadata(ARROW_BONUS_DAMAGE_METAKEY).get(0).asDouble();
		}
		return 0;
	}

	public static void addArrowBonusDamage(Plugin plugin, Arrow arrow, double damage) {
		arrow.setMetadata(ARROW_BONUS_DAMAGE_METAKEY, new FixedMetadataValue(plugin, getArrowBonusDamage(arrow) + damage));
	}

	public static double getArrowBaseDamage(Arrow arrow) {
		if (arrow.hasMetadata(ARROW_BASE_DAMAGE_METAKEY)) {
			return arrow.getMetadata(ARROW_BASE_DAMAGE_METAKEY).get(0).asDouble();
		}
		return 0;
	}

	public static void setArrowBaseDamage(Plugin plugin, Arrow arrow, double damage) {
		arrow.setMetadata(ARROW_BASE_DAMAGE_METAKEY, new FixedMetadataValue(plugin, damage));
	}

	private static ItemStack getAlchemistPotion() {
		ItemStack stack = new ItemStack(Material.SPLASH_POTION, 1);

		PotionMeta meta = (PotionMeta)stack.getItemMeta();
		meta.setBasePotionData(new PotionData(PotionType.MUNDANE));
		meta.setColor(Color.WHITE);
		meta.setDisplayName(ChatColor.AQUA + "Alchemist's Potion");
		List<String> lore = Arrays.asList(new String[] {
			ChatColor.GRAY + "A unique potion for Alchemists",
		});
		meta.setLore(lore);
		stack.setItemMeta(meta);
		return stack;
	}

	public static void addAlchemistPotions(Player player, int numAddedPotions) {
		if (numAddedPotions == 0) {
			return;
		}

		Inventory inv = player.getInventory();
		ItemStack firstFoundPotStack = null;
		int potCount = 0;

		for (ItemStack item : inv.getContents()) {
			if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
				if (firstFoundPotStack == null) {
					firstFoundPotStack = item;
				}
				potCount += item.getAmount();
			}
		}

		if (potCount < 32) {
			if (firstFoundPotStack != null) {
				firstFoundPotStack.setAmount(firstFoundPotStack.getAmount() + numAddedPotions);
			} else {
				ItemStack newPotions = getAlchemistPotion();
				newPotions.setAmount(numAddedPotions);
				inv.addItem(newPotions);
			}
		}
	}

	// You can't just use a negative value with the add method if the potions to be remove are distributed across multiple stacks
	// Returns false if the player doesn't have enough potions in their inventory
	public static boolean removeAlchemistPotions(Player player, int numPotionsToRemove) {
		Inventory inv = player.getInventory();
		List<ItemStack> potionStacks = new ArrayList<ItemStack>();
		int potionCount = 0;

		// Make sure the player has enough potions
		for (ItemStack item : inv.getContents()) {
			if (InventoryUtils.testForItemWithName(item, "Alchemist's Potion")) {
				potionCount += item.getAmount();
				potionStacks.add(item);
				if (potionCount >= numPotionsToRemove) {
					break;
				}
			}
		}

		if (potionCount >= numPotionsToRemove) {
			for (ItemStack potionStack : potionStacks) {
				if (potionStack.getAmount() >= numPotionsToRemove) {
					potionStack.setAmount(potionStack.getAmount() - numPotionsToRemove);
					break;
				} else {
					numPotionsToRemove -= potionStack.getAmount();
					potionStack.setAmount(0);
					if (numPotionsToRemove == 0) {
						break;
					}
				}
			}

			return true;
		}

		return false;
	}

	public static void refundArrow(Player player, Arrow arrow) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		ItemStack offHand = player.getInventory().getItemInOffHand();
		//Only refund arrow once
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, ARROW_REFUNDED_METAKEY)) {
			if (InventoryUtils.isBowItem(mainHand) || InventoryUtils.isBowItem(offHand)) {
				int infLevel = Math.max(mainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE), offHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE));
				if (infLevel == 0) {
					arrow.setPickupStatus(Arrow.PickupStatus.ALLOWED);
					Inventory playerInv = player.getInventory();
					int firstArrow = playerInv.first(Material.ARROW);
					int firstTippedArrow = playerInv.first(Material.TIPPED_ARROW);

					final int arrowSlot;
					if (firstArrow == -1 && firstTippedArrow > -1) {
						arrowSlot = firstTippedArrow;
					} else if (firstArrow > - 1 && firstTippedArrow == -1) {
						arrowSlot = firstArrow;
					} else if (firstArrow > - 1 && firstTippedArrow > -1) {
						arrowSlot = Math.min(firstArrow, firstTippedArrow);
					} else {
						/* Player shot their last arrow - abort here */
						return;
					}

					// Make sure the duplicate arrow can't be picked up
					arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
					// I'm not sure why this works, but it does.
					playerInv.setItem(arrowSlot, playerInv.getItem(arrowSlot));
				}
			}
		}
	}

	public static void refundPotion(Player player, ThrownPotion potion) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		ItemStack offHand = player.getInventory().getItemInOffHand();
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, POTION_REFUNDED_METAKEY)) {
			ItemStack item = potion.getItem();
			if (mainHand != null && mainHand.isSimilar(item)) {
				mainHand.setAmount(mainHand.getAmount() + 1);
			} else if (offHand != null && offHand.isSimilar(item)) {
				offHand.setAmount(offHand.getAmount() + 1);
			}
		}
	}

	public static String getClass(Player player) {
		int classVal = ScoreboardUtils.getScoreboardValue(player, "Class");
		switch (classVal) {
		case 1:
			return "Mage";
		case 2:
			return "Warrior";
		case 3:
			return "Cleric";
		case 4:
			return "Rogue";
		case 5:
			return "Alchemist";
		case 6:
			return "Scout";
		case 7:
			return "Warlock";
		default:
			return "???";
		}
	}

	public static int getClass(String str) {
		switch (str) {
		case "Mage":
			return 1;
		case "Warrior":
			return 2;
		case "Cleric":
			return 3;
		case "Rogue":
			return 4;
		case "Alchemist":
			return 5;
		case "Scout":
			return 6;
		case "Warlock":
			return 7;
		default:
			return 0;
		}
	}

	public static String getSpec(Player player) {
		int classVal = ScoreboardUtils.getScoreboardValue(player, "Specialization");
		switch (classVal) {
		case 1:
			return "Arcanist";
		case 2:
			return "Elementalist";
		case 3:
			return "Berserker";
		case 4:
			return "Guardian";
		case 5:
			return "Paladin";
		case 6:
			return "Hierophant";
		case 7:
			return "Swordsage";
		case 8:
			return "Assassin";
		case 9:
			return "Harbinger";
		case 10:
			return "Apothecary";
		case 11:
			return "Ranger";
		case 12:
			return "Hunter";
		case 13:
			return "Reaper";
		case 14:
			return "Tenebrist";
		default:
			return "No Spec";
		}
	}

	public static int getSpec(String str) {
		switch (str) {
		case "Arcanist":
			return 1;
		case "Elementalist":
			return 2;
		case "Berserker":
			return 3;
		case "Guardian":
			return 4;
		case "Paladin":
			return 5;
		case "Hierophant":
			return 6;
		case "Swordsage":
			return 7;
		case "Assassin":
			return 8;
		case "Harbinger":
			return 9;
		case "Apothecary":
			return 10;
		case "Ranger":
			return 11;
		case "Hunter":
			return 12;
		case "Reaper":
			return 13;
		case "Tenebrist":
			return 14;
		default:
			return 0;
		}
	}
}
