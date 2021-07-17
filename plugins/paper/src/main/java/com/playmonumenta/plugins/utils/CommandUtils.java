package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.playmonumenta.plugins.point.AreaBounds;
import com.playmonumenta.plugins.point.Point;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class CommandUtils {

	public static CommandSender getCallee(CommandSender sender) {
		if (sender instanceof ProxiedCommandSender) {
			return ((ProxiedCommandSender)sender).getCallee();
		}
		return sender;
	}

	/**
	 * Gets a CommandSender's location (player, command block, /execute, etc.)
	 *
	 * @return sender's location or raises an exception
	 */
	public static Location getLocation(CommandSender sender) throws Exception {
		return getLocation(sender, false);
	}

	public static Location getLocation(CommandSender sender, boolean doSubtractEntityOffset) throws Exception {
		if (sender == null) {
			throw new Exception("sender is null!");
		} else if (sender instanceof Entity) {
			Location senderLoc = ((Entity)sender).getLocation();
			if (doSubtractEntityOffset) {
				senderLoc.subtract(0.5, 0.5, 0.5);
			}
			return senderLoc;
		} else if (sender instanceof BlockCommandSender) {
			return ((BlockCommandSender)sender).getBlock().getLocation();
		} else if (sender instanceof ProxiedCommandSender) {
			return getLocation(((ProxiedCommandSender)sender).getCallee(), doSubtractEntityOffset);
		} else {
			throw new Exception("Failed to get required command sender coordinates");
		}
	}

	public static int parseIntFromString(CommandSender sender, String str) throws Exception {
		int value = 0;

		try {
			value = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			if (sender != null) {
				error(sender, "Invalid parameter " + str + ". Must be whole number value between " + Integer.MIN_VALUE + " and " + Integer.MAX_VALUE);
			}
			throw new Exception(e);
		}

		return value;
	}

	public static double parseDoubleFromString(CommandSender sender, String str) throws Exception {
		double value = 0;

		try {
			value = Float.parseFloat(str);
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Invalid parameter " + str + ". Must be a value between " + Float.MIN_VALUE + " and " + Float.MAX_VALUE);
			}
			throw new Exception(e);
		}

		return value;
	}

	public static AreaBounds parseAreaFromString(CommandSender sender,
	                                             String xStr1, String yStr1, String zStr1,
	                                             String xStr2, String yStr2, String zStr2) throws Exception {
		Point pos1;
		Point pos2;

		try {
			pos1 = Point.fromString(sender, xStr1, yStr1, zStr1, true);
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Failed to parse first coordinate");
			}
			throw new Exception(e);
		}

		try {
			pos2 = Point.fromString(sender, xStr2, yStr2, zStr2, true);
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Failed to parse second coordinate");
			}
			throw new Exception(e);
		}

		pos2.mX += 1;
		pos2.mY += 1;
		pos2.mZ += 1;

		return new AreaBounds(pos1, pos2);
	}

	public static double parseCoordFromString(CommandSender sender,
	                                          double senderPos, String str) throws Exception {
		try {
			if (str.equals("~")) {
				return senderPos;
			} else if (str.startsWith("~")) {
				return senderPos + parseDoubleFromString(sender, str.substring(1));
			} else {
				return parseDoubleFromString(sender, str);
			}
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Failed to parse coordinate '" + str + "'");
			}
			throw new Exception(e);
		}
	}

	public static void error(CommandSender sender, String msg) {
		if ((sender instanceof Player)
			|| ((sender instanceof ProxiedCommandSender) && (((ProxiedCommandSender)sender).getCaller() instanceof Player))) {
			sender.sendMessage(ChatColor.RED + msg);
		} else {
			sender.sendMessage(msg);
		}
	}

	public static void enchantify(CommandSender sender, Player player, String enchantment) throws WrapperCommandSyntaxException {
		enchantify(sender, player, enchantment, null);
	}

	public static void enchantify(CommandSender sender, Player player, String enchantment, String ownerPrefix) throws WrapperCommandSyntaxException {
		ItemStack item = player.getEquipment().getItemInMainHand();

		try {
			ItemUtils.enchantifyItem(item, enchantment, ownerPrefix, player);
		} catch (Exception e) {
			CommandAPI.fail(e.getMessage());
		}

		if (sender != null) {
			sender.sendMessage("Successfully added " + enchantment + " to player's held item");
		}
	}

	public static void deEnchantifyHeldItem(CommandSender sender, Player player, String enchantment) throws WrapperCommandSyntaxException {
		enchantment = ChatColor.stripColor(enchantment);

		ItemStack item = player.getInventory().getItemInMainHand();

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			CommandAPI.fail("Player must have a " + enchantment + " item in their main hand!");
		}

		List<String> lore = meta.getLore();
		if (lore == null || lore.isEmpty()) {
			CommandAPI.fail("Player must have a " + enchantment + " item in their main hand!");
		}

		List<String> newLore = new ArrayList<>();
		boolean hasEnchant = false;
		for (String loreEntry : lore) {
			if (ChatColor.stripColor(loreEntry).startsWith(enchantment)) {
				hasEnchant = true;
			} else {
				newLore.add(loreEntry);
			}
		}

		if (!hasEnchant) {
			CommandAPI.fail("Player must have a " + enchantment + " item in their main hand!");
		} else {
			meta.setLore(newLore);
			item.setItemMeta(meta);
			ItemUtils.setPlainLore(item);

			sender.sendMessage("Successfully removed " + enchantment + " from the player's held item");
		}
	}

	public static void runCommandViaConsole(String cmd) {
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
	}

	// returns the sender as Player, if that sender is a player instance, or a proxied player
	// returns null otherwise
	@Nullable
	public static @NotNull Player getPlayerFromSender(CommandSender sender) throws WrapperCommandSyntaxException {
		if (sender instanceof Player) {
			return ((Player)sender);
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender) sender).getCallee();
			if (callee instanceof Player) {
				return ((Player)callee);
			}
		}
		CommandAPI.fail("This command must be run by/as a player");
		return null; // This can never happen but is required by the compiler
	}
}
