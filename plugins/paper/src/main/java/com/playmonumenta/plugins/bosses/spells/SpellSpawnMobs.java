package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class SpellSpawnMobs extends Spell {
	public static final int MOB_DETECTION_RADIUS = 10;
	public static final int DEFAULT_MOB_CAP = 15;

	private final int mSummonRange;
	private final int mMinSummonRange;
	private final int mCooldownTicks;
	private final String mSummonName;
	private final int mSpawns;
	private final int mMobCap;

	private final LivingEntity mBoss;

	public SpellSpawnMobs(LivingEntity boss, int spawns, String losname, int cooldown, int range, int minrange, int mobcap) {
		mBoss = boss;
		mSummonRange = range;
		mMinSummonRange = minrange;
		mCooldownTicks = cooldown;
		mSummonName = losname;
		mSpawns = spawns;
		mMobCap = mobcap;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();

		for (int i = 0; i < mSpawns; i++) {
			double r = FastUtils.randomDoubleInRange(mMinSummonRange, mSummonRange);
			double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);

			double x = r * Math.cos(theta);
			double z = r * Math.sin(theta);

			Location sLoc = loc.clone().add(x, 0.25, z);
			Entity entity = LibraryOfSoulsIntegration.summon(sLoc, mSummonName);
			if (entity != null) {
				summonPlugins(entity);
			}
		}
	}

	//overwrite this function to modify summon mobs with extra tags/stats
	public void summonPlugins(@NotNull Entity summon) {

	}

	@Override
	public boolean canRun() {
		if (EntityUtils.getNearbyMobs(mBoss.getLocation(), MOB_DETECTION_RADIUS).size() > mMobCap) {
			return false;
		}
		return true;
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
