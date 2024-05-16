package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.plugins.commands.DungeonAccessCommand;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DungeonUtils.DungeonCommandMapping;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class OrinCustomInventory extends CustomInventory {
	private static final Material FILLER = GUIUtils.FILLER_MATERIAL;
	private static final int[] INSTANCE_UPTO3_LOCS = {20, 22, 24};
	private static final int[] INSTANCE_UPTO9_LOCS = {20, 22, 24, 29, 31, 33, 38, 40, 42};
	private static final int[] INSTANCE_UPTO20_LOCS = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42, 47, 48, 49, 50, 51};
	private static final int[] INSTANCE_UPTO28_LOCS = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43, 46, 47, 48, 49, 50, 51, 52};

	public static class TeleportEntry {
		int mPage;
		int mSlot;
		String mName;
		@Nullable String mScoreboard;
		String mLore;
		int mScoreRequired;
		Material mType;
		@Nullable BiConsumer<OrinCustomInventory, Player> mLeftClick;
		@Nullable BiConsumer<OrinCustomInventory, Player> mRightClick;
		int mItemCount;

		public TeleportEntry(int page, int slot, String name, String lore, Material type, @Nullable String scoreboard, int scoreRequired, @Nullable String left) {
			this(page, slot, name, lore, type, scoreboard, scoreRequired, left, null, 1);
		}

		public TeleportEntry(int page, int slot, String name, String lore, Material type, @Nullable String scoreboard, int scoreRequired, @Nullable String left, @Nullable String right) {
			this(page, slot, name, lore, type, scoreboard, scoreRequired, left, right, 1);
		}

		public TeleportEntry(int page, int slot, String name, String lore, Material type, @Nullable String scoreboard, int scoreRequired, @Nullable String left, @Nullable String right, int count) {
			this(page, slot, name, lore, type, scoreboard, scoreRequired,
				StringUtils.isBlank(left) ? null : (gui, player) -> gui.completeCommand(player, left),
				StringUtils.isBlank(right) ? null : (gui, player) -> gui.completeCommand(player, right), count);
		}

		public TeleportEntry(int page, int slot, String name, String lore, Material type, @Nullable String scoreboard, int scoreRequired, @Nullable BiConsumer<OrinCustomInventory, Player> left, @Nullable BiConsumer<OrinCustomInventory, Player> right, int count) {
			mPage = page;
			mSlot = slot;
			mName = name;
			mLore = lore;
			mType = type;
			mScoreboard = scoreboard;
			mScoreRequired = scoreRequired;
			mLeftClick = left;
			mRightClick = right;
			mItemCount = count;
		}
	}

	/* Page Info
	 * Page 0: Common for 1-9
	 * Page 1: Region 1
	 * Page 2: Region 2
	 * Page 3: Region 3
	 * Page 4: Plots
	 * Page 5: Playerplots
	 * Page 6: Default Page (dungeon shards, etc.)
	 * Page 10: Common for 11-19
	 * Page 11: Region 1 Instance Bot
	 * Page 12: Region 2 Instance Bot
	 * Page 13: Region 3 Instance Bot
	 * Page 20: Common for 21-29
	 * Page 21: Dungeon Instances
	 */

	private static final ArrayList<TeleportEntry> ORIN_ITEMS = new ArrayList<>();
	private final ArrayList<TeleportEntry> INSTANCE_ITEMS = new ArrayList<>();

	static {
		String sortedDesc = "Left Click to be sorted to a shard, right click to choose the shard.";

		//R1 Page
		ORIN_ITEMS.add(new TeleportEntry(1, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/sierhaven_to_plots"));
		ORIN_ITEMS.add(new TeleportEntry(1, 27, "Player Plot", "Click to teleport!", Material.GRASS_BLOCK, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(1, 12, "Sierhaven", sortedDesc, Material.GREEN_CONCRETE, null, 0, "tp @S -765.5 107.0625 70.5 180 0", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(1, 15, "Mistport", sortedDesc, Material.SAND, "Quest101", 13, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(1, 39, "Galengarde", sortedDesc, Material.RED_MUSHROOM_BLOCK, "R3Access", 1, "execute as @S run function monumenta:mechanisms/teleporters/goto/ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(1, 42, "Dungeon Instances", "Click to view all open dungeon instances.", Material.SPAWNER, null, 0, "page 21"));
		ORIN_ITEMS.add(new TeleportEntry(1, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));

		//R2 Page
		ORIN_ITEMS.add(new TeleportEntry(2, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/mistport_to_plots"));
		ORIN_ITEMS.add(new TeleportEntry(2, 27, "Player Plot", "Click to teleport!", Material.GRASS_BLOCK, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(2, 12, "Sierhaven", sortedDesc, Material.GREEN_CONCRETE, null, 0, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(2, 15, "Mistport", sortedDesc, Material.SAND, "Quest101", 13, "tp @S -762.5 70.1 1344.5 180 0", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(2, 39, "Galengarde", sortedDesc, Material.RED_MUSHROOM_BLOCK, "R3Access", 1, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(2, 42, "Dungeon Instances", "Click to view all open dungeon instances.", Material.SPAWNER, null, 0, "page 21"));
		ORIN_ITEMS.add(new TeleportEntry(2, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));

		//R3 Page
		ORIN_ITEMS.add(new TeleportEntry(3, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/tp/galengarde_to_plots"));
		ORIN_ITEMS.add(new TeleportEntry(3, 27, "Player Plot", "Click to teleport!", Material.GRASS_BLOCK, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(3, 12, "Sierhaven", sortedDesc, Material.GREEN_CONCRETE, null, 0, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(3, 15, "Mistport", sortedDesc, Material.SAND, "Quest101", 13, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(3, 39, "Galengarde", sortedDesc, Material.RED_MUSHROOM_BLOCK, "R3Access", 1, "tp @S -303.5 83 -654.5 90 0", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(3, 42, "Dungeon Instances", "Click to view all open dungeon instances.", Material.SPAWNER, null, 0, "page 21"));
		ORIN_ITEMS.add(new TeleportEntry(3, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));

		//Plots Page
		ORIN_ITEMS.add(new TeleportEntry(4, 0, "Docks", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "tp @S -2456.0 56.5 1104.0 90 0"));
		ORIN_ITEMS.add(new TeleportEntry(4, 18, "Market", "Click to teleport!", Material.BARREL, null, 0, "execute as @S run function monumenta:mechanisms/teleporters/enter_new_market"));
		ORIN_ITEMS.add(new TeleportEntry(4, 27, "Player Plot", "Click to teleport!", Material.GRASS_BLOCK, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(4, 36, "Guild Plot", "Click to teleport!", Material.YELLOW_BANNER, null, 0, "teleportguild @S", "guild teleportgui @S"));
		ORIN_ITEMS.add(new TeleportEntry(4, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));
		ORIN_ITEMS.add(new TeleportEntry(4, 12, "Sierhaven", sortedDesc, Material.GREEN_CONCRETE, null, 0, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(4, 15, "Mistport", sortedDesc, Material.SAND, "Quest101", 13, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(4, 39, "Galengarde", sortedDesc, Material.RED_MUSHROOM_BLOCK, "R3Access", 1, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(4, 42, "Dungeon Instances", "Click to view all open dungeon instances.", Material.SPAWNER, null, 0, "page 21"));

		//Playerplots Page
		ORIN_ITEMS.add(new TeleportEntry(5, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "transferserver plots"));
		ORIN_ITEMS.add(new TeleportEntry(5, 27, "Player Plot", "Click to teleport!", Material.GRASS_BLOCK, "CurrentPlot", 1, "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(5, 12, "Sierhaven", sortedDesc, Material.GREEN_CONCRETE, null, 0, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(5, 15, "Mistport", sortedDesc, Material.SAND, "Quest101", 13, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(5, 39, "Galengarde", sortedDesc, Material.RED_MUSHROOM_BLOCK, "R3Access", 1, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(5, 42, "Dungeon Instances", "Click to view all open dungeon instances.", Material.SPAWNER, null, 0, "page 21"));
		ORIN_ITEMS.add(new TeleportEntry(5, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));

		ORIN_ITEMS.add(new TeleportEntry(6, 9, "Plots", "Click to teleport!", Material.LIGHT_BLUE_CONCRETE, null, 0, "transferserver plots"));
		ORIN_ITEMS.add(new TeleportEntry(6, 27, "Player Plot", "Click to teleport!", Material.GRASS_BLOCK, "CurrentPlot", 1, "plot send @S", "plot gui @S"));
		ORIN_ITEMS.add(new TeleportEntry(6, 12, "Sierhaven", sortedDesc, Material.GREEN_CONCRETE, null, 0, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/valley", "instancebot valley"));
		ORIN_ITEMS.add(new TeleportEntry(6, 15, "Mistport", sortedDesc, Material.SAND, "Quest101", 13, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/isles", "instancebot isles"));
		ORIN_ITEMS.add(new TeleportEntry(6, 39, "Galengarde", sortedDesc, Material.RED_MUSHROOM_BLOCK, "R3Access", 1, "execute as @S at @s run function monumenta:mechanisms/teleporters/shards/ring", "instancebot ring"));
		ORIN_ITEMS.add(new TeleportEntry(6, 42, "Dungeon Instances", "Click to view all open dungeon instances.", Material.SPAWNER, null, 0, "page 21"));
		ORIN_ITEMS.add(new TeleportEntry(6, 45, "Build Server", "Click to teleport!", Material.STONE_PICKAXE, null, 0, "transferserver build"));

		//Common 10-19: Instance Bot Choices
		ORIN_ITEMS.add(new TeleportEntry(10, 0, "Back", "Return to the main page.", Material.ARROW, null, 0, "back"));
		ORIN_ITEMS.add(new TeleportEntry(10, 4, "Available Shards", "Choose your shard below.", Material.SCUTE, null, 0, ""));

		//Common 20-29: Dungeon Instances
		ORIN_ITEMS.add(new TeleportEntry(20, 0, "Back", "Return to the main page.", Material.ARROW, null, 0, "back"));
		ORIN_ITEMS.add(new TeleportEntry(20, 9, "Region 1 Dungeons", "Dungeons located with the King's Valley.", Material.GREEN_CONCRETE, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 10, "Region 1 Dungeons", "Dungeons located with the King's Valley.", Material.GREEN_CONCRETE, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 11, "Region 1 Dungeons", "Dungeons located with the King's Valley.", Material.GREEN_CONCRETE, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 12, "Region 2 Dungeons", "Dungeons located with the Celsian Isles.", Material.SAND, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 13, "Region 2 Dungeons", "Dungeons located with the Celsian Isles.", Material.SAND, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 14, "Region 2 Dungeons", "Dungeons located with the Celsian Isles.", Material.SAND, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 15, "Region 3 Dungeons", "Dungeons located with the Architect's Ring.", Material.RED_MUSHROOM_BLOCK, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 16, "Region 3 Dungeons", "Dungeons located with the Architect's Ring.", Material.RED_MUSHROOM_BLOCK, null, 0, "", ""));
		ORIN_ITEMS.add(new TeleportEntry(20, 17, "Region 3 Dungeons", "Dungeons located with the Architect's Ring.", Material.RED_MUSHROOM_BLOCK, null, 0, "", ""));

		//21: Dungeon Instances
		//Group: R1 Dungeons
		ORIN_ITEMS.add(new TeleportEntry(21, 18, "Labs", "Click to teleport!", Material.GLASS_BOTTLE, "D0Access", 1, sendToDungeonAction(DungeonCommandMapping.LABS), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 19, "White", "Click to teleport!", Material.WHITE_WOOL, "D1Access", 1, sendToDungeonAction(DungeonCommandMapping.WHITE), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 20, "Orange", "Click to teleport!", Material.ORANGE_WOOL, "D2Access", 1, sendToDungeonAction(DungeonCommandMapping.ORANGE), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 27, "Magenta", "Click to teleport!", Material.MAGENTA_WOOL, "D3Access", 1, sendToDungeonAction(DungeonCommandMapping.MAGENTA), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 28, "Light Blue", "Click to teleport!", Material.LIGHT_BLUE_WOOL, "D4Access", 1, sendToDungeonAction(DungeonCommandMapping.LIGHTBLUE), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 29, "Yellow", "Click to teleport!", Material.YELLOW_WOOL, "D5Access", 1, sendToDungeonAction(DungeonCommandMapping.YELLOW), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 36, "Willows", "Click to teleport!", Material.JUNGLE_LEAVES, "DB1Access", 1, sendToDungeonAction(DungeonCommandMapping.WILLOWS), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 37, "Reverie", "Click to teleport!", Material.FIRE_CORAL, "DCAccess", 1, sendToDungeonAction(DungeonCommandMapping.REVERIE), null, 1));

		//Group: R2 Dungeons
		ORIN_ITEMS.add(new TeleportEntry(21, 21, "Lime", "Click to teleport!", Material.LIME_WOOL, "D6Access", 1, sendToDungeonAction(DungeonCommandMapping.LIME), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 22, "Pink", "Click to teleport!", Material.PINK_WOOL, "D7Access", 1, sendToDungeonAction(DungeonCommandMapping.PINK), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 23, "Gray", "Click to teleport!", Material.GRAY_WOOL, "D8Access", 1, sendToDungeonAction(DungeonCommandMapping.GRAY), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 30, "Light Gray", "Click to teleport!", Material.LIGHT_GRAY_WOOL, "D9Access", 1, sendToDungeonAction(DungeonCommandMapping.LIGHTGRAY), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 31, "Cyan", "Click to teleport!", Material.CYAN_WOOL, "D10Access", 1, sendToDungeonAction(DungeonCommandMapping.CYAN), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 32, "Purple", "Click to teleport!", Material.PURPLE_WOOL, "D11Access", 1, sendToDungeonAction(DungeonCommandMapping.PURPLE), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 39, "Teal", "Click to teleport!", Material.CYAN_CONCRETE_POWDER, "DTLAccess", 1, sendToDungeonAction(DungeonCommandMapping.TEAL), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 40, "Shifting City", "Click to teleport!", Material.PRISMARINE_BRICKS, "DRL2Access", 1, sendToDungeonAction(DungeonCommandMapping.SHIFTINGCITY), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 41, "The Fallen Forum", "Click to teleport!", Material.BOOKSHELF, "DFFAccess", 1, sendToDungeonAction(DungeonCommandMapping.FORUM), null, 1));

		//Group: R3 Dungeons
		ORIN_ITEMS.add(new TeleportEntry(21, 24, "Silver Knight's Tomb", "Click to teleport!", Material.DEEPSLATE, "DSKTAccess", 1, sendToDungeonAction(DungeonCommandMapping.SKT), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 25, "Blue", "Click to teleport!", Material.BLUE_WOOL, "D12Access", 1, sendToDungeonAction(DungeonCommandMapping.BLUE), null, 1));
		ORIN_ITEMS.add(new TeleportEntry(21, 26, "Brown", "Click to teleport!", Material.BROWN_WOOL, "D13Access", 1, sendToDungeonAction(DungeonCommandMapping.BROWN), null, 1));
	}

	private int mCurrentPage;
	private final String mCurrentShard = ServerProperties.getShardName();
	private final boolean mBackButtonEnabled;

	public OrinCustomInventory(Player player, int page) {
		super(player, 54, "Teleportation Choices");
		if (page == -1) {
			mBackButtonEnabled = true;
			if (mCurrentShard.contains("valley")) {
				mCurrentPage = 1;
			} else if (mCurrentShard.contains("isles")) {
				mCurrentPage = 2;
			} else if (mCurrentShard.contains("ring")) {
				mCurrentPage = 3;
			} else if (mCurrentShard.equals("plots")) {
				mCurrentPage = 4;
			} else if (mCurrentShard.equals("playerplots")) {
				mCurrentPage = 5;
			} else {
				mCurrentPage = 6;
			}
		} else {
			mBackButtonEnabled = false;
			mCurrentPage = page;
		}

		setLayout(player);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != mInventory) {
			return;
		}

		int commonPage = (int) Math.floor(mCurrentPage / 10.0) * 10;
		if (clickedItem != null && clickedItem.getType() != FILLER && !event.isShiftClick()) {
			int chosenSlot = event.getSlot();
			for (TeleportEntry item : ORIN_ITEMS) {
				if (item.mSlot == chosenSlot && item.mPage == mCurrentPage) {
					if (event.isLeftClick()) {
						if (item.mLeftClick != null) {
							item.mLeftClick.accept(this, player);
						}
					} else {
						if (item.mRightClick != null) {
							item.mRightClick.accept(this, player);
						}
					}
				}
				if (item.mSlot == chosenSlot && item.mPage == commonPage) {
					if (event.isLeftClick()) {
						if (item.mLeftClick != null) {
							item.mLeftClick.accept(this, player);
						}
					} else {
						if (item.mRightClick != null) {
							item.mRightClick.accept(this, player);
						}
					}
				}
			}
			if (mCurrentPage == 11 || mCurrentPage == 12 || mCurrentPage == 13) {
				for (TeleportEntry item : INSTANCE_ITEMS) {
					if (item.mSlot == chosenSlot && item.mPage == mCurrentPage) {
						if (event.isLeftClick()) {
							if (item.mLeftClick != null) {
								item.mLeftClick.accept(this, player);
							}
						} else {
							if (item.mRightClick != null) {
								item.mRightClick.accept(this, player);
							}
						}
					}
				}
			}
		}
	}

	public boolean isInternalCommand(String command) {
		return command.equals("exit") || command.startsWith("page") || command.startsWith("instancebot") || command.equals("back");
	}

	public void runInternalCommand(Player player, String cmd) {
		if (cmd.startsWith("page")) {
			mCurrentPage = Integer.parseInt(cmd.split(" ")[1]);
			setLayout(player);
		} else if (cmd.startsWith("exit")) {
			player.closeInventory();
		} else if (cmd.startsWith("instancebot")) {
			String searchTerm = cmd.split(" ")[1];
			if (searchTerm.startsWith("valley")) {
				mCurrentPage = 11;
			} else if (searchTerm.startsWith("isles")) {
				mCurrentPage = 12;
			} else {
				mCurrentPage = 13;
			}
			setLayout(player);
		} else if (cmd.equals("back")) {
			if (mCurrentShard.contains("valley")) {
				mCurrentPage = 1;
			} else if (mCurrentShard.contains("isles")) {
				mCurrentPage = 2;
			} else if (mCurrentShard.contains("ring")) {
				mCurrentPage = 3;
			} else if (mCurrentShard.equals("plots")) {
				mCurrentPage = 4;
			} else if (mCurrentShard.equals("playerplots")) {
				mCurrentPage = 5;
			} else {
				mCurrentPage = 6;
			}
			setLayout(player);
		}
	}

	public void completeCommand(Player player, String cmd) {
		if (cmd.isEmpty()) {
			return;
		}
		if (isInternalCommand(cmd)) {
			runInternalCommand(player, cmd);
		} else {
			if (cmd.startsWith("transferserver")) {
				//input format should be "transferserver <shard_name>"
				String[] splitCommand = cmd.split(" ");
				String targetShard = splitCommand[1];

				try {
					/* Note that this API accepts null returnLoc, returnYaw, returnPitch as default current player location */
					MonumentaRedisSyncAPI.sendPlayer(player, targetShard);
				} catch (Exception e) {
					MessagingUtils.sendStackTrace(player, e);
				}
			} else {
				player.closeInventory();
				String finalCommand = cmd.replace("@S", player.getName());
				NmsUtils.getVersionAdapter().runConsoleCommandSilently(finalCommand);
			}
		}
	}

	private static BiConsumer<OrinCustomInventory, Player> sendToDungeonAction(DungeonCommandMapping dungeon) {
		return (gui, player) -> DungeonAccessCommand.send(player, dungeon, player.getLocation());
	}

	public ItemStack createCustomItem(TeleportEntry location) {
		return GUIUtils.createBasicItem(location.mType, location.mItemCount, location.mName, NamedTextColor.GOLD, false, location.mLore, NamedTextColor.DARK_PURPLE, 30, true);
	}

	public void setLayout(Player player) {
		mInventory.clear();
		int commonPage = (int) Math.floor(mCurrentPage / 10.0) * 10;
		for (TeleportEntry item : ORIN_ITEMS) {
			if (item.mPage == commonPage) {
				if (item.mSlot == 0 && !mBackButtonEnabled) {
					continue;
				}
				if (item.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, item.mScoreboard) >= item.mScoreRequired) {
					mInventory.setItem(item.mSlot, createCustomItem(item));
				}
			} //intentionally not else, so overrides can happen
			if (item.mPage == mCurrentPage) {
				if (item.mScoreboard == null || ScoreboardUtils.getScoreboardValue(player, item.mScoreboard) >= item.mScoreRequired) {
					mInventory.setItem(item.mSlot, createCustomItem(item));
				}
			}
		}

		GUIUtils.fillWithFiller(mInventory);

		if (mCurrentPage == 11) {
			showInstances(player, "valley");
		} else if (mCurrentPage == 12) {
			showInstances(player, "isles");
		} else if (mCurrentPage == 13) {
			showInstances(player, "ring");
		}
	}

	public void showInstances(Player player, String searchTerm) {
		Set<String> results = null;
		INSTANCE_ITEMS.clear();
		try {
			results = NetworkRelayAPI.getOnlineShardNames();
		} catch (Exception e) {
			player.sendMessage(Component.text("Unable to get list of online shards, please report this bug:", NamedTextColor.RED));
			MessagingUtils.sendStackTrace(player, e);
			player.closeInventory();
		}
		int index = 0;
		if (results == null) {
			return;
		}
		results.removeIf(item -> !item.startsWith(searchTerm));

		int page;
		Material itemType;

		if (searchTerm.startsWith("valley")) {
			page = 11;
			itemType = Material.JUNGLE_SAPLING;
		} else if (searchTerm.startsWith("isles")) {
			page = 12;
			itemType = Material.KELP;
		} else {
			itemType = Material.DARK_OAK_SAPLING;
			page = 13;
		}
		int[] instanceLocations;

		switch (results.size()) {
			case 0, 1, 2, 3 -> instanceLocations = INSTANCE_UPTO3_LOCS;
			case 4, 5, 6, 7, 8, 9 -> instanceLocations = INSTANCE_UPTO9_LOCS;
			case 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 -> instanceLocations = INSTANCE_UPTO20_LOCS;
			default -> instanceLocations = INSTANCE_UPTO28_LOCS;
		}

		ArrayList<Integer> resultSortedList = new ArrayList<>();
		for (String shard : results) {
			if (shard.equalsIgnoreCase(searchTerm)) {
				resultSortedList.add(0);
			} else {
				resultSortedList.add(Integer.parseInt(shard.split("-")[1]));
			}
		}
		Collections.sort(resultSortedList);

		for (Integer shard : resultSortedList) {
			String shardName = searchTerm;
			if (shard != 0) {
				shardName += "-" + shard;
			}
			if (index <= instanceLocations.length) {
				INSTANCE_ITEMS.add(new TeleportEntry(page, instanceLocations[index++], shardName, "Click to teleport!", itemType, null, 0, "transferserver " + shardName, "", shard < 1 ? 1 : shard));
			}
		}

		for (TeleportEntry item : INSTANCE_ITEMS) {
			mInventory.setItem(item.mSlot, createCustomItem(item));
		}
	}
}
