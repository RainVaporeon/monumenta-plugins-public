package com.playmonumenta.plugins.bosses.spells;

import java.util.function.Predicate;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class SpellConditionalTeleport extends Spell {
	private Entity mLauncher;
	private Location mDest;
	private Predicate<Entity> mPredicate;

	public SpellConditionalTeleport(Entity launcher, Location dest, Predicate<Entity> predicate) {
		mLauncher = launcher;
		mDest = dest;
		mPredicate = predicate;
	}

	@Override
	public void run() {
		if (mPredicate.test(mLauncher)) {
			mLauncher.teleport(mDest);
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
