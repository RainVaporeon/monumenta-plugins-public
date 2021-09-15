package com.playmonumenta.plugins.bosses.spells.lich;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;

public class SpellCrystalRespawn extends Spell {

	private Plugin mPlugin;
	private int mT;
	private double mInc;
	private int mMinCooldown = (int) (20 * 7.5);
	private int mMaxCooldown = 20 * 30;
	private int mCooldown;
	private Location mCenter;
	private double mRange;
	private List<Location> mLoc;
	private String mCrystalNBT;
	private boolean mTrigger = false;
	private List<Player> mPlayers = new ArrayList<Player>();

	public SpellCrystalRespawn(Plugin plugin, Location loc, double range, List<Location> crystalLoc, String crystalnbt) {
		mPlugin = plugin;
		mCenter = loc;
		mRange = range;
		mLoc = crystalLoc;
		mCrystalNBT = crystalnbt;
	}

	@Override
	public void run() {
		//update player count every 5 seconds
		if (!mTrigger) {
			mPlayers = Lich.playersInRange(mCenter, mRange, true);
			mTrigger = true;
			new BukkitRunnable() {

				@Override
				public void run() {
					mTrigger = false;
				}

			}.runTaskLater(mPlugin, 20 * 5);
		}

		//cooldown
		double factor = Math.sqrt(mPlayers.size());
		mCooldown = (int) Math.max(mMinCooldown, Math.round(mMaxCooldown / factor));
		mT -= 5;
		if (mT <= 0 && !SpellDiesIrae.getActive()) {
			mT = mCooldown;
			mInc = mPlayers.size() / 5.0;
			Lich.spawnCrystal(mLoc, mInc, mCrystalNBT);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}