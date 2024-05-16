package com.playmonumenta.plugins.potion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.ToIntFunction;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class PotionMap {
	// PotionID is the type (safezone, item, etc.)
	// Each PotionID has an iterable TreeMap with one entry per effect level
	//
	// This implementation allows only one status effect of each (type, level)
	// So you can have a level 0 regen from a safezone and a level 0 regen from an item,
	//   but not two level 0 regens from items
	private final EnumMap<PotionID, TreeMap<Integer, PotionInfo>> mPotionMap;

	// Type of this particular map
	private final PotionEffectType mType;

	private final boolean mIsNegative;

	protected PotionMap(PotionEffectType type) {
		mPotionMap = new EnumMap<>(PotionID.class);
		mType = type;
		mIsNegative = PotionUtils.hasNegativeEffects(type);
	}

	private void addPotion(PotionID id, PotionInfo newPotionInfo) {
		Integer amplifier = newPotionInfo.mAmplifier;

		TreeMap<Integer, PotionInfo> trackedPotionInfo = mPotionMap.computeIfAbsent(id, key -> new TreeMap<>());

		if (mIsNegative) {
			// Negative potions don't track multiple levels - only the highest / longest one, from any source
			PotionInfo bestEffect = getBestEffect();

			// If the current "best" negative effect is less than this new one, track it
			// Make sure the last effect has had a chance to trigger before refreshing it
			PotionEffectType newPotionType = newPotionInfo.mType;
			if (bestEffect == null
					|| bestEffect.mAmplifier < newPotionInfo.mAmplifier
					|| (bestEffect.mAmplifier == newPotionInfo.mAmplifier
					&& bestEffect.mDuration < newPotionInfo.mDuration
					&& (!PotionEffectType.POISON.equals(newPotionType)
					|| newPotionInfo.mDuration - bestEffect.mDuration >= 25 / (bestEffect.mAmplifier + 1) + 1)
					&& (!PotionEffectType.WITHER.equals(newPotionType)
					|| newPotionInfo.mDuration - bestEffect.mDuration >= 40 / (bestEffect.mAmplifier + 1) + 1))) {

				// remove any other (lower level/duration) effects
				if (newPotionType != null) {
					for (TreeMap<Integer, PotionInfo> infoMap : mPotionMap.values()) {
						infoMap.values().removeIf(info -> newPotionType.equals(info.mType));
					}
				}

				trackedPotionInfo.put(amplifier, newPotionInfo);
			}
		} else {
			// Only add the new effect if it is longer for the same effect amplifier
			PotionInfo currentInfo = trackedPotionInfo.get(amplifier);
			if (currentInfo == null
					|| ((currentInfo.mDuration < newPotionInfo.mDuration || (!currentInfo.mInfinite && newPotionInfo.mInfinite))
					&& (!PotionEffectType.REGENERATION.equals(currentInfo.mType)
					|| newPotionInfo.mDuration - currentInfo.mDuration >= 50 / (currentInfo.mAmplifier + 1) + 1))) {
				trackedPotionInfo.put(amplifier, newPotionInfo);
			}
		}
	}

	protected void addPotion(Player player, PotionID id, PotionInfo newPotionInfo) {
		addPotion(id, newPotionInfo);

		applyBestPotionEffect(player);
	}

	/**
	 * Clears out all effects from this source
	 */
	protected void clearPotion(Player player, PotionID id) {
		mPotionMap.remove(id);

		applyBestPotionEffect(player);
	}

	public void modifyPotionDuration(Player player, ToIntFunction<PotionInfo> function) {
		boolean changed = false;
		for (TreeMap<Integer, PotionInfo> treeMap : mPotionMap.values()) {
			for (PotionInfo potionInfo : treeMap.values()) {
				int newDuration = function.applyAsInt(potionInfo);
				if (newDuration != potionInfo.mDuration) {
					changed = true;
					potionInfo.mDuration = newDuration;
				}
			}
		}
		if (changed) {
			applyBestPotionEffect(player);
		}
	}

	protected void removePotion(Player player, PotionID id, int amplifier) {
		TreeMap<Integer, PotionInfo> map = mPotionMap.get(id);
		if (map != null) {
			map.remove(amplifier);
			if (map.isEmpty()) {
				mPotionMap.remove(id);
			}
		}

		applyBestPotionEffect(player);
	}

	protected Collection<PotionInfo> getPotionInfos(PotionID id) {
		TreeMap<Integer, PotionInfo> tree = mPotionMap.get(id);
		if (tree != null) {
			return tree.values();
		}
		return new ArrayList<>();
	}

	protected void updatePotionStatus(Player player, int ticks) {
		//  First update the timers of all our tracked potion timers.
		boolean effectWoreOff = false;

		Iterator<Entry<PotionID, TreeMap<Integer, PotionInfo>>> potionIter = mPotionMap.entrySet().iterator();
		while (potionIter.hasNext()) {
			Entry<PotionID, TreeMap<Integer, PotionInfo>> potionMapping = potionIter.next();
			if (potionMapping != null) {
				TreeMap<Integer, PotionInfo> potionInfo = potionMapping.getValue();
				Iterator<Entry<Integer, PotionInfo>> potionInfoIter = potionInfo.entrySet().iterator();
				while (potionInfoIter.hasNext()) {
					PotionInfo info = potionInfoIter.next().getValue();

					if (info.mDuration > 0) {
						info.mDuration -= ticks;
					}
					if (info.mDuration <= 0 && !info.mInfinite) {
						effectWoreOff = true;
						potionInfoIter.remove();
					}
				}

				if (potionInfo.size() == 0) {
					potionIter.remove();
				}
			}
		}

		//  If a timer wears out, run another check to make sure the best potion effect is applied.
		if (effectWoreOff) {
			applyBestPotionEffect(player);
		}
	}

	private @Nullable PotionInfo getBestEffect() {
		PotionInfo bestEffect = null;

		for (TreeMap<Integer, PotionInfo> potionInfos : mPotionMap.values()) {
			Entry<Integer, PotionInfo> lastEntry = potionInfos.lastEntry();
			if (lastEntry != null) {
				PotionInfo info = lastEntry.getValue();
				if (bestEffect == null) {
					bestEffect = info;
				} else if (info.mAmplifier > bestEffect.mAmplifier) {
					bestEffect = info;
				} else if (info.mAmplifier == bestEffect.mAmplifier &&
						info.mDuration > bestEffect.mDuration) {
					bestEffect = info;
				}
			}
		}

		return bestEffect;
	}

	void applyBestPotionEffect(Player player) {
		PotionInfo bestEffect = getBestEffect();

		PotionEffect currentVanillaEffect = player.getPotionEffect(mType);
		if (currentVanillaEffect != null) {
			if (bestEffect == null
					|| currentVanillaEffect.getDuration() > (bestEffect.mDuration + 20)
					|| currentVanillaEffect.getDuration() < (bestEffect.mDuration - 20)
					|| bestEffect.mAmplifier != currentVanillaEffect.getAmplifier()) {

				// The current effect must be removed because the "best" effect is either less than it
				// OR the same strength but less duration
				player.removePotionEffect(mType);
			}
		}

		if (bestEffect != null) {
			// Effects over 100 "mask" all other effects of that type
			if (bestEffect.mAmplifier < 100) {
				PotionEffect effect = new PotionEffect(mType, bestEffect.mDuration, bestEffect.mAmplifier, bestEffect.mAmbient, bestEffect.mShowParticles, bestEffect.mShowIcon);
				player.addPotionEffect(effect);
			}
		}
	}

	protected @Nullable JsonObject getAsJsonObject(boolean includeAll) {
		JsonObject potionMapObject = null;

		for (Entry<PotionID, TreeMap<Integer, PotionInfo>> potionMapping : mPotionMap.entrySet()) {
			if (!includeAll && (potionMapping.getKey().equals(PotionID.ITEM)
					|| potionMapping.getKey().equals(PotionID.ABILITY_SELF)
					|| potionMapping.getKey().equals(PotionID.SAFE_ZONE))) {
				/*
				 * Don't save ITEM, ABILITY_SELF, or SAFE_ZONE potions
				 * These will be re-applied elsewhere when the player rejoins
				 */
				continue;
			}

			JsonArray effectListArray = new JsonArray();

			for (Entry<Integer, PotionInfo> entry : potionMapping.getValue().entrySet()) {
				effectListArray.add(entry.getValue().getAsJsonObject());
			}

			if (effectListArray.size() > 0) {
				if (potionMapObject == null) {
					potionMapObject = new JsonObject();
				}
				potionMapObject.add(potionMapping.getKey().getName(), effectListArray);
			}
		}

		return potionMapObject;
	}

	protected void loadFromJsonObject(JsonObject object) throws Exception {
		for (Entry<String, JsonElement> entry : object.entrySet()) {
			PotionID id = PotionID.getFromString(entry.getKey());
			if (id != null) {
				for (JsonElement element : entry.getValue().getAsJsonArray()) {
					PotionInfo info = new PotionInfo(element.getAsJsonObject());

					addPotion(id, info);
				}
			}
		}
	}
}
