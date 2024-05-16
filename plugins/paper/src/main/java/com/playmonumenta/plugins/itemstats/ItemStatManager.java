package com.playmonumenta.plugins.itemstats;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.ArrowConsumeEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.attributes.ProjectileSpeed;
import com.playmonumenta.plugins.itemstats.enchantments.AntiCritScaling;
import com.playmonumenta.plugins.itemstats.enchantments.CritScaling;
import com.playmonumenta.plugins.itemstats.enchantments.SKTQuestDamageDealt;
import com.playmonumenta.plugins.itemstats.enchantments.SKTQuestDamageTaken;
import com.playmonumenta.plugins.itemstats.enchantments.StrengthApply;
import com.playmonumenta.plugins.itemstats.enchantments.StrengthCancel;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemstats.infusions.Phylactery;
import com.playmonumenta.plugins.itemstats.infusions.Understanding;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ItemStatManager implements Listener {

	public static class PlayerItemStats {

		public static class ItemStatsMap implements Iterable<Entry<ItemStat, Double>> {
			private final Map<ItemStat, Double> mMap = new LinkedHashMap<>();

			public void add(ItemStat stat, double value) {
				if (value != 0) {
					Double old = mMap.get(stat);
					if (old == null) {
						mMap.put(stat, value);
					} else {
						double newValue = old + value;
						if (newValue == 0) {
							mMap.remove(stat);
						} else {
							mMap.put(stat, newValue);
						}
					}
				}
			}

			public void set(ItemStat stat, double value) {
				if (value != 0) {
					mMap.put(stat, value);
				} else {
					mMap.remove(stat);
				}
			}

			public double get(@Nullable ItemStat stat) {
				if (stat == null) {
					return 0;
				}
				Double value = mMap.get(stat);
				return value == null ? stat.getDefaultValue() : value;
			}

			public double get(@Nullable EnchantmentType type) {
				if (type == null) {
					return 0;
				}
				ItemStat stat = type.getItemStat();
				if (stat == null) {
					return 0;
				}
				return get(stat);
			}

			public void setTo(ItemStatsMap other) {
				mMap.clear();
				mMap.putAll(other.mMap);
			}

			@Override
			public Iterator<Entry<ItemStat, Double>> iterator() {
				return mMap.entrySet().iterator();
			}
		}

		private ItemStatsMap mArmorAddStats = new ItemStatsMap();
		private ItemStatsMap mArmorMultiplyStats = new ItemStatsMap();
		private ItemStatsMap mMainhandAddStats = new ItemStatsMap();
		private ItemStatsMap mStats = new ItemStatsMap();

		private Region mRegion;

		private Set<InfusionType> mArmorRelevationInfusions = new HashSet<>();

		/**
		 * The mainhand item held when these stats were last updated. Used to check for mainhand modifications by commands or similar.
		 */
		private @Nullable ItemStack mMainhand;

		public PlayerItemStats(Region region) {
			mRegion = region;
		}

		public PlayerItemStats(Player player) {
			mRegion = ServerProperties.getRegion(player);
		}

		public PlayerItemStats(PlayerItemStats playerItemStats) {
			mStats.setTo(playerItemStats.mStats);
			mMainhandAddStats.setTo(playerItemStats.mMainhandAddStats);
			mRegion = playerItemStats.mRegion;
		}

		public ItemStatsMap getItemStats() {
			return mStats;
		}

		public ItemStatsMap getMainhandAddStats() {
			return mMainhandAddStats;
		}

		public Region getRegion() {
			return mRegion;
		}

		public void setRegion(Region region) {
			mRegion = region;
		}

		public void updateStats(Player player, boolean updateAll, boolean checkHealth) {
			double priorHealth = EntityUtils.getMaxHealth(player);
			PlayerInventory inventory = player.getInventory();
			updateStats(inventory.getItemInMainHand(), inventory.getItemInOffHand(), inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots(), player, updateAll, null);
			// Tell the ItemStats that there has been an update
			Plugin plugin = Plugin.getInstance();
			for (ItemStat stat : ITEM_STATS) {
				stat.onEquipmentUpdate(plugin, player);
			}

			// If health changed, wipe Absorption + Max Health
			if (checkHealth) {
				Bukkit.getScheduler().runTaskLater(plugin, () -> {
					if (Math.abs(priorHealth - EntityUtils.getMaxHealth(player)) > 0.01) {
						player.setAbsorptionAmount(0);
						Plugin.getInstance().mEffectManager.clearEffects(player, EffectType.MAX_HEALTH_INCREASE.getName());
					}
				}, 1);
			}

			// Reapply anti-speed for worn gear changes.
			Bukkit.getScheduler().runTaskLater(plugin, () -> recalculateAntiSpeed(player), 1);
		}

		public void updateStats(@Nullable ItemStack mainhand, @Nullable ItemStack offhand, @Nullable ItemStack head, @Nullable ItemStack chest, @Nullable ItemStack legs, @Nullable ItemStack feet, Player player, boolean updateAll, @Nullable Region region) {
			mMainhand = mainhand;
			mRegion = region != null ? region : ServerProperties.getRegion(player);

			ItemStatsMap newArmorAddStats;
			ItemStatsMap newMainhandAddStats = new ItemStatsMap();
			ItemStatsMap newArmorMultiplyStats;
			ItemStatsMap newMainhandMultiplyStats = new ItemStatsMap();
			ItemStatsMap newStats = new ItemStatsMap();
			List<Infusion> baseInfusionsArmor = new ArrayList<>();
			List<Infusion> baseInfusionsMainhand = new ArrayList<>();
			Set<InfusionType> delveInfusionsWithRevelation = new HashSet<>();

			if (!updateAll && mMainhandAddStats.get(InfusionType.UNDERSTANDING.getItemStat()) != ItemStatUtils.getInfusionLevel(mainhand, InfusionType.UNDERSTANDING)) {
				updateStats(mainhand, player.getInventory().getItemInOffHand(), player.getInventory().getHelmet(), player.getInventory().getChestplate(), player.getInventory().getLeggings(), player.getInventory().getBoots(), player, true, region);
				return;
			}

			if (updateAll) {
				newArmorAddStats = new ItemStatsMap();
				newArmorMultiplyStats = new ItemStatsMap();

				EnumMap<Slot, ItemStack> items = new EnumMap<>(Slot.class);
				items.put(Slot.OFFHAND, offhand);
				items.put(Slot.HEAD, head);
				items.put(Slot.CHEST, chest);
				items.put(Slot.LEGS, legs);
				items.put(Slot.FEET, feet);

				for (Slot slot : Slot.values()) {
					if (slot == Slot.PROJECTILE) {
						continue;
					}
					ItemStack item = items.get(slot);
					if (item == null || item.getType() == Material.AIR) {
						continue;
					}

					if (slot.equals(Slot.OFFHAND) && ItemUtils.isArmorOrWearable(item) && !ItemStatUtils.hasAttributeInSlot(item, Slot.OFFHAND)) {
						continue;
					}

					NBT.get(item, nbt -> {
						ReadableNBT enchantments = ItemStatUtils.getEnchantments(nbt);
						ReadableNBT infusions = ItemStatUtils.getInfusions(nbt);
						ReadableNBTList<ReadWriteNBT> attributes = ItemStatUtils.getAttributes(nbt);

						double regionScaling = getEffectiveRegionScaling(player, item, mRegion, 1, 0.33, 0.165);
						boolean shattered = ItemStatUtils.getInfusionLevel(infusions, InfusionType.SHATTERED) > 0;
						boolean appliedUnderstanding = false;

						for (ItemStat stat : ITEM_STATS) {
							if (stat instanceof Attribute attribute) {
								double multiplier = attribute.getAttributeType().isRegionScaled() ? regionScaling : 1.0;
								newArmorAddStats.add(stat, ItemStatUtils.getAttributeAmount(attributes, attribute.getAttributeType(), Operation.ADD, slot) * multiplier);
								newArmorMultiplyStats.add(stat, ItemStatUtils.getAttributeAmount(attributes, attribute.getAttributeType(), Operation.MULTIPLY, slot) * multiplier);
							} else if (stat instanceof Enchantment enchantment) {
								double multiplier = enchantment.getEnchantmentType().isRegionScaled() ? regionScaling : 1.0;
								if (enchantment.getEnchantmentType() == EnchantmentType.MAINHAND_OFFHAND_DISABLE && ItemStatUtils.getEnchantmentLevel(enchantments, enchantment.getEnchantmentType()) > 0) {
									break;
								}
								if (enchantment.getSlots().contains(slot)) {
									newArmorAddStats.add(stat, ItemStatUtils.getEnchantmentLevel(enchantments, enchantment.getEnchantmentType()) * multiplier);
								}
								if (enchantment.getEnchantmentType() == EnchantmentType.REGION_SCALING_DAMAGE_TAKEN) {
									newArmorAddStats.set(stat, Math.max(newArmorAddStats.get(enchantment), getEffectiveRegionScaling(player, item, mRegion, 0, 1, 2)));
								}
							} else if (stat instanceof Infusion infusion) {
								if (!(infusion.getInfusionType().isDisabledByShatter() && shattered)) {
									int revelation = 0;
									int infusionLevel = ItemStatUtils.getInfusionLevel(infusions, infusion.getInfusionType());

									if (infusion.getInfusionType().isDelveInfusion() && infusion.getInfusionType() != InfusionType.REVELATION && infusionLevel > 0) {
										revelation = Math.min(1, ItemStatUtils.getInfusionLevel(infusions, InfusionType.REVELATION));
										if (revelation == 1) {
											delveInfusionsWithRevelation.add(infusion.getInfusionType());
										}
									}
									if (infusion.getInfusionType().isBaseInfusion() && infusionLevel > 0 && !appliedUnderstanding) {
										baseInfusionsArmor.add(infusion);
										appliedUnderstanding = true;
									}
									newArmorAddStats.add(stat, revelation + infusionLevel);
								}
							}
						}
					});
				}
				mArmorRelevationInfusions = new HashSet<>(delveInfusionsWithRevelation);
			} else {
				newArmorAddStats = mArmorAddStats;
				newArmorMultiplyStats = mArmorMultiplyStats;
				delveInfusionsWithRevelation.addAll(mArmorRelevationInfusions);
			}

			if (mainhand != null && mainhand.getType() != Material.AIR && !ItemUtils.isArmorOrWearable(mainhand)) {
				NBT.get(mainhand, nbt -> {
					ReadableNBT enchantments = ItemStatUtils.getEnchantments(nbt);
					ReadableNBT infusions = ItemStatUtils.getInfusions(nbt);
					ReadableNBTList<ReadWriteNBT> attributes = ItemStatUtils.getAttributes(nbt);

					double regionScaling = getEffectiveRegionScaling(player, mainhand, mRegion, 1, 0.33, 0.165);
					boolean appliedUnderstanding = false;

					if (!ItemStatUtils.hasEnchantment(mainhand, EnchantmentType.ALCHEMICAL_ALEMBIC) ||
							(ItemStatUtils.hasEnchantment(mainhand, EnchantmentType.ALCHEMICAL_ALEMBIC) && PlayerUtils.isAlchemist(player))) {
						for (ItemStat stat : ITEM_STATS) {
							if (stat instanceof Attribute attribute) {
								double multiplier = attribute.getAttributeType().isMainhandRegionScaled() ? regionScaling : 1.0;
								if (attribute instanceof ProjectileSpeed) {
									// Hack for mainhands using projectile speed multiply instead of add
									newMainhandAddStats.add(stat, (ItemStatUtils.getAttributeAmount(attributes, attribute.getAttributeType(), Operation.ADD, Slot.MAINHAND)
																		+ ItemStatUtils.getAttributeAmount(attributes, attribute.getAttributeType(), Operation.MULTIPLY, Slot.MAINHAND)) * multiplier);
								} else {
									newMainhandAddStats.add(stat, ItemStatUtils.getAttributeAmount(attributes, attribute.getAttributeType(), Operation.ADD, Slot.MAINHAND) * multiplier);
									newMainhandMultiplyStats.add(stat, ItemStatUtils.getAttributeAmount(attributes, attribute.getAttributeType(), Operation.MULTIPLY, Slot.MAINHAND) * multiplier);
								}
							} else if (stat instanceof Enchantment enchantment) {
								double multiplier = enchantment.getEnchantmentType().isRegionScaled() ? regionScaling : 1.0;
								if (enchantment.getEnchantmentType() == EnchantmentType.OFFHAND_MAINHAND_DISABLE && ItemStatUtils.getEnchantmentLevel(enchantments, enchantment.getEnchantmentType()) > 0) {
									break;
								}
								if (enchantment.getSlots().contains(Slot.MAINHAND)) {
									newMainhandAddStats.add(stat, ItemStatUtils.getEnchantmentLevel(enchantments, enchantment.getEnchantmentType()) * multiplier);
								}
								if (enchantment.getEnchantmentType() == EnchantmentType.REGION_SCALING_DAMAGE_DEALT) {
									newMainhandAddStats.add(stat, getEffectiveRegionScaling(player, mainhand, mRegion, 0, 1, 2));
								}
							} else if (stat instanceof Infusion infusion) {
								int revelation = 0;
								int infusionLevel = ItemStatUtils.getInfusionLevel(infusions, infusion.getInfusionType());

								if (infusion.getInfusionType().isDelveInfusion() && infusion.getInfusionType() != InfusionType.REVELATION && infusionLevel > 0) {
									revelation = Math.min(1, ItemStatUtils.getInfusionLevel(infusions, InfusionType.REVELATION));
									if (revelation == 1) {
										delveInfusionsWithRevelation.add(infusion.getInfusionType());
									}
								}
								if (infusion.getInfusionType().isBaseInfusion() && infusionLevel > 0 && !appliedUnderstanding) {
									baseInfusionsMainhand.add(infusion);
									appliedUnderstanding = true;
								}
								newMainhandAddStats.add(stat, revelation + infusionLevel);
							}
						}
					}
				});
			}

			int understanding = Math.min(delveInfusionsWithRevelation.contains(InfusionType.UNDERSTANDING) ? DelveInfusionUtils.MAX_LEVEL + 1 : DelveInfusionUtils.MAX_LEVEL,
				(int) (newArmorAddStats.get(InfusionType.UNDERSTANDING.getItemStat()) + newMainhandAddStats.get(InfusionType.UNDERSTANDING.getItemStat())));

			for (Infusion infusion : baseInfusionsArmor) {
				newArmorAddStats.set(infusion.getInfusionType().getItemStat(), newArmorAddStats.get(infusion.getInfusionType().getItemStat()) + understanding * Understanding.POINTS_PER_LEVEL);
			}

			for (Infusion infusion : baseInfusionsMainhand) {
				newMainhandAddStats.set(infusion.getInfusionType().getItemStat(), newMainhandAddStats.get(infusion.getInfusionType().getItemStat()) + understanding * Understanding.POINTS_PER_LEVEL);
			}

			for (ItemStat stat : ITEM_STATS) {
				double newAdd = newArmorAddStats.mMap.getOrDefault(stat, 0.0) + newMainhandAddStats.mMap.getOrDefault(stat, 0.0);
				double newMultiply = newArmorMultiplyStats.mMap.getOrDefault(stat, 0.0) + newMainhandMultiplyStats.mMap.getOrDefault(stat, 0.0);
				if (newAdd == 0 && newMultiply != 0 && stat instanceof Attribute) {
					newStats.set(stat, 1 + newMultiply);
				} else if (stat instanceof Phylactery) {
					newStats.set(stat, newAdd * (1 + newMultiply) + Phylactery.BASE_POTION_KEEP_LEVEL);
				} else if (stat instanceof Infusion infusion && infusion.getInfusionType().isDelveInfusion() && newAdd > 0) {
					newStats.set(stat, Math.min(newAdd, delveInfusionsWithRevelation.contains(infusion.getInfusionType()) ? DelveInfusionUtils.MAX_LEVEL + 1 : DelveInfusionUtils.MAX_LEVEL));
				} else {
					newStats.set(stat, newAdd * (1 + newMultiply));
				}
				if (stat instanceof CritScaling || stat instanceof AntiCritScaling ||
					    stat instanceof StrengthApply || stat instanceof StrengthCancel) {
					newStats.set(stat, 1);
				}
				if ((stat instanceof SKTQuestDamageDealt || stat instanceof SKTQuestDamageTaken)
					    && ServerProperties.getShardName().startsWith("skt")) {
					newStats.set(stat, 1);
				}
			}

			mArmorAddStats = newArmorAddStats;
			mArmorMultiplyStats = newArmorMultiplyStats;
			mMainhandAddStats = newMainhandAddStats;
			mStats = newStats;

			// Mainhand items take longer to update. Make sure to catch those.
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> recalculateAntiSpeed(player), 2);
		}

		public void print(Player player) {
			MessagingUtils.sendRawMessage(player, "");
			for (Entry<ItemStat, Double> entry : mStats) {
				MessagingUtils.sendRawMessage(player, String.format("%s: %f", entry.getKey().getName(), entry.getValue()));
			}
		}
	}

	private static final List<ItemStat> ITEM_STATS = new ArrayList<>();

	static {
		for (AttributeType type : AttributeType.values()) {
			if (type.getItemStat() != null) {
				ITEM_STATS.add(type.getItemStat());
			}
		}

		for (EnchantmentType type : EnchantmentType.values()) {
			if (type.getItemStat() != null) {
				ITEM_STATS.add(type.getItemStat());
			}
		}

		for (InfusionType type : InfusionType.values()) {
			if (type.getItemStat() != null) {
				ITEM_STATS.add(type.getItemStat());
			}
		}
		ITEM_STATS.sort(Comparator.comparingDouble(ItemStat::getPriorityAmount));
	}

	private final Plugin mPlugin;
	private final Map<UUID, PlayerItemStats> mPlayerItemStatsMappings = new HashMap<>();
	private static final int PERIOD = 5;

	public ItemStatManager(Plugin plugin) {
		mPlugin = plugin;

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks += PERIOD;
				boolean twoHertz = mTicks % 10 == 0;
				boolean oneHertz = mTicks % 20 == 0;

				try {
					try {
						Iterator<Entry<UUID, PlayerItemStats>> iterator = mPlayerItemStatsMappings.entrySet().iterator();
						while (iterator.hasNext()) {
							Entry<UUID, PlayerItemStats> entry = iterator.next();
							Player player = Bukkit.getPlayer(entry.getKey());
							if (player == null) {
								iterator.remove();
								continue;
							}
							tick(mPlugin, player, entry.getValue(), twoHertz, oneHertz);
						}
					} catch (Exception ex) {
						Plugin.getInstance().getLogger().severe("Error in item stat manager tick: " + ex.getMessage());
						ex.printStackTrace();
					}
				} catch (Exception ex) {
					Plugin.getInstance().getLogger().severe("SEVERE error in item stat manager ticking task that caused many pieces to be skipped: " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		}.runTaskTimer(plugin, 0, PERIOD);
	}

	public Map<UUID, PlayerItemStats> getPlayerItemStatsMappings() {
		return mPlayerItemStatsMappings;
	}

	private static final Set<Player> mUpdateStatsDelayed = new HashSet<>();

	private void updateStatsDelayed(Player player) {
		if (mUpdateStatsDelayed.isEmpty()) {
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				for (Player p : mUpdateStatsDelayed) {
					PlayerItemStats playerItemStats = mPlayerItemStatsMappings.get(p.getUniqueId());
					if (playerItemStats != null) {
						playerItemStats.updateStats(p, true, true);
					}
				}
				mUpdateStatsDelayed.clear();
			});
		}
		mUpdateStatsDelayed.add(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		PlayerItemStats playerItemStats = new PlayerItemStats(player);
		mPlayerItemStatsMappings.put(player.getUniqueId(), playerItemStats);
		playerItemStats.updateStats(player, true, false);
		updateStatsDelayed(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerQuitEvent(PlayerQuitEvent event) {
		mPlayerItemStatsMappings.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerRespawnEvent(PlayerRespawnEvent event) {
		updateStatsDelayed(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.useItemInHand() == Result.DENY) {
			return;
		}

		if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && ItemUtils.isArmor(event.getItem())) {
			updateStatsDelayed(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
		updateStatsDelayed(event.getPlayer());
	}

	// It is possible to switch items fast enough that we need an exact stat update
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerItemHeldEvent(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		PlayerInventory inv = player.getInventory();
		ItemStack oldItem = inv.getItem(event.getPreviousSlot());
		ItemStack newItem = inv.getItem(event.getNewSlot());
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			mPlayerItemStatsMappings.get(player.getUniqueId()).updateStats(newItem, null, null, null, null, null, player, false, null);
			for (ItemStat stat : ITEM_STATS) {
				stat.onEquipmentUpdate(mPlugin, player);
			}
		}
		if (newItem != null && oldItem != null && newItem.getType() == oldItem.getType()) {
			PlayerUtils.resetAttackCooldown(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		Player player = event.getPlayer();
		updateStatsDelayed(player);
		PlayerUtils.resetAttackCooldown(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerDropItemEvent(PlayerDropItemEvent event) {
		updateStatsDelayed(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerItemBreakEvent(PlayerItemBreakEvent event) {
		updateStatsDelayed(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityPickupItemEvent(EntityPickupItemEvent event) {
		Entity entity = event.getEntity();
		if (!event.isCancelled() && entity instanceof Player player) {
			updateStatsDelayed(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void inventoryClickEvent(InventoryClickEvent event) {
		HumanEntity human = event.getWhoClicked();
		if (human instanceof Player player) {
			updateStatsDelayed(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void inventoryDragEvent(InventoryDragEvent event) {
		HumanEntity human = event.getWhoClicked();
		if (human instanceof Player player) {
			updateStatsDelayed(player);
		}
	}

	public PlayerItemStats getPlayerItemStats(Player player) {
		PlayerItemStats playerItemStats = mPlayerItemStatsMappings.get(player.getUniqueId());
		if (playerItemStats == null) {
			playerItemStats = new PlayerItemStats(player);
			mPlayerItemStatsMappings.put(player.getUniqueId(), playerItemStats);
			playerItemStats.updateStats(player, true, false);
		}
		return playerItemStats;
	}

	public int getEnchantmentLevel(Player player, EnchantmentType type) {
		return getEnchantmentLevel(getPlayerItemStats(player), type);
	}

	public int getEnchantmentLevel(@Nullable PlayerItemStats playerItemStats, EnchantmentType type) {
		if (playerItemStats == null) {
			return 0;
		}
		return (int) playerItemStats.getItemStats().get(type.getItemStat());
	}

	public double getInfusionLevel(Player player, InfusionType type) {
		return getInfusionLevel(getPlayerItemStats(player), type);
	}

	public double getInfusionLevel(@Nullable PlayerItemStats playerItemStats, InfusionType type) {
		if (playerItemStats == null) {
			return 0;
		}
		return playerItemStats.getItemStats().get(type.getItemStat());
	}

	public double getAttributeAmount(Player player, AttributeType type) {
		return getAttributeAmount(getPlayerItemStats(player), type);
	}

	public double getAttributeAmount(@Nullable PlayerItemStats playerItemStats, AttributeType type) {
		if (playerItemStats == null) {
			return 0;
		}
		return playerItemStats.getItemStats().get(type.getItemStat());
	}

	public void tick(Plugin plugin, Player player, PlayerItemStats stats, boolean twoHz, boolean oneHz) {
		if (!Objects.equals(stats.mMainhand, player.getInventory().getItemInMainHand())) {
			stats.updateStats(player, false, true);
		}
		for (Entry<ItemStat, Double> entry : stats.getItemStats()) {
			entry.getKey().tick(plugin, player, entry.getValue(), twoHz, oneHz);
		}
	}

	public void onDamage(Plugin plugin, Player player, DamageEvent event, LivingEntity enemy) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			onDamage(plugin, player, mPlayerItemStatsMappings.get(player.getUniqueId()), event, enemy);
		}
	}

	public void onDamage(Plugin plugin, Player player, PlayerItemStats stats, DamageEvent event, LivingEntity enemy) {
		for (Entry<ItemStat, Double> entry : stats.getItemStats()) {
			if (event.isCancelled()) {
				return;
			}
			entry.getKey().onDamage(plugin, player, entry.getValue(), event, enemy);
		}
	}

	public void onDamageDelayed(Plugin plugin, Player player, DamageEvent event, LivingEntity enemy) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			onDamageDelayed(plugin, player, mPlayerItemStatsMappings.get(player.getUniqueId()), event, enemy);
		}
	}

	public void onDamageDelayed(Plugin plugin, Player player, PlayerItemStats stats, DamageEvent event, LivingEntity enemy) {
		for (Entry<ItemStat, Double> entry : stats.getItemStats()) {
			if (event.isCancelled()) {
				return;
			}
			entry.getKey().onDamageDelayed(plugin, player, entry.getValue(), event, enemy);
		}
	}

	public void onHurt(Plugin plugin, Player player, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				if (event.isCancelled()) {
					return;
				}
				entry.getKey().onHurt(plugin, player, entry.getValue(), event, damager, source);
			}
		}
	}

	public void onHurtFatal(Plugin plugin, Player player, DamageEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				if (event.isCancelled() || event.getFinalDamage(true) < player.getHealth()) {
					return;
				}
				entry.getKey().onHurtFatal(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onLoadCrossbow(Plugin plugin, Player player, EntityLoadCrossbowEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onLoadCrossbow(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onLaunchProjectile(Plugin plugin, Player player, ProjectileLaunchEvent event, Projectile projectile) {
		PlayerItemStats stats = DamageListener.getProjectileItemStats(projectile);
		if (stats != null) {
			for (Entry<ItemStat, Double> entry : stats.getItemStats()) {
				entry.getKey().onProjectileLaunch(plugin, player, entry.getValue(), event, projectile);
			}
		}
	}

	public void onConsumeArrow(Plugin plugin, Player player, ArrowConsumeEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onConsumeArrow(plugin, player, entry.getValue(), event);
				if (event.isCancelled()) {
					return;
				}
			}
		}
	}

	public void onProjectileHit(Plugin plugin, Player player, ProjectileHitEvent event, Projectile projectile) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onProjectileHit(plugin, player, entry.getValue(), event, projectile);
			}
		}
	}

	public void onKill(Plugin plugin, Player player, EntityDeathEvent event, LivingEntity enemy) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onKill(plugin, player, entry.getValue(), event, enemy);
			}
		}
	}

	public void onBlockBreak(Plugin plugin, Player player, BlockBreakEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onBlockBreak(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onPlayerInteract(Plugin plugin, Player player, PlayerInteractEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onPlayerInteract(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onPlayerSwapHands(Plugin plugin, Player player, PlayerSwapHandItemsEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onPlayerSwapHands(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onPlayerDropItem(Plugin plugin, Player player) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onPlayerDropItem(plugin, player, entry.getValue());
			}
		}
	}

	public void onConsume(Plugin plugin, Player player, PlayerItemConsumeEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onConsume(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onPlayerPotionSplashEvent(Plugin plugin, Player player, PotionSplashEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onPlayerPotionSplash(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void playerRegainHealthEvent(Plugin plugin, Player player, EntityRegainHealthEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onRegain(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onItemDamage(Plugin plugin, Player player, PlayerItemDamageEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onItemDamage(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onExpChange(Plugin plugin, Player player, PlayerExpChangeEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onExpChange(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onCombust(Plugin plugin, Player player, EntityCombustEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onCombust(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onDeath(Plugin plugin, Player player, PlayerDeathEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onDeath(plugin, player, entry.getValue(), event);
			}
		}
	}

	public void onRiptide(Plugin plugin, Player player, PlayerRiptideEvent event) {
		if (mPlayerItemStatsMappings.containsKey(player.getUniqueId())) {
			for (Entry<ItemStat, Double> entry : mPlayerItemStatsMappings.get(player.getUniqueId()).getItemStats()) {
				entry.getKey().onRiptide(plugin, player, entry.getValue(), event);
			}
		}
	}

	/*
	 * Watch for spawned or loaded items
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		if (event.getEntity() instanceof Item item) {
			// delayed to not run in the EntityAddToWorldEvent which is finicky
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> checkSpawnedItem(item));
		}
	}

	private void checkSpawnedItem(Item item) {
		if (item != null) {
			ItemStack stack = item.getItemStack();
			if (stack.getType().isAir()) {
				UUID throwerUUID = item.getThrower();
				if (throwerUUID != null) {
					Entity thrower = Bukkit.getEntity(throwerUUID);
					if (thrower != null) {
						MMLog.warning("Item spawned into world, but is air. Thrown by " + thrower);
						return;
					}
				}
				MMLog.warning("Item spawned into world, but is air.");
				return;
			}

			NBT.get(stack, nbt -> {
				ReadableNBT enchantments = ItemStatUtils.getEnchantments(nbt);
				ReadableNBT infusions = ItemStatUtils.getInfusions(nbt);

				for (EnchantmentType ench : EnchantmentType.SPAWNABLE_ENCHANTMENTS) {
					int level = ItemStatUtils.getEnchantmentLevel(enchantments, ench);
					if (level > 0) {
						Objects.requireNonNull(ench.getItemStat()).onSpawn(mPlugin, item, level);
					}
				}

				for (InfusionType infusion : InfusionType.SPAWNABLE_INFUSIONS) {
					int level = ItemStatUtils.getInfusionLevel(infusions, infusion);
					if (level > 0) {
						Objects.requireNonNull(infusion.getItemStat()).onSpawn(mPlugin, item, level);
					}
				}
			});
		}
	}

	public PlayerItemStats getPlayerItemStatsCopy(Player player) {
		return new PlayerItemStats(getPlayerItemStats(player));
	}

	public void updateStats(Player player) {
		PlayerItemStats stats = getPlayerItemStats(player);
		if (stats != null) {
			stats.updateStats(player, true, true);
		}
	}

	public static double getRegionScaling(Player player, Region itemRegion, Region serverRegion, double baseScaling, double oneRegionScaling, double twoRegionScaling) {
		if (itemRegion == Region.RING) {
			return serverRegion == Region.VALLEY ? twoRegionScaling
				       : serverRegion == Region.ISLES ? oneRegionScaling
					         : baseScaling;
		} else if (itemRegion == Region.ISLES) {
			// TODO Remove this if-statement after we get rid of R2 in R3 penalty
			if (serverRegion == Region.RING
				    && !(player.getScoreboardTags().contains("SKTQuest") && ServerProperties.getShardName().startsWith("skt"))
				    && !(ServerProperties.getShardName().startsWith("dev") || ServerProperties.getShardName().contains("plots") || ServerProperties.getShardName().equals("mobs") || player.getGameMode() == GameMode.CREATIVE)) {
				return oneRegionScaling;
			}
			return serverRegion == Region.VALLEY ? oneRegionScaling : baseScaling;
		}
		return baseScaling;
	}

	public static double getEffectiveRegionScaling(Player player, @Nullable ItemStack item, Region serverRegion, double baseScaling, double oneRegionScaling, double twoRegionScaling) {
		Region region;
		if (item == null || ItemStatUtils.hasEnchantment(item, EnchantmentType.WORLDLY_PROTECTION)) {
			region = Region.VALLEY;
		} else {
			region = ItemStatUtils.getRegion(item);
		}
		return getRegionScaling(player, region, serverRegion, baseScaling, oneRegionScaling, twoRegionScaling);
	}

	/**
	 * This method handles the Anti Speed zone, which can be used to cancel speed gear.
	 * The movement penalty attribute has to be recalculated every time the player changes gear, since they could
	 * have put on or removed speed gear.
	 * The recalculation is done only if the player is still in an Anti Speed zone.
	 * PlayerTracking already handles applying and removing the speed penalty when entering/leaving the zone.
	 * @param player the affected player.
	 */
	private static void recalculateAntiSpeed(Player player) {
		// Remove anti speed, if the player has it.
		if (EntityUtils.hasAttributesContaining(player, org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED, Constants.ANTI_SPEED_MODIFIER)) {
			EntityUtils.removeAttribute(player, org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED, Constants.ANTI_SPEED_MODIFIER);
		}
		// Calculate and apply anti speed if the player is in an anti speed zone.
		if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.ANTI_SPEED)) {
			PlayerUtils.cancelGearSpeed(player);
		}
	}

	public static Map<AttributeType, Double> getDamageMultipliers(PlayerItemStats playerItemStats) {
		PlayerItemStats.ItemStatsMap itemStatsMap = playerItemStats.getItemStats();
		List<AttributeType> types = Arrays.asList(AttributeType.ATTACK_DAMAGE_MULTIPLY, AttributeType.PROJECTILE_DAMAGE_MULTIPLY, AttributeType.MAGIC_DAMAGE_MULTIPLY);
		Map<AttributeType, Double> map = new HashMap<>();
		for (AttributeType type : types) {
			map.put(type, itemStatsMap.get(type.getItemStat()));
		}
		return map;
	}

}
