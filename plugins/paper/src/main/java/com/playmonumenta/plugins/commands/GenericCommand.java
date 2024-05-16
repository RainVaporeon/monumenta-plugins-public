package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.CommandUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class GenericCommand {
	@FunctionalInterface
	protected interface PlayerCommandExecutor {
		/**
		 * Called on each player targeted by the command
		 *
		 * @param sender Sender of the command
		 * @param player Target of the command
		 */
		void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException;
	}

	@FunctionalInterface
	protected interface EntityCommandExecutor {
		/**
		 * Called on each player targeted by the command
		 *
		 * @param sender Sender of the command
		 * @param entity Target of the command
		 */
		void run(CommandSender sender, Entity entity);
	}

	@SuppressWarnings("unchecked")
	protected static void registerPlayerCommand(String command, String permission, PlayerCommandExecutor exec) {
		CommandPermission perms = CommandPermission.fromString(permission);

		List<Argument<?>> arguments = new ArrayList<>();

		/* No-argument variant which just is the sender (if they are a player) */
		new CommandAPICommand(command)
			.withPermission(perms)
			.executes((sender, args) -> {
				exec.run(sender, CommandUtils.getPlayerFromSender(sender));
			})
			.register();

		/* Variant with player selector as arguments */
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					exec.run(sender, player);
				}
			})
			.register();
	}

	@SuppressWarnings("unchecked")
	protected static void registerEntityCommand(String command, String permission, EntityCommandExecutor exec) {
		CommandPermission perms = CommandPermission.fromString(permission);

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.ManyEntities("entities"));
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>)args[0]) {
					exec.run(sender, entity);
				}
			})
			.register();
	}

	protected static void error(CommandSender sender, String msg) {
		sender.sendMessage(Component.text(msg, NamedTextColor.RED));
	}
}
