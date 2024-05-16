package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellBaseLeapAttack extends Spell {

	@FunctionalInterface
	public interface AestheticAction {
		/**
		 * Location to do aesthetics
		 */
		void run(World world, Location loc);
	}

	@FunctionalInterface
	public interface HitAction {
		/**
		 * Called when the boss intersects a player or lands
		 *
		 * @param player Player being targeted (null if landed)
		 * @param loc    Location where the projectile hit (either at player or location landed)
		 */
		void run(World world, @Nullable Player player, Location loc, Vector dir);
	}

	@FunctionalInterface
	public interface JumpVelocityModifier {
		/**
		 * Called just before the boss's velocity is set to leap them towards the player
		 *
		 * @param velocity  Initial velocity
		 * @param bossLoc   Boss's starting leap location
		 * @param targetLoc Target leap location
		 * @return Modified velocity
		 */
		Vector run(Vector velocity, Location bossLoc, Location targetLoc);
	}

	@FunctionalInterface
	public interface MidLeapTickAction {
		/**
		 * Called on the boss every tick while leaping.
		 * <p>
		 * Can use this to adjust the boss's velocity to nudge towards the player, for example, or other effects.
		 *
		 * @param boss         The boss
		 * @param targetPlayer The player the boss leaped towards
		 */
		void run(LivingEntity boss, Player targetPlayer);
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final int mRange;
	private final int mMinRange;
	private final int mRunDistance;
	private final int mCooldown;
	private final double mVelocityMultiplier;
	private final AestheticAction mInitiateAesthetic;
	private final AestheticAction mLeapAesthetic;
	private final AestheticAction mLeapingAesthetic;
	private final HitAction mHitAction;
	private final @Nullable JumpVelocityModifier mVelocityModifier;
	private final @Nullable MidLeapTickAction mMidLeapTick;
	private final boolean mPreferTarget;
	private final boolean mIgnoreWalls;

	public SpellBaseLeapAttack(Plugin plugin, LivingEntity boss, int range, int minRange, int runDistance, int cooldown,
							   double velocityMultiplier, AestheticAction initiateAesthetic, AestheticAction leapAesthetic,
							   AestheticAction leapingAesthetic, HitAction hitAction, @Nullable JumpVelocityModifier velocityModifier,
							   @Nullable MidLeapTickAction midLeapTick) {
		this(plugin, boss, range, minRange, runDistance, cooldown, velocityMultiplier, initiateAesthetic, leapAesthetic,
			leapingAesthetic, hitAction, velocityModifier, midLeapTick, true, false);
	}

	/**
	 * @param plugin             Plugin
	 * @param boss               Boss
	 * @param range              Range within which players may be targeted
	 * @param minRange           Minimum range for the attack to initiate
	 * @param runDistance        How far the mob runs before leaping
	 * @param cooldown           How often this spell can be cast
	 * @param velocityMultiplier Adjusts distance of the leap (multiplier of 1 usually lands around the target at a distance of 8+ blocks away)
	 * @param initiateAesthetic  Called when the attack initiates
	 * @param leapAesthetic      Called when the boss leaps
	 * @param leapingAesthetic   Called each tick at boss location during leap
	 * @param hitAction          Called when the boss intersects a player or lands
	 * @param velocityModifier   Called just before the boss's velocity is set to leap them towards the player
	 * @param midLeapTick        Called whilet he boss is in mid air heading towards a target player
	 * @param preferTarget       Leap will target the mob's target instead of a random player
	 */
	public SpellBaseLeapAttack(Plugin plugin, LivingEntity boss, int range, int minRange, int runDistance, int cooldown,
	                           double velocityMultiplier, AestheticAction initiateAesthetic, AestheticAction leapAesthetic,
	                           AestheticAction leapingAesthetic, HitAction hitAction, @Nullable JumpVelocityModifier velocityModifier,
	                           @Nullable MidLeapTickAction midLeapTick, boolean preferTarget, boolean ignoreWalls) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mRange = range;
		mMinRange = minRange;
		mRunDistance = runDistance;
		mCooldown = cooldown;
		mVelocityMultiplier = velocityMultiplier;
		mInitiateAesthetic = initiateAesthetic;
		mLeapAesthetic = leapAesthetic;
		mLeapingAesthetic = leapingAesthetic;
		mHitAction = hitAction;
		mVelocityModifier = velocityModifier;
		mMidLeapTick = midLeapTick;
		mPreferTarget = preferTarget;
		mIgnoreWalls = ignoreWalls;
	}

	@Override
	public void run() {
		if (!(mBoss instanceof Mob)) {
			return;
		}
		Player targetPlayer = null;
		Location loc = mBoss.getLocation();
		List<Player> players = PlayerUtils.playersInRange(loc, mRange, false);
		if (!players.isEmpty()) {
			Collections.shuffle(players);
			for (Player player : players) {
				Location locPlayer = player.getLocation();
				if ((LocationUtils.hasLineOfSight(mBoss, player) || mIgnoreWalls) && loc.distance(locPlayer) > mMinRange) {
					targetPlayer = player;
					break;
				}
			}
		}

		LivingEntity target = ((Mob) mBoss).getTarget();
		if (mPreferTarget && target instanceof Player) {
			targetPlayer = (Player) target;
		}

		if (targetPlayer == null || EntityUtils.isStunned(mBoss) || EntityUtils.isSilenced(mBoss)) {
			return;
		}

		launch(targetPlayer.getLocation(), loc, targetPlayer, !mIgnoreWalls);
	}

	public void launch(Location locTarget, Location loc, Player targetPlayer, boolean checkPassable) {
		if (!locTarget.getWorld().equals(loc.getWorld())) {
			return;
		}
		mInitiateAesthetic.run(mWorld, mBoss.getEyeLocation());

		Vector offset = locTarget.clone().subtract(loc).toVector().normalize().multiply(mRunDistance);
		Location moveTo = loc.clone().add(offset);
		int i;
		for (i = 0; i < 3; i++) {
			if (!moveTo.getBlock().isPassable()) {
				moveTo.add(0, 1, 0);
			} else {
				break;
			}
		}

		if (i == 3 && checkPassable) {
			// Failed to find a good path
			return;
		}

		((Mob) mBoss).getPathfinder().moveTo(moveTo);


		double distance = moveTo.distance(locTarget);
		Vector velocity = locTarget.subtract(moveTo).toVector().multiply(0.19 * mVelocityMultiplier);
		velocity.setY(velocity.getY() * 0.5 + distance * 0.08);
		if (mVelocityModifier != null) {
			velocity = mVelocityModifier.run(velocity, mBoss.getLocation(), locTarget);
		}

		final Vector finalVelocity = velocity;

		BukkitRunnable leap = new BukkitRunnable() {
			final Location mLeapLocation = moveTo;
			final Vector mDirection = finalVelocity.clone().setY(0).normalize();
			boolean mLeaping = false;
			boolean mHasBeenOneTick = false;
			int mTime = 0;

			@Override
			public void run() {
				if (!mLeaping) {
					if (mBoss.getLocation().distance(mLeapLocation) < 1) {
						mLeapAesthetic.run(mWorld, mBoss.getLocation());
						((Mob) mBoss).getPathfinder().stopPathfinding();
						mBoss.setVelocity(finalVelocity);
						mLeaping = true;
					} else {
						mTime++;

						// Still hasn't reached the leap point after half the cooldown elapsed, so cancel leap
						if (mTime > mCooldown / 2) {
							this.cancel();
							return;
						}
					}
				} else {
					mLeapingAesthetic.run(mWorld, mBoss.getLocation());
					mBoss.setFallDistance(0);
					if (mBoss.isOnGround() && mHasBeenOneTick) {
						mHitAction.run(mWorld, null, mBoss.getLocation(), mDirection);
						this.cancel();
						return;
					}

					BoundingBox hitbox = mBoss.getBoundingBox();
					for (Player player : mBoss.getWorld().getPlayers()) {
						if (player.getBoundingBox().overlaps(hitbox) && mHasBeenOneTick) {
							((Mob) mBoss).setTarget(player);
							mHitAction.run(mWorld, player, player.getLocation(), mDirection);
							this.cancel();
							return;
						}
					}

					// Give the caller a chance to run extra effects or manipulate the boss's leap velocity
					if (targetPlayer.isOnline() && targetPlayer.getWorld().equals(mBoss.getWorld()) && mMidLeapTick != null) {
						mMidLeapTick.run(mBoss, targetPlayer);
					}

					// At least one tick has passed to avoid insta smacking a nearby player
					mHasBeenOneTick = true;
				}
			}
		};

		leap.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(leap);
	}

	@Override
	public boolean canRun() {
		Location loc = mBoss.getLocation();
		List<Player> players = PlayerUtils.playersInRange(loc, mRange, false);
		if (!players.isEmpty()) {
			for (Player player : players) {
				if ((LocationUtils.hasLineOfSight(mBoss, player) || mIgnoreWalls) && loc.distance(player.getLocation()) > mMinRange) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

}
