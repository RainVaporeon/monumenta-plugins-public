package com.playmonumenta.plugins.abilities.rogue.assassin;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.rogue.Smokescreen;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class BodkinBlitz extends MultipleChargeAbility {

	private static final int COOLDOWN_1 = 20 * 20;
	private static final int COOLDOWN_2 = 20 * 18;
	private static final int BONUS_DMG_1 = 7;
	private static final int BONUS_DMG_2 = 14;
	private static final int STEALTH_DURATION_1 = 20;
	private static final int STEALTH_DURATION_2 = 30;
	private static final int DISTANCE_1 = 10;
	private static final int DISTANCE_2 = 14;
	private static final int TELEPORT_TICKS = 4;
	private static final int MAX_CHARGES = 2;

	private final int mStealthDuration;
	private final int mBonusDmg;

	private @Nullable BukkitRunnable mRunnable = null;
	private boolean mTeleporting = false;
	private int mTicks;

	private boolean mHasSmokescreen = false;

	public BodkinBlitz(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Bodkin Blitz");
		mInfo.mLinkedSpell = ClassAbility.BODKIN_BLITZ;
		mInfo.mScoreboardId = "BodkinBlitz";
		mInfo.mShorthandName = "BB";
		mInfo.mDescriptions.add("Sneak right click while holding two swords to teleport 10 blocks forwards. Gain 1 second of Stealth upon teleporting. Upon teleporting, your next melee attack deals 7 bonus damage if your target is not focused on you. This ability cannot be used in safe zones. Cooldown: 20s. Charges: 2.");
		mInfo.mDescriptions.add("Range increased to 14 blocks, Stealth increased to 1.5 seconds. Upon teleporting, your next melee attack deals 14 bonus damage if your target is not focused on you. Cooldown: 18s.");
		mInfo.mCooldown = getAbilityScore() == 1 ? COOLDOWN_1 : COOLDOWN_2;
		mInfo.mTrigger = AbilityTrigger.RIGHT_CLICK;
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.BLAZE_POWDER, 1);
		mMaxCharges = MAX_CHARGES;
		mCharges = getTrackedCharges();

		mStealthDuration = getAbilityScore() == 1 ? STEALTH_DURATION_1 : STEALTH_DURATION_2;
		mBonusDmg = getAbilityScore() == 1 ? BONUS_DMG_1 : BONUS_DMG_2;

		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			mHasSmokescreen = AbilityManager.getManager().getPlayerAbilityIgnoringSilence(player, Smokescreen.class) != null;
		});
	}

	@Override
	public void cast(Action action) {
		if (mPlayer == null || mTeleporting || !mPlayer.isSneaking() || ZoneUtils.hasZoneProperty(mPlayer, ZoneProperty.NO_MOBILITY_ABILITIES)
			|| !InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {
			return;
		}

		Location loc = mPlayer.getLocation();
		// Smokescreen trigger conflict
		if (mHasSmokescreen && loc.getPitch() > 50) {
			return;
		}

		if (!consumeCharge()) {
			return;
		}

		mTeleporting = true;

		World world = mPlayer.getWorld();
		world.playSound(loc, Sound.ENTITY_PLAYER_BREATH, 1f, 2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2f);

		new BukkitRunnable() {
			final BoundingBox mPlayerBox = mPlayer.getBoundingBox();
			final Vector mDirection = mPlayer.getLocation().getDirection().normalize();
			final double mDistancePerTick = 1.0 * (getAbilityScore() == 1 ? DISTANCE_1 : DISTANCE_2) / TELEPORT_TICKS;
			int mTick = 0;

			@Override
			public void run() {
				// The teleport is run over multiple ticks so that we can have a small projectile animation.
				BoundingBox travelBox = mPlayerBox.clone();
				boolean isBlocked = LocationUtils.travelTillObstructed(world, travelBox,
					mDistancePerTick, mDirection, 0.1, true,
					loc -> {
						world.spawnParticle(Particle.FALLING_DUST, loc, 5, 0.15, 0.45, 0.1,
							Bukkit.createBlockData("gray_concrete"));
						world.spawnParticle(Particle.CRIT, loc, 4, 0.25, 0.5, 0.25, 0);
						world.spawnParticle(Particle.SMOKE_NORMAL, loc, 5, 0.15, 0.45, 0.15, 0.01);
					}, 1, 1);
				Location tpLoc = travelBox.getCenter().toLocation(world).add(0, -travelBox.getHeight() / 2, 0);
				if (isBlocked) {
					// If no spot was found, then you've literally hit a wall. Stop iterating.
					mTick = TELEPORT_TICKS;
				} else {
					// Shift player box by travel distance for next tick's check
					// Does not use travelBox as that may have been shifted to evade obstacles
					mPlayerBox.shift(mDirection.clone().multiply(mDistancePerTick));
				}

				// Don't allow teleporting outside the world border
				if (!tpLoc.getWorld().getWorldBorder().isInside(tpLoc)) {
					this.cancel();
					return;
				}

				// Teleport player
				mTick++;
				if (mTick >= TELEPORT_TICKS) {
					tpLoc.setDirection(mPlayer.getLocation().getDirection());
					tpLoc.add(0, 0.1, 0);
					mPlayer.teleport(tpLoc, TeleportCause.UNKNOWN);

					mTeleporting = false;

					world.playSound(tpLoc, Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 2f);
					world.playSound(tpLoc, Sound.ITEM_TRIDENT_RETURN, 1f, 0.8f);
					world.playSound(tpLoc, Sound.ITEM_TRIDENT_THROW, 1f, 0.5f);
					world.playSound(tpLoc, Sound.ITEM_TRIDENT_HIT, 1f, 1f);
					world.playSound(tpLoc, Sound.ENTITY_PHANTOM_HURT, 1f, 0.75f);
					world.playSound(tpLoc, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);

					world.spawnParticle(Particle.SMOKE_LARGE, tpLoc.clone().add(0, 1, 0), 30, 0.25, 0.5, 0.25, 0.18);
					world.spawnParticle(Particle.SMOKE_LARGE, tpLoc.clone().add(0, 1, 0), 15, 0.25, 0.5, 0.25, 0.04);
					world.spawnParticle(Particle.SPELL_WITCH, tpLoc.clone().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
					world.spawnParticle(Particle.SMOKE_NORMAL, tpLoc.clone().add(0, 1, 0), 50, 0.75, 0.5, 0.75, 0.05);
					world.spawnParticle(Particle.CRIT, tpLoc.clone().add(0, 1, 0), 25, 1, 1, 1, 0.3);

					mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
						new PotionEffect(PotionEffectType.FAST_DIGGING, 5, 19, true, false));

					AbilityUtils.applyStealth(mPlugin, mPlayer, mStealthDuration);

					mTicks = 100;
					if (mRunnable == null || mRunnable.isCancelled()) {
						mRunnable = new BukkitRunnable() {
							@Override
							public void run() {
								world.spawnParticle(Particle.FALLING_DUST, mPlayer.getLocation().clone().add(0, 0.5, 0), 1, 0.35, 0.25, 0.35, Bukkit.createBlockData("gray_concrete"));
								if (mTicks <= 0) {
									mTicks = 0;
									this.cancel();
									mRunnable = null;
								}
								mTicks--;
							}
						};
						mRunnable.runTaskTimer(mPlugin, 0, 1);
					}

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mRunnable != null && (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH)) {
			mTicks = 0;
			mRunnable.cancel();
			mRunnable = null;
			if (enemy instanceof Mob) {
				Mob m = (Mob) enemy;
				if (m.getTarget() == null || !m.getTarget().getUniqueId().equals(mPlayer.getUniqueId())) {
					Location entityLoc = m.getLocation().clone().add(0, 1, 0);

					World world = entityLoc.getWorld();
					world.playSound(entityLoc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 2f);
					world.playSound(entityLoc, Sound.BLOCK_ANVIL_LAND, 0.8f, 2f);
					world.spawnParticle(Particle.FALLING_DUST, entityLoc, 35, 0.35, 0.5, 0.35, Bukkit.createBlockData("gray_concrete"));
					world.spawnParticle(Particle.BLOCK_CRACK, entityLoc, 20, 0.25, 0.25, 0.25, 1, Bukkit.createBlockData("redstone_block"));

					DamageUtils.damage(mPlayer, m, DamageType.MELEE_SKILL, mBonusDmg, mInfo.mLinkedSpell, true);
				}
			}
		}
		return false; // already prevents multiple calls by clearing mRunnable
	}

}
