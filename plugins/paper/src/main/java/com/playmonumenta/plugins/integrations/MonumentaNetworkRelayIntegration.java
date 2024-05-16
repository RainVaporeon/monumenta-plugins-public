package com.playmonumenta.plugins.integrations;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class MonumentaNetworkRelayIntegration {
	public static final String AUDIT_LOG_CHANNEL = "Monumenta.Automation.AuditLog";
	public static final String AUDIT_LOG_SEVERE_CHANNEL = "Monumenta.Automation.AuditLogSevere";
	public static final String AUDIT_LOG_DEATH_CHANNEL = "Monumenta.Automation.DeathAuditLog";
	public static final String AUDIT_LOG_PLAYERS_CHANNEL = "Monumenta.Automation.PlayerAuditLog";
	public static final String AUDIT_LOG_MARKET_CHANNEL = "Monumenta.Automation.MarketAuditLog";
	public static final String ADMIN_ALERT_CHANNEL = "Monumenta.Automation.AdminNotification";

	private final Logger mLogger;
	private static @Nullable MonumentaNetworkRelayIntegration INSTANCE = null;

	public MonumentaNetworkRelayIntegration(Logger logger) {
		logger.info("Enabling MonumentaNetworkRelay integration");
		mLogger = logger;
		INSTANCE = this;
	}

	private static void sendAuditLogMessage(String message, String channel) {
		if (INSTANCE != null) {
			JsonObject data = new JsonObject();
			data.addProperty("message", message);
			try {
				NetworkRelayAPI.sendMessage("automation-bot", channel, data);
			} catch (Exception ex) {
				INSTANCE.mLogger.severe("Failed to send audit log message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	public static void sendDeathAuditLogMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_DEATH_CHANNEL);
	}

	public static void sendMarketAuditLogMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_MARKET_CHANNEL);
	}

	public static void sendPlayerAuditLogMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_PLAYERS_CHANNEL);
	}

	public static void sendAuditLogSevereMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_SEVERE_CHANNEL);
	}

	public static void sendModAuditLogMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_CHANNEL);
	}

	public static void broadcastCommand(String command) {
		if (INSTANCE != null) {
			try {
				NetworkRelayAPI.sendBroadcastCommand(command);
			} catch (Exception ex) {
				INSTANCE.mLogger.severe("Failed to send broadcast message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	public static void sendAdminMessage(String message) {
		if (INSTANCE != null) {
			JsonObject data = new JsonObject();
			data.addProperty("message", message);
			try {
				NetworkRelayAPI.sendMessage("automation-bot", ADMIN_ALERT_CHANNEL, data);
			} catch (Exception ex) {
				INSTANCE.mLogger.severe("Failed to send admin alert message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
}
