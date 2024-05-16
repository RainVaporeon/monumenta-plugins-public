package com.playmonumenta.plugins.potion;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class PotionManager {
	//  Player ID / Player Potion Info
	private final HashMap<UUID, PlayerPotionInfo> mPlayerPotions = new HashMap<UUID, PlayerPotionInfo>();

	public enum PotionID {
		APPLIED_POTION(0, "APPLIED_POTION"),
		ABILITY_SELF(1, "ABILITY_SELF"),
		ABILITY_OTHER(2, "ABILITY_OTHER"),
		SAFE_ZONE(3, "SAFE_ZONE"),
		ITEM(4, "ITEM");

		private final int mValue;
		private final String mName;
		PotionID(int value, String name) {
			this.mValue = value;
			this.mName = name;
		}

		public int getValue() {
			return mValue;
		}

		public String getName() {
			return mName;
		}

		public static @Nullable PotionID getFromString(String name) {
			if (name.equals(PotionID.APPLIED_POTION.getName())) {
				return PotionID.APPLIED_POTION;
			} else if (name.equals(PotionID.ABILITY_SELF.getName())) {
				return PotionID.ABILITY_SELF;
			} else if (name.equals(PotionID.ABILITY_OTHER.getName())) {
				return PotionID.ABILITY_OTHER;
			} else if (name.equals(PotionID.SAFE_ZONE.getName())) {
				return PotionID.SAFE_ZONE;
			} else if (name.equals(PotionID.ITEM.getName())) {
				return PotionID.ITEM;
			} else {
				return null;
			}
		}
	}

	public void addPotion(Player player, PotionID id, Collection<PotionEffect> effects, double intensity) {
		for (PotionEffect effect : effects) {
			addPotion(player, id, effect, intensity);
		}
	}

	public void addPotion(Player player, PotionID id, Collection<PotionEffect> effects) {
		addPotion(player, id, effects, 1.0);
	}

	public void addPotion(Player player, PotionID id, PotionEffect effect, double intensity) {
		addPotion(player, id, new PotionInfo(effect.getType(), (int)(((double)effect.getDuration()) * intensity),
		                                     effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon()));
	}

	public void addPotion(Player player, PotionID id, PotionEffect effect) {
		addPotion(player, id, effect, 1.0);
	}

	public void addPotion(Player player, PotionID id, @Nullable PotionInfo info) {
		// Instant potions do not need to be tracked
		if (Constants.POTION_MANAGER_ENABLED
			&& info != null
			&& info.mType != null
			&& !info.mType.equals(PotionEffectType.HARM)
			&& !info.mType.equals(PotionEffectType.HEAL)) {

			mPlayerPotions.computeIfAbsent(player.getUniqueId(), key -> new PlayerPotionInfo())
				.addPotionInfo(player, id, info);
		}
	}

	public void addPotionInfos(Player player, PotionID id, Collection<PotionInfo> infos) {
		for (PotionInfo info : infos) {
			addPotion(player, id, info);
		}
	}

	public @Nullable PlayerPotionInfo getPlayerPotionInfo(Player player) {
		return mPlayerPotions.get(player.getUniqueId());
	}

	public void removePotion(Player player, PotionID id, PotionEffectType type) {
		PlayerPotionInfo potionInfo = mPlayerPotions.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.clearPotionInfo(player, id, type);
		}
	}

	public void removePotion(Player player, PotionID id, PotionEffectType type, int amplifier) {
		PlayerPotionInfo potionInfo = mPlayerPotions.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.removePotionInfo(player, id, type, amplifier);
		}
	}

	public void clearAllPotions(Player player) {
		mPlayerPotions.remove(player.getUniqueId());

		// Make a copy of the list to prevent ConcurrentModificationException's
		for (PotionEffect type : new ArrayList<>(player.getActivePotionEffects())) {
			player.removePotionEffect(type.getType());
		}
	}

	public void clearPotionIDType(Player player, PotionID id) {
		PlayerPotionInfo potionInfo = mPlayerPotions.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.clearPotionIDType(player, id);
		}
	}

	public void clearPotionEffectType(Player player, PotionEffectType type) {
		PlayerPotionInfo potionInfo = mPlayerPotions.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.clearPotionEffectType(type);
		}
		player.removePotionEffect(type);
	}

	public Map<PotionID, List<PotionInfo>> getAllPotionInfos(Player player) {
		Map<PotionID, List<PotionInfo>> infos = new HashMap<>();
		for (PotionID id : PotionID.values()) {
			infos.put(id, new ArrayList<>());
		}
		PlayerPotionInfo playerPotionInfo = getPlayerPotionInfo(player);
		if (playerPotionInfo != null) {
			for (PotionMap potionMap : playerPotionInfo.getAllPotionMaps()) {
				for (PotionID id : PotionID.values()) {
					infos.computeIfAbsent(id, key -> new ArrayList<>()).addAll(potionMap.getPotionInfos(id));
				}
			}
		}
		return infos;
	}

	public List<PotionInfo> getPotionInfoList(Player player) {
		Map<PotionID, List<PotionInfo>> map = getAllPotionInfos(player);
		List<PotionInfo> list = new ArrayList<>();
		map.values().forEach(list::addAll);
		return list;
	}

	public void updatePotionStatus(Player player, int ticks) {
		PlayerPotionInfo potionInfo = mPlayerPotions.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.updatePotionStatus(player, ticks);
		}
	}

	public void refreshEffects(Player player) {
		PlayerPotionInfo potionInfo = mPlayerPotions.get(player.getUniqueId());
		if (potionInfo != null) {
			potionInfo.refreshEffects(player);
		}
	}

	public void modifyPotionDuration(Player player, ToIntFunction<PotionInfo> function) {
		PlayerPotionInfo info = mPlayerPotions.get(player.getUniqueId());
		if (info != null) {
			info.modifyPotionDuration(player, function);
		}
	}

	public JsonObject getAsJsonObject(Player player, boolean includeAll) {
		final PlayerPotionInfo info;

		info = mPlayerPotions.get(player.getUniqueId());

		if (info != null) {
			return info.getAsJsonObject(includeAll);
		}

		return new JsonObject();
	}

	public void loadFromJsonObject(Player player, JsonObject object) throws Exception {
		PlayerPotionInfo info = new PlayerPotionInfo();
		info.loadFromJsonObject(object);
		mPlayerPotions.put(player.getUniqueId(), info);
	}
}
