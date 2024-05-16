package com.playmonumenta.plugins.commands;

import com.bergerkiller.bukkit.common.utils.MathUtil;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.discoveries.DiscoveryManager;
import com.playmonumenta.plugins.discoveries.ItemDiscovery;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ParticleUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LootTableArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.sql.Date;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class DiscoveryCommand {
	private static final TextColor MESSAGE_COLOR = TextColor.color(224, 137, 51);
	private static final String PERMISSION = "monumenta.command.discovery";

	public static void register() {
		CommandAPICommand[] devCommands = {
			new CommandAPICommand("create")
				.withArguments(
					new LocationArgument("location"),
					new LootTableArgument("loot path"),
					new MultiLiteralArgument(Arrays.stream(ItemDiscovery.ItemDiscoveryTier.values()).map(ItemDiscovery.ItemDiscoveryTier::name).toArray(String[]::new))
				)
				.executesPlayer((player, args) -> {
					Location location = (Location) args[0];
					@Nullable ItemDiscovery discovery = DiscoveryManager.createDiscovery(location, ((LootTable) args[1]).getKey(), ItemDiscovery.ItemDiscoveryTier.valueOf((String) args[2]), null);
					if (discovery == null) {
						player.sendMessage(Component.text("Failed to create discovery", MESSAGE_COLOR));
					} else {
						player.sendMessage(Component.text("Created discovery with id " + discovery.mId, MESSAGE_COLOR));
					}
				}),
			new CommandAPICommand("create")
				.withArguments(
					new LocationArgument("location"),
					new LootTableArgument("loot path"),
					new MultiLiteralArgument(Arrays.stream(ItemDiscovery.ItemDiscoveryTier.values()).map(ItemDiscovery.ItemDiscoveryTier::name).toArray(String[]::new)),
					new FunctionArgument("executes")
				)
				.executesPlayer((player, args) -> {
					FunctionWrapper[] function = (FunctionWrapper[]) args[3];
					if (function.length == 0) {
						player.sendMessage(Component.text("Failed to get provided function"));
					}

					Location location = (Location) args[0];
					@Nullable ItemDiscovery discovery = DiscoveryManager.createDiscovery(location, ((LootTable) args[1]).getKey(), ItemDiscovery.ItemDiscoveryTier.valueOf((String) args[2]), function[0].getKey());
					if (discovery == null) {
						player.sendMessage(Component.text("Failed to create discovery", MESSAGE_COLOR));
					} else {
						player.sendMessage(Component.text("Created discovery with id " + discovery.mId, MESSAGE_COLOR));
					}
				}),
			new CommandAPICommand("getnearestid")
				.executesPlayer((player, args) -> {
					ItemDiscovery nearest = DiscoveryManager.getNearestToLocation(player.getLocation());

					if (nearest == null) {
						player.sendMessage(Component.text("There are no nearby discoveries", MESSAGE_COLOR));
						return;
					}

					Location location = nearest.mMarkerEntity.getLocation();
					player.sendMessage(Component.text(String.format("Discovery with id %s is at [%s, %s, %s]", nearest.mId, MathUtil.round(location.x(), 2), MathUtil.round(location.y(), 2), MathUtil.round(location.z(), 2)), MESSAGE_COLOR)
						.hoverEvent(HoverEvent.showText(Component.text("Click to teleport")))
						.clickEvent(ClickEvent.runCommand(String.format("/tp %s %s %s", location.x(), location.y(), location.z()))));
				}),
			new CommandAPICommand("edit")
				.withSubcommands(
				new CommandAPICommand("nearest")
					.withSubcommands(
						new CommandAPICommand("id")
							.withArguments(
								new IntegerArgument("new id")
							)
							.executesPlayer((player, args) -> {
								Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
									ItemDiscovery discovery = DiscoveryManager.getNearestToLocation(player.getLocation());
									if (discovery != null) {
										DiscoveryManager.setNewId(discovery, (int) args[0]);
										player.sendMessage(Component.text("Updated 1 discovery", MESSAGE_COLOR));
									} else {
										player.sendMessage(Component.text("There are no nearby discoveries", MESSAGE_COLOR));
									}
								});
							}),
						new CommandAPICommand("loot")
							.withArguments(
								new LootTableArgument("new loot path")
							)
							.executesPlayer((player, args) -> {
								Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
									ItemDiscovery discovery = DiscoveryManager.getNearestToLocation(player.getLocation());
									if (discovery != null) {
										DiscoveryManager.setNewLoot(discovery, ((LootTable) args[0]).getKey());
										player.sendMessage(Component.text("Updated 1 discovery", MESSAGE_COLOR));
									} else {
										player.sendMessage(Component.text("There are no nearby discoveries", MESSAGE_COLOR));
									}
								});
							}),
						new CommandAPICommand("tier")
							.withArguments(
								new MultiLiteralArgument(Arrays.stream(ItemDiscovery.ItemDiscoveryTier.values()).map(ItemDiscovery.ItemDiscoveryTier::name).toArray(String[]::new))
							)
							.executesPlayer((player, args) -> {
								Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
									ItemDiscovery discovery = DiscoveryManager.getNearestToLocation(player.getLocation());
									if (discovery != null) {
										DiscoveryManager.setNewTier(discovery, ItemDiscovery.ItemDiscoveryTier.valueOf((String) args[0]));
										player.sendMessage(Component.text("Updated 1 discovery", MESSAGE_COLOR));
									} else {
										player.sendMessage(Component.text("There are no nearby discoveries", MESSAGE_COLOR));
									}
								});
							}),
						new CommandAPICommand("function")
							.executesPlayer((player, args) -> {
								Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
									ItemDiscovery discovery = DiscoveryManager.getNearestToLocation(player.getLocation());
									if (discovery != null) {
										DiscoveryManager.setNewFunction(discovery, null);
										player.sendMessage(Component.text("Updated 1 discovery", MESSAGE_COLOR));
									} else {
										player.sendMessage(Component.text("There are no nearby discoveries", MESSAGE_COLOR));
									}
								});
							}),
						new CommandAPICommand("function")
							.withArguments(
								new FunctionArgument("new function path")
							)
							.executesPlayer((player, args) -> {
								Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
									ItemDiscovery discovery = DiscoveryManager.getNearestToLocation(player.getLocation());
									if (discovery != null) {
										FunctionWrapper[] function = (FunctionWrapper[]) args[0];
										if (function.length == 0) {
											player.sendMessage(Component.text("Failed to get provided function"));
										}
										DiscoveryManager.setNewFunction(discovery, function[0].getKey());
										player.sendMessage(Component.text("Updated 1 discovery", MESSAGE_COLOR));
									} else {
										player.sendMessage(Component.text("There are no nearby discoveries", MESSAGE_COLOR));
									}
								});
							}),
						new CommandAPICommand("location")
							.withArguments(
								new LocationArgument("new location")
							)
							.executesPlayer((player, args) -> {
								ItemDiscovery discovery = DiscoveryManager.getNearestToLocation(player.getLocation());
								if (discovery != null) {
									discovery.mMarkerEntity.teleport((Location) args[0]);
									player.sendMessage(Component.text("Updated 1 discovery", MESSAGE_COLOR));
								} else {
									player.sendMessage(Component.text("There are no nearby discoveries", MESSAGE_COLOR));
								}
							}),
						new CommandAPICommand("relativelocation")
							.withArguments(
								new DoubleArgument("x"),
								new DoubleArgument("y"),
								new DoubleArgument("z")
							)
							.executesPlayer((player, args) -> {
								ItemDiscovery discovery = DiscoveryManager.getNearestToLocation(player.getLocation());
								if (discovery != null) {
									discovery.mMarkerEntity.teleport(discovery.mMarkerEntity.getLocation().clone().add((double) args[0], (double) args[1], (double) args[2]));
									player.sendMessage(Component.text("Updated 1 discovery", MESSAGE_COLOR));
								} else {
									player.sendMessage(Component.text("There are no nearby discoveries", MESSAGE_COLOR));
								}
							})
					),
				new CommandAPICommand("id")
					.withSubcommands(
						new CommandAPICommand("id")
							.withArguments(
								new IntegerArgument("id"),
								new IntegerArgument("new id")
							)
							.executesPlayer((player, args) -> {
								Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
									int successes = DiscoveryManager.setNewId((int) args[0], (int) args[1]);

									player.sendMessage(Component.text(String.format("Updated %s %s", successes, successes == 1 ? "discovery" : "discoveries"), MESSAGE_COLOR));
								});
							}),
						new CommandAPICommand("loot")
							.withArguments(
								new IntegerArgument("id"),
								new LootTableArgument("new loot path")
							)
							.executesPlayer((player, args) -> {
								Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
									int successes = DiscoveryManager.setNewLoot((int) args[0], ((LootTable) args[1]).getKey());

									player.sendMessage(Component.text(String.format("Updated %s %s", successes, successes == 1 ? "discovery" : "discoveries"), MESSAGE_COLOR));
								});
							}),
						new CommandAPICommand("tier")
							.withArguments(
								new IntegerArgument("id"),
								new MultiLiteralArgument(Arrays.stream(ItemDiscovery.ItemDiscoveryTier.values()).map(ItemDiscovery.ItemDiscoveryTier::name).toArray(String[]::new))
							)
							.executesPlayer((player, args) -> {
								Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
									int successes = DiscoveryManager.setNewTier((int) args[0], ItemDiscovery.ItemDiscoveryTier.valueOf((String) args[1]));

									player.sendMessage(Component.text(String.format("Updated %s %s", successes, successes == 1 ? "discovery" : "discoveries"), MESSAGE_COLOR));
								});
							}),
						new CommandAPICommand("function")
							.withArguments(
								new IntegerArgument("id")
							)
							.executesPlayer((player, args) -> {
								Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
									int successes = DiscoveryManager.setNewFunction((int) args[0], null);

									player.sendMessage(Component.text(String.format("Updated %s %s", successes, successes == 1 ? "discovery" : "discoveries"), MESSAGE_COLOR));
								});
							}),
						new CommandAPICommand("function")
							.withArguments(
								new IntegerArgument("id"),
								new FunctionArgument("new function path")
							)
							.executesPlayer((player, args) -> {
								Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
									FunctionWrapper[] function = (FunctionWrapper[]) args[1];
									if (function.length == 0) {
										player.sendMessage(Component.text("Failed to get provided function"));
									}

									int successes = DiscoveryManager.setNewFunction((int) args[0], function[0].getKey());

									player.sendMessage(Component.text(String.format("Updated %s %s", successes, successes == 1 ? "discovery" : "discoveries"), MESSAGE_COLOR));
								});
							}),
						new CommandAPICommand("location")
							.withArguments(
								new IntegerArgument("id"),
								new LocationArgument("new location")
							)
							.executesPlayer((player, args) -> {
								List<ItemDiscovery> discoveries = DiscoveryManager.getById((int) args[0]);

								if (discoveries.size() == 0) {
									player.sendMessage(Component.text("Could not find any discoveries", MESSAGE_COLOR));
									return;
								}

								discoveries.forEach(discovery -> discovery.mMarkerEntity.teleport((Location) args[1]));

								player.sendMessage(Component.text(String.format("Updated %s %s", discoveries.size(), discoveries.size() == 1 ? "discovery" : "discoveries"), MESSAGE_COLOR));
							}),
						new CommandAPICommand("relativelocation")
							.withArguments(
								new IntegerArgument("id"),
								new DoubleArgument("x"),
								new DoubleArgument("y"),
								new DoubleArgument("z")
							)
							.executesPlayer((player, args) -> {
								List<ItemDiscovery> discoveries = DiscoveryManager.getById((int) args[0]);

								if (discoveries.size() == 0) {
									player.sendMessage(Component.text("Could not find any discoveries", MESSAGE_COLOR));
									return;
								}

								discoveries.forEach(discovery -> discovery.mMarkerEntity.teleport(discovery.mMarkerEntity.getLocation().clone().add((double) args[1], (double) args[2], (double) args[3])));

								player.sendMessage(Component.text(String.format("Updated %s %s", discoveries.size(), discoveries.size() == 1 ? "discovery" : "discoveries"), MESSAGE_COLOR));
							})
					)
			),
			new CommandAPICommand("delete")
				.withSubcommands(
				new CommandAPICommand("nearest")
					.executesPlayer((player, args) -> {
						ItemDiscovery discovery = DiscoveryManager.getNearestToLocation(player.getLocation());
						if (discovery != null && DiscoveryManager.removeDiscovery(discovery)) {
							player.sendMessage(Component.text("Removed discovery", MESSAGE_COLOR));
						} else {
							player.sendMessage(Component.text("Could not find discovery", MESSAGE_COLOR));
						}
					}),
				new CommandAPICommand("id")
					.withArguments(
						new IntegerArgument("id")
					)
					.executesPlayer((player, args) -> {
						int successes = DiscoveryManager.removeDiscovery((int) args[0]);

						player.sendMessage(Component.text(String.format("Removed %s %s", successes, successes == 1 ? "discovery" : "discoveries"), MESSAGE_COLOR));
					})
			),
			new CommandAPICommand("nearbyidpopup")
				.executesPlayer((player, args) -> {
					List<ItemDiscovery> discoveries = DiscoveryManager.getDiscoveriesInRange(player.getLocation(), 20);
					new BukkitRunnable() {
						int mIters = 0;

						@Override
						public void run() {
							discoveries.forEach(discovery -> ParticleUtils.drawSevenSegmentNumber(
								discovery.mId,
								discovery.mMarkerEntity.getLocation().clone().add(0, 1.5, 0),
								player, 0.65, 0.5,
								Particle.SCRAPE,
								null));
							mIters++;
							if (mIters >= 10) {
								this.cancel();
							}
						}
					}.runTaskTimer(Plugin.getInstance(), 0, 5);
				}),
			new CommandAPICommand("list")
				.withSubcommands(
					new CommandAPICommand("all")
						.executesPlayer((player, args) -> {
							showAllDiscoveryInfo(player, 1, true, true);
						}),
					new CommandAPICommand("all")
						.withArguments(
							new IntegerArgument("page")
						)
						.executesPlayer((player, args) -> {
							showAllDiscoveryInfo(player, (int) args[0], true, true);
						}),
					new CommandAPICommand("all")
						.withArguments(
							new MultiLiteralArgument("existing", "deleted")
						)
						.executesPlayer((player, args) -> {
							String show = (String) args[0];
							showAllDiscoveryInfo(player, 1, show.equals("existing"), show.equals("deleted"));
						}),
					new CommandAPICommand("all")
						.withArguments(
							new MultiLiteralArgument("existing", "deleted"),
							new IntegerArgument("page")
						)
						.executesPlayer((player, args) -> {
							String show = (String) args[0];
							showAllDiscoveryInfo(player, (int) args[1], show.equals("existing"), show.equals("deleted"));
						}),
					new CommandAPICommand("loaded")
						.executesPlayer((player, args) -> {
							showLoadedDiscoveryInfo(player, 1);
						}),
					new CommandAPICommand("loaded")
						.withArguments(
							new IntegerArgument("page")
						)
						.executesPlayer((player, args) -> {
							showLoadedDiscoveryInfo(player, (int) args[0]);
						})
				),
				new CommandAPICommand("removedeleted")
					.withArguments(
						new StringArgument("deleted uuid")
					)
					.executesPlayer((player, args) -> {
						Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
							if (DiscoveryManager.removeDeleted((String) args[0])) {
								player.sendMessage(Component.text("Successfully removed", MESSAGE_COLOR));
							} else {
								player.sendMessage(Component.text("Failed to remove", MESSAGE_COLOR));
							}
						});
					}),
				new CommandAPICommand("setnextid")
					.withArguments(
						new IntegerArgument("value")
					)
					.executesPlayer((player, args) -> {
						Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
							int nextId = (int) args[0];
							if (DiscoveryManager.setNextId(nextId)) {
								player.sendMessage(Component.text("Updated next id to " + nextId, MESSAGE_COLOR));
							} else {
								player.sendMessage(Component.text("Failed to update", MESSAGE_COLOR));
							}
						});
					})
		};

		CommandAPICommand[] buildCommands = {
			new CommandAPICommand("setcollected")
				.withArguments(
					new EntitySelectorArgument.OnePlayer("player"),
					new LiteralArgument("id"),
					new IntegerArgument("id"),
					new BooleanArgument("collected")
				)
				.executes((sender, args) -> {
					Player player = (Player) args[0];
					if (DiscoveryManager.setPlayerCollected(player, (int) args[1], (boolean) args[2])) {
						sender.sendMessage(Component.text("Updated discovery for " + player.getName(), MESSAGE_COLOR));
					} else {
						sender.sendMessage(Component.text("Failed for update discovery for " + player.getName(), MESSAGE_COLOR));
					}
				}),
			new CommandAPICommand("setcollected")
				.withArguments(
					new EntitySelectorArgument.OnePlayer("player"),
					new LiteralArgument("nearest"),
					new BooleanArgument("collected")
				)
				.executesPlayer((player, args) -> {
					ItemDiscovery nearest = DiscoveryManager.getNearestToLocation(player.getLocation());

					if (nearest == null) {
						player.sendMessage(Component.text("There are no nearby discoveries", MESSAGE_COLOR));
						return;
					}

					Player targetPlayer = (Player) args[0];
					if (DiscoveryManager.setPlayerCollected(targetPlayer, nearest.mId, (boolean) args[1])) {
						player.sendMessage(Component.text("Updated discovery for " + targetPlayer.getName(), MESSAGE_COLOR));
					} else {
						player.sendMessage(Component.text("Failed to update discovery for " + targetPlayer.getName(), MESSAGE_COLOR));
					}
				}),
			new CommandAPICommand("listcollected")
				.withArguments(
					new PlayerArgument("player")
				)
				.executesPlayer((player, args) -> {
					Player targetPlayer = (Player) args[0];

					List<Integer> collected = DiscoveryManager.getPlayerCollected(targetPlayer);
					if (collected == null) {
						player.sendMessage(Component.text("Could not get data", MESSAGE_COLOR));
						return;
					}
					if (collected.size() == 0) {
						player.sendMessage(Component.text(targetPlayer.getName() + " has not collected anything", MESSAGE_COLOR));
						return;
					}

					Collections.sort(collected);

					Component message = Component.text(targetPlayer.getName() + " has collected ids: ", MESSAGE_COLOR);
					for (int id : collected) {
						message = message.append(Component.text(id, MESSAGE_COLOR)
							.hoverEvent(HoverEvent.showText(Component.text("Click to prompt to remove")))
							.clickEvent(ClickEvent.suggestCommand(String.format("/discovery setcollected %s id %s false", targetPlayer.getName(), id))));

						if (collected.indexOf(id) != collected.size() - 1) {
							message = message.append(Component.text(", ", MESSAGE_COLOR));
						}
					}

					player.sendMessage(message);
				}),
			new CommandAPICommand("checkcollected")
				.withArguments(
					new PlayerArgument("player"),
					new IntegerArgument("id")
				)
				.executes((sender, args) -> {
					Player player = (Player) args[0];

					@Nullable List<Integer> collected = DiscoveryManager.getPlayerCollected(player);
					if (collected == null) {
						sender.sendMessage(Component.text("Could not get data", MESSAGE_COLOR));
						return -1;
					}

					int id = (int) args[1];
					boolean isCollected = collected.contains(id);

					sender.sendMessage(Component.text(String.format("%s %s collected %s", player.getName(), isCollected ? "has" : "has not", id), MESSAGE_COLOR));
					return isCollected ? 1 : 0;
				}),
			new CommandAPICommand("info")
				.withSubcommands(
				new CommandAPICommand("nearest")
					.executesPlayer((player, args) -> {
						ItemDiscovery discovery = DiscoveryManager.getNearestToLocation(player.getLocation());

						if (discovery != null) {
							player.sendMessage(formatDiscoveryListElement(discovery));
						} else {
							player.sendMessage(Component.text("Could not find any loaded discoveries", MESSAGE_COLOR));
						}
					}),
				new CommandAPICommand("id")
					.withArguments(
						new IntegerArgument("id")
					)
					.executesPlayer((player, args) -> {
						List<ItemDiscovery> discoveries = DiscoveryManager.getById((int) args[0]);

						if (discoveries.size() == 0) {
							player.sendMessage(Component.text("Could not find any loaded discoveries", MESSAGE_COLOR));
						}

						discoveries.forEach(discovery -> player.sendMessage(formatDiscoveryListElement(discovery)));
					})
			)
		};

		CommandAPICommand[] allCommands = ArrayUtils.clone(buildCommands);
		if (!Plugin.IS_PLAY_SERVER) {
			allCommands = ArrayUtils.addAll(allCommands, devCommands);
		}

		new CommandAPICommand("discovery")
			.withPermission(PERMISSION)
			.withSubcommands(
				allCommands
			).register();
	}

	private static void showLoadedDiscoveryInfo(Player player, int providedPage) {
		List<ItemDiscovery> allLoadedDiscoveries = DiscoveryManager.getAllLoadedDiscoveries();

		allLoadedDiscoveries.sort(Comparator.comparingInt(o -> o.mId));
		Collections.reverse(allLoadedDiscoveries);

		int maxPage = (int)Math.ceil((double)allLoadedDiscoveries.size() / 10);
		int page = Math.min(providedPage, maxPage);

		List<ItemDiscovery> toShow = allLoadedDiscoveries.subList(Math.max(0, (page - 1) * 10), Math.min(page * 10, allLoadedDiscoveries.size()));

		if (toShow.size() == 0) {
			player.sendMessage(Component.text("There is nothing to show", MESSAGE_COLOR));
			return;
		}

		player.sendMessage(Component.text(String.format("Showing page %s/%s", page, maxPage), MESSAGE_COLOR).decorate(TextDecoration.UNDERLINED));
		toShow.forEach(discovery -> player.sendMessage(formatDiscoveryListElement(discovery)));
	}

	private static void showAllDiscoveryInfo(Player player, int providedPage, boolean showExisting, boolean showDeleted) {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			@Nullable List<JsonObject> allDiscoveries = DiscoveryManager.getAllDiscoveries();

			if (allDiscoveries == null) {
				player.sendMessage(Component.text("Could not get all discoveries", MESSAGE_COLOR));
				return;
			}

			allDiscoveries.sort(Comparator.comparingInt(o -> o.get("id").getAsInt()));
			Collections.reverse(allDiscoveries);

			// remove entries that are not to be shown
			allDiscoveries.removeIf(object -> (!showExisting && !object.has("deleted")) || (!showDeleted && object.has("deleted")));

			int maxPage = (int)Math.ceil((double)allDiscoveries.size() / 10);
			int page = Math.min(providedPage, maxPage);

			List<JsonObject> toShow = allDiscoveries.subList(Math.max(0, (page - 1) * 10), Math.min(page * 10, allDiscoveries.size()));

			if (toShow.size() == 0) {
				player.sendMessage(Component.text("There is nothing to show", MESSAGE_COLOR));
				return;
			}

			player.sendMessage(Component.text(String.format("Showing page %s/%s", page, maxPage), MESSAGE_COLOR).decorate(TextDecoration.UNDERLINED));
			toShow.forEach(object -> player.sendMessage(formatDiscoveryListElement(object)));
		});
	}

	private static Component formatDiscoveryListElement(JsonObject object) {
		JsonObject location = object.getAsJsonObject("location");
		return formatDiscoveryListElement(
			object.get("id").getAsInt(),
			object.get("marker_uuid").getAsString(),
			object.get("tier").getAsString(),
			object.get("loot").getAsString(),
			object.get("function").getAsString(),
			location.has("shard") ? location.get("shard").getAsString() : null,
			location.get("world").getAsString(),
			location.get("x").getAsDouble(),
			location.get("y").getAsDouble(),
			location.get("z").getAsDouble(),
			object.has("deleted") ? object.get("deleted").getAsInt() : -1
		);
	}

	private static Component formatDiscoveryListElement(ItemDiscovery discovery) {
		return formatDiscoveryListElement(
			discovery.mId,
			discovery.mMarkerEntity.getUniqueId().toString(),
			discovery.mTier.name(),
			discovery.mLootTablePath.getNamespace() + ":" + discovery.mLootTablePath.getKey(),
			discovery.mOptionalFunctionPath == null ? "None" : (discovery.mOptionalFunctionPath.getNamespace() + ":" + discovery.mOptionalFunctionPath.getKey()),
			ServerProperties.getShardName(),
			discovery.mMarkerEntity.getWorld().getKey().asString(),
			discovery.mMarkerEntity.getLocation().getX(),
			discovery.mMarkerEntity.getLocation().getY(),
			discovery.mMarkerEntity.getLocation().getZ(),
			-1
		);
	}

	// provide -1 if the element has not been deleted
	private static Component formatDiscoveryListElement(int id, String uuid, String tier, String lootPath, String functionPath, @Nullable String shard, String world, double x, double y, double z, int deleted) {
		Component text = Component.text(String.format("Id: %s in %s", id, shard == null || ServerProperties.getShardName().equals(shard) ? "World: " + world : "Shard: " + shard), MESSAGE_COLOR);

		// deleted != -1 means that the discovery has been deleted
		if (deleted != -1) {
			text = text.decorate(TextDecoration.STRIKETHROUGH)
				.color(NamedTextColor.DARK_GRAY)
				.hoverEvent(HoverEvent.showText(Component.text("Deleted on " + Date.from(Instant.ofEpochSecond(deleted)))));
		}

		text = text.hoverEvent(HoverEvent.showText(
				Component.text("Id: " + id)
				.appendNewline()
				.append(Component.text("UUID: " + uuid))
				.appendNewline()
				.append(Component.text("Tier: " + tier))
				.appendNewline()
				.append(Component.text("Loot: " + lootPath))
				.appendNewline()
				.append(Component.text("Function: " + (functionPath.equals("") ? "None" : functionPath)))
				.appendNewline()
				.append(Component.text(shard == null || ServerProperties.getShardName().equals(shard) ? "World: " + world : "Shard: " + shard)) // if the shard matches the current shard, display the world
				.appendNewline()
				.append(Component.text(String.format("Location: %s %s %s", MathUtil.round(x, 2), MathUtil.round(y, 2), MathUtil.round(z, 2))))
				.appendNewline().appendNewline()
				.append(Component.text(
					deleted != -1 ? "Click to remove" : (shard == null || ServerProperties.getShardName().equals(shard) ? "Click to teleport" : "Click to switch shards")
				))
			))
			.clickEvent(ClickEvent.runCommand(
				deleted != -1 ? String.format("/discovery removedeleted %s", uuid) : (shard == null || ServerProperties.getShardName().equals(shard) ? String.format("/execute in %s run tp %s %s %s", world, x, y, z) : "/s " + shard)
			));

		return text;
	}
}
