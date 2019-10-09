package com.playmonumenta.plugins;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.commands.Bot;
import com.playmonumenta.plugins.commands.BroadcastCommand;
import com.playmonumenta.plugins.commands.CalculateReforge;
import com.playmonumenta.plugins.commands.DeCluckifyHeldItem;
import com.playmonumenta.plugins.commands.DeathMsg;
import com.playmonumenta.plugins.commands.DebugInfo;
import com.playmonumenta.plugins.commands.Effect;
import com.playmonumenta.plugins.commands.FestiveHeldItem;
import com.playmonumenta.plugins.commands.GildifyHeldItem;
import com.playmonumenta.plugins.commands.GiveSoulbound;
import com.playmonumenta.plugins.commands.HopeifyHeldItem;
import com.playmonumenta.plugins.commands.MonumentaDebug;
import com.playmonumenta.plugins.commands.MonumentaReload;
import com.playmonumenta.plugins.commands.ReforgeHeldItem;
import com.playmonumenta.plugins.commands.ReforgeInventory;
import com.playmonumenta.plugins.commands.RefreshClass;
import com.playmonumenta.plugins.commands.RemoveTags;
import com.playmonumenta.plugins.commands.Spectate;
import com.playmonumenta.plugins.commands.SpectateBot;
import com.playmonumenta.plugins.commands.TestNoScore;
import com.playmonumenta.plugins.commands.TransferScores;
import com.playmonumenta.plugins.commands.TransferServer;
import com.playmonumenta.plugins.commands.UpdateApartments;
import com.playmonumenta.plugins.enchantments.EnchantmentManager;
import com.playmonumenta.plugins.integrations.PlaceholderAPIIntegration;
import com.playmonumenta.plugins.integrations.VotifierIntegration;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.listeners.EntityListener;
import com.playmonumenta.plugins.listeners.ExceptionListener;
import com.playmonumenta.plugins.listeners.MobListener;
import com.playmonumenta.plugins.listeners.PlayerListener;
import com.playmonumenta.plugins.listeners.ShulkerEquipmentListener;
import com.playmonumenta.plugins.listeners.VehicleListener;
import com.playmonumenta.plugins.listeners.WorldListener;
import com.playmonumenta.plugins.network.HttpManager;
import com.playmonumenta.plugins.network.SocketManager;
import com.playmonumenta.plugins.overrides.ItemOverrides;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.safezone.SafeZoneManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.server.reset.DailyReset;
import com.playmonumenta.plugins.spawnzone.SpawnZoneManager;
import com.playmonumenta.plugins.timers.CombatLoggingTimers;
import com.playmonumenta.plugins.timers.CooldownTimers;
import com.playmonumenta.plugins.timers.ProjectileEffectTimers;
import com.playmonumenta.plugins.tracking.TrackingManager;
import com.playmonumenta.plugins.utils.MetadataUtils;

public class Plugin extends JavaPlugin {
	public CooldownTimers mTimers = null;
	public ProjectileEffectTimers mProjectileEffectTimers = null;
	public CombatLoggingTimers mCombatLoggingTimers = null;
	public Random mRandom = null;
	int mPeriodicTimer = -1;

	public ServerProperties mServerProperties = new ServerProperties();
	public EnchantmentManager mEnchantmentManager = new EnchantmentManager();
	public HttpManager mHttpManager;

	public TrackingManager mTrackingManager;
	public PotionManager mPotionManager;
	public SpawnZoneManager mZoneManager;
	public AbilityManager mAbilityManager;
	public SafeZoneManager mSafeZoneManager;

	public SocketManager mSocketManager;

	public ItemOverrides mItemOverrides;

	public World mWorld;

	private static Plugin STATIC_PLUGIN_REF = null;

	public static Plugin getInstance() {
		return STATIC_PLUGIN_REF;
	}

	@Override
	public void onLoad() {
		/*
		 * CommandAPI commands which register directly and are usable in functions
		 *
		 * These need to register immediately on load to prevent function loading errors
		 */

		TransferServer.register(this);
		GiveSoulbound.register();
		HopeifyHeldItem.register();
		FestiveHeldItem.register();
		GildifyHeldItem.register();
		DeCluckifyHeldItem.register();
		CalculateReforge.register();
		ReforgeHeldItem.register();
		ReforgeInventory.register();
		DebugInfo.register(this);
		RefreshClass.register(this);
		Effect.register(this);
		RemoveTags.register();
		DeathMsg.register();
		UpdateApartments.register();
		TransferScores.register(this);
		MonumentaReload.register(this);
		MonumentaDebug.register(this);

		mHttpManager = new HttpManager(this);
		try {
			mHttpManager.start();
		} catch (IOException err) {
			getLogger().warning("HTTP manager failed to start");
			err.printStackTrace();
		}

		mSafeZoneManager = new SafeZoneManager(this);
		mServerProperties.load(this, null);
		mEnchantmentManager.load(mServerProperties.mForbiddenItemLore);

		Bot.register(this);
		if (mServerProperties.getBroadcastCommandEnabled()) {
			BroadcastCommand.register(this);
		}
	}

	//  Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		STATIC_PLUGIN_REF = this;
		PluginManager manager = getServer().getPluginManager();

		mSocketManager = new SocketManager(this, mServerProperties.getSocketHost(),
		                                   mServerProperties.getSocketPort(),
										   mServerProperties.getShardName());
		mSocketManager.open();

		mItemOverrides = new ItemOverrides();

		//  Initialize Variables.
		mRandom = new Random();
		mTimers = new CooldownTimers(this);
		mCombatLoggingTimers = new CombatLoggingTimers();

		mWorld = Bukkit.getWorlds().get(0);
		mProjectileEffectTimers = new ProjectileEffectTimers(mWorld);

		mPotionManager = new PotionManager();
		mTrackingManager = new TrackingManager(this, mWorld);
		mZoneManager = new SpawnZoneManager(this);
		mAbilityManager = new AbilityManager(this, mWorld, mRandom);

		DailyReset.startTimer(this);

		//  Load info.
		reloadMonumentaConfig(null);

		// These are both a command and an event listener
		manager.registerEvents(new Spectate(this), this);
		manager.registerEvents(new SpectateBot(this), this);

		manager.registerEvents(new ExceptionListener(this), this);
		manager.registerEvents(new PlayerListener(this, mWorld, mRandom), this);
		manager.registerEvents(new MobListener(this), this);
		manager.registerEvents(new EntityListener(this, mWorld, mAbilityManager), this);
		manager.registerEvents(new VehicleListener(this), this);
		manager.registerEvents(new WorldListener(this, mWorld), this);
		manager.registerEvents(new ShulkerEquipmentListener(), this);

		// The last remaining Spigot-style command...
		this.getCommand("testNoScore").setExecutor(new TestNoScore());

		//  Move the logic out of Plugin and into it's own class that derives off Runnable, a Timer class of some type.
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			int ticks = 0;

			@Override
			public void run() {
				final boolean oneHertz = (ticks % 20) == 0;
				final boolean twoHertz = (ticks % 10) == 0;
				final boolean fourHertz = (ticks % 5) == 0;
				final boolean twentyHertz = true;

				if (oneHertz) {
					mHttpManager.tick();
				}

				// NOW IT'S TWICE A SECOND MOTHAFUCKAAAASSSSSSSSS!!!!!!!!!!
				// FREQUENCY ANARCHY HAPPENING UP IN HERE

				if (twoHertz) {
					//  Update cooldowns.
					try {
						mTimers.UpdateCooldowns(Constants.HALF_TICKS_PER_SECOND);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (fourHertz) {
					for (Player player : mTrackingManager.mPlayers.getPlayers()) {
						try {
							mAbilityManager.PeriodicTrigger(player, fourHertz, twoHertz, oneHertz, ticks);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				//  4 times a second.
				if (fourHertz) {
					try {
						mTrackingManager.update(mWorld, Constants.QUARTER_TICKS_PER_SECOND);
					} catch (Exception e) {
						e.printStackTrace();
					}

					try {
						mCombatLoggingTimers.update(mWorld, Constants.QUARTER_TICKS_PER_SECOND);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				//  Every tick.
				if (twentyHertz) {
					//  Update cooldowns.
					try {
						mProjectileEffectTimers.update();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				ticks = (ticks + 1) % Constants.TICKS_PER_SECOND;
			}
		}, 0L, 1L);

		// Provide placeholder API replacements if it is present
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new PlaceholderAPIIntegration(this).register();
		}

		// Get voting events if Votifier is present
		if (Bukkit.getPluginManager().isPluginEnabled("Votifier")) {
			manager.registerEvents(new VotifierIntegration(this), this);
		}

		// Register luckperms commands if LuckPerms is present
		if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
			new LuckPermsIntegration(this);
		}
	}

	//  Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		STATIC_PLUGIN_REF = null;
		getServer().getScheduler().cancelTasks(this);

		mTrackingManager.unloadTrackedEntities();
		mHttpManager.stop();
		mSocketManager.close();
		MetadataUtils.removeAllMetadata(this);
	}

	public Player getPlayer(UUID playerID) {
		return getServer().getPlayer(playerID);
	}

	/* Sender will be sent debugging info if non-null */
	public void reloadMonumentaConfig(CommandSender sender) {
		mServerProperties.load(this, sender);
		mEnchantmentManager.load(mServerProperties.mForbiddenItemLore);
	}
}
