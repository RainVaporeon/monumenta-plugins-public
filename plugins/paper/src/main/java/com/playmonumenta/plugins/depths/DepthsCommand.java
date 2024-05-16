package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.commands.GenericCommand;
import com.playmonumenta.plugins.depths.charmfactory.CharmFactory;
import com.playmonumenta.plugins.depths.guis.DepthsDebugGUI;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class DepthsCommand extends GenericCommand {

	public static void register(Plugin plugin) {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.depths");

		//INIT COMMAND - loads nearby players into one party, or joins new player to an existing one

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("init"));

		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				DepthsManager.getInstance().init(player);
			})
			.register();

		//ABILITY COMMAND - debug to set a player's level in an ability directly

		arguments.clear();
		arguments.add(new LiteralArgument("ability"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new TextArgument("Ability Name")
			              .replaceSuggestions(ArgumentSuggestions.strings(DepthsManager.getAbilities().stream().map(a -> '"' + a.getDisplayName() + '"').toList())));
		arguments.add(new IntegerArgument("Rarity"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				DepthsManager.getInstance().setPlayerLevelInAbility((String) args[1], player, (int) args[2]);
			}).register();

		//DELETE COMMAND - removes a player from the depths system and clears their data

		arguments.clear();
		arguments.add(new LiteralArgument("delete"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				DepthsManager.getInstance().deletePlayer(player);
				player.sendMessage("Removed you from depths system");
			}).register();

		//PARTY COMMAND - prints a summary string of party information to the player

		arguments.clear();
		arguments.add(new LiteralArgument("party"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				player.sendMessage(DepthsManager.getInstance().getPartySummary(player));
			}).register();

		//SPAWN COMMAND - ran at the end of each room to load the next one dynamically

		arguments.clear();
		arguments.add(new LiteralArgument("spawn"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new LocationArgument("loc", LocationType.BLOCK_POSITION));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				DepthsManager.getInstance().gotRoomEndpoint(player, (Location) args[1]);
			}).register();

		//WEAPON COMMAND - run once by each player to select their weapon aspect

		arguments.clear();
		arguments.add(new LiteralArgument("weapon"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "opendepthsgui weaponaspect " + player.getName());
			}).register();

		//TREASURE COMMAND - run to increase the party's treasure score by given amount (used in select rooms)

		arguments.clear();
		arguments.add(new LiteralArgument("treasure"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("amount"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				DepthsManager.getInstance().incrementTreasure(null, player, (int) args[1]);

			}).register();

		//CHAOS COMMAND - run in select utility room to replace one player's ability with two others

		arguments.clear();
		arguments.add(new LiteralArgument("chaos"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				DepthsManager.getInstance().chaos(player);

			}).register();

		//WHEEL COMMAND - run in select utility room to trigger a curious effect depending on the result

		arguments.clear();
		arguments.add(new LiteralArgument("wheel"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("result"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				DepthsManager.getInstance().wheel(player, (int) args[1]);

			}).register();

		//FLOOR COMMAND - debug command to manually send a party to the next floor

		arguments.clear();
		arguments.add(new LiteralArgument("floor"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				DepthsManager.getInstance().goToNextFloor(player);

			}).register();

		//BOSSFIGHT COMMAND - run by boss rooms to spawn the boss and initiate combat

		arguments.clear();
		arguments.add(new LiteralArgument("bossfight"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new LocationArgument("loc", LocationType.BLOCK_POSITION));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				DepthsManager.getInstance().startBossFight(player, (Location) args[1]);
			}).register();

		//ROOM COMMAND - debug command to manually set the party's room number to specified value
		arguments.clear();
		arguments.add(new LiteralArgument("room"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("amount"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				DepthsManager.getInstance().setRoomDebug(player, (int) args[1]);

			}).register();

		//ROOM COMMAND - debug command to manually set the party's room number to specified value
		arguments.clear();
		arguments.add(new LiteralArgument("debug"));
		arguments.add(new EntitySelectorArgument.OnePlayer("moderator"));
		arguments.add(new EntitySelectorArgument.OnePlayer("target player"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player requestingPlayer = (Player) args[0];
				Player targetPlayer = (Player) args[1];

				new DepthsDebugGUI(requestingPlayer, targetPlayer, plugin).openInventory(requestingPlayer, plugin);

			}).register();

		//CHARMGEN COMMAND - debug command to gen charmfactory charm
		arguments.clear();
		arguments.add(new LiteralArgument("charmfactory"));
		arguments.add(new LiteralArgument("create"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("rarity"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				Location loc = player.getLocation();
				int power = new Random().nextInt(5) + 1;
				ItemStack charm = CharmFactory.generateCharm((int) args[1], power, 0, null, null, null, null, null);
				player.sendMessage("DEBUG SEED " + Objects.requireNonNull(ItemStatUtils.getPlayerModified(new NBTItem(charm))).getLong(CharmFactory.CHARM_UUID_KEY));
				loc.getWorld().dropItem(loc, charm);
			}).register();

		//CHARMGEN COMMAND - debug command to update charmfactory charm
		arguments.clear();
		arguments.add(new LiteralArgument("charmfactory"));
		arguments.add(new LiteralArgument("update"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				ItemStack item = player.getInventory().getItemInMainHand();

				ItemStack newCharm = CharmFactory.updateCharm(item);
				if (newCharm != null) {
					player.getInventory().setItemInMainHand(newCharm);
					item.setAmount(0);
					player.updateInventory();
					player.sendMessage("DEBUG SEED " + Objects.requireNonNull(ItemStatUtils.getPlayerModified(new NBTItem(newCharm))).getLong(CharmFactory.CHARM_UUID_KEY));
				}

			}).register();

		//CHECK ENDLESS COMMAND - mech command that returns 1 if endless, else 0
		arguments.clear();
		arguments.add(new LiteralArgument("check"));
		arguments.add(new LiteralArgument("endless"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				return DepthsManager.getInstance().getIsEndless(player) ? 1 : 0;
			}).register();

		// UNSTUCK COMMAND - moderator command to be used if a party somehow gets stuck in the state of loading a room and can't choose a new one
		arguments.clear();
		arguments.add(new LiteralArgument("unstuck"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("depths")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				DepthsParty party = DepthsManager.getInstance().getDepthsParty(player);
				if (party != null) {
					party.mIsLoadingRoom = false;
				}
			}).register();
	}
}
