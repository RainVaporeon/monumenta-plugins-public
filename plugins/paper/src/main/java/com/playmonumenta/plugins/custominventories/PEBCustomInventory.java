package com.playmonumenta.plugins.custominventories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;

public class PEBCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	public static class PebItem {
		int mPage = 1;
		int mSlot;
		String mName;
		String mLore;
		Material mType;
		String mCommand;
		ChatColor mChatColor = null;
		Boolean mCloseAfter;

		public PebItem(int pg, int sl, String n, String l, ChatColor cc, Material t, String cmd, Boolean closeAfter) {
			mSlot = sl;
			mName = n;
			mLore = l;
			mType = t;
			mCommand = cmd;
			mPage = pg;
			mChatColor = cc;
			mCloseAfter = closeAfter;
		}

	}

	private static ArrayList<PebItem> PEB_ITEMS = new ArrayList<>();

	static {
		//If the command is internal to the GUI, closeAfter is ignored. Otherwise, the GUI abides by that boolean.

		//Common items for all but main menu are "page 0"
		//Page 1 is the top level menu, 2-9 saved for the next level of menus.
		//Pages 10 and beyond are used for implementation of specialized menus.

		PEB_ITEMS.add(new PebItem(0, 0, "Back to Main Menu", "Returns you to page 1.", ChatColor.GOLD, Material.OBSERVER, "page 1", false));
		PEB_ITEMS.add(new PebItem(0, 8, "Exit PEB", "Exits this menu.", ChatColor.GOLD, Material.RED_CONCRETE, "exit", false));
		PEB_ITEMS.add(new PebItem(0, 45, "Delete P.E.B.s ✗",
				"Click to remove P.E.B.s from your inventory.", ChatColor.LIGHT_PURPLE,
				Material.FLINT_AND_STEEL, "clickable peb_delete", true));

		//page 1: main menu
		PEB_ITEMS.add(new PebItem(1, 0, "", "", ChatColor.LIGHT_PURPLE, FILLER, "", false));
		PEB_ITEMS.add(new PebItem(1, 11, "Player Information",
				"Details about Housing, Dailies, and other player-focused options.", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, "page 2", false));
		PEB_ITEMS.add(new PebItem(1, 15, "Toggle-able Options",
				"Inventory Sort, Filtered Pickup, and more toggleable choices.", ChatColor.LIGHT_PURPLE,
				Material.LEVER, "page 3", false));
		PEB_ITEMS.add(new PebItem(1, 38, "Server Information",
				"Information such as how to use the PEB and random tips.", ChatColor.LIGHT_PURPLE,
				Material.DISPENSER, "page 4", false));
		PEB_ITEMS.add(new PebItem(1, 42, "Book Skins",
				"Change the color of the cover on your P.E.B.", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "page 5", false));


		//page 2: Player Info
		PEB_ITEMS.add(new PebItem(2, 4, "Player Information",
				"", ChatColor.LIGHT_PURPLE,
				Material.PLAYER_HEAD, "", false));
		PEB_ITEMS.add(new PebItem(2, 20, "Housing",
				"Click to view housing information.", ChatColor.LIGHT_PURPLE,
				Material.OAK_DOOR, "clickable peb_housing", true));
		PEB_ITEMS.add(new PebItem(2, 22, "Class",
				"Click to view your class and skills.", ChatColor.LIGHT_PURPLE,
				Material.STONE_SWORD, "clickable peb_class", true));
		PEB_ITEMS.add(new PebItem(2, 24, "Dungeon Instances",
				"Click to view what dungeon instances you have open, and how old they are.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, "clickable peb_dungeoninfo", true));
		PEB_ITEMS.add(new PebItem(2, 39, "Patron",
				"Click to view patron information. Use /donate to learn about donating.", ChatColor.LIGHT_PURPLE,
				Material.GLOWSTONE_DUST, "clickable peb_patroninfo", true));
		PEB_ITEMS.add(new PebItem(2, 41, "Dailies",
				"Click to see what daily content you have and haven't done today.", ChatColor.LIGHT_PURPLE,
				Material.ACACIA_BOAT, "clickable peb_dailies", true));

		//page 3: Toggle-able Options
		PEB_ITEMS.add(new PebItem(3, 4, "Toggleable Options",
				"", ChatColor.LIGHT_PURPLE,
				Material.LEVER, "", false));
		PEB_ITEMS.add(new PebItem(3, 19, "Self Particles",
				"Click to toggle self particles.", ChatColor.LIGHT_PURPLE,
				Material.FIREWORK_STAR, "clickable peb_selfparticles", false));
		PEB_ITEMS.add(new PebItem(3, 20, "UA Rocket Jumping",
				"Click to toggle rocket jumping with Unstable Arrows.", ChatColor.LIGHT_PURPLE,
				Material.FIREWORK_ROCKET, "clickable peb_uarj", false));
		PEB_ITEMS.add(new PebItem(3, 21, "Show name on patron buff announcement.",
				"Toggles whether the player has their IGN in the buff announcement when they"
				+ " activate " + ChatColor.GOLD + "Patreon " + ChatColor.LIGHT_PURPLE + "buffs.", ChatColor.LIGHT_PURPLE,
				Material.GLOWSTONE, "clickable toggle_patron_buff_thank", false));
		PEB_ITEMS.add(new PebItem(3, 23, "Inventory Drink",
				"Click to toggle drinking potions with a right click in any inventory.", ChatColor.LIGHT_PURPLE,
				Material.GLASS_BOTTLE, "clickable peb_tid", false));
		PEB_ITEMS.add(new PebItem(3, 24, "Filtered Pickup and Disabled Drop",
				"Click to choose your pickup and disabled drop preferences.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, "page 20", false));
		PEB_ITEMS.add(new PebItem(3, 25, "Compass Particles",
				"Click to toggle a trail of guiding particles when following the quest compass.", ChatColor.LIGHT_PURPLE,
				Material.COMPASS, "clickable peb_comp_particles", false));
		PEB_ITEMS.add(new PebItem(3, 37, "Death Sort",
				"Click to toggle death sorting, which attempts to return items dropped on death to the slot they were in prior to death.", ChatColor.LIGHT_PURPLE,
				Material.CHEST, "clickable peb_toggle_dso", false));
		PEB_ITEMS.add(new PebItem(3, 38, "Toggle Darksight",
				"Click to toggle whether Darksight provides Night Vision", ChatColor.LIGHT_PURPLE,
				Material.LANTERN, "execute as @S run function monumenta:mechanisms/darksight_toggle", false));
		PEB_ITEMS.add(new PebItem(3, 39, "Toggle Radiant",
				"Click to toggle whether Radiant provides Night Vision.", ChatColor.LIGHT_PURPLE,
				Material.SOUL_LANTERN, "execute as @S run function monumenta:mechanisms/radiant_toggle", false));
		PEB_ITEMS.add(new PebItem(3, 41, "Offhand Swapping",
				"Click to toggle whether pressing your swap key will be fully cancelled or only cancelled when a spellcast does so", ChatColor.LIGHT_PURPLE,
				Material.SHIELD, "toggleswap", false));
		PEB_ITEMS.add(new PebItem(3, 42, "Spawner Equipment",
				"Click to toggle whether mob equipment is displayed in spawners (significantly decreases FPS in many areas)", ChatColor.LIGHT_PURPLE,
				Material.IRON_CHESTPLATE, "clickable peb_spawnerequipment", false));

		//page 4: Server Info
		PEB_ITEMS.add(new PebItem(4, 4, "Server Information",
				"", ChatColor.LIGHT_PURPLE,
				Material.DISPENSER, "", false));
		PEB_ITEMS.add(new PebItem(4, 20, "P.E.B. Introduction",
				"Click to hear the P.E.B. Introduction.", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "clickable peb_intro", true));
		PEB_ITEMS.add(new PebItem(4, 24, "Get a random tip!",
				"Click to get a random tip!", ChatColor.LIGHT_PURPLE,
				Material.REDSTONE_TORCH, "clickable peb_tip", true));

		//page 5: Book Skins
		PEB_ITEMS.add(new PebItem(5, 4, "Book Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "", false));
		PEB_ITEMS.add(new PebItem(5, 40, "Wool Colors",
				"Click to jump to a page of wool colors.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, "page 10", false));
		PEB_ITEMS.add(new PebItem(5, 19, "Enchanted Book",
				"Click to change skin to Enchanted Book. (Default)", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "clickable peb_skin_enchantedbook", true));
		PEB_ITEMS.add(new PebItem(5, 21, "Regal",
				"Click to change skin to Regal.", ChatColor.LIGHT_PURPLE,
				Material.YELLOW_CONCRETE, "clickable peb_skin_regal", true));
		PEB_ITEMS.add(new PebItem(5, 23, "Crimson King",
				"Upon the ancient powers creep...", ChatColor.DARK_RED,
				Material.RED_TERRACOTTA, "clickable peb_skin_ck", true));
		PEB_ITEMS.add(new PebItem(5, 25, "Rose",
				"Red like roses!", ChatColor.RED,
				Material.RED_CONCRETE, "clickable peb_skin_rose", true));

		//page 10: Wool book skins
		PEB_ITEMS.add(new PebItem(10, 9, "Back to Book Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "page 5", false));
		PEB_ITEMS.add(new PebItem(10, 4, "Wool Skins",
				"", ChatColor.LIGHT_PURPLE,
				Material.ENCHANTED_BOOK, "", false));
		PEB_ITEMS.add(new PebItem(10, 11, "White",
				"Click to change skin to White.", ChatColor.LIGHT_PURPLE,
				Material.WHITE_WOOL, "clickable peb_skin_white", true));
		PEB_ITEMS.add(new PebItem(10, 12, "Orange",
				"Click to change skin to Orange.", ChatColor.LIGHT_PURPLE,
				Material.ORANGE_WOOL, "clickable peb_skin_orange", true));
		PEB_ITEMS.add(new PebItem(10, 20, "Magenta",
				"Click to change skin to Magenta.", ChatColor.LIGHT_PURPLE,
				Material.MAGENTA_WOOL, "clickable peb_skin_magenta", true));
		PEB_ITEMS.add(new PebItem(10, 21, "Light Blue",
				"Click to change skin to Light Blue.", ChatColor.LIGHT_PURPLE,
				Material.LIGHT_BLUE_WOOL, "clickable peb_skin_lightblue", true));
		PEB_ITEMS.add(new PebItem(10, 29, "Yellow",
				"Click to change skin to Yellow.", ChatColor.LIGHT_PURPLE,
				Material.YELLOW_WOOL, "clickable peb_skin_yellow", true));
		PEB_ITEMS.add(new PebItem(10, 30, "Lime",
				"Click to change skin to Lime.", ChatColor.LIGHT_PURPLE,
				Material.LIME_WOOL, "clickable peb_skin_lime", true));
		PEB_ITEMS.add(new PebItem(10, 38, "Pink",
				"Click to change skin to Pink.", ChatColor.LIGHT_PURPLE,
				Material.PINK_WOOL, "clickable peb_skin_pink", true));
		PEB_ITEMS.add(new PebItem(10, 39, "Gray",
				"Click to change skin to Gray.", ChatColor.LIGHT_PURPLE,
				Material.GRAY_WOOL, "clickable peb_skin_gray", true));
		PEB_ITEMS.add(new PebItem(10, 14, "Light Gray",
				"Click to change skin to Light Gray.", ChatColor.LIGHT_PURPLE,
				Material.LIGHT_GRAY_WOOL, "clickable peb_skin_lightgray", true));
		PEB_ITEMS.add(new PebItem(10, 15, "Cyan",
				"Click to change skin to Cyan.", ChatColor.LIGHT_PURPLE,
				Material.CYAN_WOOL, "clickable peb_skin_cyan", true));
		PEB_ITEMS.add(new PebItem(10, 23, "Purple",
				"Click to change skin to Purple.", ChatColor.LIGHT_PURPLE,
				Material.PURPLE_WOOL, "clickable peb_skin_purple", true));
		PEB_ITEMS.add(new PebItem(10, 24, "Blue",
				"Click to change skin to Blue.", ChatColor.LIGHT_PURPLE,
				Material.BLUE_WOOL, "clickable peb_skin_blue", true));
		PEB_ITEMS.add(new PebItem(10, 32, "Brown",
				"Click to change skin to Brown.", ChatColor.LIGHT_PURPLE,
				Material.BROWN_WOOL, "clickable peb_skin_brown", true));
		PEB_ITEMS.add(new PebItem(10, 33, "Green",
				"Click to change skin to Green.", ChatColor.LIGHT_PURPLE,
				Material.GREEN_WOOL, "clickable peb_skin_green", true));
		PEB_ITEMS.add(new PebItem(10, 41, "Red",
				"Click to change skin to Red.", ChatColor.LIGHT_PURPLE,
				Material.RED_WOOL, "clickable peb_skin_red", true));
		PEB_ITEMS.add(new PebItem(10, 42, "Black",
				"Click to change skin to Black.", ChatColor.LIGHT_PURPLE,
				Material.BLACK_WOOL, "clickable peb_skin_black", true));

		//page 20: Pickup and Disable Drop
		PEB_ITEMS.add(new PebItem(20, 0, "Back to Toggleable Options",
				"", ChatColor.LIGHT_PURPLE,
				Material.OBSERVER, "page 3", false));
		PEB_ITEMS.add(new PebItem(20, 4, "Pickup and Disable Drop Settings",
				"Choose the appropriate level of pickup filter and drop filter below.", ChatColor.LIGHT_PURPLE,
				Material.PRISMARINE_CRYSTALS, "", false));
		PEB_ITEMS.add(new PebItem(20, 19, "Disable Drop:",
				"", ChatColor.LIGHT_PURPLE,
				Material.BLACK_CONCRETE, "", false));
		PEB_ITEMS.add(new PebItem(20, 20, "None",
				"Disable no drops, the vanilla drop behavior.", ChatColor.LIGHT_PURPLE,
				Material.BARRIER, "disabledrop none", false));
		PEB_ITEMS.add(new PebItem(20, 21, "Holding",
				"Disable dropping of only held items.", ChatColor.LIGHT_PURPLE,
				Material.WOODEN_PICKAXE, "disabledrop holding", false));
		PEB_ITEMS.add(new PebItem(20, 22, "Tiered",
				"Disable dropping of tiered items.", ChatColor.LIGHT_PURPLE,
				Material.OAK_STAIRS, "disabledrop tiered", false));
		PEB_ITEMS.add(new PebItem(20, 23, "Lore",
				"Disable the drop of items with custom lore.", ChatColor.LIGHT_PURPLE,
				Material.LECTERN, "disabledrop lore", false));
		PEB_ITEMS.add(new PebItem(20, 24, "Interesting",
				"Disable the dropping of anything that matches the default pickup filter of interesting items.", ChatColor.LIGHT_PURPLE,
				Material.GOLD_NUGGET, "disabledrop interesting", false));
		PEB_ITEMS.add(new PebItem(20, 25, "All",
				"Disable all drops.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, "disabledrop all", false));

		PEB_ITEMS.add(new PebItem(20, 37, "Pickup Filter:",
				"", ChatColor.LIGHT_PURPLE,
				Material.WHITE_CONCRETE, "", false));
		PEB_ITEMS.add(new PebItem(20, 40, "Lore",
				"Only pick up items that have custom lore.", ChatColor.LIGHT_PURPLE,
				Material.LECTERN, "pickup lore", false));
		PEB_ITEMS.add(new PebItem(20, 41, "Interesting",
				"Only pick up items are of interest for the adventuring player, like arrows, torches, and anything with custom lore.", ChatColor.LIGHT_PURPLE,
				Material.GOLD_NUGGET, "pickup interesting", false));
		PEB_ITEMS.add(new PebItem(20, 42, "All",
				"Pick up anything and everything, matching vanilla functionality.", ChatColor.LIGHT_PURPLE,
				Material.DIRT, "pickup all", false));
		PEB_ITEMS.add(new PebItem(20, 43, "Threshold",
				"Set the minimum size of a stack of uninteresting items to pick up.", ChatColor.LIGHT_PURPLE,
				Material.OAK_SIGN, "threshold", false));

	}

	public PEBCustomInventory(Player player) {
		super(player, 54, player.getName() + "'s P.E.B");

		ScoreboardUtils.setScoreboardValue(player, "PEBPage", 1);

		setLayout(1, player);
	}
	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		Player player = null;
		if (event.getWhoClicked() instanceof Player) {
			player = (Player) event.getWhoClicked();
		} else {
			return;
		}
		ItemStack clickedItem = event.getCurrentItem();
		if (event.getClickedInventory() != _inventory) {
			return;
		}
		int currentPage = ScoreboardUtils.getScoreboardValue(player, "PEBPage");
		if (clickedItem != null && clickedItem.getType() != FILLER && !event.isShiftClick()) {
			int chosenSlot = event.getSlot();
			for (PebItem item : PEB_ITEMS) {
				if (item.mSlot == chosenSlot && item.mPage == currentPage) {
					completeCommand(player, item);
				}
				if (item.mSlot == chosenSlot && item.mPage == 0) {
					completeCommand(player, item);
				}
			}
		}
	}

	public Boolean isInternalCommand(String command) {
		if (command.equals("exit") || command.startsWith("page") || command.equals("threshold")) {
			return true;
		}
		return false;
	}

	public Boolean isPlayerCommand(String command) {
		if (command.startsWith("clickable") ||
				command.startsWith("pickup") ||
				command.equals("toggleswap") ||
				command.startsWith("disabledrop")) {
			return true;
		}
		return false;
	}

	public void runInternalCommand(Player player, PebItem item) {
		if (item.mCommand.startsWith("page")) {
			int newPageValue = Integer.parseInt(item.mCommand.split(" ")[1]);
			setLayout(newPageValue, player);
			return;
		} else if (item.mCommand.startsWith("exit")) {
			player.closeInventory();
			return;
		} else if (item.mCommand.equals("threshold")) {
			player.closeInventory();
			callSignUI(player);
		}
	}

	public void completeCommand(Player player, PebItem item) {
		if (item.mCommand == "") {
			return;
		}
		if (isInternalCommand(item.mCommand)) {
			runInternalCommand(player, item);
			return;
		} else if (isPlayerCommand(item.mCommand)) {
			player.performCommand(item.mCommand);
			if (item.mCloseAfter) {
				player.closeInventory();
			}
			return;
		} else {
			String finalCommand = item.mCommand.replace("@S", player.getName());
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
			if (item.mCloseAfter) {
				player.closeInventory();
			}
			return;
		}
	}

	public void callSignUI(Player target) {
		SignUtils.Menu menu = SignUtils.newMenu(
				new ArrayList<String>(Arrays.asList("", "~~~~~~~~~~~", "Input a number", "from 1-65 above.")))
	            .reopenIfFail(false)
	            .response((player, strings) -> {
					int inputVal = -1;
					try {
						inputVal = Integer.parseInt(strings[0]);
					} catch (Exception e) {
						new BukkitRunnable() {
							@Override
							public void run() {
								player.sendMessage("Input is not an integer.");
							}
						}.runTaskLater(Plugin.getInstance(), 2);
						return false;
					}
					if (inputVal >= 1 && inputVal <= 65) {
						new BukkitRunnable() {
							@Override
							public void run() {
								player.performCommand("pickup threshold " + strings[0]);
							}
						}.runTaskLater(Plugin.getInstance(), 2);
					    return false;
					} else {
						new BukkitRunnable() {
							@Override
							public void run() {
								player.sendMessage("Input is not with the bounds of 1 - 65.");
							}
						}.runTaskLater(Plugin.getInstance(), 2);
					}
					return true;
	            });

	    menu.open(target);
	}

	public ItemStack createCustomItem(PebItem item, Player player) {
		ItemStack newItem = new ItemStack(item.mType, 1);
		if (item.mType == Material.PLAYER_HEAD) {
			SkullMeta meta = (SkullMeta) newItem.getItemMeta();
			meta.setOwningPlayer(player);
			newItem.setItemMeta(meta);
		}
		ItemMeta meta = newItem.getItemMeta();
		if (item.mName != "") {
			meta.displayName(Component.text(item.mName, NamedTextColor.WHITE)
					.decoration(TextDecoration.ITALIC, false));
		}
		ChatColor defaultColor = (item.mChatColor != null) ? item.mChatColor : ChatColor.LIGHT_PURPLE;
		if (item.mLore != "") {
			splitLoreLine(meta, item.mLore, 30, defaultColor);
		}
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		newItem.setItemMeta(meta);
		return newItem;
	}

	public void splitLoreLine(ItemMeta meta, String lore, int maxLength, ChatColor defaultColor) {
		String[] splitLine = lore.split(" ");
		String currentString = defaultColor + "";
		List<String> finalLines = new ArrayList<String>();
		int currentLength = 0;
		for (String word : splitLine) {
			if (currentLength + word.length() > maxLength) {
				finalLines.add(currentString);
				currentString = defaultColor + "";
				currentLength = 0;
			}
			currentString += word + " ";
			currentLength += word.length() + 1;
		}
		if (currentString != defaultColor + "") {
			finalLines.add(currentString);
		}
		meta.setLore(finalLines);
	}

	public void setLayout(int page, Player player) {

		_inventory.clear();
		for (PebItem item : PEB_ITEMS) {
			if (item.mPage == 0) {
				_inventory.setItem(item.mSlot, createCustomItem(item, player));
			} //intentionally not else, so overrides can happen
			if (item.mPage == page) {
				_inventory.setItem(item.mSlot, createCustomItem(item, player));
			}
		}

		for (int i = 0; i < 54; i++) {
			if (_inventory.getItem(i) == null) {
				_inventory.setItem(i, new ItemStack(FILLER, 1));
			}
		}
		ScoreboardUtils.setScoreboardValue(player, "PEBPage", page);
	}
}
