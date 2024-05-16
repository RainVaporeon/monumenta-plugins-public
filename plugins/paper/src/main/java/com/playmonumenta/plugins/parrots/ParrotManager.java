package com.playmonumenta.plugins.parrots;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.plots.AnimalLimits;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ParrotManager implements Listener {

	public static final String PARROT_LOCKBOX_SWAP_TAG = "ParrotLockboxSwap";

	protected static final String SHOULDER_PARROT_TAG = "ParrotPet";
	protected static final String PLACED_PARROT_TAG = "PlacedParrotPet";

	public enum ParrotVariant {
		//minecraft default
		RED("Scarlet Macaw", 1, Parrot.Variant.RED, Material.RED_WOOL),
		BLUE("Hyacinth Macaw", 2, Parrot.Variant.BLUE, Material.BLUE_WOOL),
		CYAN("Blue-Yellow Macaw", 3, Parrot.Variant.CYAN, Material.LIGHT_BLUE_WOOL),
		GREEN("Green Parakeet", 4, Parrot.Variant.GREEN, Material.GREEN_WOOL),
		GRAY("Gray Cockatiel", 5, Parrot.Variant.GRAY, Material.LIGHT_GRAY_WOOL),

		//added with texture
		PATREON("Patreon Parakeet", 6, Parrot.Variant.RED, Material.ORANGE_WOOL) {
			@Override
			public boolean hasUnlocked(Player player) {
				return ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS)
					.orElse(0) >= Constants.PATREON_TIER_1;
			}
		},
		PULSATING_GOLD("Golden Conure", 7, Parrot.Variant.CYAN, Material.YELLOW_CONCRETE),
		PULSATING_EMERALD("Emerald Conure", 8, Parrot.Variant.GREEN, Material.GREEN_CONCRETE),
		PIRATE("Scoundrel Macaw", 9, Parrot.Variant.BLUE, Material.PURPLE_WOOL),
		KAUL("Blackroot Kakapo", 10, Parrot.Variant.GREEN, Material.JUNGLE_LEAVES),
		ELDRASK("Permafrost Kea", 11, Parrot.Variant.CYAN, Material.BLUE_ICE),
		RAINBOW("Rainbow Parrot", 12, Parrot.Variant.CYAN, Material.YELLOW_GLAZED_TERRACOTTA),
		SNOWY("Snowy Cockatoo", 13, Parrot.Variant.GRAY, Material.SNOW_BLOCK),
		DEPTHS("Otherworldly Myiopsitta", 14, Parrot.Variant.RED, Material.RED_GLAZED_TERRACOTTA),
		DEPTHS_UPGRADE("Otherworldly Myiopsitta (u)", 15, Parrot.Variant.BLUE, Material.CRYING_OBSIDIAN),
		BEE("Bee Conure", 16, Parrot.Variant.CYAN, Material.HONEYCOMB_BLOCK),
		RADIANT("Radiant Conure", 17, Parrot.Variant.CYAN, Material.GLOWSTONE),
		HEKAWT("Veil Electus", 18, Parrot.Variant.BLUE, Material.MAGMA_BLOCK),
		BLITZ("Plushy Parrot", 19, Parrot.Variant.RED, Material.RED_WOOL),
		CORRIDORS("Crimson Lace", 20, Parrot.Variant.RED, Material.NETHER_WART_BLOCK),
		TWISTED("Twisted Torrap", 21, Parrot.Variant.BLUE, Material.BEDROCK);

		private final String mName;
		private final int mNumber;
		private final Parrot.Variant mVariant;
		private final Material mDisplayitem;

		ParrotVariant(String name, int num, Parrot.Variant variant, Material displayitem) {
			this.mName = name;
			this.mNumber = num;
			this.mVariant = variant;
			mDisplayitem = displayitem;
		}

		public int getNumber() {
			return this.mNumber;
		}

		public String getName() {
			return this.mName;
		}

		public Parrot.Variant getVariant() {
			return this.mVariant;
		}

		public Material getDisplayitem() {
			return mDisplayitem;
		}

		public static @Nullable ParrotVariant getVariantByNumber(int number) {
			if (number == 0) {
				return null;
			}
			for (ParrotVariant variant : values()) {
				if (variant.mNumber == number) {
					return variant;
				}
			}
			return null;
		}

		public String getScoreboard() {
			return "ParrotBought" + mNumber;
		}

		public boolean hasUnlocked(Player player) {
			return ScoreboardUtils.getScoreboardValue(player, getScoreboard()).orElse(0) > 0;
		}
	}

	public enum PlayerShoulder {
		LEFT,
		RIGHT,
		NONE;
	}


	private static @Nullable Plugin mPlugin;

	private static final String SCOREBOARD_PARROT_VISIBLE = "ParrotVisible";
	// 0 if invisible, 1 if visible.

	private static final String SCOREBOARD_PARROT_BOTH = "ParrotBoth";
	// 0 if player can hold only one parrot

	public static final String SCOREBOARD_PARROT_LEFT = "ParrotLeft";
	// store the info about which parrot is on the left shoulder

	public static final String SCOREBOARD_PARROT_RIGHT = "ParrotRight";
	// store the info about which parrot is on the right shoulder

	private static final Set<Player> mPrideRight = new HashSet<>();
	private static final Set<Player> mPrideLeft = new HashSet<>();

	private static final Map<Player, ParrotVariant> mLeftShoulders = new HashMap<>();
	private static final Map<Player, ParrotVariant> mRightShoulders = new HashMap<>();

	private static final int PRIDE_FREQUENCY = 3;

	private static @Nullable BukkitRunnable mPrideRunnable;

	public ParrotManager(Plugin plugin) {
		mPlugin = plugin;
		mPrideRunnable = null;

		// Periodically updates all players' parrots.
		// Workaround for an Optifine bug that only shows custom parrot textures if the parrot has been a standalone entity before it was put on a shoulder.
		// Updates only a few players at a time to spread out server load, as this causes noticeable lag when done for many players at once.
		// TODO temporarily disabled because Optifine is no longer displaying shoulder parrot textures even with this hack
		// Since it worked with 1.18 client and 1.16 server, somehow it must still be possible, so this is left in as a base to work off of.
		/*
				new BukkitRunnable() {
					Iterator<? extends Player> mPlayers = Collections.emptyIterator();

					@Override
		*/
	}
	/*
				public void run() {
					if (!mPlayers.hasNext()) {
						mPlayers = ImmutableList.copyOf(Bukkit.getOnlinePlayers()).iterator();
					}
					for (int i = 0; i < 10 && mPlayers.hasNext(); i++) {
						Player player = mPlayers.next();
						// Flying players lose parrots almost instantly, causing flickering, so don't update parrots for them. They'll get their parrots back once they land.
						if (player.isOnline() && !player.isDead() && !player.isFlying() && !PremiumVanishIntegration.isInvisibleOrSpectator(player)) {
							respawnParrots(player);
						}
					}
				}
			}.runTaskTimer(plugin, 10 * 20L, 10 * 20L); // low priority task, so can start after a long delay
	 */

	private static void respawnParrots(Player player) {
		Location spawnLocation = player.getLocation().add(0, -256, 0);

		ParrotVariant leftParrot = mLeftShoulders.get(player);
		if (leftParrot != null) {
			Parrot parrot = new ParrotPet(leftParrot, player).spawnParrot(spawnLocation);
			parrot.addScoreboardTag(ParrotManager.SHOULDER_PARROT_TAG);
			parrot.setInvisible(true);
			player.setShoulderEntityLeft(parrot);
		}

		ParrotVariant rightParrot = mRightShoulders.get(player);
		if (rightParrot != null) {
			Parrot parrot = new ParrotPet(rightParrot, player).spawnParrot(spawnLocation);
			parrot.addScoreboardTag(ParrotManager.SHOULDER_PARROT_TAG);
			parrot.setInvisible(true);
			player.setShoulderEntityRight(parrot);
		}
	}

	public static @Nullable ParrotVariant getLeftShoulderParrotVariant(Player player) {
		return getParrotVariantFromScoreboard(player, SCOREBOARD_PARROT_LEFT);
	}

	public static @Nullable ParrotVariant getRightShoulderParrotVariant(Player player) {
		return getParrotVariantFromScoreboard(player, SCOREBOARD_PARROT_RIGHT);
	}

	private static @Nullable ParrotVariant getParrotVariantFromScoreboard(Player player, String objective) {
		return areParrotsVisible(player) ? ParrotVariant.getVariantByNumber(ScoreboardUtils.getScoreboardValue(player, objective).orElse(0)) : null;
	}

	public static void updateParrots(Player player) {

		ParrotVariant leftVariant = getLeftShoulderParrotVariant(player);
		ParrotVariant rightVariant = getRightShoulderParrotVariant(player);

		// Verify player unlocks and remove if anything doesn't match
		if (!hasDoubleShoulders(player) && leftVariant != null && rightVariant != null) {
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_RIGHT, 0);
			rightVariant = null;
		}
		if (leftVariant != null && !leftVariant.hasUnlocked(player)) {
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_LEFT, 0);
			leftVariant = null;
		}
		if (rightVariant != null && !rightVariant.hasUnlocked(player)) {
			ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_RIGHT, 0);
			rightVariant = null;
		}

		if (leftVariant != null) {
			mLeftShoulders.put(player, leftVariant);
			if (leftVariant == ParrotVariant.RAINBOW) {
				mPrideLeft.add(player);
			} else {
				mPrideLeft.remove(player);
			}
		} else {
			mLeftShoulders.remove(player);
			mPrideLeft.remove(player);
		}
		if (rightVariant != null) {
			mRightShoulders.put(player, rightVariant);
			if (rightVariant == ParrotVariant.RAINBOW) {
				mPrideRight.add(player);
			} else {
				mPrideRight.remove(player);
			}
		} else {
			mRightShoulders.remove(player);
			mPrideRight.remove(player);
		}

		player.setShoulderEntityLeft(null);
		player.setShoulderEntityRight(null);
		respawnParrots(player);

		if (mPrideRunnable == null && (!mPrideLeft.isEmpty() || !mPrideRight.isEmpty())) {
			// no task is running so create a new one
			mPrideRunnable = new BukkitRunnable() {
				static final Parrot.Variant[] VARIANTS = Parrot.Variant.values();
				int mVariant = 0;

				@Override
				public void run() {
					if (mPrideLeft.isEmpty() && mPrideRight.isEmpty()) {
						this.cancel();
					}

					if (this.isCancelled()) {
						mPrideLeft.clear();
						mPrideRight.clear();
						mPrideRunnable = null;
						return;
					}

					mVariant = (mVariant + 1) % VARIANTS.length;
					Parrot.Variant variant = VARIANTS[mVariant];
					for (Player player : mPrideRight) {
						if (player.getShoulderEntityRight() instanceof Parrot parrot) {
							parrot.setVariant(variant);
							player.setShoulderEntityRight(parrot);
						}
					}

					for (Player player : mPrideLeft) {
						if (player.getShoulderEntityLeft() instanceof Parrot parrot) {
							parrot.setVariant(variant);
							player.setShoulderEntityLeft(parrot);
						}
					}
				}
			};
			mPrideRunnable.runTaskTimer(mPlugin, 0, PRIDE_FREQUENCY);
		}
	}

	public static void setParrotVisible(Player player, boolean visible) {
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE, visible ? 1 : 0);
		updateParrots(player);
	}

	public static void clearParrots(Player player) {
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE, 0);
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_LEFT, 0);
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_RIGHT, 0);
		mPrideLeft.remove(player);
		mPrideRight.remove(player);
		mLeftShoulders.remove(player);
		mRightShoulders.remove(player);
		updateParrots(player);
	}

	public static void selectParrot(Player player, ParrotVariant variant, PlayerShoulder shoulder) {
		ScoreboardUtils.setScoreboardValue(player, shoulder == PlayerShoulder.LEFT ? SCOREBOARD_PARROT_LEFT : SCOREBOARD_PARROT_RIGHT, variant.mNumber);
		if (!hasDoubleShoulders(player)) {
			// if the player only has a single shoulder available, remove the parrot from the other shoulder (if applicable)
			ScoreboardUtils.setScoreboardValue(player, shoulder == PlayerShoulder.LEFT ? SCOREBOARD_PARROT_RIGHT : SCOREBOARD_PARROT_LEFT, 0);
		}
		ScoreboardUtils.setScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE, 1);
		updateParrots(player);
	}

	public static boolean hasParrotOnShoulders(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_LEFT).orElse(0) != 0 || ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_RIGHT).orElse(0) != 0;
	}

	public static boolean areParrotsVisible(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_VISIBLE).orElse(0) != 0;
	}

	public static boolean hasDoubleShoulders(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, SCOREBOARD_PARROT_BOTH).orElse(0) > 0;
	}

	/**
	 * Spawns a parrot as a standalone entity, not on a player's shoulders.
	 */
	public static void spawnParrot(Player player, ParrotVariant variant) {
		if (!AnimalLimits.maySummonPlotAnimal(player.getLocation())) {
			// sends an error message already
			return;
		}
		// only one parrot of each type is allowed per plot, so remove any matching existing ones first
		for (Parrot parrot : player.getWorld().getEntitiesByClass(Parrot.class)) {
			if (parrot.getVariant() == variant.getVariant()
				    && parrot.getScoreboardTags().contains(PLACED_PARROT_TAG)
				    && variant.getName().equals(MessagingUtils.plainText(parrot.customName()))) {
				parrot.remove();
			}
		}
		Parrot parrot = new ParrotPet(variant, player).spawnParrot(player.getLocation());
		parrot.addScoreboardTag(ParrotManager.PLACED_PARROT_TAG);
		NmsUtils.getVersionAdapter().disablePerching(parrot);
	}

	// need to re-disable the perching when a parrot is loaded again
	@EventHandler(ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		if (event.getEntity() instanceof Parrot parrot && parrot.getScoreboardTags().contains(PLACED_PARROT_TAG)) {
			NmsUtils.getVersionAdapter().disablePerching(parrot);
		}
	}

	// remove drops from spawned parrots (exp, feathers)
	@EventHandler(ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		if (event.getEntity() instanceof Parrot parrot
			    && parrot.getScoreboardTags().contains(PLACED_PARROT_TAG)) {
			event.setDroppedExp(0);
			event.getDrops().clear();
		}
	}

	// Called when a parrot leaves a player's shoulder
	@EventHandler(ignoreCancelled = true)
	public void parrotSpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();

		if (entity instanceof Parrot parrot) {
			if (entity.getScoreboardTags().contains(SHOULDER_PARROT_TAG)) {
				event.setCancelled(true);
				return;
			}

			Component customName = parrot.customName();
			if (customName == null) {
				return;
			}
			String name = MessagingUtils.plainText(customName);

			// parrot spawned with an old version will not have the tag, so we need to check the name
			for (ParrotVariant variant : ParrotVariant.values()) {
				if (name.contains(variant.mName)) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onCFlight(PlayerToggleFlightEvent event) {
		final Player player = event.getPlayer();
		if (areParrotsVisible(player) && !event.isFlying()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					updateParrots(player);
				}
			}.runTaskLater(mPlugin, 5L);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onJoinBed(PlayerBedEnterEvent event) {
		// we need to remove the parrots when someone sleeps in a bed
		final Player player = event.getPlayer();
		mPrideLeft.remove(player);
		mPrideRight.remove(player);
		mLeftShoulders.remove(player);
		mRightShoulders.remove(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void onLeaveBed(PlayerBedLeaveEvent event) {
		final Player player = event.getPlayer();
		if (areParrotsVisible(player)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					updateParrots(player);
				}
			}.runTaskLater(mPlugin, 5L);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (areParrotsVisible(player)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					updateParrots(player);
				}
			}.runTaskLater(mPlugin, 5L);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		// remove the player from any sets and maps when quitting
		final Player player = event.getPlayer();
		mPrideLeft.remove(player);
		mPrideRight.remove(player);
		mLeftShoulders.remove(player);
		mRightShoulders.remove(player);
	}

}
