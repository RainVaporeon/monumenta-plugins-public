package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.depths.guis.DepthsAscensionGUI;
import com.playmonumenta.plugins.depths.guis.ZenithCharmPowerGUI;
import com.playmonumenta.plugins.guis.FishingDifficultyGui;
import com.playmonumenta.plugins.guis.IchorSelectionGUI;
import com.playmonumenta.plugins.guis.MusicGui;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiShowMobs;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class CustomInventoryCommands {
	public static void register(Plugin plugin) {
		//Avoid unused arguments, make sure you have a permission tied to the GUI command,
		//and perform any checks that should reject the player from opening the GUI here.
		//Once in the constructor for the GUI, it's much more difficult to properly
		//reject the player.
		new CommandAPICommand("openexamplecustominvgui")
			.withPermission("monumenta.command.openexamplecustominvgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new ExampleCustomInventory(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openteleportergui")
			.withPermission("monumenta.command.openteleportergui")
			.executesPlayer((player, args) -> {
				new OrinCustomInventory(player, -1).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openteleportergui")
			.withPermission("monumenta.command.openteleportergui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new OrinCustomInventory(player, -1).openInventory(player, plugin);
			})
			.register();

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("region #"));
		new CommandAPICommand("openinstancebot")
			.withPermission("monumenta.command.openinstancebot")
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				new OrinCustomInventory(player, (10 + (int) args[1])).openInventory(player, plugin);
			})
			.register();


		new CommandAPICommand("openpeb")
			.withPermission("monumenta.command.openpeb")
			.executesPlayer((player, args) -> {
				new PEBCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openpeb")
			.withPermission("monumenta.command.openpeb")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new PEBCustomInventory(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openzenithcharmpowergui")
			.withPermission("monumenta.command.openzenithcharmpowergui")
			.executesPlayer((player, args) -> {
				new ZenithCharmPowerGUI(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openzenithcharmpowergui")
			.withPermission("monumenta.command.openzenithcharmpowergui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new ZenithCharmPowerGUI(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openinfusiongui")
			.withPermission("monumenta.command.openinfusiongui")
			.executesPlayer((player, args) -> {
				if (!player.hasPermission("monumenta.infusions")) {
					player.sendMessage(Component.text("Infusions are disabled, try again later.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
					return;
				}
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new InfusionCustomInventory(player).openInventory(player, plugin), 1);
			})
			.register();
		new CommandAPICommand("openinfusiongui")
			.withPermission("monumenta.command.openinfusiongui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				if (!player.hasPermission("monumenta.infusions")) {
					player.sendMessage(Component.text("Infusions are disabled, try again later.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
					return;
				}
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new InfusionCustomInventory(player).openInventory(player, plugin), 1);
			})
			.register();

		new CommandAPICommand("opendelveinfusiongui")
			.withPermission("monumenta.command.opendelveinfusiongui")
			.executesPlayer((player, args) -> {
				if (!player.hasPermission("monumenta.infusions")) {
					player.sendMessage(Component.text("Infusions are disabled, try again later.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
					return;
				}
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new DelveInfusionCustomInventory(player).openInventory(player, plugin), 1);

			})
			.register();
		new CommandAPICommand("opendelveinfusiongui")
			.withPermission("monumenta.command.opendelveinfusiongui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				if (!player.hasPermission("monumenta.infusions")) {
					player.sendMessage(Component.text("Infusions are disabled, try again later.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
					return;
				}
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new DelveInfusionCustomInventory(player).openInventory(player, plugin), 1);
			})
			.register();

		new CommandAPICommand("openparrotgui")
			.withPermission("monumenta.command.openparrotgui")
			.executesPlayer((player, args) -> {
				try {
					new ParrotCustomInventory(player).open();
				} catch (Exception ex) {
					String msg = "Failed to open Parrot GUI: " + ex.getMessage();
					player.sendMessage(msg);
					ex.printStackTrace();
				}
			})
			.register();
		new CommandAPICommand("openparrotgui")
			.withPermission("monumenta.command.openparrotgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				try {
					new ParrotCustomInventory(player).open();
				} catch (Exception ex) {
					String msg = "Failed to open Parrot GUI: " + ex.getMessage();
					sender.sendMessage(msg);
					player.sendMessage(msg);
					ex.printStackTrace();
				}
			})
			.register();

		new CommandAPICommand("openblitzmobgui")
			.withPermission("monumenta.command.openblitzmobgui")
			.executesPlayer((player, args) -> {
				new TowerGuiShowMobs(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openblitzmobgui")
			.withPermission("monumenta.command.openblitzmobgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new TowerGuiShowMobs(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openfishingdifficultygui")
			.withPermission("monumenta.command.openfishingdifficultygui")
			.executesPlayer((player, args) -> {
				new FishingDifficultyGui(player).open();
			})
			.register();
		new CommandAPICommand("openfishingdifficultygui")
			.withPermission("monumenta.command.openfishingdifficultygui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new FishingDifficultyGui(player).open();
			})
			.register();
		new CommandAPICommand("openichorinfusiongui")
			.withPermission("monumenta.command.openichorinfusiongui")
			.executesPlayer((player, args) -> {
				ItemStack mainhand = player.getInventory().getItemInMainHand();
				if (IchorListener.isIchor(mainhand)) {
					new IchorSelectionGUI(player).open();
				}
			})
			.register();
		new CommandAPICommand("openichorinfusiongui")
			.withPermission("monumenta.command.openichorinfusiongui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				ItemStack mainhand = player.getInventory().getItemInMainHand();
				if (IchorListener.isIchor(mainhand)) {
					new IchorSelectionGUI(player).open();
				}
			})
			.register();
		new CommandAPICommand("openclassgui")
			.withPermission("monumenta.command.openclassgui")
			.executesPlayer((player, args) -> {
				new ClassSelectionCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openclassgui")
			.withPermission("monumenta.command.openclassgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				Player viewer = player;
				if (sender instanceof Player playerSender) {
					viewer = playerSender;
				}
				new ClassSelectionCustomInventory(player).openInventory(viewer, plugin);
			})
			.register();
		new CommandAPICommand("openclassdisplaygui")
			.withPermission("monumenta.command.openclassdisplaygui")
			.executesPlayer((player, args) -> {
				if (!AbilityUtils.getClass(player).equals("No Class")) {
					new ClassDisplayCustomInventory(player).open();
				}
			})
			.register();
		new CommandAPICommand("openclassdisplaygui")
			.withPermission("monumenta.command.openclassdisplaygui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				if (!AbilityUtils.getClass(player).equals("No Class")) {
					new ClassDisplayCustomInventory(player).open();
				}
			})
			.register();
		new CommandAPICommand("playerdetails")
			.withPermission("monumenta.command.playerdetails")
			.executesPlayer((player, args) -> {
				new ClassSelectionCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("playerdetails")
			.withPermission("monumenta.command.playerdetails")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				Player viewer = player;
				if (sender instanceof Player playerSender) {
					viewer = playerSender;
				}
				new PlayerDisplayCustomInventory(viewer, player).openInventory(viewer, plugin);
			})
			.register();
		new CommandAPICommand("openmasterworkgui")
			.withPermission("monumenta.command.openmasterworkgui")
			.executesPlayer((player, args) -> {
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new MasterworkCustomInventory(player).openInventory(player, plugin), 1);

			})
			.register();
		new CommandAPICommand("openmasterworkgui")
			.withPermission("monumenta.command.openmasterworkgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new MasterworkCustomInventory(player).openInventory(player, plugin), 1);
			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("region #"));
		arguments.add(new IntegerArgument("level"));

		List<String> questScore = new ArrayList<>(Arrays.asList("DailyQuest", "Daily2Quest", "Daily3Quest"));
		List<String> rewardScore = new ArrayList<>(Arrays.asList("DailyReward", "Daily2Reward", "Daily3Reward"));

		new CommandAPICommand("openbountygui")
			.withPermission("monumenta.command.openbountygui")
			.withArguments(arguments)
			.executes((sender, args) -> {
				try {
					Player player = (Player) args[0];
					int region = (int) args[1];
					int level = (int) args[2];
					if (ScoreboardUtils.getScoreboardValue(player, questScore.get(region - 1)).orElse(0) == 0 &&
						ScoreboardUtils.getScoreboardValue(player, rewardScore.get(region - 1)).orElse(0) == 0) {
						new BountyGui(player, region, level).open();
					}
				} catch (Exception e) {
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);
				}
			})
			.register();

		new CommandAPICommand("emoji")
			.withPermission("monumenta.command.emoji")
			.executesPlayer((player, args) -> {
				new EmojiCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("emoji")
			.withPermission("monumenta.command.emoji.others")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new EmojiCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("emote")
			.withPermission("monumenta.command.emote.self")
			.executesPlayer((player, arg) -> {
				emote(player);
			})
			.register();
		new CommandAPICommand("emote")
			.withPermission("monumenta.command.emote.others")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				emote(player);
			})
			.register();

		new CommandAPICommand("opentrinketgui")
			.withPermission("monumenta.command.opentrinketgui")
			.executesPlayer((player, args) -> {
				new KnickKnackSackGui(player).open();
			})
			.register();
		new CommandAPICommand("opentrinketgui")
			.withPermission("monumenta.command.opentrinketgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new KnickKnackSackGui(player).open();
			})
			.register();
		new CommandAPICommand("openascensiongui")
			.withPermission("monumenta.command.openascensiongui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player p = (Player) args[0];
				new DepthsAscensionGUI(p).open();
			}).register();

		new CommandAPICommand("openmusicgui")
			.withPermission("monumenta.command.openmusicgui")
			.withArguments(
				new EntitySelectorArgument.OnePlayer("player"),
				new MultiLiteralArgument(Arrays.stream(MusicGui.MusicPage.values()).map(page -> page.mLabel).toArray(String[]::new)),
				new BooleanArgument("fromRecordPlayer"),
				new BooleanArgument("playToOthers")
			)
			.executes((sender, args) -> {
				Player p = (Player) args[0];
				String label = (String) args[1];
				MusicGui.MusicPage musicPage = Arrays.stream(MusicGui.MusicPage.values()).filter(page -> page.mLabel.equals(label)).findAny().orElse(null);
				boolean fromRecordPlayer = (boolean) args[2];
				boolean playToOthers = (boolean) args[3];
				if (musicPage != null) {
					new MusicGui(p, musicPage, fromRecordPlayer, playToOthers).open();
				}
			}).register();

		new CommandAPICommand("openenchantexplanations")
			.withPermission("monumenta.command.openenchantexplanations.self")
			.executesPlayer((player, args) -> {
				new EnchantopediaGui(player).open();
			})
			.register();
		new CommandAPICommand("openenchantexplanationsfor")
			.withPermission("monumenta.command.openenchantexplanations.others")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new EnchantopediaGui(player).open();
			})
			.register();
	}

	private static void emote(Player player) {
		int defaultEmote = ScoreboardUtils.getScoreboardValue(player, EmojiCustomInventory.EMOJI_CHOICE_BOARD).orElse(0);
		ArrayList<EmojiCustomInventory.Emoji> list = new ArrayList<>(EmojiCustomInventory.EMOJI_LIST);
		list.removeIf(item -> item.mDefaultID != defaultEmote);
		if (list.isEmpty()) {
			player.sendMessage(Component.text("Select an emote in the emoji selection GUI as a default first!"));
			return;
		}
		if (list.get(0).mPatreon && !(ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_2)) {
			player.sendMessage(Component.text("You must be a T2+ Patron to use this emote!"));
			return;
		}
		EmojiCustomInventory.trySpawnEmoji(player, list.get(0).mEmojiName);
	}
}
