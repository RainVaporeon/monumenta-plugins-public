package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellFinalLaser extends Spell {
	private static final double BOX_SIZE = 0.4;
	private static final double CHECK_INCREMENT = 0.75;
	private static final int SOLO_COOLDOWN = 20 * 15;
	private static final double MAX_FACTOR = 1.35;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final double mRange;
	private final PartialParticle mMob;
	private final PartialParticle mSmoke;
	private final PartialParticle mExpL;
	private final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.AIR,
		Material.CAVE_AIR,
		Material.VOID_AIR,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER,
		Material.WATER,
		Material.LAVA,
		Material.END_PORTAL
	);

	private double mT = 20 * 4;
	private double mCooldown;
	private boolean mTrigger = false;
	private List<Player> mPlayers = new ArrayList<>();

	public SpellFinalLaser(Plugin plugin, LivingEntity boss, Location loc, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = range;
		mMob = new PartialParticle(Particle.SPELL_MOB, mBoss.getLocation(), 1, 0.02, 0.02, 0.02, 1);
		mSmoke = new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 2, 0.02, 0.02, 0.02, 0);
		mExpL = new PartialParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 30, 0, 0, 0, 0.3);
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
		double cooldownFactor = Math.min(MAX_FACTOR, (Math.sqrt(mPlayers.size()) / 5 + 0.8) / 4 * 3);
		mCooldown = SOLO_COOLDOWN / cooldownFactor;
		mT -= 5;
		if (mT <= 0) {
			mT += mCooldown;
			laser();
		}
	}

	private void laser() {
		List<Player> potentialTargets = Lich.playersInRange(mBoss.getLocation(), mRange, true);
		List<Player> toRemove = new ArrayList<>();
		for (Player target : potentialTargets) {
			if (target.getLocation().getY() > mCenter.getY() + 3) {
				launch(target);
				toRemove.add(target);
			}
		}
		//remove all targeted players above 5 blocks of the ground, and target 1/3 of the remaining players
		potentialTargets.removeAll(toRemove);
		Collections.shuffle(potentialTargets);
		for (int i = 0; i < potentialTargets.size() / 3; i++) {
			launch(potentialTargets.get(i));
		}
	}

	private void launch(Player target) {
		BukkitRunnable runA = new BukkitRunnable() {
			private int mTicks = 0;

			@Override
			public void run() {
				Location startLocation = mBoss.getLocation().add(0, 5, 0);
				Location targetedLocation = target.getLocation().add(0, target.getEyeHeight() / 2, 0);

				World world = mBoss.getWorld();
				BoundingBox movingLaserBox = BoundingBox.of(startLocation, BOX_SIZE, BOX_SIZE, BOX_SIZE);
				Vector vector = new Vector(
					targetedLocation.getX() - startLocation.getX(),
					targetedLocation.getY() - startLocation.getY(),
					targetedLocation.getZ() - startLocation.getZ()
				);

				LocationUtils.travelTillObstructed(
					world,
					movingLaserBox,
					startLocation.distance(targetedLocation),
					vector,
					CHECK_INCREMENT,
					false,
					(Location loc) -> {
						mSmoke.location(loc).spawnAsBoss();
						mMob.location(loc).spawnAsBoss();
					},
					1,
					6
				);

				if (mTicks % 8 == 0) {
					target.playSound(target.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (mTicks / 100f) * 1.5f);
				} else if (mTicks % 8 == 2) {
					world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (mTicks / 100f) * 1.5f);
				} else if (mTicks % 8 == 4) {
					target.playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 2, 0.5f + (mTicks / 100f) * 1.5f);
				} else if (mTicks % 8 == 6) {
					world.playSound(mBoss.getLocation(), Sound.UI_TOAST_IN, SoundCategory.HOSTILE, 2, 0.5f + (mTicks / 100f) * 1.5f);
				}

				if (mTicks >= 100) {
					world.playSound(movingLaserBox.getCenter().toLocation(world), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.HOSTILE, 1f, 1.5f);
					mExpL.location(movingLaserBox.getCenter().toLocation(world)).spawnAsBoss();
					breakBlocks(movingLaserBox.getCenter().toLocation(world));
					if (movingLaserBox.overlaps(target.getBoundingBox())) {
						DamageUtils.damage(mBoss, target, DamageType.MAGIC, 50, null, false, true, "Death Laser");
						MovementUtils.knockAway(mCenter, target, 3.2f, false);
						Lich.cursePlayer(target);
					}
					this.cancel();
				}
				if (Lich.bossDead()) {
					this.cancel();
				}
				mTicks += 2;
			}
		};
		runA.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runA);
	}

	private void breakBlocks(Location l) {
		List<Block> badBlockList = new ArrayList<>();
		Location testloc = l.clone();
		for (int x = -1; x <= 1; x++) {
			testloc.setX(l.getX() + x);
			for (int z = -1; z <= 1; z++) {
				testloc.setZ(l.getZ() + z);
				for (int y = -1; y <= 1; y++) {
					testloc.setY(l.getY() + y + 0.2);

					Block block = testloc.getBlock();
					if (!mIgnoredMats.contains(block.getType())) {
						badBlockList.add(block);
					}
				}
			}
		}

		/* If there are any blocks, destroy all blocking blocks */
		if (badBlockList.size() > 0) {

			/* Call an event with these exploding blocks to give plugins a chance to modify it */
			EntityExplodeEvent event = new EntityExplodeEvent(mBoss, l, badBlockList, 0f);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}

			/* Remove any remaining blocks, which might have been modified by the event */
			for (Block block : badBlockList) {
				if (block.getState() instanceof Container) {
					block.breakNaturally();
				} else {
					block.setType(Material.AIR);
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
