package com.playmonumenta.plugins.cosmetics;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.GenericCommand;
import com.playmonumenta.plugins.cosmetics.finishers.EliteFinishers;
import com.playmonumenta.plugins.cosmetics.gui.CosmeticsGUI;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.plots.PlotBorderCustomInventory;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CosmeticsCommand extends GenericCommand {

	public static void register(Plugin plugin) {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.cosmetics");
		String[] types = Arrays.stream(CosmeticType.values()).map(CosmeticType::getType).toArray(String[]::new);

		Argument<?> allCosmeticsArgument = new GreedyStringArgument("name")
			.replaceSuggestions(ArgumentSuggestions.strings(info -> {
				CosmeticType type = CosmeticType.valueOf(((String) info.previousArgs()[1]).toUpperCase(Locale.ROOT));
				if (type == CosmeticType.ELITE_FINISHER) {
					return EliteFinishers.getNames();
				} else if (type == CosmeticType.PLOT_BORDER) {
					return PlotBorderCustomInventory.getCosmeticNames();
				} else if (type == CosmeticType.VANITY) {
					return Arrays.stream(Material.values())
						.filter(mat -> !mat.isLegacy())
						.map(mat -> mat.name().toLowerCase(Locale.ROOT) + ":")
						.toArray(String[]::new);
				} else if (type == CosmeticType.COSMETIC_SKILL) {
					return CosmeticSkills.getNames();
				} else {
					return new String[0];
				}
			}));

		// requires the first argument to be the player, and the second to be the cosmetic type
		Argument<?> ownedCosmeticsArgument = new GreedyStringArgument("name")
			.replaceSuggestions(ArgumentSuggestions.strings(info ->
				CosmeticsManager.getInstance()
					.getCosmeticsOfTypeAlphabetical((Player) info.previousArgs()[0],
						CosmeticType.valueOf(((String) info.previousArgs()[1]).toUpperCase(Locale.ROOT))).stream()
					.map(Cosmetic::getName)
					.filter(n -> n.startsWith(info.currentArg()))
					.toArray(String[]::new)));

		// ADD COSMETIC COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("add"),
				new EntitySelectorArgument.OnePlayer("player"),
				new MultiLiteralArgument(types),
				allCosmeticsArgument)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticType type = CosmeticType.valueOf(((String) args[1]).toUpperCase(Locale.ROOT));
				String name = (String) args[2];
				boolean added = CosmeticsManager.getInstance().addCosmetic(player, type, name);
				if (sender instanceof Player) {
					if (added) {
						sender.sendMessage(Component.text("Added " + type.getDisplayName() + " '" + name + "' to " + player.getName(), NamedTextColor.WHITE));
					} else {
						sender.sendMessage(Component.text(player.getName() + " already has " + type.getDisplayName() + " '" + name + "', or the cosmetic is invalid.", NamedTextColor.RED));
					}
				}
			})
			.register();

		// EQUIP COSMETIC COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("equip"),
				new EntitySelectorArgument.OnePlayer("player"),
				new MultiLiteralArgument(types),
				ownedCosmeticsArgument)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticType type = CosmeticType.valueOf(((String) args[1]).toUpperCase(Locale.ROOT));
				String name = (String) args[2];
				boolean equipped = CosmeticsManager.getInstance().equipCosmetic(player, type, name, true);
				if (sender instanceof Player) {
					if (equipped) {
						sender.sendMessage(Component.text("Equipped " + type.getDisplayName() + " '" + name + "' on " + player.getName(), NamedTextColor.WHITE));
					} else {
						sender.sendMessage(Component.text(player.getName() + " does not have " + type.getDisplayName() + " '" + name + "'", NamedTextColor.RED));
					}
				}
			})
			.register();

		// REMOVE COSMETIC COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("remove"),
				new EntitySelectorArgument.OnePlayer("player"),
				new MultiLiteralArgument(types),
				ownedCosmeticsArgument)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticType type = CosmeticType.valueOf(((String) args[1]).toUpperCase(Locale.ROOT));
				String name = (String) args[2];
				boolean removed = CosmeticsManager.getInstance().removeCosmetic(player, type, name);
				if (sender instanceof Player) {
					if (removed) {
						sender.sendMessage(Component.text("Removed " + type.getDisplayName() + " '" + name + "' from " + player.getName(), NamedTextColor.WHITE));
					} else {
						sender.sendMessage(Component.text(player.getName() + " does not have " + type.getDisplayName() + " '" + name + "'", NamedTextColor.RED));
					}
				}
			})
			.register();

		// CLEAR COSMETICS COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("clear"),
				new EntitySelectorArgument.OnePlayer("player"),
				new MultiLiteralArgument(types))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticType type = CosmeticType.valueOf(((String) args[1]).toUpperCase(Locale.ROOT));

				CosmeticsManager.getInstance().clearCosmetics(player, type);
				if (sender instanceof Player) {
					sender.sendMessage(Component.text("Removed all " + type.getDisplayName() + "s from " + player.getName(), NamedTextColor.RED));
				}
			}).register();

		// GET COSMETICS COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("get"),
				new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				for (CosmeticType type : CosmeticType.values()) {
					listCosmetics(player, type, sender);
				}
			}).register();
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("get"),
				new EntitySelectorArgument.OnePlayer("player"),
				new MultiLiteralArgument(types))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticType type = CosmeticType.valueOf(((String) args[1]).toUpperCase(Locale.ROOT));
				listCosmetics(player, type, sender);
			}).register();

		// TEST COSMETIC COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("test"),
				new EntitySelectorArgument.OnePlayer("player"),
				new MultiLiteralArgument(types),
				allCosmeticsArgument)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				CosmeticType type = CosmeticType.valueOf(((String) args[1]).toUpperCase(Locale.ROOT));
				String name = (String) args[2];
				boolean hasCosmetic = CosmeticsManager.getInstance().playerHasCosmetic(player, type, name);
				if (sender instanceof Player) {
					if (hasCosmetic) {
						sender.sendMessage(Component.text(player.getName() + " has " + type.getDisplayName() + " '" + name + "'", NamedTextColor.GREEN));
					} else {
						sender.sendMessage(Component.text(player.getName() + " does not have " + type.getDisplayName() + " '" + name + "'", NamedTextColor.RED));
					}
				}
				return hasCosmetic ? 1 : 0;
			})
			.register();

		//OPEN GUI COMMAND
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("gui"),
				new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new CosmeticsGUI(plugin, player).openInventory(player, plugin);
			}).register();
		new CommandAPICommand("cosmetics")
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("gui"))
			.executesPlayer((player, args) -> {
				new CosmeticsGUI(plugin, player).openInventory(player, plugin);
			}).register();

	}

	private static void listCosmetics(Player player, CosmeticType type, CommandSender sender) {
		List<Cosmetic> cosmetics = CosmeticsManager.getInstance().getCosmeticsOfTypeAlphabetical(player, type);
		sender.sendMessage(Component.text(type.getDisplayName() + ":", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		if (cosmetics.isEmpty()) {
			sender.sendMessage(Component.text(" - none -", NamedTextColor.GRAY));
		} else {
			int lengthLimit = 1000;
			int length = 0;
			Component line = Component.empty();
			for (int i = 0; i < cosmetics.size(); i++) {
				Cosmetic c = cosmetics.get(i);
				String name = c.getName();
				if (length != 0 && name.length() + length > lengthLimit) {
					sender.sendMessage(line);
					length = 0;
					line = Component.empty();
				}
				if (name.length() <= lengthLimit) {
					line = line.append(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
					length += name.length();
				} else {
					line = line.append(Component.text(name.substring(0, lengthLimit), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
						.append(Component.text("...", NamedTextColor.GRAY));
					length += lengthLimit + 3;
				}
				if (i != cosmetics.size() - 1) {
					line = line.append(Component.text(", ", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
					length += 3;
				}
			}
			if (length != 0) {
				sender.sendMessage(line);
			}
		}
	}
}
