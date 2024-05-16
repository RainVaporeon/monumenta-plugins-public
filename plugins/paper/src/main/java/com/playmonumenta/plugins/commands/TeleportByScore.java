package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class TeleportByScore extends GenericCommand {
	private static final String COMMAND = "teleportbyscore";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.teleportbyscore");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OneEntity("entity"));
		arguments.add(new ObjectiveArgument("x objective"));
		arguments.add(new ObjectiveArgument("y objective"));
		arguments.add(new ObjectiveArgument("z objective"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				teleport(sender, (Entity)args[0], (String)args[1], (String)args[2], (String)args[3], null, null, 1.0f, (FunctionWrapper[])null);
			})
			.register();

		arguments.add(new ObjectiveArgument("yaw objective"));
		arguments.add(new ObjectiveArgument("pitch objective"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				teleport(sender, (Entity)args[0], (String)args[1], (String)args[2], (String)args[3], (String)args[4], (String)args[5], 1.0f, (FunctionWrapper[])null);
			})
			.register();

		arguments.add(new FloatArgument("scale", 1));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				teleport(sender, (Entity)args[0], (String)args[1], (String)args[2], (String)args[3], (String)args[4], (String)args[5], (Float)args[6], (FunctionWrapper[])null);
			})
			.register();

		arguments.add(new MultiLiteralArgument("async"));
		arguments.add(new FunctionArgument("function"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				teleport(sender, (Entity) args[0], (String) args[1], (String) args[2], (String) args[3], (String) args[4], (String) args[5], (Float) args[6], (FunctionWrapper[]) args[8]);
			})
			.register();
	}

	private static @Nullable Integer getValue(Entity entity, @Nullable String obj) {
		if (obj == null || obj.equals("~")) {
			return null;
		}

		OptionalInt scoreboardValue = ScoreboardUtils.getScoreboardValue(entity, obj);
		return scoreboardValue.isPresent() ? scoreboardValue.getAsInt() : null;
	}

	private static void teleport(CommandSender sender, Entity entity,
	                             String objX, String objY, String objZ,
	                             @Nullable String objYaw, @Nullable String objPitch, float scale,
	                             FunctionWrapper @Nullable [] asyncFunctions) {
		Integer x = getValue(entity, objX);
		Integer y = getValue(entity, objY);
		Integer z = getValue(entity, objZ);
		Integer yawNullable = getValue(entity, objYaw);
		Integer pitchNullable = getValue(entity, objPitch);

		if (x == null) {
			error(sender, "Could not get value " + objX);
			return;
		} else if (y == null) {
			error(sender, "Could not get value " + objY);
			return;
		} else if (z == null) {
			error(sender, "Could not get value " + objZ);
			return;
		}

		float yaw;
		float pitch;
		if (yawNullable == null) {
			yaw = entity.getLocation().getYaw();
		} else {
			yaw = (float) yawNullable / scale;
		}
		if (pitchNullable == null) {
			pitch = entity.getLocation().getPitch();
		} else {
			pitch = (float) pitchNullable / scale;
		}

		Location loc = entity.getLocation();
		float offset = (scale == 1 ? 0.5f : 0.0f);
		loc.setX((float) x / scale + offset);
		loc.setY((float) y / scale + 0.1);
		loc.setZ((float) z / scale + offset);
		loc.setPitch(pitch);
		loc.setYaw(yaw);
		if (entity instanceof Mob) {
			entity.setVelocity(new Vector(0, 0.1, 0));
		}
		if (asyncFunctions != null) {
			TeleportAsync.teleport(sender, asyncFunctions, entity, loc, TeleportAsync.getLocationRotation(loc));
		} else {
			entity.teleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND);
		}
	}
}

