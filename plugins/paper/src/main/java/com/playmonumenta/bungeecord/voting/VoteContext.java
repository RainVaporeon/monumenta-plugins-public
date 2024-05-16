package com.playmonumenta.bungeecord.voting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.redissync.RemoteDataAPI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

/*
 * This is all the voting data for one player
 * This includes their "scores", and when they will be eligible to vote next
 * It is saved to disk every time it is updated - there is no need to save it again when stopping bungee or the player logs out
 */
public class VoteContext {
	private static final String VOTES_THIS_WEEK = "votesThisWeek";
	private static final String VOTES_TOTAL = "votesTotal";
	private static final String VOTES_UNCLAIMED = "votesUnclaimed";
	private static final String RAFFLE_ENTRIES = "raffleEntries";
	private static final String RAFFLE_WINS_TOTAL = "raffleWinsTotal";
	private static final String RAFFLE_WINS_UNCLAIMED = "raffleWinsUnclaimed";
	private static final String VOTES_OFF_COOLDOWN_TIMES = "votesOffCooldownTimes";

	private final Plugin mPlugin;
	private final UUID mUUID;

	/* This lock used to protect mOffCooldownTimes */
	private final ReadWriteLock mLock = new ReentrantReadWriteLock();

	/*
	 * Map of the time when a site will go off cooldown
	 * Entries are removed as time has been passed
	 */
	private final Map<String, Long> mOffCooldownTimes;

	/*
	 * Creates a new VoteContext for the player, loading their current off cooldown times if they exist
	 *
	 * Will block for a single round trip to redis
	 */
	private VoteContext(Plugin plugin, UUID uuid, Map<String, Long> offCooldownTimes) {
		mPlugin = plugin;
		mUUID = uuid;
		mOffCooldownTimes = offCooldownTimes;
	}

	protected static CompletableFuture<VoteContext> getVoteContext(Plugin plugin, UUID uuid) {
		return RemoteDataAPI.get(uuid, VOTES_OFF_COOLDOWN_TIMES).thenApply((data) -> {
			HashMap<String, Long> offCooldownTimes = new HashMap<>();

			if (data != null && !data.isEmpty()) {
				Gson gson = new Gson();
				JsonObject object = gson.fromJson(data, JsonObject.class);
				for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
					if (VoteManager.getSiteTimers().containsKey(entry.getKey())) {
						offCooldownTimes.put(entry.getKey(), entry.getValue().getAsLong());
					} else {
						plugin.getLogger().warning("Failed to load offCooldownTimes entry '" + entry.getKey() +
													   "' for player '" + uuid.toString() + "'");
					}
				}
			}

			return new VoteContext(plugin, uuid, offCooldownTimes);
		});
	}

	private void saveOffCooldownTimes() {
		/* Convert the cooldown times to a JSON string that can be saved in a single field */
		Gson gson = new GsonBuilder().create();
		JsonObject cooldownObj = new JsonObject();
		mLock.readLock().lock();
		for (Map.Entry<String, Long> entry : mOffCooldownTimes.entrySet()) {
			cooldownObj.addProperty(entry.getKey(), entry.getValue());
		}
		mLock.readLock().unlock();
		String cooldownStr = gson.toJson(cooldownObj);

		RemoteDataAPI.set(mUUID, VOTES_OFF_COOLDOWN_TIMES, cooldownStr).whenComplete((result, ex) -> {
			if (ex != null) {
				mPlugin.getLogger().warning("Failed to set remote data off cooldown times: " + ex.getMessage());
			}
		});
	}

	private String timeDeltaStr(long totalSecondsLeft) {
		long minutesLeft = (totalSecondsLeft / 60) % 60;
		long hoursLeft = (totalSecondsLeft / (60 * 60));
		String str = "";

		if (hoursLeft > 0) {
			str += hoursLeft + " hour";
			if (hoursLeft != 1) {
				str += "s";
			}
		}

		if (minutesLeft > 0) {
			if (str.length() > 0) {
				str += " ";
			}
			str += minutesLeft + " minute";
			if (minutesLeft != 1) {
				str += "s";
			}
		}

		if (hoursLeft <= 0 && minutesLeft <= 0) {
			/* The task doesn't tick fast enough to update at second-level granularity */
			str += "1 minute";
		}

		return str;
	}

	/* Pattern to extract the domain name from the full URL */
	private static final Pattern pattern = Pattern.compile("https*://([^/]*)/");

	protected Component getSiteInfo(boolean withClickEvents) {
		mLock.readLock().lock();

		TextComponent.Builder builder = Component.text().append(Component.text("Sites:", NamedTextColor.GOLD));
		for (String site : VoteManager.getSiteTimers().keySet()) {
			/* If there are no click events, don't display the full site url, just the domain name */
			final String displaySite;
			if (withClickEvents) {
				displaySite = site;
			} else {
				Matcher matcher = pattern.matcher(site);
				if (matcher.find()) {
					displaySite = matcher.group(1);
				} else {
					displaySite = site;
				}
			}

			builder.append(Component.text("\n"));

			Long offCooldownTime = mOffCooldownTimes.get(site);
			if (offCooldownTime == null) {
				/* Off cooldown */
				builder.append(Component.text(displaySite, NamedTextColor.GREEN));
			} else {
				/* Still on cooldown */
				long currentTime = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
				long totalSecondsLeft = offCooldownTime - currentTime;

				builder.append(Component.text(displaySite + ": " + timeDeltaStr(totalSecondsLeft), NamedTextColor.RED));
			}

			if (withClickEvents) {
				builder.clickEvent(ClickEvent.openUrl(site));
			}
		}

		mLock.readLock().unlock();

		return builder.build();
	}

	protected void sendVoteInfoShort(ProxiedPlayer player) {
		mLock.readLock().lock();
		int numSites = VoteManager.getSiteTimers().size() - mOffCooldownTimes.size();
		mLock.readLock().unlock();

		Component builder = Component.text()
			.append(Component.text("You are eligible to ", NamedTextColor.GOLD))
			.hoverEvent(HoverEvent.showText(getSiteInfo(false)))
			.clickEvent(ClickEvent.runCommand("/vote"))
			.append(Component.text("/vote ", NamedTextColor.AQUA))
			.append(Component.text("on " + numSites + " sites!", NamedTextColor.GOLD))
			.build();

		player.sendMessage(BungeeComponentSerializer.get().serialize(builder));
	}

	protected void sendVoteInfoLong(ProxiedPlayer player) {
		RemoteDataAPI.getMulti(player.getUniqueId(), VOTES_TOTAL, VOTES_THIS_WEEK, VOTES_UNCLAIMED, RAFFLE_ENTRIES, RAFFLE_WINS_TOTAL, RAFFLE_WINS_UNCLAIMED).whenComplete((data, ex) -> {
			if (ex != null) {
				MMLog.warning("Exception getting vote data in sendVoteInfoLong: " + ex.getMessage());
				MessagingUtils.sendProxiedMessage(player, "Encountered an unexpected error getting vote information.", NamedTextColor.RED);
			}

			int unclaimedRaffleWins = Integer.parseInt(data.getOrDefault(RAFFLE_WINS_UNCLAIMED, "0"));

			TextComponent.Builder builder = Component.text()
				.append(Component.text("Vote for Monumenta\n", NamedTextColor.WHITE, TextDecoration.BOLD))
				.append(Component.text("Voting helps other people find Monumenta!\nClaim rewards at Pollmaster Tennenbaum in Sierhaven\n\n", NamedTextColor.GOLD))
				.append(Component.text("Voting status:\n", NamedTextColor.GOLD))
				.append(Component.text("Total votes: " + data.getOrDefault(VOTES_TOTAL, "0") + "\n", NamedTextColor.WHITE))
				.append(Component.text("Votes this week: " + data.getOrDefault(VOTES_THIS_WEEK, "0") + "\n", NamedTextColor.WHITE))
				.append(Component.text("Unclaimed rewards: " + data.getOrDefault(VOTES_UNCLAIMED, "0") + "\n", NamedTextColor.WHITE))
				.append(Component.text("Raffle Entries: " + data.getOrDefault(RAFFLE_ENTRIES, "0") + "\n", NamedTextColor.WHITE))
				.append(Component.text("Raffle wins: " + data.getOrDefault(RAFFLE_WINS_TOTAL, "0") + "\n", NamedTextColor.WHITE));
			if (unclaimedRaffleWins > 0) {
				builder.append(Component.text("Unclaimed raffle rewards: " + unclaimedRaffleWins + "\n", NamedTextColor.WHITE, TextDecoration.BOLD));
				builder.append(Component.text("To claim your raffle reward, run /claimraffle", NamedTextColor.WHITE));
			} else {
				builder.append(Component.text("Unclaimed raffle rewards: " + unclaimedRaffleWins + "\n", NamedTextColor.WHITE));
			}
			player.sendMessage(BungeeComponentSerializer.get().serialize(builder.build()));

			player.sendMessage(BungeeComponentSerializer.get().serialize(getSiteInfo(true)));
		});
	}

	/* Got vote - put vote on cooldown, increment scores, thank player for voting if they are online */
	protected void voteReceived(String matchingSite, long cooldownMinutes) {
		/* Fire and forget increments */
		RemoteDataAPI.increment(mUUID, VOTES_THIS_WEEK, 1).whenComplete((unused, ex) -> {
			if (ex != null) {
				MMLog.warning("Failed to increment " + VOTES_THIS_WEEK + " for player " + mUUID);
			}
		});
		RemoteDataAPI.increment(mUUID, VOTES_TOTAL, 1).whenComplete((unused, ex) -> {
			if (ex != null) {
				MMLog.warning("Failed to increment " + VOTES_TOTAL + " for player " + mUUID);
			}
		});
		RemoteDataAPI.increment(mUUID, VOTES_UNCLAIMED, 1).whenComplete((unused, ex) -> {
			if (ex != null) {
				MMLog.warning("Failed to increment " + VOTES_UNCLAIMED + " for player " + mUUID);
			}
		});
		RemoteDataAPI.increment(mUUID, RAFFLE_ENTRIES, 1).whenComplete((unused, ex) -> {
			if (ex != null) {
				MMLog.warning("Failed to increment " + RAFFLE_ENTRIES + " for player " + mUUID);
			}
		});

		long currentTime = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
		mLock.writeLock().lock();
		mOffCooldownTimes.put(matchingSite, currentTime + (cooldownMinutes * 60));
		mLock.writeLock().unlock();

		saveOffCooldownTimes();
	}

	/* A bungee shard got a vote and broadcasted this notification - need to notify the player */
	protected void voteNotify(String matchingSite, long cooldownMinutes) {
		long currentTime = LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC);
		mLock.writeLock().lock();
		mOffCooldownTimes.put(matchingSite, currentTime + (cooldownMinutes * 60));
		mLock.writeLock().unlock();

		ProxiedPlayer player = mPlugin.getProxy().getPlayer(mUUID);
		if (player != null) {
			MessagingUtils.sendProxiedMessage(player, "Thanks for voting at " + matchingSite + "!", NamedTextColor.GOLD);
			sendVoteInfoShort(player);
		}

		/* Don't save here - the bungee node that got this request will have already saved the updated times */
	}

	/* Check if any are off cooldown, and prune the ones that are from the cooldown list
	 * Returns true if any voting site cooldown expired, false otherwise */
	protected boolean tick(long currentTime) {
		/* Read section - check if anything came off cooldown */
		mLock.readLock().lock();

		boolean anyOffCooldown = false;
		for (Long time : mOffCooldownTimes.values()) {
			if (currentTime > time) {
				anyOffCooldown = true;
				break;
			}
		}

		mLock.readLock().unlock();

		if (anyOffCooldown) {
			/* At least one timer expired - iterate again and clean up any expired entries */
			mLock.writeLock().lock();

			mOffCooldownTimes.entrySet().removeIf((entry) -> currentTime > entry.getValue());

			mLock.writeLock().unlock();

			saveOffCooldownTimes();
		}

		return anyOffCooldown;
	}
}
