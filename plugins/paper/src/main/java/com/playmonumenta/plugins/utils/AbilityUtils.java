package com.playmonumenta.plugins.utils;

import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Cleric;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.classes.Rogue;
import com.playmonumenta.plugins.classes.Scout;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.classes.Warlock;
import com.playmonumenta.plugins.classes.Warrior;
import com.playmonumenta.plugins.cosmetics.skills.StealthCosmeticSkill;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class AbilityUtils {

	private static final String POTION_REFUNDED_METAKEY = "PotionRefunded";
	public static final String TOTAL_LEVEL = "TotalLevel";
	public static final String TOTAL_SPEC = "TotalSpec";
	public static final String TOTAL_ENHANCE = "TotalEnhance";
	public static final String REMAINING_SKILL = "Skill";
	public static final String REMAINING_SPEC = "SkillSpec";

	public static final String SCOREBOARD_CLASS_NAME = "Class";
	public static final String SCOREBOARD_SPEC_NAME = "Specialization";
	public static final String REMAINING_ENHANCE = "Enhancements";
	public static final String CHARM_POWER = "CharmPower";
	public static final String DEPTHS_CHARM_POWER = "DepthsCharmPower";

	public static final int MAX_SKILL_POINTS = 10;
	public static final int MAX_SPEC_POINTS = 4;

	public static final String PASSIVE_SOUNDS_DISABLED_TAG = "PassiveAbilitySoundsDisabled";

	private static final Map<Player, Integer> INVISIBLE_PLAYERS = new HashMap<>();
	private static @Nullable BukkitRunnable invisTracker = null;

	public static final String IGNORE_TAG = "summon_ignore";

	private static void startInvisTracker(Plugin plugin, @Nullable StealthCosmeticSkill cosmetic) {
		invisTracker = new BukkitRunnable() {
			@Override
			public void run() {
				if (INVISIBLE_PLAYERS.isEmpty()) {
					this.cancel();
				} else {
					Iterator<Entry<Player, Integer>> iter = INVISIBLE_PLAYERS.entrySet().iterator();
					while (iter.hasNext()) {
						Entry<Player, Integer> entry = iter.next();

						Player player = entry.getKey();
						if (player.isDead() || !player.isOnline()) {
							iter.remove();
							continue;
						}

						ItemStack item = player.getInventory().getItemInMainHand();
						if (entry.getValue() <= 0) {
							// Run after this loop is complete to avoid concurrent modification
							Bukkit.getScheduler().runTask(plugin, () -> removeStealth(plugin, player, false, cosmetic));
						} else if (ItemUtils.isPickaxe(item) || !(DepthsUtils.isWeaponItem(item) || ItemUtils.isProjectileWeapon(item))) {
							// Run after this loop is complete to avoid concurrent modification
							Bukkit.getScheduler().runTask(plugin, () -> removeStealth(plugin, player, false, cosmetic));
						} else {
							new PartialParticle(Particle.SMOKE_NORMAL, player.getLocation().clone().add(0, 0.5, 0), 1, 0.35, 0.25, 0.35, 0.05f).spawnAsPlayerBuff(player);
							entry.setValue(entry.getValue() - 1);
						}
					}
				}
			}
		};
		invisTracker.runTaskTimer(plugin, 0, 1);
	}

	public static boolean isStealthed(@Nullable Player player) {
		if (player == null) {
			return false;
		}
		return INVISIBLE_PLAYERS.containsKey(player) || Plugin.getInstance().mEffectManager.hasEffect(player, RespawnStasis.class);
	}

	public static void removeStealth(Plugin plugin, Player player, boolean inflictPenalty, @Nullable StealthCosmeticSkill cosmetic) {
		if (cosmetic == null) {
			defaultStealthRemove(player);
		} else {
			cosmetic.removeStealthCosmetic(player);
		}

		int currentDuration = INVISIBLE_PLAYERS.getOrDefault(player, 0);
		plugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.INVISIBILITY);

		INVISIBLE_PLAYERS.remove(player);

		if (inflictPenalty) {
			plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.SLOW_DIGGING, currentDuration, 1));
		}
	}

	public static void applyStealth(Plugin plugin, Player player, int duration) {
		applyStealth(plugin, player, duration, null);
	}

	public static void applyStealth(Plugin plugin, Player player, int duration, @Nullable StealthCosmeticSkill cosmetic) {
		if (cosmetic == null) {
			defaultStealthApply(player);
		} else {
			cosmetic.applyStealthCosmetic(player);
		}

		if (!isStealthed(player)) {
			plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0));
			INVISIBLE_PLAYERS.put(player, duration);
		} else {
			int currentDuration = INVISIBLE_PLAYERS.getOrDefault(player, 0);
			plugin.mPotionManager.removePotion(player, PotionID.ABILITY_SELF, PotionEffectType.INVISIBILITY);
			plugin.mPotionManager.addPotion(player, PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.INVISIBILITY, duration + currentDuration, 0));
			INVISIBLE_PLAYERS.put(player, duration + currentDuration);
		}

		if (invisTracker == null || invisTracker.isCancelled()) {
			startInvisTracker(plugin, cosmetic);
		}

		for (LivingEntity entity : EntityUtils.getNearbyMobs(player.getLocation(), 64)) {
			if (entity instanceof Mob mob) {
				if (mob.getTarget() != null && mob.getTarget().getUniqueId().equals(player.getUniqueId())) {
					mob.setTarget(null);
				}
			}
		}
	}

	public static void defaultStealthApply(Player player) {
		Location loc = player.getLocation();
		World world = player.getWorld();

		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.1f).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.5f).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_SNOW_GOLEM_DEATH, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.5f, 2f);
	}

	public static void defaultStealthRemove(Player player) {
		Location loc = player.getLocation();
		World world = player.getWorld();

		new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.1f).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc.clone().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.5f).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1f, 0.5f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 0.6f, 0.5f);
	}

	private static final String ABILITY_SILENCE_EFFECT_NAME = "AbilitySilence";

	public static void silencePlayer(Player player, int tickDuration) {
		Plugin.getInstance().mEffectManager.addEffect(player, ABILITY_SILENCE_EFFECT_NAME, new AbilitySilence(tickDuration));
	}

	public static void unsilencePlayer(Player player) {
		Plugin.getInstance().mEffectManager.clearEffects(player, ABILITY_SILENCE_EFFECT_NAME);
	}

	public static boolean isSilenced(Player player) {
		if (player == null) {
			return false;
		}
		return Plugin.getInstance().mEffectManager.hasEffect(player, ABILITY_SILENCE_EFFECT_NAME);
	}

	public static int getSilenceDuration(Player player) {
		Effect effect = Plugin.getInstance().mEffectManager.getActiveEffect(player, ABILITY_SILENCE_EFFECT_NAME);
		if (effect != null) {
			return effect.getDuration();
		} else {
			return 0;
		}
	}

	public static void increaseHealingPlayer(Player player, int duration, double healBoost, String cause) {
		Plugin.getInstance().mEffectManager.addEffect(player, cause, new PercentHeal(duration, healBoost));
		if (healBoost < 0) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.BAD_OMEN, duration, -1));
			if (healBoost <= -1.0) {
				player.sendActionBar(Component.text("You cannot heal for " + duration / 20 + "s", NamedTextColor.RED));
			} else {
				player.sendActionBar(Component.text("You have reduced healing for " + duration / 20 + "s", NamedTextColor.DARK_RED));
			}
		}
	}

	// the unluck potion effect does not increase nor decrease luck attribute
	public static void increaseDamageRecievedPlayer(Player player, int duration, double damageBoost, String cause) {
		Plugin.getInstance().mEffectManager.addEffect(player, cause, new PercentDamageReceived(duration, damageBoost));
		if (damageBoost > 0) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.UNLUCK, duration, -1));
		} else if (damageBoost < 0) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration, -1));
		}
	}

	// the weakness potion effect does not increase nor decrease melee damage
	public static void increaseDamageDealtPlayer(Player player, int duration, double damageBoost, String cause) {
		Plugin.getInstance().mEffectManager.addEffect(player, cause, new PercentDamageDealt(duration, damageBoost));
		if (damageBoost < 0) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, -1));
		} else if (damageBoost > 0) {
			player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration, -1));
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
			if (item != null && InventoryUtils.testForItemWithName(item, "Alchemist's Potion", true)) {
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

	public static boolean refundPotion(Player player, ThrownPotion potion) {
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		if (MetadataUtils.checkOnceThisTick(Plugin.getInstance(), player, POTION_REFUNDED_METAKEY)) {
			ItemStack item = potion.getItem();
			if (mainHand != null && mainHand.isSimilar(item) && !mainHand.containsEnchantment(Enchantment.ARROW_INFINITE)) {
				mainHand.setAmount(mainHand.getAmount() + 1);
				return true;
			}
		}
		return false;
	}

	public static String getClass(Player player) {
		return getClass(ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0));
	}

	public static String getClass(int classId) {
		return switch (classId) {
			case Mage.CLASS_ID -> "Mage";
			case Warrior.CLASS_ID -> "Warrior";
			case Cleric.CLASS_ID -> "Cleric";
			case Rogue.CLASS_ID -> "Rogue";
			case Alchemist.CLASS_ID -> "Alchemist";
			case Scout.CLASS_ID -> "Scout";
			case Warlock.CLASS_ID -> "Warlock";
			case Shaman.CLASS_ID -> "Shaman";
			default -> "No Class";
		};
	}

	public static int getClass(String str) {
		return switch (str) {
			case "Mage" -> Mage.CLASS_ID;
			case "Warrior" -> Warrior.CLASS_ID;
			case "Cleric" -> Cleric.CLASS_ID;
			case "Rogue" -> Rogue.CLASS_ID;
			case "Alchemist" -> Alchemist.CLASS_ID;
			case "Scout" -> Scout.CLASS_ID;
			case "Warlock" -> Warlock.CLASS_ID;
			case "Shaman" -> Shaman.CLASS_ID;
			default -> 0;
		};
	}

	public static String getSpec(Player player) {
		return getSpec(ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME).orElse(0));
	}

	public static String getSpec(int specId) {
		return switch (specId) {
			case Mage.ARCANIST_SPEC_ID -> "Arcanist";
			case Mage.ELEMENTALIST_SPEC_ID -> "Elementalist";
			case Warrior.BERSERKER_SPEC_ID -> "Berserker";
			case Warrior.GUARDIAN_SPEC_ID -> "Guardian";
			case Cleric.PALADIN_SPEC_ID -> "Paladin";
			case Cleric.HIEROPHANT_SPEC_ID -> "Hierophant";
			case Rogue.SWORDSAGE_SPEC_ID -> "Swordsage";
			case Rogue.ASSASSIN_SPEC_ID -> "Assassin";
			case Alchemist.HARBINGER_SPEC_ID -> "Harbinger";
			case Alchemist.APOTHECARY_SPEC_ID -> "Apothecary";
			case Scout.RANGER_SPEC_ID -> "Ranger";
			case Scout.HUNTER_SPEC_ID -> "Hunter";
			case Warlock.REAPER_SPEC_ID -> "Reaper";
			case Warlock.TENEBRIST_SPEC_ID -> "Tenebrist";
			case Shaman.SOOTHSAYER_ID -> "Soothsayer";
			case Shaman.HEXBREAKER_ID -> "Hexbreaker";
			default -> "No Spec";
		};
	}

	public static int getSpec(String str) {
		return switch (str) {
			case "Arcanist" -> Mage.ARCANIST_SPEC_ID;
			case "Elementalist" -> Mage.ELEMENTALIST_SPEC_ID;
			case "Berserker" -> Warrior.BERSERKER_SPEC_ID;
			case "Guardian" -> Warrior.GUARDIAN_SPEC_ID;
			case "Paladin" -> Cleric.PALADIN_SPEC_ID;
			case "Hierophant" -> Cleric.HIEROPHANT_SPEC_ID;
			case "Swordsage" -> Rogue.SWORDSAGE_SPEC_ID;
			case "Assassin" -> Rogue.ASSASSIN_SPEC_ID;
			case "Harbinger" -> Alchemist.HARBINGER_SPEC_ID;
			case "Apothecary" -> Alchemist.APOTHECARY_SPEC_ID;
			case "Ranger" -> Scout.RANGER_SPEC_ID;
			case "Hunter" -> Scout.HUNTER_SPEC_ID;
			case "Reaper" -> Warlock.REAPER_SPEC_ID;
			case "Tenebrist" -> Warlock.TENEBRIST_SPEC_ID;
			case "Soothsayer" -> Shaman.SOOTHSAYER_ID;
			case "Soulbreaker" -> Shaman.HEXBREAKER_ID;
			case "Hexbreaker" -> Shaman.HEXBREAKER_ID;
			default -> 0;
		};
	}

	public static final ImmutableSet<PotionEffectType> DEBUFFS = ImmutableSet.of(
		PotionEffectType.WITHER,
		PotionEffectType.SLOW,
		PotionEffectType.SLOW_DIGGING,
		PotionEffectType.POISON,
		PotionEffectType.BLINDNESS,
		PotionEffectType.CONFUSION,
		PotionEffectType.HUNGER
	);

	public static int getDebuffCount(Plugin plugin, LivingEntity entity) {
		int debuffCount = 0;
		for (PotionEffectType effectType : DEBUFFS) {
			PotionEffect effect = entity.getPotionEffect(effectType);
			if (effect != null) {
				debuffCount++;
			}
		}

		if (entity.getFireTicks() > 0) {
			debuffCount++;
		}

		if (EntityUtils.isStunned(entity)) {
			debuffCount++;
		}

		if (EntityUtils.isParalyzed(plugin, entity)) {
			debuffCount++;
		}

		if (EntityUtils.isSilenced(entity)) {
			debuffCount++;
		}

		if (EntityUtils.isBleeding(plugin, entity)) {
			debuffCount++;
		}

		//Custom slow effect interaction
		if (EntityUtils.isSlowed(plugin, entity) && entity.getPotionEffect(PotionEffectType.SLOW) == null) {
			debuffCount++;
		}

		//Custom weaken interaction
		if (EntityUtils.isWeakened(plugin, entity)) {
			debuffCount++;
		}

		//Custom vuln interaction
		if (EntityUtils.isVulnerable(plugin, entity)) {
			debuffCount++;
		}

		//Custom DoT interaction
		if (EntityUtils.hasDamageOverTime(plugin, entity)) {
			debuffCount++;
		}

		return debuffCount;
	}

	public static int getEffectiveTotalSkillPoints(Player player) {
		// fast track: full skill and spec points in R3; and also in plots if having been to R3 at least once
		if (ServerProperties.getAbilityEnhancementsEnabled(player)
			    && ScoreboardUtils.getScoreboardValue(player, "R3Access").orElse(0) > 0) {
			return MAX_SKILL_POINTS;
		}
		return ScoreboardUtils.getScoreboardValue(player, TOTAL_LEVEL).orElse(0);
	}

	public static int getEffectiveTotalSpecPoints(Player player) {
		// fast track: full skill and spec points in R3; and also in plots if having been to R3 at least once
		if (ServerProperties.getAbilityEnhancementsEnabled(player)
			    && ScoreboardUtils.getScoreboardValue(player, "R3Access").orElse(0) > 0) {
			return MAX_SPEC_POINTS;
		}
		return ScoreboardUtils.getScoreboardValue(player, TOTAL_SPEC).orElse(0);
	}

	public static boolean getEffectiveSpecs(Player player) {
		// fast track: all specs in R3; and also in plots if having been to R3 at least once
		return ServerProperties.getAbilityEnhancementsEnabled(player) && (ScoreboardUtils.getScoreboardValue(player, "R3Access").orElse(0) > 0);
	}

	public static void resetClass(Player player) {
		if (ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_CLASS_NAME).orElse(0) == 0) {
			player.sendMessage(Component.text("You do not have a class.", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));
			return;
		}
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_CLASS_NAME, 0);
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_SPEC_NAME, 0);
		AbilityManager.getManager().resetPlayerAbilities(player);
		updateAbilityScores(player);
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1, 0.7f);
		player.sendMessage(Component.text("Your skill points have been reset. You can pick a new class now.", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));
		refreshClass(player);
	}

	public static void resetSpec(Player player) {
		if (ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_SPEC_NAME).orElse(0) == 0) {
			player.sendMessage(Component.text("You do not have a specialization.", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));
			return;
		}
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_SPEC_NAME, 0);
		updateAbilityScores(player);
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1, 0.7f);
		player.sendMessage(Component.text("Your specialization points have been reset. You can pick a new specialization now.", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));
		refreshClass(player);
	}

	public static void refreshClass(Player player) {
		AbilityManager.getManager().updatePlayerAbilities(player, true);
		InventoryUtils.scheduleDelayedEquipmentCheck(Plugin.getInstance(), player, null);
		Bukkit.getConsoleSender().sendMessage(Component.text("Refreshed class for player '" + player.getName() + "'", NamedTextColor.GOLD));
	}

	public static void updateAbilityScores(Player player) {

		// Remove any skills from other classes (this is also used to clear skills by first clearing selected class and/or spec)

		int playerClass = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_CLASS_NAME).orElse(0);
		int playerSpec = ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_SPEC_NAME).orElse(0);
		MonumentaClasses mClasses = new MonumentaClasses();
		for (PlayerClass mClass : mClasses.mClasses) {
			if (playerClass != mClass.mClass) {
				for (AbilityInfo<?> ability : mClass.mAbilities) {
					String scoreboard = ability.getScoreboard();
					if (scoreboard != null) {
						ScoreboardUtils.setScoreboardValue(player, scoreboard, 0);
					}
				}
			}
			if (playerSpec != mClass.mSpecOne.mSpecialization) {
				for (AbilityInfo<?> ability : mClass.mSpecOne.mAbilities) {
					String scoreboard = ability.getScoreboard();
					if (scoreboard != null) {
						ScoreboardUtils.setScoreboardValue(player, scoreboard, 0);
					}
				}
			}
			if (playerSpec != mClass.mSpecTwo.mSpecialization) {
				for (AbilityInfo<?> ability : mClass.mSpecTwo.mAbilities) {
					String scoreboard = ability.getScoreboard();
					if (scoreboard != null) {
						ScoreboardUtils.setScoreboardValue(player, scoreboard, 0);
					}
				}
			}
			if (playerClass == mClass.mClass &&
				(mClass.mQuestReq != null && (ScoreboardUtils.getScoreboardValue(player, mClass.mQuestReq).orElse(0) < mClass.mQuestReqMin && !getEffectiveSpecs(player))) &&
				(mClass.mPermissionString != null && !player.hasPermission(mClass.mPermissionString))) {
				player.sendMessage(Component.text("You have not unlocked this class yet.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
				AbilityManager.getManager().resetPlayerAbilities(player);
				player.sendMessage(Component.text("Your class has been reset!", NamedTextColor.RED));
			} else if (playerSpec == mClass.mSpecOne.mSpecialization && ScoreboardUtils.getScoreboardValue(player, Objects.requireNonNull(mClass.mSpecOne.mSpecQuestScoreboard)).orElse(0) < 100 && !getEffectiveSpecs(player)) {
				player.sendMessage(Component.text("You have not unlocked this specialization yet.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
				AbilityManager.getManager().resetPlayerAbilities(player);
				player.sendMessage(Component.text("Your class has been reset!", NamedTextColor.RED));
			} else if (playerSpec == mClass.mSpecTwo.mSpecialization && ScoreboardUtils.getScoreboardValue(player, Objects.requireNonNull(mClass.mSpecTwo.mSpecQuestScoreboard)).orElse(0) < 100 && !getEffectiveSpecs(player)) {
				player.sendMessage(Component.text("You have not unlocked this specialization yet.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
				AbilityManager.getManager().resetPlayerAbilities(player);
				player.sendMessage(Component.text("Your class has been reset!", NamedTextColor.RED));
			}


		}

		// Update remaining skill point scores (and reset class if more skills are assigned than total skill points are available)

		int remainingSkillPoints = getEffectiveTotalSkillPoints(player);
		int remainingSpecPoints = getEffectiveTotalSpecPoints(player);
		int remainingEnhancementPoints = ScoreboardUtils.getScoreboardValue(player, TOTAL_ENHANCE).orElse(0);
		for (PlayerClass mClass : mClasses.mClasses) {
			if (mClass.mClass == ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_CLASS_NAME).orElse(0)) {
				List<AbilityInfo<?>> abilities = mClass.mAbilities;
				List<AbilityInfo<?>> specAbilities = mClass.mSpecOne.mAbilities;
				specAbilities.addAll(mClass.mSpecTwo.mAbilities);
				// Loop over base abilities
				for (AbilityInfo<?> ability : abilities) {
					// Enhanced ability
					String scoreboard = ability.getScoreboard();
					if (scoreboard == null) {
						continue;
					}
					int score = ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0);
					if (score > 2) {
						remainingEnhancementPoints--;
						remainingSkillPoints -= score - 2;
					} else {
						remainingSkillPoints -= score;
					}
				}
				// Loop over specs
				for (AbilityInfo<?> specAbility : specAbilities) {
					String scoreboard = specAbility.getScoreboard();
					if (scoreboard == null) {
						continue;
					}
					remainingSpecPoints -= ScoreboardUtils.getScoreboardValue(player, scoreboard).orElse(0);
				}
			}
		}

		if (remainingSkillPoints < 0 || remainingSpecPoints < 0 || remainingEnhancementPoints < 0) {
			player.sendMessage(Component.text("You had more skills than expected. Available skills have been reset.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
			AbilityManager.getManager().resetPlayerAbilities(player);
			player.sendMessage(Component.text("Your class has been reset!", NamedTextColor.RED));
		} else {
			ScoreboardUtils.setScoreboardValue(player, REMAINING_SKILL, remainingSkillPoints);
			ScoreboardUtils.setScoreboardValue(player, REMAINING_SPEC, remainingSpecPoints);
			ScoreboardUtils.setScoreboardValue(player, REMAINING_ENHANCE, remainingEnhancementPoints);
		}

	}

	private static final EnumSet<ClassAbility> TRIGGERS_ASPECTS = EnumSet.of(
		ClassAbility.ERUPTION,
		ClassAbility.QUAKE,
		ClassAbility.EXPLOSIVE,
		ClassAbility.ARCANE_STRIKE_ENHANCED,
		ClassAbility.PREDATOR_STRIKE,
		ClassAbility.ALCHEMIST_POTION,
		ClassAbility.ALCHEMICAL_ARTILLERY,
		ClassAbility.UNSTABLE_AMALGAM
	);

	public static boolean isAspectTriggeringEvent(DamageEvent event, Player player) {
		DamageEvent.DamageType type = event.getType();

		// Is:
		// Melee from a weapon that is not only a projectile weapon
		// Projectile
		// One of a few "class abilities" that trigger aspects (i.e. Eruption, Quake)
		return (type == DamageEvent.DamageType.MELEE && ItemStatUtils.isNotExclusivelyRanged(player.getInventory().getItemInMainHand())) || type == DamageEvent.DamageType.PROJECTILE || TRIGGERS_ASPECTS.contains(event.getAbility());
	}

	public static void produceDurationString(LivingEntity totem, ArmorStand target, int totalDuration, int currentDuration, double whirlwindBuffed, boolean decayBuffed) {
		Component indicatorName = Component.empty();
		if (decayBuffed) {
			indicatorName = indicatorName.append(Component.text("☠", NamedTextColor.BLACK)
				.append(Component.text("☠", NamedTextColor.DARK_GREEN)));
		}
		indicatorName = indicatorName.append(Component.text("[", NamedTextColor.WHITE));
		int fullDuration;
		int verticalBars;
		if (whirlwindBuffed != 0) {
			fullDuration = (int) (totalDuration * whirlwindBuffed);
			verticalBars = (int) (20 * whirlwindBuffed);
		} else {
			fullDuration = totalDuration;
			verticalBars = 20;
		}

		int greenIndicator = (int) (verticalBars * ((double) currentDuration / (double) fullDuration));
		int redIndicator = verticalBars - greenIndicator;
		for (int i = 0; i < verticalBars; i++) {
			if (greenIndicator > 0) {
				greenIndicator--;
				indicatorName = indicatorName.append(Component.text("|", NamedTextColor.GREEN));
			} else if (redIndicator > 0 && i <= 20) {
				redIndicator--;
				indicatorName = indicatorName.append(Component.text("|", NamedTextColor.RED));
			} else if (redIndicator > 0) {
				redIndicator--;
				indicatorName = indicatorName.append(Component.text("|", NamedTextColor.YELLOW));
			}
		}
		indicatorName = indicatorName.append(Component.text("]", NamedTextColor.WHITE));
		if (decayBuffed) {
			indicatorName = indicatorName.append(Component.text("☠", NamedTextColor.DARK_GREEN)
				.append(Component.text("☠", NamedTextColor.BLACK)));
		}

		target.customName(indicatorName);
		target.teleport(totem.getEyeLocation().add(0, 0.5, 0));
	}

	public static void produceChargeUpString(LivingEntity totem, ArmorStand target, int chargeDuration, int currentDuration) {
		Component indicatorName = Component.empty();
		indicatorName = indicatorName.append(Component.text("[", NamedTextColor.WHITE));
		indicatorName = indicatorName.append(Component.text("   Charging Up...   ", NamedTextColor.RED));
		indicatorName = indicatorName.append(Component.text("]", NamedTextColor.WHITE));

		target.customName(indicatorName);
		target.teleport(totem.getEyeLocation().add(0, 0.5, 0));
	}

	public static Snowball spawnAbilitySnowball(Plugin plugin, Player player, World world, double velocity, String name, @Nullable Particle particle) {
		Location loc = player.getEyeLocation();
		Snowball proj = world.spawn(loc, Snowball.class);
		proj.setVelocity(loc.getDirection().normalize().multiply(velocity));
		proj.setShooter(player);
		if (particle != null) {
			plugin.mProjectileEffectTimers.addEntity(proj, particle);
		}
		Component nameComponent = Component.text(name);
		proj.customName(nameComponent);
		ItemStack item = proj.getItem();
		ItemMeta meta = item.getItemMeta();
		meta.displayName(nameComponent);
		item.setItemMeta(meta);
		ItemUtils.setPlainName(item);
		proj.setItem(item);
		return proj;
	}

	public static void playPassiveAbilitySound(Location location, Sound sound, float volume, float pitch) {
		for (Player player : PlayerUtils.playersInRange(location, 50, true)) {
			playPassiveAbilitySound(player, location, sound, volume, pitch);
		}
	}

	public static void playPassiveAbilitySound(Player player, Location location, Sound sound, float volume, float pitch) {
		if (hasPassiveAbilitySoundsEnabled(player)) {
			player.playSound(location, sound, SoundCategory.PLAYERS, volume, pitch);
		}
	}

	public static boolean hasPassiveAbilitySoundsEnabled(Player player) {
		return !player.getScoreboardTags().contains(PASSIVE_SOUNDS_DISABLED_TAG);
	}

	public static Item spawnAbilityItem(World world, Location loc, Material mat, String name, boolean dropNaturally, double velocity, boolean glow, boolean invulnerable) {
		ItemStack stack = new ItemStack(mat);
		ItemMeta meta = stack.getItemMeta();
		meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		stack.setItemMeta(meta);
		if (ItemUtils.isSomePotion(mat)) {
			// Add infinity enchantment so that potion injector cannot use it if it is a potion
			ItemStatUtils.addEnchantment(stack, EnchantmentType.INFINITY, 1);
		}
		ItemUtils.setPlainName(stack, name);
		Item droppedItem;
		if (dropNaturally) {
			droppedItem = world.dropItemNaturally(loc, stack);
		} else {
			droppedItem = world.dropItem(loc, stack);
			Vector vel = loc.getDirection().normalize().multiply(velocity);
			droppedItem.setVelocity(vel);
		}
		droppedItem.setPickupDelay(Integer.MAX_VALUE);
		droppedItem.setGlowing(glow);
		if (invulnerable) {
			EntityUtils.makeItemInvulnereable(droppedItem);
		}
		return droppedItem;
	}
}
